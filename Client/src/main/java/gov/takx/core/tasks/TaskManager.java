package gov.takx.core.tasks;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.tasks.IExitAffectingTask;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The core task manager.  It is responsible for creating managed tasks and handling progress notifications.
 * Interested parties can register as a listener to receive notifications when tasks are queued, started, and
 * finished.
 * <p>
 * Each task operates within a {@link TaskGroup}.  Each group can have a maximum execution limit set.  The task
 * manager will ensure that the number of concurrently executing tasks within a group never exceeds this limit.
 * The default limit is unlimited, which means that the parameters of the core executor service will control the
 * maximum number of concurrent background operations.
 *
 * @see LimitingExecutor
 * @since 1.42
 */
@ApplicationScoped
public class TaskManager implements ITaskManager
{
    @Inject
    private Instance<LimitingExecutor> executorInstance;

    // Executors per task group
    private final ConcurrentHashMap<TaskGroup, LimitingExecutor> executors = new ConcurrentHashMap<>();

    private boolean shutdown = false;

    /**
     * Crank up our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        shutdown = false;
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        shutdown = true;
        executors.values().forEach(LimitingExecutor::shutdown); // Shutdown all executors
    }

    @Override
    public ITaskStatusHandler execute(String name, Runnable runnable)
    {
        return execute(TaskGroup.OTHER, name, runnable);
    }

    @Override
    public ITaskStatusHandler execute(TaskGroup taskGroup, String name, Runnable runnable)
    {
        return execute(new RunnableTask(taskGroup, name, runnable));
    }

    @Override
    public ITaskStatusHandler execute(@Nonnull IManagedTask task)
    {
        if (shutdown) throw new IllegalStateException("TaskManager has been shutdown");

        getExecutorForGroup(task.getTaskGroup()).queueTask(task);
        return task.getTaskStatusHandler();
    }

    @Override
    public int getExecutingTaskCount()
    {
        return (int) allExecutingTasks().count();
    }

    @Override
    public List<IManagedTask> getExecutingTasks()
    {
        return allExecutingTasks().collect(Collectors.toList());
    }

    @Override
    public int getTaskGroupExecutionLimit(TaskGroup taskGroup)
    {
        return getExecutorForGroup(taskGroup).getLimit();
    }

    @Override
    public void setTaskGroupExecutionLimit(TaskGroup taskGroup, int limit) throws IllegalArgumentException
    {
        getExecutorForGroup(taskGroup).setLimit(limit);
    }

    /**
     * Get the executor for a task group, creating it if needed.
     *
     * @param taskGroup Task group for which to get the executor service
     * @return Executor for the given task group
     */
    private LimitingExecutor getExecutorForGroup(TaskGroup taskGroup)
    {
        return executors.computeIfAbsent(taskGroup, k -> executorInstance.get().forTaskGroup(taskGroup));
    }

    @Override
    public List<IExitAffectingTask> getExitAffectingTasks()
    {
        return executingTasksOfType(IExitAffectingTask.class).collect(Collectors.toList());
    }

    /**
     * @return Stream of all tasks that have been submitted for execution across all task groups
     */
    private Stream<IManagedTask> allExecutingTasks()
    {
        return executors.values().stream().flatMap(LimitingExecutor::allExecutingTasks);
    }

    /**
     * Get a subset of the executing task list where the task implements the given task interface.
     *
     * @param taskType Type of task to return
     * @param <T>      The inferred type (will match taskType)
     * @return Stream of executing tasks that are of the requested type, may be an empty stream
     */
    private <T extends IManagedTask> Stream<T> executingTasksOfType(Class<T> taskType)
    {
        return executors.values()
                .stream()
                .flatMap(es -> es.executingTasksOfType(taskType, task -> true));
    }
}
