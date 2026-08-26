package gov.takx.platform.utils;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.timer.ITimerService;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A batching queue that has an initial delay and a max delay.  When an item is added to the batch, a timer
 * of {@code initialDelay} is kicked off.  If we receive another addition before the timer fires, we
 * reset the timer.  When the timer fires, we process all the collected elements in the batch.  The {@code maxDelay}
 * places an upper bound on how long we will allow the timer to be postponed.  This ensures that a continuous
 * stream of adds will eventually get processed at roughly {@code maxDelay} intervals.
 * <p>
 * This is an abstract class, implementors must provide the {@link #runBatch(Collection)} method to perform
 * the actual work of processing the batch.
 * <p>
 * Batches generated from this queue are submitted for execution using an executor service.  This is done to
 * minimize the overhead placed on the Timer execution thread (we don't want the actual batch work taking place
 * on the timer thread).  By default, the batch work will still be done sequentially (i.e., only a single
 * batch will ever be submitted to the executor service at a time).  If you want to allow potential parallel
 * execution of the batches, then call {@link #setRunBatchesInParallel(boolean)} with a value of {@code true}.
 * <p>
 * @param <C> is the type of Collection used to hold entries of type T<br>
 * @param <T> is the type of object (entries in C) managed by this queue
 *
 * @since 1.46
 */
@PseudoBean
public abstract class TimeBatchedQueue<C extends Collection<T>, T>
{
    // Using a typical static logger here can lead to deadlocks since there are loggers
    // that use TimeBatchedQueue instances to control output frequency.
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;

    protected final String workName;
    protected final long initialDelay;
    protected final long maxDelay;
    protected final int maxSize;
    protected C batch;
    protected final Supplier<C> batchSupplier;
    protected TimerTask batchTask;
    protected long batchStart;
    protected long lastEntryTime;
    protected long lastBatchTest;

    private boolean runBatchesInParallel;
    private volatile boolean executing;
    private final Queue<C> pendingBatches = new ArrayDeque<>();

    private final ReentrantLock batchLock = new ReentrantLock();

    @Inject
    protected ITimerService batchTimer;

    @Inject
    protected ICoreExecutorService executorService;

    /**
     * Construct a batched queue using the given batch container supplier.
     * <p>
     * Commonly, the {@code batchSupplier} is just a method reference to a collection constructor,
     * like {@code ArrayList<Foo>::new}.
     *
     * @param workName      The name to give to the task performing the batch of work
     * @param initialDelay  The initial amount of time to wait for additional work
     * @param maxDelay      The max time during which we'll collect work
     * @param batchSupplier The supplier to generate new batch collections
     */
    protected TimeBatchedQueue(String workName, long initialDelay, long maxDelay, Supplier<C> batchSupplier)
    {
        this(workName, initialDelay, maxDelay, Integer.MAX_VALUE, batchSupplier);
    }

    /**
     * Construct a size-limited time batched queue using the given batch container supplier.
     * <p>
     * Commonly, the {@code batchSupplier} is just a method reference to a collection constructor,
     * like {@code ArrayList<Foo>::new}.
     *
     * @param workName      The name to give to the task performing the batch of work
     * @param initialDelay  The initial amount of time to wait for additional work
     * @param maxDelay      The max time during which we'll collect work
     * @param maxSize       The maximum number of elements allowed in the queue, reaching this limit forces the
     *                      batch to be processed regardless of current timeouts
     * @param batchSupplier The supplier to generate new batch collections
     * @since 2.3
     */
    protected TimeBatchedQueue(String workName, long initialDelay, long maxDelay, int maxSize, Supplier<C> batchSupplier)
    {
        logger = LoggerFactory.getLogger(TimeBatchedQueue.class.getName() + "." + workName.replace(' ', '_'));

        this.workName = workName;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.maxSize = maxSize;
        this.batchSupplier = batchSupplier;

        batch = batchSupplier.get();
    }

    /**
     * @return Should work batches be executed in parallel
     * @since 2.29
     */
    public boolean isRunBatchesInParallel()
    {
        return runBatchesInParallel;
    }

    /**
     * Specify if this queue should process work sequentially or in parallel.
     * <p>
     * If {@code true}, then work batches are submitted to an executor service, which could potentially
     * allow multiple batches to be executing in parallel.
     * <p>
     * If {@code false}, work batches will be executed sequentially. Tasks are still executed using an executor
     * service in order to avoid starvation on the timer thread that causes a task to be executed.
     *
     * @param parallel {@code false} to execute batches sequentially, {@code true} to allow parallel execution
     * @since 2.29
     */
    public void setRunBatchesInParallel(boolean parallel)
    {
        runBatchesInParallel = parallel;
    }

    /**
     * Base method defaults to using the core executor service.
     *
     * @return The executor service to use for batch processing
     * @since 2.2
     */
    protected ExecutorService getExecutorService()
    {
        return executorService;
    }

    /**
     * Lock access to the current batch.  No new additions can be made and the batch can not be processed while this
     * lock is held.  It is critical that you always call {@link #unlockBatch()} after a call to this method.
     *
     * @see #unlockBatch()
     * @since 1.48 (return changed to void in 5.4)
     */
    protected void lockBatch()
    {
        batchLock.lock();
    }

    /**
     * Unlock the batch.  This will allow new batch additions and batch processing to resume.
     *
     * @see #lockBatch()
     * @since 1.48 (method signature changed in 5.4)
     */
    protected void unlockBatch()
    {
        batchLock.unlock();
    }

    /**
     * Get the batch collection.  This method returns the raw collection, so take care not to modify it by mistake.
     * Also, the batch lock should be held while you work with the batch, or bad things may occur.
     *
     * @return The current batch
     * @see #lockBatch()
     * @since 1.48
     */
    protected C getBatch()
    {
        return batch;
    }

    /**
     * Adds an entry to the batch as a result of the method call {@link TimeBatchedQueue#add(Object)}.
     * <p>
     * Override this method to add an entry to the batch in a custom manner when the method
     * {@link TimeBatchedQueue#add(Object)} is invoked.  For instance, an overriding class might want to remove a
     * specific entry from the batch before adding the new one.  This method call is made under a batch lock to
     * guarantee thread safe updates to the batch.
     *
     * @param batch the batch
     * @param entry the entry to add to the batch collection
     * @see #add(Object)
     * @since 2.33
     */
    protected void addToCollection(C batch, T entry)
    {
        batch.add(entry);
    }

    /**
     * Adds a collection of entries to the batch as a result of the method call
     * {@link TimeBatchedQueue#addAll(Collection)}.
     * <p>
     * Override this method to add a collection of entries to the batch in a custom manner when the method
     * {@link TimeBatchedQueue#addAll(Collection)} is invoked.  For instance, an overriding class might want to remove
     * some of the entries from the batch prior to adding all or some of the new entries.  This method call is made
     * under a batch lock to guarantee thread safe updates to the batch.
     * <p>
     *
     * @param batch   the batch
     * @param entries the entry to add to the batch collection
     * @see #addAll(Collection)
     * @since 2.33
     */
    protected void addAllToCollection(C batch, Collection<T> entries)
    {
        batch.addAll(entries);
    }

    /**
     * Add an entry to the batch.  Manage the timer task that will execute the batch in the future.
     *
     * @param entry Entry to add to batch
     */
    public void add(T entry)
    {
        lockBatch();

        try
        {
            addToCollection(batch, entry);
            updateAfterAdd();
        } finally
        {
            unlockBatch();
        }
    }

    /**
     * Add a collection of entries to the batch.  Manage the timer task that will execute the batch in the future.
     *
     * @param entries Entries to add to batch
     */
    public void addAll(Collection<T> entries)
    {
        lockBatch();

        try
        {
            addAllToCollection(batch, entries);
            updateAfterAdd();
        } finally
        {
            unlockBatch();
        }
    }

    /**
     * Update our state following the addition of elements to the batch queue.  This may cause the batch to be
     * processed if our maximum size has been reached.
     */
    private void updateAfterAdd()
    {
        // If we've hit the configured maximum batch size, then fire the batch off now
        if (batch.size() >= maxSize)
        {
            cancelBatchTask();
            fireRunBatch(false);
        } else
        {
            updateBatchTimes();
        }
    }

    /**
     * Initiate the processing of the batch, either immediately on the calling thread, or using the
     * configured executor service.
     *
     * @param immediate Pass {@code true} to perform the batch handling on the current thread
     */
    private void fireRunBatch(boolean immediate)
    {
        if (batch.isEmpty()) return; // No work to do

        final C work = batch;
        onPendingBatch(work);
        batch = batchSupplier.get();

        if (immediate)
        {
            internalRunBatch(work);
        } else
        {
            queueBatch(work);
        }
    }

    /**
     * Internal version of runBatch that also makes a callback to the onBatchDone callback allowing override logic
     * to perform any cleanup on the batch after it is run.  Default implementation is empty.
     *
     * @param work the batch to run
     * @since 2.18
     */
    protected void internalRunBatch(C work)
    {
        runBatch(work);
        onBatchDone(work);
    }

    /**
     * Notification a batch has been pulled out for pending work.  Override to perform any custom logic associated
     * with this batch.
     *
     * @param work pending batch
     * @since 2.18
     */
    protected void onPendingBatch(C work)
    {
    }

    /**
     * Notification work has been done on a batch.  Override to perform any custom logic pertaining to clean up.
     *
     * @param work the completed batch work
     * @since 2.18
     */
    protected void onBatchDone(C work)
    {
    }

    /**
     * Add the work batch to the executor for processing.
     *
     * @param work The batch of work to queue
     * @since 2.14
     */
    protected void queueBatch(C work)
    {
        if (isRunBatchesInParallel())
        {
            getExecutorService().execute(new BatchRunner(work));
        } else
        {
            queueBatchSequentially(work);
        }
    }

    /**
     * Queue pending work in a sequential manner.  Ensure that we never have more than one work batch submitted to our
     * executor service.
     *
     * @param work Batch to queue
     */
    protected synchronized void queueBatchSequentially(C work)
    {
        // If we are not already executing a batch, then we can dispatch it to the executor

        if (!executing)
        {
            executing = true;

            getExecutorService().execute(new BatchRunner(work, () -> {
                synchronized (this)
                {
                    executing = false;
                    queuePendingWork();
                }
            }));
        } else
        {
            addPendingWork(work);
        }
    }

    /**
     * If there is any pending work, submit it for execution.
     * <p>
     * This method must be called while synchronized.
     *
     * @since 2.29
     */
    protected void queuePendingWork()
    {
        final C pendingWork = pendingBatches.poll();

        if (pendingWork != null) queueBatch(pendingWork);
    }

    /**
     * Add the given batch to our pending work.
     * <p>
     * This method must be called while synchronized.
     *
     * @param work Batch to queue for later execution
     * @since 2.29
     */
    protected void addPendingWork(C work)
    {
        pendingBatches.add(work);
    }

    /**
     * Update the batch control times, start the timer task as needed.
     * The batch lock must be held when this method is called.
     */
    private void updateBatchTimes()
    {
        // If this is the first item added to the batch, then start our timer at initialDelay
        if (batchTask == null)
        {
            logger.debug("First add for {} delay = {}", workName, initialDelay);

            // Set the batch start time, last test time, and last entry time to "now"
            batchStart = lastBatchTest = lastEntryTime = System.currentTimeMillis();
            batchTask = batchTimer.createTask(new BatchHandler(), initialDelay);
        } else
        {
            lastEntryTime = System.currentTimeMillis(); // else track the time of the last addition to the batch
        }
    }

    /**
     * Force the processing of all batched operations.  If {@code immediate} is true, then this work is performed
     * on the current thread.  If {@code false}, then it is handed to the executor as per normal batch execution.
     *
     * @param immediate Pass {@code true} to execute on calling thread, {@code false} to use executor service
     */
    public void processNow(boolean immediate)
    {
        lockBatch();

        try
        {
            cancelBatchTask();
            fireRunBatch(immediate);
        } finally
        {
            unlockBatch();
        }
    }

    /**
     * Shutdown this queue, cancel any outstanding timer task we may have started.
     */
    public void shutdown()
    {
        lockBatch();

        try
        {
            cancelBatchTask();
            batch.clear();
        } finally
        {
            unlockBatch();
        }
    }

    /**
     * Cancel the current batch task, if any.  The batch lock must be held when this method is called.
     *
     * @see #lockBatch()
     */
    private void cancelBatchTask()
    {
        if (batchTask != null)
        {
            logger.trace("Cancel timer task");

            batchTask.cancel();
            batchTask = null;
        }
    }

    /**
     * Set the thread name to our work name when processing a batch.  This is useful when looking at thread
     * dumps to know what a random pool thread is doing.
     *
     * @since 2.2
     */
    protected void setThreadName()
    {
        Thread.currentThread().setName(workName);
    }

    /**
     * Perform needed actions on the collected batch of work.  To be Implemented in subclasses.
     *
     * @param batch Collection of batched entries to process
     */
    protected abstract void runBatch(C batch);

    /**
     * Runnable that will grab a copy of the batch and spawn a background task to perform the work on the batch.
     */
    private class BatchHandler implements Runnable
    {
        @Override
        public void run()
        {
            lockBatch();

            try
            {
                final long now = System.currentTimeMillis();
                final long currentDelay = now - batchStart;

                // Determine if no messages have come in since we last tested or if we've passed our max delay time.
                // In either case, we need to process the batch.  Otherwise, re-submit this task unless the max wait
                // time has passed.

                final boolean newEntryAndNotPastMax = lastEntryTime > lastBatchTest && currentDelay < maxDelay;

                // It is common for a few additions to be made quickly at the start of the batch. Consider a
                // preference dialog applying all the changes at once.  They'll all happen within a few millis, and
                // then no more changes. To ensure that cluster of changes is handled quickly (i.e., within the
                // initial delay period), we allow any number of additions to the queue within the first half of
                // the initial delay period without rescheduling the timer.
                //
                // So, if no entries have come in during the last half of the initial delay, then don't reschedule,
                // just execute the batch now.

                final long timeSinceLastEntry = now - lastEntryTime;
                final boolean noRecentEntries = timeSinceLastEntry > (initialDelay / 2);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Test: nre {} let {} > lbt {} && (now {} - start {}) {} < max {} {} size {}",
                            noRecentEntries, lastEntryTime, lastBatchTest, now, batchStart, currentDelay, maxDelay,
                            newEntryAndNotPastMax, batch.size());
                }

                if (!noRecentEntries && newEntryAndNotPastMax)
                {
                    // Ok, a message has arrived since the batch start (or the last execution of this task)
                    // but we haven't exceeded our max time yet, so reschedule this task, but no later than maxDelay.

                    lastBatchTest = now;

                    final long delay = Math.min(initialDelay, maxDelay - currentDelay);
                    logger.debug("reschedule {} delay={}", workName, delay);
                    batchTask = batchTimer.createTask(new BatchHandler(), delay);
                    return;
                }

                logger.debug("Submitting {}", workName);

                batchTask = null;

                fireRunBatch(false); // Kick off the batch
            } finally
            {
                unlockBatch();
            }
        }
    }

    /**
     * Runnable to actually perform the processing of a batch.  This extension of Runnable ensures that it has
     * a usable 'toString' value if exceptions are thrown and caught in the executor service.
     *
     * @since 2.37
     */
    private final class BatchRunner implements Runnable
    {
        private final C work;
        private final Runnable finalizer;

        /**
         * @param work      Work batch to process
         * @param finalizer Optional code to run after the batch processing is complete, may be {@code null}
         */
        BatchRunner(C work, Runnable finalizer)
        {
            this.work = work;
            this.finalizer = finalizer;
        }

        BatchRunner(C work)
        {
            this(work, null);
        }

        @Override
        public void run()
        {
            setThreadName();
            internalRunBatch(work);

            if (finalizer != null) finalizer.run();
        }

        @Override
        public String toString()
        {
            return "[BatchRunner for " + workName + "]";
        }
    }
}
