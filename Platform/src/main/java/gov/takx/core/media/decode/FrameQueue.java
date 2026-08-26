package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.core.media.MediaCleaner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Queue of decoded frames to be displayed or processed.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class FrameQueue
{
    private final DecodedFrame[] queue;
    private final int maxSize;
    private int readIndex;
    private int readIndexShown;
    private final AtomicInteger writeIndex = new AtomicInteger(0);
    private final AtomicInteger size = new AtomicInteger(0);
    private volatile boolean abort;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    /**
     * Create queue with a fixed capacity
     *
     * @param maxSize The capacity
     */
    FrameQueue(int maxSize)
    {
        this.maxSize = maxSize;
        queue = new DecodedFrame[maxSize];
        for (int i = 0; i < maxSize; ++i) queue[i] = new DecodedFrame();

        clear();
        MediaCleaner.getInstance().register(this, new FrameQueueState(queue)::cleanup);
    }

    /**
     * Empty the queue
     */
    public void clear()
    {
        size.set(0);
        readIndex = 0;
        readIndexShown = 0;
        writeIndex.set(0);
    }

    /**
     * Cause any calls to {@link #getReadable} or {@link #getWritable()} to unblock and return {@code null}.
     */
    public void abort()
    {
        abort = true;
    }

    /**
     * @return next frame to be read, non-blocking
     */
    public DecodedFrame peek()
    {
        return queue[(readIndex + readIndexShown) % maxSize];
    }

    /**
     * @return The last frame read, non-blocking
     */
    public DecodedFrame peekLast()
    {
        return queue[readIndex];
    }

    /**
     * Block until a frame is free for writing.
     *
     * @return The free frame, or {@code null} if abort was called while blocked
     */
    public DecodedFrame getWritable()
    {
        lock.lock();
        try
        {
            boolean gotSignal = false;
            while (!gotSignal && size.get() >= maxSize && !abort)
            {
                try
                {
                    gotSignal = condition.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e)
                {
                    // Loop again to check for abort
                }
            }
        } finally
        {
            lock.unlock();
        }

        return abort ? null : queue[writeIndex.get()];
    }

    /**
     * Block until a readable frame is available.
     *
     * @param exitTest supplier to determine if the wait loop should exit
     * @return The readable frame, or {@code null} if abort was called while blocked, or {@code exitTest} returns {@code true}
     */
    public DecodedFrame getReadable(Supplier<Boolean> exitTest)
    {
        lock.lock();
        try
        {
            boolean gotSignal = false;
            while (!gotSignal && size.get() - readIndexShown <= 0 && !abort && !exitTest.get())
            {
                try
                {
                    gotSignal = condition.await(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored)
                {
                    // Loop again to check for abort
                }
            }
        } finally
        {
            lock.unlock();
        }

        return abort ? null : peek();
    }

    /**
     * Called when a frame has been fully written to the queue.
     * <p>
     * Note that {@link #getWritable} must be called and the frame written before calling this method. This notifies
     * the consumer thread that a new frame is available for reading.
     */
    public void push()
    {
        writeIndex.accumulateAndGet(1, (current, x) -> (current + x) % maxSize);
        lock.lock();
        try
        {
            size.incrementAndGet();
            condition.signal();
        } finally
        {
            lock.unlock();
        }
    }

    public void next()
    {
        if (readIndexShown == 0)
        {
            readIndexShown++;
            return;
        }

        queue[readIndex].unReference();
        readIndex = (readIndex + 1) % maxSize;
        lock.lock();
        try
        {
            size.decrementAndGet();
            condition.signal();
        } finally
        {
            lock.unlock();
        }
    }

    /**
     * @return {@code true} if there are no readable frames in the queue
     */
    public boolean isEmpty()
    {
        return size.get() - readIndexShown <= 0;
    }

    /**
     * @return {@code true} if a readable frame has been returned by {@link #next()}
     */
    public boolean canShow()
    {
        return readIndexShown == 1;
    }

    /**
     * State to be used to clean up when this queue is GCed.
     *
     * @param queue The decoded frame queue
     * @since 5.1.0
     */
    private record FrameQueueState(DecodedFrame[] queue)
    {
        void cleanup()
        {
            for (int i = 0; i < queue.length; ++i)
            {
                if (queue[i] != null) queue[i].destroy();
                queue[i] = null;
            }
        }
    }
}
