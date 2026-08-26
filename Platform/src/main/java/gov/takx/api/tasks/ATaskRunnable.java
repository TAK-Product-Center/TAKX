package gov.takx.api.tasks;

/**
 * Helper class that is both cancelable and allows exit of TAKX without any disagreement if the task is
 * still running. Extending this class will require implementing the {@link ITaskRunnable#doCancel()} method
 * to provide logic for a user cancelling the task and {@link ITaskRunnable#getName()} to provide a name for
 * the task (must be non-null)
 *
 * @since 2.14
 */
public abstract class ATaskRunnable implements ITaskRunnable
{
    /**
     * The task progress monitor
     */
    protected ITaskProgressMonitor monitor;

    /**
     * {@inheritDoc}
     * <p>
     * Store the progress monitor.
     *
     * @param monitor progress monitor
     */
    @Override
    public void setProgressMonitor(ITaskProgressMonitor monitor)
    {
        this.monitor = monitor;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always {@code true}, forcing classes extending this one to implement {@link ITaskRunnable#doCancel()}
     */
    @Override
    public boolean allowCancel()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always {@code true}, subclasses must override to modify this behavior
     */
    @Override
    public boolean allowExit()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @return Always an empty string since we override {@link #allowExit()} to return true;
     */
    @Override
    public String getExitMessage()
    {
        return "";
    }
}
