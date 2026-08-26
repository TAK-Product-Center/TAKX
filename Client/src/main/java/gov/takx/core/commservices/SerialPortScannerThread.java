package gov.takx.core.commservices;

import com.fazecast.jSerialComm.SerialPort;
import gov.takx.api.commservices.IPortAnalyzerParser;
import gov.takx.api.plugin.PluginConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;

import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING;
import static com.fazecast.jSerialComm.SerialPort.TIMEOUT_WRITE_BLOCKING;

/**
 * Runnable (as thread) to open a serial port and pass any data that are read to parsers participating in the
 * serial port scanning.
 *
 */
public class SerialPortScannerThread implements Runnable
{
    private final String portName;
    private volatile boolean running = false;
    private volatile boolean valid = false;
    private Thread enumeratorThread;
    private final SerialPortScanner scanner;
    private final int baudRate;
    private SerialPort serialPort = null;

    private static final int INPUT_BUFFER_SIZE = 1024;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Constructor for the scanning thread
     *
     * @param name    port name
     * @param baud    baud rate
     * @param scanner scanner, for callback notifications
     */
    public SerialPortScannerThread(String name, int baud, SerialPortScanner scanner)
    {
        portName = name;
        this.scanner = scanner;
        baudRate = baud;
    }

    /**
     * Start a thread with this class as the Runnable
     */
    protected void startThread()
    {
        running = true;

        if (enumeratorThread == null)
        {
            enumeratorThread = new Thread(this);
            enumeratorThread.setName("Serial Port Enumerator Thread for " + portName + "," + baudRate);
            enumeratorThread.start();
            logger.debug("Starting scanner thread on {}, {}", portName, baudRate);
        } else
        {
            logger.warn("Tried to start SerialPortScanner Thread: {} that was already running or not equal to null", portName);
        }
    }

    /**
     * Stop the thread.
     */
    protected synchronized void stopThread(boolean valid)
    {
        this.valid = valid;
        if (enumeratorThread != null)
        {
            logger.debug("Stopping scanner thread on {}, {}", portName, baudRate);

            if (valid) closePort(serialPort);
            running = false;
            enumeratorThread.interrupt();
            enumeratorThread = null;
        } else
        {
            logger.warn("Trying to stop SerialPortScanner thread that is already null for port: {}", portName);
        }
    }

    /**
     * Return the running status of the thread.
     *
     * @return true if running, false otherwise
     */
    protected boolean isRunning()
    {
        return running;
    }

    /**
     * Main run() method of Runnable.  Open the serial port and listen for data.  Pass any data on to parsers, and
     * notify the scanner of any errors.
     */
    @Override
    public void run()
    {
        long startTime = System.currentTimeMillis();
        byte[] byteArray = new byte[INPUT_BUFFER_SIZE];  // buffer for reading data in the port.
        boolean sendStatus = true;

        try
        {
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setComPortTimeouts(TIMEOUT_READ_SEMI_BLOCKING | TIMEOUT_WRITE_BLOCKING,
                    1_000, 0);

            final InputStream inputStream = serialPort.getInputStream();

            while (running)
            {
                int numberRead = inputStream.read(byteArray, 0, INPUT_BUFFER_SIZE);

                if (numberRead > 0)
                {
                    logger.debug("Read {} bytes from {}", numberRead, portName);
                }

                if (numberRead == -1)
                {
                    logger.info("Port {} input stopped unexpectedly while requesting data", portName);
                    closePort(serialPort);
                    serialPort = null;
                    if (!valid)
                    {
                        scanner.setPortStatus(portName, baudRate, "interrupted", SerialPortScanResult.INVALID_DATA);
                        sendStatus = false;
                    }
                    running = false;
                } else if (numberRead > 0 && running)
                {
                    for (IPortAnalyzerParser parser : scanner.getParsers())
                    {
                        try
                        {
                            byte[] safeArray = new byte[INPUT_BUFFER_SIZE];
                            System.arraycopy(byteArray, 0, safeArray, 0, INPUT_BUFFER_SIZE);
                            parser.parseBytes(safeArray, numberRead);
                        } catch (Exception e)
                        {
                            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception parsing bytes in analyzer parser ", e);
                        }
                    }
                } else if (!running && !valid)
                {
                    scanner.setPortStatus(portName, baudRate, "interrupted", SerialPortScanResult.INVALID_DATA);
                    sendStatus = false;
                }
            }
        } catch (IOException e)
        {
            logger.debug("IOException on port {}, valid = {}", portName, valid);
            if (!valid)
            {
                scanner.setPortStatus(portName, baudRate, "Not available", SerialPortScanResult.UNAVAILABLE);
                sendStatus = false;
            }
        }

        closePort(serialPort);
        if (sendStatus && !valid)
        {
            scanner.setPortStatus(portName, baudRate, "interrupted", SerialPortScanResult.INVALID_DATA);
        }

        logger.debug("Thread exiting scan run method for port {} need to send status = {}", portName, sendStatus);

        if (logger.isDebugEnabled())
        {
            logger.debug("Run time of {} is {}ms", portName, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Close the serial port being scanned.
     *
     * @param port name of the serial port.
     */
    private void closePort(SerialPort port)
    {
        if (port != null)
        {
            if (port.isOpen())
            {
                logger.debug("Close serial port {}, {}", portName, baudRate);
                port.closePort();
            }
        }
    }
}
