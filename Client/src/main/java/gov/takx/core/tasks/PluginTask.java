package gov.takx.core.tasks;

import gov.takx.api.tasks.ITaskRunnable;
import gov.takx.api.tasks.TaskGroup;

/**
 * Wrapper for tasks submitted by plugins.  Task type will always be {@link TaskGroup#PLUGIN}.  Basically acts like
 * and exit affecting task wrapping the necessary configuration parameters from {@link ITaskRunnable}
 *
 * @since 2.14
 */
public class PluginTask extends AExitAffectingTask
{
    /**
     * The Plugin contributed task to wrap.
     */
    private final ITaskRunnable runnable;

    /**
     * Wrap the plugin contributed task and inject the progress monitor.
     *
     * @param runnable plugin contributed task
     */
    public PluginTask(final ITaskRunnable runnable)
    {
        super(TaskGroup.PLUGIN, runnable.getName());
        setCancelable(runnable.allowCancel());
        this.runnable = runnable;
        runnable.setProgressMonitor(getTaskStatusHandler());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Call the wrapped task method of the same name.
     */
    @Override
    public void doCancel()
    {
        runnable.doCancel();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Execute the wrapped task.
     */
    @Override
    public void execute() throws Exception
    {
        runnable.run();
    }

    /**
     * {@inheritDoc}
     *
     * @return the exit message from the wrapped task
     */
    @Override
    public String getExitMessage()
    {
        return runnable.getExitMessage();
    }

    /**
     * {@inheritDoc}
     *
     * @return the result from the wrapped task
     */
    @Override
    public boolean allowExit()
    {
        return runnable.allowExit();
    }
}
