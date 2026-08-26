package gov.takx.api.platform;

/**
 * Abstract base to support a runnable where we change the name of the thread executing the runnable and then
 * ensure that it is changed back to its original name when completed.
 *
 * @since 2.0.0
 */
public abstract class NameFixingRunnable implements Runnable
{
    private final String name;

    public NameFixingRunnable(String name)
    {
        this.name = name;
    }

    @Override
    public void run()
    {
        final Thread thread = Thread.currentThread();
        final String oldName = thread.getName();

        thread.setName(name);

        try
        {
            doRun();
        } finally
        {
            thread.setName(oldName);
        }
    }

    /**
     * Implement the actual logic to be performed.
     */
    protected abstract void doRun();
}
