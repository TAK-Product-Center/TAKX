package gov.takx.api.platform;

/**
 * All objects that need a "shutdown" sequence to clear state will implement this interface.
 *
 */
public interface IRaptorShutdownListener
{
    /**
     * Called on shutdown of the application to allow listeners to shutdown.
     *
     * @param progressMonitor Progress monitor that can be updated with status of the shutdown
     */
    void onShutdown(IProgressMonitor progressMonitor);
}
