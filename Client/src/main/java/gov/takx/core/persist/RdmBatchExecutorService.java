package gov.takx.core.persist;

import com.google.common.util.concurrent.ForwardingExecutorService;
import gov.takx.api.event.project.ProjectClosing;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An executor service specifically for handling operations from {@link RdmBatchQueue}.
 * We use a discrete executor to prevent a massive data import from clogging up the normal executor service.
 *
 * @since 2.2
 */
@Singleton
public class RdmBatchExecutorService extends ForwardingExecutorService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private RdmBatchExecutor currentExecutor;

    private final AtomicInteger outstandingRDMs = new AtomicInteger(0);

    /**
     * Default CDI constructor.
     */
    RdmBatchExecutorService()
    {
        currentExecutor = new RdmBatchExecutor(); // Instantiate initial executor
    }

    /**
     * Shutdown our current delegate executor and create a new one.
     */
    private void shutdownCurrentExecutor(@SuppressWarnings("UnusedParameters") @Observes ProjectClosing event)
    {
        logger.debug("Shutting down executor and restarting!");

        currentExecutor.shutdown();
        try
        {
            //noinspection ResultOfMethodCallIgnored
            currentExecutor.awaitTermination(5, TimeUnit.SECONDS); // Allow some time to complete operations
        } catch (InterruptedException e)
        {
            logger.debug("Interrupted while awaiting shutdown");
        } finally
        {
            final List<Runnable> runnables = currentExecutor.shutdownNow();// We tried to give them time, so be more forceful
            if (logger.isDebugEnabled()) logger.debug("Dropped tasks: {}", runnables.size());
        }

        currentExecutor = new RdmBatchExecutor(); // And create a new one
    }

    /**
     * @return The number of RDMs that are still waiting to be persisted
     * @since 2.14
     */
    public int getOutstandingRDMCount()
    {
        return outstandingRDMs.get();
    }

    /**
     * Add the given batch size to the outstanding RDM count.
     *
     * @param size Number of RDMs being queued for persistence
     * @since 2.14
     */
    public void addRDMBatch(int size)
    {
        outstandingRDMs.addAndGet(size);
    }

    /**
     * Subtract the given batch size from the outstanding RDM count.
     *
     * @param size Number of RDMs that have been persisted
     * @since 2.14
     */
    public void completeRDMBatch(int size)
    {
        outstandingRDMs.addAndGet(-size);
    }

    @Override
    protected ExecutorService delegate()
    {
        return currentExecutor;
    }

    /**
     * Simple executor using a named thread pool with 1 thread.
     */
    private static final class RdmBatchExecutor extends ThreadPoolExecutor
    {
        private static final BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern("RDM-BATCH-%d")
                .priority(Thread.NORM_PRIORITY + 2) // Bump the priority of the thread
                .build();

        /**
         * Construct with a single thread.
         */
        public RdmBatchExecutor()
        {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t)
        {
            logger.debug("Complete: {}", this);
        }

        public String toString()
        {
            return String.format("RdmBatchExecutor: tc=%d cc=%d", getTaskCount(), getCompletedTaskCount());
        }
    }
}
