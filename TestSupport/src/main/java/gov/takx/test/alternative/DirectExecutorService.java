package gov.takx.test.alternative;

import gov.takx.api.platform.ICoreExecutorService;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Alternative executor service used during testing to cause submitted tasks to execute immediately on the calling
 * thread.  Note that any task scheduled for periodic execution will execute exactly once.
 *
 * @since 2.0.0
 */
@Alternative
@ApplicationScoped
public class DirectExecutorService extends AbstractExecutorService implements ICoreExecutorService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private volatile boolean running = true;

    @Override
    public void shutdown()
    {
        logger.debug("shutdown");
        running = false;
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown()
    {
        return !running;
    }

    @Override
    public boolean isTerminated()
    {
        return isShutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit)
    {
        return true;
    }

    @Override
    public void execute(@Nonnull Runnable command)
    {
        logger.debug("execute: {}", command);

        final String name = Thread.currentThread().getName();
        try
        {
            command.run();
        } finally
        {
            Thread.currentThread().setName(name);
        }
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit)
    {
        execute(command);
        return new DoneFuture<Void>();
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit)
    {
        final AtomicReference<DoneFuture<V>> futureReference = new AtomicReference<>(new DoneFuture<>());

        logger.debug("call: {}", callable);

        execute(() -> {
            try
            {
                futureReference.set(new DoneFuture<>(callable.call()));
            } catch (Exception e)
            {
                throw new RuntimeException("scheduled callable failed", e);
            }
        });

        return futureReference.get();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit)
    {
        execute(command);
        return new DoneFuture<Void>();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit)
    {
        execute(command);
        return new DoneFuture<Void>();
    }

    /**
     * Implementation of {@link ScheduledFuture} to provide return values for the various scheduling methods.
     * The future is immediately done, see {@link #isDone()}.
     *
     * @since 3.0
     */
    @SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
    private static class DoneFuture<V> implements ScheduledFuture<V>
    {
        private final V result;

        DoneFuture()
        {
            this(null);
        }

        DoneFuture(V result)
        {
            this.result = result;
        }

        @Override
        public long getDelay(@Nonnull TimeUnit unit)
        {
            return 0;
        }

        @Override
        public int compareTo(@Nonnull Delayed o)
        {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return false;
        }

        @Override
        public boolean isCancelled()
        {
            return false;
        }

        @Override
        public boolean isDone()
        {
            return true;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException
        {
            return result;
        }

        @Override
        public V get(long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return result;
        }
    }
}
