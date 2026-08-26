package gov.takx.core.media.decode;

import gov.takx.core.media.writer.MediaCleaner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;

/**
 * Queue of decoded frames to be displayed or processed
 *
 * @since 2.8
 */
public class FrameQueue
{
    private final DecodedFrame[] queue;
    private final int maxSize;
    private int readIndex;
    private int readIndexShown;
    private volatile int writeIndex;
    private volatile int size;
    private volatile boolean abort = false;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    /**
     * Create queue with a fixed capacity
     *
     * @param maxSize The capacity
     */
    FrameQueue(int maxSize)
    {
        this.maxSize = maxSize;
        queue = new DecodedFrame[maxSize];
        for (int i = 0; i < maxSize; ++i)
        {
            queue[i] = new DecodedFrame();
        }

        clear();
        MediaCleaner.getInstance().register(this, new FrameQueueState(queue, maxSize)::cleanup);
    }

    /**
     * Empty the queue
     */
    public void clear()
    {
        size = 0;
        readIndex = 0;
        readIndexShown = 0;
        writeIndex = 0;
    }

    /**
     * Cause any calls to peekReadable or peakReadable to unblock and return {@code null}
     */
    public void abort()
    {
        abort = true;
    }

    public DecodedFrame peek()
    {
        return queue[(readIndex + readIndexShown) % maxSize];
    }

    public DecodedFrame peekNext()
    {
        return queue[(readIndex + readIndexShown + 1) % maxSize];
    }

    public DecodedFrame peekLast()
    {
        return queue[readIndex];
    }

    /**
     * Block until a frame is free for writing
     *
     * @return The free frame, or {@code null} if abort was called while blocked
     */
    public DecodedFrame peekWritable()
    {
        lock.lock();
        try
        {
            boolean gotSignal = false;
            while (!gotSignal && size >= maxSize && !abort)
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

        if (abort)
        {
            return null;
        }

        return queue[writeIndex];
    }

    public DecodedFrame peekReadable()
    {
        lock.lock();
        try
        {
            while (size - readIndexShown <= 0 && !abort)
            {
                try
                {
                    condition.await();
                } catch (InterruptedException e)
                {
                    // Loop again to check for abort
                }
            }
        } finally
        {
            lock.unlock();
        }

        if (abort)
        {
            return null;
        }

        return queue[(readIndex + readIndexShown) % maxSize];
    }

    /**
     * Called when a frame has been fully written to the queue
     * <p>
     * Note that peekWritable must be called and the frame written before calling this method. This notifies
     * the consumer thread that a new frame is available for reading.
     */
    public void push()
    {
        writeIndex = (writeIndex + 1) % maxSize;
        lock.lock();
        try
        {
            size++;
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

        av_frame_unref(queue[readIndex].frame);
        readIndex = (readIndex + 1) % maxSize;
        lock.lock();
        try
        {
            size--;
            condition.signal();
        } finally
        {
            lock.unlock();
        }
    }

    public boolean isEmpty()
    {
        return (size - readIndexShown) <= 0;
    }

    public int size()
    {
        return size - readIndexShown;
    }

    public boolean canShow()
    {
        return readIndexShown == 1;
    }

    /**
     * Construct a state from the decoded frame queue and it's size.
     *
     * @param queue The decoded frame queue.
     * @param size  The size of the queue.
     * @since 5.1.0
     */
    private record FrameQueueState(DecodedFrame[] queue, int size)
    {
        void cleanup()
        {
            for (int i = 0; i < size; ++i)
            {
                if (queue[i] != null)
                {
                    queue[i].destroy();
                }
                queue[i] = null;
            }
        }
    }
}
