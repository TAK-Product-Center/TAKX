package gov.takx.api.tasks;

import java.util.List;

/**
 * Contract for the core task manager.  It is responsible for generating monitored task instances
 * for various operations like File import, arbitrary Import/Export plugins, etc.
 * <p/>
 * In general, managed tasks are made visible to the user via progress dialogs and may impact the
 * choice to exit TAKX by showing the user a list of incomplete tasks.
 *
 * @since 1.42
 */
public interface ITaskManager
{
    /**
     * Execute the given runnable as a managed task.  It will be placed into the {@link TaskGroup#OTHER} group.
     *
     * @param name     Display name for task
     * @param runnable Runnable to execute
     * @return Task status handler
     */
    ITaskStatusHandler execute(String name, Runnable runnable);

    /**
     * Execute the given runnable as a managed task.
     *
     * @param taskGroup The task group in which to execute the task
     * @param name      Display name for task
     * @param runnable  Runnable to execute
     * @return Task status handler
     */
    ITaskStatusHandler execute(TaskGroup taskGroup, String name, Runnable runnable);

    /**
     * Execute the given managed task.
     *
     * @param task The task to execute
     * @return Task status handler
     */
    ITaskStatusHandler execute(IManagedTask task);

    /**
     * @return The number of tasks currently queued for execution
     */
    int getExecutingTaskCount();

    /**
     * Get a list of all the tasks that have been queued for execution, i.e., not blocked.
     *
     * @return List of tasks, this is a copy of the internal data
     */
    List<IManagedTask> getExecutingTasks();

    /**
     * Get a list of all the tasks that have started execution and are "exit affecting" tasks.
     *
     * @return list of tasks that are exit affecting and have started execution
     */
    List<IExitAffectingTask> getExitAffectingTasks();

    /**
     * Set a limit on the number of tasks that can simultaneously be submitted for execution to the core
     * executor service.
     *
     * @param taskGroup Task group to limit
     * @param limit     limit to impose, use {@link Integer#MAX_VALUE} for no limit
     * @throws IllegalArgumentException if limit is < 0
     */
    void setTaskGroupExecutionLimit(TaskGroup taskGroup, int limit) throws IllegalArgumentException;

    /**
     * Get the current execution limit for the given task group.
     *
     * @param taskGroup TaskGroup of interest
     * @return current limit, {@link Integer#MAX_VALUE} if none has been configured
     */
    int getTaskGroupExecutionLimit(TaskGroup taskGroup);
}
