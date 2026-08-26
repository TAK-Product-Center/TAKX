package gov.takx.core.platform;

import com.google.common.util.concurrent.ForwardingExecutorService;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.platform.ICoreExecutorService;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executor service for the core.
 * <p>
 * Underneath, it uses instances of {@link SafeNameFixingExecutor} so that tasks are free to rename their thread
 * without having to worry about changing it back.
 *
 * @see SafeNameFixingExecutor
 * @since 2.0.0
 */
@Alternative
@ApplicationScoped
public class CoreExecutorService extends ForwardingExecutorService implements ICoreExecutorService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Instance<SafeNameFixingExecutor> executorProducer;

    private SafeNameFixingExecutor currentExecutor;

    /**
     * CDI constructor.
     */
    @Inject
    CoreExecutorService(Instance<SafeNameFixingExecutor> executorProducer)
    {
        this.executorProducer = executorProducer;
        currentExecutor = executorProducer.get();
    }

    /**
     * Shutdown our current delegate executor and create a new one.
     */
    private void shutdownCurrentExecutor(@Observes ProjectClosing event)
    {
        logger.debug("Shutting down executor and restarting!");

        executorProducer.destroy(currentExecutor);

        currentExecutor = executorProducer.get(); // And create a new one
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit)
    {
        return delegate().schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit)
    {
        return delegate().schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit)
    {
        return delegate().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit)
    {
        return delegate().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    protected ScheduledExecutorService delegate()
    {
        return currentExecutor;
    }
}
