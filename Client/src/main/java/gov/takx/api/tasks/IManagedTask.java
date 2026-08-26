package gov.takx.api.tasks;

import gov.takx.api.systemmessage.MessageAction;

/**
 * Contract for a managed background task that is handled by the TAKX core. The platform is responsible for executing
 * this task on a non-EDT thread. Execution status is made available via the task manager and in the UI.
 * <p/>
 * Every task has a name used in UI displays and messages. All tasks are organized into groups for use
 * when interacting with users to display similar tasks together.
 *
 * @since 1.42
 */
public interface IManagedTask
{
    /**
     * @return Task name
     */
    String getName();

    /**
     * @return This tasks task group
     */
    TaskGroup getTaskGroup();

    /**
     * @return The status handler monitoring this task
     */
    ITaskStatusHandler getTaskStatusHandler();

    /**
     * Set the status handler to use on this task.
     *
     * @param taskStatusHandler Status handler to use
     */
    void setTaskStatusHandler(ITaskStatusHandler taskStatusHandler);

    /**
     * Execute the task.  This will be called on a background thread (never on the EDT).
     */
    void execute() throws Exception;

    /**
     * Determine if any location data has been registered during the execution of the task.
     *
     * @return {@code true} if a location, sector, or Map object key has been registered
     * @since 2.37
     */
    boolean hasLocationInfo();

    /**
     * Construct a MessageAction to navigate the globe to the location registered with this task.
     *
     * @return action that will navigate the globe to the registered location
     * @since 2.37
     */
    MessageAction makeGotoAction();
}
