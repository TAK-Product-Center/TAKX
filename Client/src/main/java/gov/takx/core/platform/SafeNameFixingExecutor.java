package gov.takx.core.platform;

import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.NameFixingRunnable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Simple extension to the {@link ScheduledThreadPoolExecutor} so that we can restore the original thread name when the
 * task completes.  This allows submitted tasks to change the name of the thread (to indicate what they are
 * doing) without worrying about changing it back. We take care of that.
 * <p>
 * We also report exceptions that are captured inside a {@link java.util.concurrent.Future} that was created if
 * the caller used any of the {@code submit} or {@code schedule} methods to queue the work.
 *
 * @see ScheduledThreadPoolExecutor
 * @see NameFixingRunnable
 * @since 1.25.6
 */
@Dependent
public class SafeNameFixingExecutor extends ScheduledThreadPoolExecutor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String RX_POOL = "RX-pool";
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 2;

    private static final BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
            .namingPattern(RX_POOL + "-%d")
            .build();

    private final IPersistServiceProvider persistServiceProvider;

    private static final ThreadLocal<String> THREAD_NAMES = new ThreadLocal<>();

    private static Field callableField; // FutureTask.callable field
    private static Field outcomeField; // FutureTask.outcome field
    private static final ThreadLocal<Object> THREAD_TASKS = new ThreadLocal<>();

    static
    {
        findFutureTaskFields();
    }

    /**
     * Construct an executor with a fixed thread pool.
     */
    @Inject
    private SafeNameFixingExecutor(IPersistServiceProvider persistServiceProvider)
    {
        super(THREAD_COUNT, threadFactory);

        this.persistServiceProvider = persistServiceProvider;

        logger.debug("Created executor service with {} threads", THREAD_COUNT);
    }

    /**
     * Save the name of the current thread, so we can restore it later.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r)
    {
        THREAD_NAMES.set(t.getName());

        // For a runnable wrapped in FutureTask, we want to grab the inner "callable" for later
        if (r instanceof FutureTask) THREAD_TASKS.set(getCallable(r));
    }

    /**
     * Restore the previous name of the thread.  Also report exceptions that might be trapped inside a Future.
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void afterExecute(Runnable r, Throwable thrown)
    {
        super.afterExecute(r, thrown);

        final Thread currentThread = Thread.currentThread();
        final boolean isFutureTask = r instanceof FutureTask;

        // Help track down tasks that don't give themselves names
        if (currentThread.getName().startsWith(RX_POOL) && !(r instanceof NameFixingRunnable) &&
                !(isFutureTask && ((FutureTask<?>) r).isCancelled())) // don't care about cancelled tasks
        {
            final Object task = isFutureTask ? THREAD_TASKS.get() : r;
            logger.warn("Unnamed task of type {} : {}", (task != null ? task.getClass() : null), task);
        }

        THREAD_TASKS.set(null); // Don't hold on to reference

        // Ensure that any exception thrown in the task is reported.
        if (thrown != null) logger.error("Background task failed", thrown);

        // If there's an exception held within the future task, then report it.
        //
        // If the runnable is a Future, we used to call Future.get(), but that seems to completely screw
        // up the thread management in a ScheduledThreadPoolExecutor.  So, we dive in and determine if the task's
        // 'outcome' is an exception.
        final Throwable exception = isFutureTask ? getOutcomeIfException(r) : null;
        if (exception != null) logger.error("Background task failed", exception);

        // Ensure that background tasks cleanup their DB sessions, but avoid creating a PS just to clean it up.
        persistServiceProvider.cleanupForThread(currentThread);

        currentThread.setName(THREAD_NAMES.get()); // Restore the thread name

        if (logger.isTraceEnabled())
        {
            logger.trace("Q {}, S {}, T {} || A {}, C {}, P {}", getQueue().size(), getTaskCount(),
                    getCompletedTaskCount(), getActiveCount(), getCorePoolSize(), getPoolSize());
        }
    }

    /**
     * @param outerRunnable The runnable/task that holds the actual runnable submitted for execution
     * @return The 'callable' field from the given outer runnable
     * @since 3.0
     */
    @Nullable
    private Object getCallable(@Nonnull Runnable outerRunnable)
    {
        try
        {
            return callableField.get(outerRunnable);
        } catch (IllegalAccessException e)
        {
            logger.error("Failed to get 'callable' field: {}", e.getMessage());
            return null;
        }
    }

    /**
     * @param outerRunnable The runnable/task that holds the actual runnable submitted for execution
     * @return The 'outcome' field from the given outer runnable if it is an exception, {@code null} otherwise
     * @since 3.1
     */
    @Nullable
    private Throwable getOutcomeIfException(@Nonnull Runnable outerRunnable)
    {
        try
        {
            final Object outcome = outcomeField.get(outerRunnable);
            return outcome instanceof Throwable ? (Throwable) outcome : null;
        } catch (IllegalAccessException e)
        {
            logger.error("Failed to get 'outcome' field: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Dig into the FutureTask class and find:
     * <ul>
     *     <li> The {@code callable} field, which holds the reference to the actual runnable/callable submitted
     *     for execution.</li>
     *     <li>The {@code outcome} field, which holds potential exceptions.</li>
     * </ul>
     * @since 3.0 (renamed in 3.1)
     */
    private static void findFutureTaskFields()
    {
        try
        {
            callableField = FutureTask.class.getDeclaredField("callable");
            callableField.setAccessible(true);

            outcomeField = FutureTask.class.getDeclaredField("outcome");
            outcomeField.setAccessible(true);
        } catch (NoSuchFieldException e)
        {
            logger.error("Failed to find required fields: {}", e.getMessage());
        }
    }

    /**
     * Shutdown the pool when we are destroyed.
     *
     * @since 2.37
     */
    @PreDestroy
    private void cleanup()
    {
        shutdownNow();
    }
}
