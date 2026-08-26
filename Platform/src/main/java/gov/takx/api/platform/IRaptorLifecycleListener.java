package gov.takx.api.platform;

/**
 * Methods all stateful services/models must implement.
 *
 */
public interface IRaptorLifecycleListener extends IRaptorShutdownListener
{
    // Default implementations are provided so classes implementing this interface only need to override the relevant
    // methods.
    /**
     * Called on project opening to allow listeners to load state information.
     *
     * @param progressMonitor Progress monitor that can be updated with status of loading state
     * @since 3.4 (default implementation added)
     */
    default void onLoadState(IProgressMonitor progressMonitor)
    {
    }

    /**
     * Called on project closing to allow listeners to clear state information.
     *
     * @param progressMonitor Progress monitor that can be updated with status of clearing state
     * @since 3.4 (default implementation added)
     */
    default void onClearState(IProgressMonitor progressMonitor)
    {
    }

    /**
     * {@inheritDoc}
     * @since 3.4 (default implementation added)
     */
    @Override
    default void onShutdown(IProgressMonitor progressMonitor)
    {
    }
}
