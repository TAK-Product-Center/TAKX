package gov.takx.api.event;

import gov.takx.api.annotation.Internal;
import gov.takx.api.platform.IProgressMonitor;

/**
 * Fired when the platform is shutting down.
 *
 * @since 2.0.0
 */
public class ShuttingDown implements PlatformEvent
{
    private final IProgressMonitor progressMonitor;

    /**
     * Construct the event.
     */
    @Internal
    public ShuttingDown(IProgressMonitor progressMonitor)
    {
        this.progressMonitor = progressMonitor;
    }

    /**
     * @return progress monitor to report to
     */
    public IProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    public String toString()
    {
        return getClass().getSimpleName();
    }
}
