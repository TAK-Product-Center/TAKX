package gov.takx.api.plugin;

import gov.takx.api.persist.IPersistableState;

/**
 * Empty persistable state implementation.
 *
 * @since 1.3
 */
public class BasicPluginStateManager implements IPersistableState
{
    /**
     * Default state to store and variable that holds restored state.
     */
    protected String managedState = null;

    @Override
    public String store()
    {
        return managedState;
    }

    @Override
    public void restore(String state)
    {
        managedState = state;
    }
}
