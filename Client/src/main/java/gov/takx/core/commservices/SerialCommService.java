package gov.takx.core.commservices;

import com.fazecast.jSerialComm.SerialPort;
import gov.takx.api.commservices.ICommOut;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;
import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

/**
 * Comm service for serial ports (RS-232).
 *
 */
class SerialCommService extends ACommService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SerialPort serialPort;

    /**
     * The service being used to echo data.
     */
    private SerialPortEcho echoService = null;

    /**
     * The port that data is being echoed on (if different from the receive port).
     *
     * @since 1.25.7
     */
    private SerialPort echoSerialPort;

    /**
     * The comm interface used for echoing data (if different from the receive port).
     *
     * @since 1.25.7
     */
    private ICommOut echoCommOut;

    /**
     * Constructor for SerialCommService.
     *
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state of the comm path.  {@code null} can be provided here if no listeners want to
     *                     be registered at this time.  Listeners can be registered later using the
     *                     {@link this#addCommPathListener(ICommListener)} method.
     */
    SerialCommService(ICommListener commListener)
    {
        super(CommServiceType.SERIAL);

        if (commListener != null)
        {
            addCommPathListener(commListener);
        }
    }

    /**
     * Open resources for the serial port.  Service can be started and running without actually opening the
     * port.  Running property in property map will denote whether or not to open the port.
     * Start an echo service if user selected to echo data back out the serial port.
     *
     * @return success of opening resources.
     */
    @Override
    protected boolean openResources()
    {
        name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());
        int baudRate = (Integer) properties.get(CommServiceConstants.BAUD.toString());
        boolean echo = (Boolean) properties.get(CommServiceConstants.ECHO.toString());
        int txTimeout = (Integer) properties.get(CommServiceConstants.TX_TIMEOUT.toString());
        int rxTimeout = (Integer) properties.get(CommServiceConstants.RX_TIMEOUT.toString());

        if ((Boolean) properties.get(CommServiceConstants.RUNNING.toString()))
        {
            try
            {
                serialPort = SerialPort.getCommPort(name);
                serialPort.setBaudRate(baudRate);

                serialPort.openPort();

                if (txTimeout > 0)
                {
                    serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING, rxTimeout, txTimeout);
                } else
                {
                    serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_BLOCKING, rxTimeout, 0);
                }

                InputStream serialInputStream = serialPort.getInputStream();
                OutputStream serialOutputStream = serialPort.getOutputStream();

                commIn = new CommIn(this);
                commIn.startCommIn(serialInputStream);

                commOut = new CommOut(name);
                commOut.startCommOut(serialOutputStream);

                // If we are echoing data, we need to start the echo service.
                if (echo)
                {
                    String echoPortName = (String) properties.get(CommServiceConstants.ECHO_PORT.toString());

                    // If echo is on but no specific port has been specified (or it's the same as the receive port),
                    // echo out the main port.
                    if (echoPortName == null || echoPortName.isEmpty() || echoPortName.equals(name))
                    {
                        echoService = new SerialPortEcho(commOut);
                    }
                    // Otherwise, set up the echo port from the ground up.
                    else
                    {
                        // Keep references to the port, commOut and service so they can be properly shutdown.
                        echoSerialPort = SerialPort.getCommPort(name);
                        echoSerialPort.setBaudRate(baudRate);
                        OutputStream echoSerialOutputStream = echoSerialPort.getOutputStream();
                        echoCommOut = new CommOut(echoPortName);
                        echoCommOut.startCommOut(echoSerialOutputStream);
                        echoService = new SerialPortEcho(echoCommOut);
                    }

                    // Listen to the comm in data so that it can be echoed.
                    commIn.registerCommListener(echoService);
                }
            } catch (Exception e)
            {
                logger.error("Error starting Serial Port {} {}", name, e);
                return false;
            }
        }

        logger.info("Registering serial port {}, baud = {}", name, baudRate);

        return true;
    }

    /**
     * Returns whether or not the serial port is open (not the same as whether or not the service is running).
     */
    @Override
    protected boolean isCommServiceOpen()
    {
        boolean isOpen = false;

        Object val = properties.get(CommServiceConstants.RUNNING.toString());
        if (null != val)
        {
            isOpen = (Boolean) val;
        } else
        {
            logger.error("Call to isCommServiceOpen() found no RUNNING property in the map.  Defaulting to 'false' state");
        }

        return isOpen;
    }

    /**
     * Override of stop comm service, mainly to override the setting of the properties to {@code null}.  For
     * comm ports, we want to just set the status of the running property to false.
     *
     * @param wasExpected boolean whether or not the stoppage was expected (true) or on error (false)
     */
    @Override
    public void stopCommService(boolean wasExpected)
    {
        isRunning = false;
        //
        // NOTE:    Threading issues on shutdown may cause commIn to go null rapidly
        //          therefore we need to individually verify null status on these commands
        //

        if (properties != null) properties.put(CommServiceConstants.RUNNING.toString(), false);

        if (commIn != null)
        {
            if (echoService != null) commIn.removeCommListener(echoService);
            commIn.stopCommIn(wasExpected);
            commIn.shutdownCommIn(wasExpected);
        }

        commIn = null;

        closeResources(wasExpected);

        if (commOut != null)
        {
            commOut.stopCommOut();
            commOut = null;
        }

        if (echoCommOut != null)
        {
            echoCommOut.stopCommOut();
            commOut = null;
        }

        if (echoService != null) echoService = null;

        if (logger.isInfoEnabled()) logger.info("StopCommService for: {}", getName());
    }

    @Override
    public void cleanup()
    {
        properties = null;
    }

    /**
     * close the serial port, if it was open.
     *
     * @param wasExpected boolean whether or not the close was expected.
     */
    @Override
    protected void closeResources(boolean wasExpected)
    {
        if (serialPort != null)
        {
            serialPort.closePort();
            serialPort = null;
        }

        if (echoSerialPort != null)
        {
            echoSerialPort.closePort();
            echoSerialPort = null;
        }
    }

    private static boolean isRealSerialPort(String sourceName)
    {
        return !SystemUtils.IS_OS_MAC || isMacSerialPort(sourceName);
    }

    /**
     * Checks a mac style port name (i.e. /dev/tty.xxxxxx) to see if it is a unique comm port
     * that can be opened.  For now, bluetooth and modems are considered not to be comm ports.
     * Also, the keyspan adapters will create an entry called KeySerial corresponding to the
     * first comm port initialized by the os.  It is a duplicate, and should be ignored.  There
     * may be other to add as well.
     *
     * @param sourceName name of mac os style port (i.e. /dev/tty.xxxxxx)
     * @return boolean value pertaining to if the port name is valid and unique as a serial port.
     */
    private static boolean isMacSerialPort(String sourceName)
    {
        return !sourceName.startsWith("cu.") && !sourceName.contains("Bluetooth") &&
                !sourceName.contains("modem") && !sourceName.contains("KeySerial");
    }

    /**
     * @return A list of the port names available on the system
     */
    public static List<String> getPortList()
    {
        final SerialPort[] commPorts = SerialPort.getCommPorts();

        return Arrays.stream(commPorts)
                .map(SerialPort::getSystemPortName)
                .filter(SerialCommService::isRealSerialPort)
                .collect(Collectors.toList());
    }
}
