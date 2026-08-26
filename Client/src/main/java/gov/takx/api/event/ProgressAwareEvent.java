package gov.takx.api.event;

import gov.takx.api.platform.IProgressMonitor;

/**
 * Base class for events that offer access to a progress monitor.
 *
 * @since 2.0.0
 */
public class ProgressAwareEvent
{
    private final IProgressMonitor progressMonitor;

    protected ProgressAwareEvent(IProgressMonitor progressMonitor)
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
}
