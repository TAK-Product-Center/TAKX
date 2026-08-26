package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommEventDelegate;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.platform.commservices.ACommIn;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class DatagramSocketCommIn extends ACommIn implements IDatagramSocketCommIn
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int UDP_MTU = 1500;
    public static final int MAX_BUFFER_SIZE = 65535;

    private final int readBufferSize;
    private DatagramSocket socket;

    @Inject
    private ICommEventDelegate commEventDelegate;

    /**
     * Initialize and set read buffer size;
     *
     * @param commService ICommService of owner
     * @param rBufferSize requested size in bytes for the read buffer.
     */
    protected DatagramSocketCommIn(ICommService commService, int rBufferSize)
    {
        super(commService, "DatagramSocketCommIn");
        if (rBufferSize < UDP_MTU)
        {
            readBufferSize = UDP_MTU;
        } else
            readBufferSize = Math.min(rBufferSize, MAX_BUFFER_SIZE);
    }

    @Override
    public void startCommIn(DatagramSocket socket)
    {
        this.socket = socket;
        startThread();  // super-class implementation
    }

    @Override
    public void startCommIn(InputStream input)
    {
    }

    @Override
    public void run()
    {
        byte[] buffer = new byte[readBufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        if (logger.isDebugEnabled()) logger.debug("Set read buffer for incoming DatagramPackets to {}", buffer.length);

        Thread.currentThread().setName(threadName + ": " + pathName);

        try
        {
            while (socket.isBound())
            {
                try
                {
                    socket.receive(packet);
                    buffer = packet.getData();
                    int numberRead = packet.getLength();

                    if (numberRead == -1)
                    {
                        logger.warn("Port {} input stopped unexpectedly while requesting data", pathName);
                        commService.commInterrupted();
                    } else if (numberRead > 0)
                    {
                        if (logger.isTraceEnabled())
                        {
                            logger.trace("Received {} bytes of UDP packet: {}", numberRead, Arrays.toString(buffer));
                        }

                        String source = packet.getAddress().getHostAddress() + ":" + packet.getPort();

                        commEventDelegate.publishCommIn(source);

                        for (ICommListener listener : listeners)
                        {
                            try
                            {
                                byte[] safeArray = new byte[numberRead];
                                System.arraycopy(buffer, 0, safeArray, 0, numberRead);

                                listener.parseBytes(safeArray, numberRead, source);
                            } catch (Exception e)
                            {
                                final Exception toReport = logger.isDebugEnabled() ? e : null;
                                logger.warn("ICommListener: Exception parsing bytes in listener {}", e.getMessage(),
                                        toReport);
                            }
                        }

                        if (commService != null && !commService.isCommPathCaptured())
                        {
                            for (IParser parser : parsers)
                            {
                                try
                                {
                                    byte[] safeArray = new byte[numberRead];
                                    System.arraycopy(buffer, 0, safeArray, 0, numberRead);

                                    parser.parseBytes(safeArray, numberRead, source);
                                } catch (Exception e)
                                {
                                    logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception parsing bytes in gateway parser ", e);
                                }
                            }
                        }
                    }
                } catch (SocketTimeoutException e)
                {
                    if (logger.isTraceEnabled()) logger.trace("UDP socket timed out, waiting");
                    Thread.sleep(100);
                }
            }
        } catch (IOException e)       // Error accessing input stream, notify CommService to stop in and out
        {
            logger.info("Port {} input stopped unexpectedly", pathName);
            if (running) commService.commInterrupted();
        } catch (InterruptedException e)
        {
            // Just exit
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception accessing plugins in DataGramSocketCommIn ", e);
        }
    }
}
