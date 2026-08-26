package gov.takx.platform.utils;

import java.util.AbstractCollection;
import java.util.Iterator;

/**
 * A time bounded task operation. Similar to {@link TimeBatchedQueue} but we don't care about a batch of objects.
 * This class simply tracks requests to perform a given task, and eventually executes that task within the
 * time boundaries.
 * <p>
 * A time bounded task has an initial delay and a max delay.  When a {@link #request()} is made, a timer
 * of {@code initialDelay} is kicked off.  If we receive another request before the timer fires, we
 * reset the timer.  When the timer finally expires, we perform the task.  The {@code maxDelay}
 * places an upper bound on how long we will allow the timer to be postponed.  This ensures that a continuous
 * stream of requests will eventually get processed at roughly {@code maxDelay} intervals.
 *
 * @since 2.2
 */
public class TimeBoundedTask extends TimeBatchedQueue<TimeBoundedTask.SingletonCollection, Object>
{
    private final Runnable task;

    private volatile boolean pendingExecutions = false;

    /**
     * Construct a batched queue using the given batch container.
     * <p>
     * Commonly, the {@code batchSupplier} is just a method reference to a collection constructor,
     * like {@code ArrayList<Foo>::new}.
     *
     * @param workName     The name to give to the task performing the batch of work
     * @param initialDelay The initial amount of time to wait for additional work
     * @param maxDelay     The max time during which we'll collect work
     * @param task         The task to perform when the timer finally expires
     */
    public TimeBoundedTask(String workName, long initialDelay, long maxDelay, Runnable task)
    {
        super(workName, initialDelay, maxDelay, SingletonCollection::new);

        this.task = task;
    }

    /**
     * Request the performance of the configured task.  The request will be queued until the timers determine it
     * should be performed.
     */
    public void request()
    {
        add(this);
    }

    @Override
    protected void runBatch(SingletonCollection batch)
    {
        runTask(); // The batch is irrelevant, just run that task
    }

    /**
     * Run the configured task.
     *
     * @since 2.14
     */
    protected void runTask()
    {
        task.run();
    }

    /**
     * Overridden since we don't need to actually track the batch, just the fact that work was triggered.
     */
    @Override
    protected void queuePendingWork()
    {
        if (pendingExecutions)
        {
            queueBatch(null);
            pendingExecutions = false;
        }
    }

    /**
     * Overridden since we don't track individual batches.  We coalesce all work requests into a single task execution.
     */
    @Override
    protected void addPendingWork(SingletonCollection work)
    {
        pendingExecutions = true;
    }

    /**
     * Trivial class to "pretend" to be a collection and just keep track of whether anything has been added or not.
     */
    static final class SingletonCollection extends AbstractCollection<Object>
    {
        private boolean empty = true;

        @Override
        public boolean add(Object o)
        {
            empty = false;
            return true;
        }

        @Override
        public void clear()
        {
            empty = true;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<Object> iterator()
        {
            return null;
        }

        @Override
        public int size()
        {
            return empty ? 0 : 1;
        }
    }
}
