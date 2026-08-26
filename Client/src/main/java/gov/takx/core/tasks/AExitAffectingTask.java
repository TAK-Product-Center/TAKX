package gov.takx.core.tasks;

import gov.takx.api.tasks.IExitAffectingTask;
import gov.takx.api.tasks.TaskGroup;

/**
 * Abstract base class for tasks that can affect the exit operation of TAKX.
 *
 * @since 1.42
 */
public abstract class AExitAffectingTask extends ACancelableManagedTask implements IExitAffectingTask
{
    protected AExitAffectingTask(TaskGroup taskGroup, String name)
    {
        super(taskGroup, name);
    }

    @Override
    public String getExitMessage()
    {
        return null;
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
}
