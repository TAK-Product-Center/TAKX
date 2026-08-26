package gov.takx.core.tasks;

import gov.takx.api.tasks.TaskGroup;

/**
 * A task to execute an arbitrary {@link Runnable}. The task will not be cancelable and it will not affect
 * an exit operation on TAKX. It will have an indeterminate progress bar.
 *
 * @since 1, 42
 */
public class RunnableTask extends AManagedTask
{
    protected final Runnable runnable;

    /**
     * Construct a task to execute the given runnable.
     *
     * @param taskGroup Group to which the task belongs
     * @param name      Task name
     * @param runnable  runnable to execute
     */
    public RunnableTask(TaskGroup taskGroup, String name, final Runnable runnable)
    {
        super(taskGroup, name);

        this.runnable = runnable;
    }

    @Override
    public void execute() throws Exception
    {
        getTaskStatusHandler().setProgressIndeterminate(true);

        runnable.run();
    }
}
