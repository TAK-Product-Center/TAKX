package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommEventDelegate;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.commons.utils.ThrottledInputStream;
import gov.takx.platform.commservices.ACommIn;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ConcurrentModificationException;

/**
 * Comm port data in. Runs in separate thread reading data from generic input stream and
 * feeds to the parser services that it gets from a service tracker.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class FileCommIn extends ACommIn
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int BLOCK_SIZE = 1024;

    private final String filePath;

    @Inject
    private ICommEventDelegate commEventDelegate;

    /**
     * Initialize and store the path name by concatenating the type ("ETHERNET", "SERIAL", etc.)
     * with the port/service name ("127.0.0.1:1024", "COM1", etc.)
     *
     * @param commService ICommService which created this comm in
     * @param filePath    Path to file being processed
     */
    protected FileCommIn(ICommService commService, String filePath)
    {
        super(commService, "FileCommIn");
        this.filePath = filePath;
    }

    protected void restart()
    {
        if (commThread != null) commThread.start();
    }

    /**
     * Loop until interrupted or error accessing input stream
     */
    @Override
    public void run()
    {
        Thread.currentThread().setName(threadName + ": " + pathName);

        boolean useBlockSize = true;
        if (inputStream instanceof ThrottledInputStream)
        {
            logger.debug("Using ThrottledInputStream to control parsing speed");

            useBlockSize = false;
        } else
        {
            logger.debug("Using BLOCK_SIZE to maximize performance");
        }

        int ch = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BLOCK_SIZE);

        boolean isCommPathOpen = !commService.isCommPathCaptured();

        try
        {
            long startMillis = System.currentTimeMillis();

            while (running)
            {
                baos.reset();

                //
                // NOTE:    Order in the while check matters!  If the inputStream is read
                //          and then the size is checked, 1 character will be lost per iteration
                //          and on a large file, that could equate to a lot of lost data.  DO NOT
                //          change the while sequence below!
                //
                if (useBlockSize)
                {
                    //
                    // Performance tweak- since we are not throttling the input stream, read BLOCK_SIZE at a time
                    while (baos.size() < BLOCK_SIZE && (ch = inputStream.read()) != -1)
                    {
                        baos.write(ch);
                    }
                } else
                {
                    //
                    // We are at the mercy of the user- they defined a throttled input stream based on a timed cycle
                    while ((ch = inputStream.read()) >= 0)
                    {
                        logger.debug("Reading...");

                        baos.write(ch);
                    }
                }
                if (baos.size() > 0)
                {
                    logger.debug("Notifying listeners...");

                    final byte[] baosData = baos.toByteArray();
                    final int baosSize = baosData.length;

                    commEventDelegate.publishCommIn(pathName);

                    for (ICommListener listener : listeners)
                    {
                        if (!running) break;

                        try
                        {
                            logger.trace("Sending {} bytes...", baosSize);

                            byte[] safeArray = new byte[baosSize];
                            System.arraycopy(baosData, 0, safeArray, 0, baosSize);

                            listener.parseBytes(safeArray, baosSize, filePath);
                        } catch (Exception e)
                        {
                            logger.warn("ICommListener: Exception parsing bytes in listener- {}", e.getMessage());
                            logger.debug("Stack trace", e);
                        }
                    }

                    if (running && isCommPathOpen)
                    {
                        try
                        {
                            for (IParser parser : parsers)
                            {
//                                logger.warn("Send bytes to parser, running=" + running + ": " + parser);
                                try
                                {
                                    byte[] safeArray = new byte[baosSize];
                                    System.arraycopy(baosData, 0, safeArray, 0, baosSize);

                                    if (!running) break;

                                    parser.parseBytes(safeArray, baosSize, filePath);
                                } catch (Exception e)
                                {
                                    logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception parsing bytes in gateway parser ", e);
                                }
                            }
                        } catch (ConcurrentModificationException e)
                        {
                            //
                            // When the user manually intervenes and terminates a
                            // continuous replay file stream, a concurrent mod exception
                            // may be thrown.  This exception can be safely ignored since
                            // disconnection is underway.
                            //
                        }
                    }
                }

                if (-1 == ch)
                {
                    if (logger.isTraceEnabled())
                    {
                        long ttlMillis = System.currentTimeMillis() - startMillis;
                        logger.trace("Total millis: {}", ttlMillis);
                    }

                    try
                    {
                        inputStream.close();

                        logger.debug("{} was properly closed.", filePath);
                    } catch (Exception e)
                    {
                        logger.warn("Unable to properly close input file: {}", e.getMessage());
                    }

                    //
                    // File has been fully read
                    logger.info("Completed reading of file {}: stopping comm in...", filePath);
                    stopCommIn(true);
                }
            }
        } catch (IOException e)       // Comm service was closed, usb virtual comm port removed, etc.
        {
            logger.debug("Exception while reading file", e);

            logger.info("Port {} input stopped unexpectedly while blocking on input", pathName);
            // Tell the comm service CommIn was
            if (running) commService.commInterrupted();
        }
    }
}
