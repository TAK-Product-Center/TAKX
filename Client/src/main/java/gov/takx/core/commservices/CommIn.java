package gov.takx.core.commservices;

import com.fazecast.jSerialComm.SerialPortTimeoutException;
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
import java.lang.invoke.MethodHandles;

/**
 * CommIn object which runs as a separate thread from the {@code ICommService} that  created it. The run method is the
 * bread and butter, which sits in a loop blocking on a read until data are available.  After reading data, the data are
 * passed first to the registered {@code ICommListener} implementations.  Then, if the comm path has not been requested
 * to be "captured" through the {@code ICommService}, the data are passed to the IGatewayParser implementations that
 * were injected into this CommIn.
 *
 * @see ACommIn#registerCommListener(ICommListener)
 * @see ACommIn#setParsers(java.util.List)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class CommIn extends ACommIn
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int inputBufferSize = 1024;

    @Inject
    private ICommEventDelegate commEventDelegate;

    /**
     * Initialize and store the path name by concatenating the type ("ETHERNET", "SERIAL", etc)
     * with the port/service name ("127.0.0.1:1024", "COM1", etc.)
     *
     * @param commService ICommService which created this comm in
     */
    CommIn(ICommService commService)
    {
        super(commService, "CommIn");
    }

    /**
     * Set the input buffer size and the max number of bytes that are read on each TCP/IP read operation.
     * Anything less than zero or greater than the max assumes we want to use the default.
     *
     * @param inputBufferSize size of input buffer.
     */
    void setInputBufferSize(int inputBufferSize)
    {
        if (inputBufferSize < 0 || inputBufferSize > 65535)
        {
            this.inputBufferSize = 1024;
        } else
        {
            this.inputBufferSize = inputBufferSize;
        }
    }

    /**
     * Loop until interrupted or error accessing input stream.  In the meantime, feed incoming data
     * to all registered {@code ICommListener} and {@code IGatewayParser} implementations.
     * Max buffer size per read is controlled by {@code INPUT_BUFFER_SIZE}.
     */
    @Override
    public void run()
    {
        byte[] byteArray = new byte[inputBufferSize];  // buffer for reading data in the port.
        byte[] safeArray = new byte[inputBufferSize];  // buffer for holding a copy of data read

        Thread.currentThread().setName(threadName + ": " + pathName);

        try
        {
            while (running)
            {
                logger.trace("Port: {} blocking", pathName);

                int numberRead = inputStream.read(byteArray, 0, inputBufferSize);

                if (numberRead == -1)
                {
                    logger.info("Port {} input stopped unexpectedly while requesting data", pathName);
                    if (running) commService.commInterrupted();
                } else if (numberRead > 0)
                {
                    logger.trace("Port: {} read {} bytes", pathName, numberRead);

                    commEventDelegate.publishCommIn(pathName);

                    // Listeners get a chance to parse the data first.
                    for (ICommListener listener : listeners)
                    {
                        try
                        {
                            System.arraycopy(byteArray, 0, safeArray, 0, inputBufferSize);
                            listener.parseBytes(safeArray, numberRead, null);
                        } catch (Exception e)
                        {
                            logger.warn("ICommListener: Exception parsing bytes in listener.", e);
                        }
                    }

                    // commService is accessible by ACommIn on another thread.
                    if (commService != null && !commService.isCommPathCaptured())
                    {
                        // Next, the parsers get a chance to parse the data
                        for (IParser parser : parsers)
                        {
                            try
                            {
                                // Always prepare a "fresh" copy of the data we read so we aren't at risk
                                // that the first parser corrupts the array by changing the values in it,
                                // which would be detrimental to the parsers down the line.
                                //  This array copy ensures that EACH plugin is given their own unique copy of the array.
                                //
                                System.arraycopy(byteArray, 0, safeArray, 0, inputBufferSize);
                                parser.parseBytes(safeArray, numberRead, null);
                            } catch (Exception e)
                            {
                                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception parsing bytes in gateway parser ", e);
                            }
                        }
                    }
                }
            }
        } catch (SerialPortTimeoutException e) // Port timed out before valid data was received.
        {
            final String msg = "Port " + pathName + " input timed out while blocking on input: " + e.getMessage();
            logger.warn(msg, e);

            // Tell the comm service CommIn was interrupted.
            if (running) commService.commInterrupted();
        } catch (IOException e) // Port having connection issues.
        {
            final String msg = "Port " + pathName + " input stopped unexpectedly while blocking on input: " + e.getMessage();
            logger.info(msg, logger.isDebugEnabled() ? e : null);

            // Tell the comm service CommIn was interrupted.
            if (running) commService.commInterrupted();
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception accessing plugins in CommIn ", e);
        }
    }
}
