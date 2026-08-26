package gov.takx.platform.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommIn;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Suggested base class for {@link ICommIn} implementations.  Actually, it's required.  Otherwise, we
 * will bury you in an anthill and pour honey in your hair.
 * <p>
 * Abstract comm in that runs as a thread separate to the comm service that started it.  Allows
 * registering of {@code ICommListener}s and {@code IParser}s that are injected
 * in.
 *
 * @since 2.0.0
 */
@PseudoBean
public abstract class ACommIn implements ICommIn, Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected ICommService commService;

    // parser listener lists can be accessed by multiple threads.
    protected List<IParser> parsers = new CopyOnWriteArrayList<>();
    protected List<ICommListener> listeners = new CopyOnWriteArrayList<>();
    protected Thread commThread;
    protected InputStream inputStream;
    protected String pathName;
    protected String threadName;

    protected volatile boolean running;

    protected ACommIn(@Nonnull ICommService commService, String threadName)
    {
        if (null == threadName) threadName = getClass().getSimpleName();

        this.commService = commService;
        this.threadName = threadName;

        pathName = KeyUtils.getUniqueKey(commService);
    }

    /**
     * Register a listener to this port.  Listeners, like parsers, will be fed bytes to parse, and will
     * and will additionally be notified if the CommIn port closes.
     *
     * @param listener ICommListener
     */
    @Override
    public void registerCommListener(@Nonnull ICommListener listener)
    {
        logger.debug("registerCommListener: {}", listener);
        listeners.add(listener);
    }

    /**
     * Remove a comm listener
     *
     * @param listener ICommListener
     */
    @Override
    public void removeCommListener(@Nonnull ICommListener listener)
    {
        logger.debug("removeCommListener: {}", listener);
        listeners.remove(listener);
    }

    /**
     * Inject gateway parsers into the list of parsers that will receive bytes to parse
     * from this comm port in.  Parsers are created in response to the comm service creation
     * and are injected into the ICommIn implementation to allow input data to be streamed
     * out for parsing
     *
     * @param parserList list of gateway parsers
     */
    @Override
    public final void setParsers(List<? extends IParser> parserList)
    {
        if (null != parserList)
        {
            for (IParser parser : parserList)
            {
                if (null != parser)
                {
                    parsers.add(parser);
                    if (logger.isDebugEnabled()) logger.debug("{} parser: {}", commService.getName(), parser);
                } else
                {
                    logger.warn("Attempt to add a null IGatewayParser; parser not added to collection");
                }
            }
        }

        // I believe it IS preferable to have the parsers loaded before the port is actually opened, just
        // so we can ensure that no bytes are missed in the case where opening a port causes a device
        // to start sending data right away.
        if (commThread != null)
        {
            commThread.start();
        }
    }

    /**
     * Set the input stream and start this object in a thread to begin reading data from the port.
     * Called by one comm service or another when there is a request to start up the service.
     *
     * @param input input stream
     */
    @Override
    public void startCommIn(InputStream input)
    {
        inputStream = input;
        startThread();
    }

    /**
     * Stop the thread, notify listeners, but don't do all of the cleanup yet, because it may be restarted.
     * The remaining cleanup will be done in {@code shutdownCommIn}.
     *
     * @see #shutdownCommIn
     */
    @Override
    public void stopCommIn(boolean wasExpected)
    {
        if (commThread != null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Stopping {} (was expected: {})", commThread.getName(), wasExpected);
            }

            running = false;
            commThread.interrupt();
            commThread = null;
        } else
        {
            logger.warn("Trying to stop Comm In on a comm thread that is already null for port: {}", pathName);
        }

        for (ICommListener listener : listeners)
        {
            ExceptionUtils.safeDoOperation(listeners, l -> listener.commPathStopped(commService, wasExpected));
        }
    }

    /**
     * Ok, now we are done with this CommIn.  Clean everything up dump the parsers and listeners,
     * after notifying the listeners.
     *
     * @param wasExpected whether it was an expected close
     */
    @Override
    public void shutdownCommIn(boolean wasExpected)
    {
        for (ICommListener listener : listeners)
        {
            ExceptionUtils.safeDoOperation(listeners, l -> listener.commPathClosed(commService, wasExpected));
        }

        listeners.clear();
        parsers.clear();
        commService = null;
    }

    /**
     * Start this thread.
     */
    protected final void startThread()
    {
        logger.debug("Calling startThread() on commIn: {}", threadName);
        running = true;

        if (commThread == null)
        {
            commThread = new Thread(this);
            commThread.setName(threadName);

            // TODO:  Determine if moving the submit to setParsers causes problems for any of the comm services
//            executorService.submit(commThread);
        } else
        {
            logger.warn("Tried to start {}: {} that was already running or not equal to null", threadName, pathName);
        }
    }
}
