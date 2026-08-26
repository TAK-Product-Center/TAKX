package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.IPortAnalyzerDelegate;
import gov.takx.api.commservices.IPortAnalyzerListener;
import gov.takx.api.commservices.IPortAnalyzerParser;
import gov.takx.api.commservices.ISerialPortAnalyzer;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.timer.ITimerService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Serial port scanner.  Created through the ConnectionCommServiceDelegate when a plugin requests the scan
 * of serial ports for possible device connections to currently inactive serial ports.  Gets a list of
 * parsers that want to participate in the scan and gives them an adequate amount of time to parse
 * a message and validate that there is indeed a device with an understood protocol physically connected to a
 * serial device.  Currently only checks 4800 and 115200 baud rates to improve scanning time (so far, only
 * devices of those 2 baud rates have implemented the IPortAnalyzerParser interface).
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SerialPortScanner implements IPortAnalyzerDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    private ITimerService timerService;

    private final int baudRate;
    private final int timeoutSeconds;
    private int nameIndex;
    private boolean scanHalted;

    private final Map<String, SerialPortScannerThread> threadMap = new HashMap<>();
    private final List<IPortAnalyzerParser> parsers = new CopyOnWriteArrayList<>();

    private List<String> names;

    private IPortAnalyzerListener listener;
    private TimerTask scanningTimerTask;

    public SerialPortScanner(IPortAnalyzerListener listener, int baudRate, int timeoutSeconds)
    {
        logger.debug("*** NEW Serial Port Scanner ***");
        this.listener = listener;
        this.baudRate = baudRate;
        this.timeoutSeconds = timeoutSeconds;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Do an initial load of all serial port analyzers.
        devicePluginRegistry.getPluginDefinitions().forEach(this::processPluginDefinition);
    }

    /**
     * Inspect a given {@code pluginDefinition}, and, if it is an {@link ISerialPortAnalyzer}, prepare its parser.
     * @since 5.7
     */
    private void processPluginDefinition(IPluginDefinition<?> pluginDefinition)
    {
        if (pluginDefinition.getPluginCategory() == PluginCategory.DevicePlugin
                && ISerialPortAnalyzer.class.isAssignableFrom(pluginDefinition.getPluginClass())
                && pluginDefinition.getInstance() instanceof ISerialPortAnalyzer analyzer)
        {
            logger.debug("Found serial port analyzer {}", pluginDefinition);

            final var parser = analyzer.getAnalyzingParser();
            if (parser != null)
            {
                parser.setDelegate(this);
                parsers.add(parser);
            }
        }
    }

    /**
     * Single interface method for plugins to notify the scanner that data were validated by a plugin.
     *
     * @param plugin plugin name validating the data
     */
    @Override
    public void validateData(String plugin)
    {
        if (listener == null)   // listener will be null after scanning is complete.
        {
            logger.warn("validateData called from plugin {} after scanning cycle was completed.  Ignoring.", plugin);
        } else if (nameIndex >= names.size())   // sanity check
        {
            logger.error("Error.  validateData called with nameIndex = {} and only {} names exist", nameIndex, names.size());
        } else
        {
            setPortStatus(names.get(nameIndex), baudRate, plugin, SerialPortScanResult.VALID_DATA);
        }
    }

    /**
     * Return the list of parsers that implement IPortAnalyzerParser, as per written into the plugins
     *
     * @return list of parsers
     */
    protected List<IPortAnalyzerParser> getParsers()
    {
        return Collections.unmodifiableList(parsers);
    }

    protected void haltScan()
    {
        if (!scanHalted)
        {
            stopScanningTimer();
            scanHalted = true;
            threadMap.values().forEach(thread -> thread.stopThread(false));
            threadMap.clear();
            clearParsers();
            listener = null;
        }
    }

    /**
     * Start the scan
     */
    protected void start()
    {
        if (scanHalted) return; // Killed before we started

        if (parsers.isEmpty())
        {
            exitWithError("No plugins support serial port scanning");
        } else
        {
            names = SerialCommService.getPortList();
            if (logger.isDebugEnabled()) logger.debug("Got a list of {} ports: {}", names.size(), String.join(", ", names));

            if (names.isEmpty())
            {
                exitWithError("No Available Serial Ports Detected");
            } else
            {
                listener.onSetPortStatus(names.get(0), baudRate, "Scanning...", false);
                startScannerThread(names.get(0), baudRate);
                if (logger.isDebugEnabled()) logger.debug("Started scanner thread for {}", names.get(0));
            }
        }
    }

    /**
     * Set the port status and notify listeners.  Check to see if there are additional ports to scan.
     *
     * @param name       name of the port
     * @param baud       port baud rate
     * @param plugin     name of the plugin
     * @param scanResult scan result of the port at the specified baud rate
     */
    protected void setPortStatus(String name, int baud, String plugin, SerialPortScanResult scanResult)
    {
        if (scanHalted)
        {
            logger.debug("setPortStatus for {} called when scan was halted", name);
            return;
        }
        logger.debug("setPortStatus {}, {}, {}, {}", name, baud, plugin, scanResult);
        if (scanResult == SerialPortScanResult.VALID_DATA)
        {
            stopScanningTimer();
            stopScannerThread(name, baud, true);
            listener.onSetPortStatus(name, baud, plugin, true);
            checkNextPort();
        } else if (scanResult == SerialPortScanResult.INVALID_DATA)
        {
            stopScanningTimer();
            listener.onSetPortStatus(name, baud, "No Valid Data", false);
            checkNextPort();
        } else if (scanResult == SerialPortScanResult.UNAVAILABLE)
        {
            stopScanningTimer();
            stopScannerThread(name, baud, false);
            listener.onSetPortStatus(names.get(nameIndex), baud, "Unavailable", false);
            checkNextPort();
        }
    }

    /**
     * Check to see if we have another comm port to scan.  If so, start the scanning thread.  Otherwise, clear
     * all the parsers and notify the listener we are done.
     */
    private void checkNextPort()
    {
        nameIndex++;
        if (nameIndex == names.size())
        {
            clearParsers();
            listener.onPortAnalyzerDone();
            listener = null;
            logger.debug("Serial port analyzer done");
        } else
        {
            final String name = names.get(nameIndex);
            listener.onSetPortStatus(name, baudRate, "Scanning", false);
            startScannerThread(name, baudRate);
            logger.debug("Started scanner thread for {}", name);
        }
    }

    /**
     * Clear the delegates from the parsers so this class will be GC'd, and parsers will not be able to call methods
     * on this delegate after the scanning is over.
     */
    private void clearParsers()
    {
        parsers.forEach(parser -> parser.setDelegate(null));
        parsers.clear();
    }

    /**
     * Start a scanner thread for a specific serial port by port name and baud
     *
     * @param name port name (i.e. COM3)
     * @param baud baud rate
     */
    private void startScannerThread(String name, int baud)
    {
        SerialPortScannerThread scannerThread = new SerialPortScannerThread(name, baud, this);
        threadMap.put(name + baud, scannerThread);
        startScanningTimer(name, baud, timeoutSeconds);
        scannerThread.startThread();
    }

    /**
     * Halt a scanner thread by port name and baud.
     *
     * @param name port name (i.e. COM3)
     * @param baud baud rate
     */
    private void stopScannerThread(String name, int baud, boolean valid)
    {
        SerialPortScannerThread thread = threadMap.remove(name + baud);
        if (thread != null)
        {
            thread.stopThread(valid);
        } else
        {
            logger.debug("Odd.... we tried to stop a scanner thread that was not in the map:  {}", name);
        }
    }

    /**
     * Stop all scanning on port error.  Notify listener.
     *
     * @param error String error.
     */
    private void exitWithError(String error)
    {
        listener.onPortAnalyzerError(error);
        listener = null;
        clearParsers();
    }

    /**
     * Start a timeout timer, which will cause a scan of a port at a specified baud rate to stop with the assumption
     * that there are no valid data.
     *
     * @param name    port name
     * @param baud    baud rate
     * @param seconds timeout in seconds
     */
    private void startScanningTimer(String name, int baud, int seconds)
    {
        scanningTimerTask = timerService.createTask("Port Scanning Timer " + name + "," + baud, new ScanningTimerTask(name, baud), TimeUnit.SECONDS.toMillis(seconds));
    }

    /**
     * Stop the scanning task
     */
    private void stopScanningTimer()
    {
        if (scanningTimerTask != null) scanningTimerTask.cancel();
        scanningTimerTask = null;
    }

    /**
     * Task to be executed on a timer to stop scanning a port at a specified baud rate
     */
    private class ScanningTimerTask extends TimerTask
    {
        private final String name;
        private final int baud;

        public ScanningTimerTask(String name, int baud)
        {
            this.name = name;
            this.baud = baud;
        }

        @Override
        public void run()
        {
            logger.debug("Timeout scanning port {}, {}", name, baud);
            stopScannerThread(name, baud, false);
        }
    }
}
