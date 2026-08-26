package gov.takx.core.tasks;

import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.tasks.ICancelableManagedTask;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.ManagedTaskEvents;
import gov.takx.api.tasks.TaskGroup;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Wrapper around core executor service to limit the number of tasks that are concurrently submitted for execution.
 * <p>
 * Care has been taken to ensure that a listener notification never happens while internal locks are held.
 * Please keep it that way.
 *
 * @since 2.37
 */
@Dependent
public class LimitingExecutor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ManagedTaskEvents taskEvents;
    private final ICoreExecutorService executorService;

    private TaskGroup taskGroup;
    private volatile int limit = Integer.MAX_VALUE; // Default to unlimited

    private final Lock lock = new ReentrantLock();
    private int submittedCount = 0;
    private final Deque<IManagedTask> blockedTasks = new ArrayDeque<>();

    // Set of all tasks which have been queued for execution.
    private final Set<TaskHolder> executingTasks = ConcurrentHashMap.newKeySet();

    @Inject
    public LimitingExecutor(ManagedTaskEvents taskEvents, ICoreExecutorService executorService)
    {
        this.taskEvents = taskEvents;
        this.executorService = executorService;
    }

    public LimitingExecutor forTaskGroup(TaskGroup taskGroup)
    {
        this.taskGroup = taskGroup;
        return this;
    }

    /**
     * @return the concurrent execution limit
     */
    public int getLimit()
    {
        return limit;
    }

    /**
     * Set the concurrent execution limit.
     *
     * @param limit must be > 0
     * @throws IllegalArgumentException If {@code} limit is <= 0
     */
    public void setLimit(int limit)
    {
        if (limit <= 0) throw new IllegalArgumentException("'limit' must be > 0");
        this.limit = limit;
    }

    /**
     * Shutdown. Clear the blocked tasks, and cancel any cancellable tasks already submitted for execution.
     */
    public void shutdown()
    {
        blockedTasks.clear();

        // Cancel all executing tasks that allow cancel
        executingTasksOfType(ICancelableManagedTask.class, ICancelableManagedTask::allowCancel)
                .forEach(ICancelableManagedTask::cancel);

        executingTasks.clear();
    }

    /**
     * Get a subset of the executing task list where the task implements the given task interface.
     *
     * @param taskType Type of task to return
     * @param filter   Predicate to further filter the list of matching tasks
     * @param <T>      The inferred type (will match taskType)
     * @return subset of executing tasks that are of the requested type, may be empty
     */
    @SuppressWarnings("unchecked")
    public <T extends IManagedTask> Stream<T> executingTasksOfType(Class<T> taskType, Predicate<T> filter)
    {
        return allExecutingTasks()
                .filter(task -> taskType.isAssignableFrom(task.getClass()))
                .map(task -> (T) task)
                .filter(filter);
    }

    /**
     * @return Stream of all tasks currently submitted for execution
     */
    public Stream<IManagedTask> allExecutingTasks()
    {
        return executingTasks.stream().map(holder -> holder.task);
    }

    /**
     * Queue a task for execution, if we're already hit our concurrent limit, then add it to the blocked queue.
     *
     * @param task The task to queue
     */
    public void queueTask(IManagedTask task)
    {
        boolean blocked = false;

        lock.lock();
        try
        {
            if (submittedCount >= limit)
            {
                blockedTasks.add(task); // already at the limit, put the task into the "blocked" queue
                blocked = true;
            } else
            {
                submittedCount += 1; // Else bump the executing task count
            }
        } finally
        {
            lock.unlock();
        }

        if (blocked)
        {
            taskEvents.fireBlocked(task);
        } else
        {
            submitTaskForExecution(task); // Else we can hand it to the real executor
        }
    }

    /**
     * Handle a task finishing execution.  Update the task count and release a blocked task if any are waiting.
     */
    private void taskFinished()
    {
        lock.lock();

        IManagedTask releasedTask;
        try
        {
            // Don't decrease the count if we're going to immediately submit a blocked task
            if ((releasedTask = blockedTasks.poll()) == null)
            {
                if (--submittedCount < 0) logger.error("TaskGroup [{}] invalid count: {}", taskGroup, submittedCount);
            }
        } finally
        {
            lock.unlock();
        }

        if (releasedTask != null) submitTaskForExecution(releasedTask);
    }

    /**
     * Submit a task for actual execution and add it to the executing tasks set.
     *
     * @param task Task to submit for execution
     */
    private void submitTaskForExecution(IManagedTask task)
    {
        final TaskHolder taskHolder = new TaskHolder(task);
        executingTasks.add(taskHolder);
        taskEvents.fireQueued(task);
        executorService.execute(new TaskRunner(taskHolder));
    }

    /**
     * Inner class to handle the actual execution of the task.  It monitors the execution and updates state accordingly.
     */
    private class TaskRunner implements Runnable
    {
        private final TaskHolder taskHolder;

        private TaskRunner(TaskHolder taskHolder)
        {
            this.taskHolder = taskHolder;
        }

        @Override
        public void run()
        {
            final IManagedTask task = taskHolder.task;
            final ITaskStatusHandler handler = task.getTaskStatusHandler();

            Thread.currentThread().setName(task.getName());  // Ensure the thread has a good name

            try
            {
                taskEvents.fireStarted(task);
                task.execute();
            } catch (Exception e)
            {
                logger.error("Task failed: {}", task.getName(), e);

                // If the task is not marked as complete, then do it now
                if (!handler.isComplete() || handler.getException() == null)
                {
                    handler.setException(e);
                }
            } finally
            {
                handler.setComplete(); // Ensure that it has been marked as complete

                executingTasks.remove(taskHolder); // It's done
                taskEvents.fireFinished(taskHolder.task);

                taskFinished();
            }
        }
    }

    /**
     * We can't assume that all implementations of IManagedTask are comparable, and we need to keep them in a
     * collection, so we need this holder object to provide an 'identity' equality semantic.
     */
    private static final class TaskHolder
    {
        private final IManagedTask task;

        private TaskHolder(IManagedTask task)
        {
            this.task = task;
        }
    }
}
