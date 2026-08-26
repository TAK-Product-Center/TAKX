package gov.takx.core.tasks;

import gov.takx.api.tasks.ICancelableManagedTask;
import gov.takx.api.tasks.TaskCompletionStatus;
import gov.takx.api.tasks.TaskGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Abstract base class for managed tasks that can be canceled.  The task can disallow cancel operation by returning
 * {@code false} from {@link #allowCancel()}. This base class provides a default implementation of
 * {@link #cancel()} that first tests {@link #allowCancel()}, and then calls {@link #doCancel()}.
 * <p/>
 * Subclasses must implement the {@link #doCancel()} method.
 * <p/>
 * Note that, by default, the task is not cancelable.  Call {@link #setCancelable(boolean)} with {@code true} to
 * make the task cancelable.
 *
 * @since 1.42
 */
public abstract class ACancelableManagedTask extends AManagedTask implements ICancelableManagedTask
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected boolean cancelable = false;
    protected boolean canceled = false;

    protected ACancelableManagedTask(TaskGroup taskGroup, String name)
    {
        super(taskGroup, name);
    }

    protected void setCancelable(boolean cancelable)
    {
        this.cancelable = cancelable;
    }

    @Override
    public boolean allowCancel()
    {
        return cancelable;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException If this task is not cancelable, i.e., {@link #allowCancel()} returns
     *                               {@code false}
     */
    @Override
    public void cancel()
    {
        if (!allowCancel()) throw new IllegalStateException(getName() + " is not cancelable");

        try
        {
            doCancel();
        } catch (Exception e)
        {
            logger.error("Exception canceling task: {}", getName(), e);
        }

        getTaskStatusHandler().setCompletionStatus(TaskCompletionStatus.CANCELED);
    }

    /**
     * Perform the actual cancel operation.
     */
    public abstract void doCancel();

    @Override
    public boolean isCanceled()
    {
        return canceled;
    }
}
