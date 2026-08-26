package gov.takx.core.tasks;

import gov.takx.api.tasks.ITaskRunnable;
import gov.takx.api.tasks.TaskGroup;

/**
 * Wrapper for maps import tasks (imagery or terrain). Task type will always be {@link TaskGroup#MAPS_IMPORT}.
 * It acts like an exit-affecting task wrapping the necessary configuration parameters from {@link ITaskRunnable}.
 *
 * @since 2.32 (renamed from MapsConverterTask in 3.4.1)
 */
public class MapsImporterTask extends AExitAffectingTask
{
    /**
     * The import task to wrap.
     */
    private final ITaskRunnable runnable;

    /**
     * Wrap the task and inject the progress monitor.
     *
     * @param runnable plugin contributed task
     */
    public MapsImporterTask(final ITaskRunnable runnable)
    {
        super(TaskGroup.MAPS_IMPORT, runnable.getName());
        setCancelable(runnable.allowCancel());
        this.runnable = runnable;
        runnable.setProgressMonitor(getTaskStatusHandler());
    }

    @Override
    public void doCancel()
    {
        runnable.doCancel();
    }

    @Override
    public void execute() throws Exception
    {
        runnable.run();
    }

    @Override
    public String getExitMessage()
    {
        return runnable.getExitMessage();
    }

    @Override
    public boolean allowExit()
    {
        return runnable.allowExit();
    }
}
