package gov.takx.platform.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract implementation of {@link TimeBatchedQueue} using an ArrayList as the batch collection type.
 * Derived classes must implement {@link #runBatch runBatch(List)}
 *
 * @since 1.48
 */
public abstract class ListTimeBatchedQueue<T> extends TimeBatchedQueue<List<T>, T>
{
    /**
     * Construct a batched queue using a default ArrayList as the batch type.
     *
     * @param workName     The name to give to the task performing the batch of work
     * @param initialDelay The initial amount of time to wait for additional work
     * @param maxDelay     The max time during which we'll collect work
     */
    public ListTimeBatchedQueue(String workName, long initialDelay, long maxDelay)
    {
        super(workName, initialDelay, maxDelay, ArrayList::new);
    }
}
