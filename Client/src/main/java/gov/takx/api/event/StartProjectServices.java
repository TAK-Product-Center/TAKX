package gov.takx.api.event;

import gov.takx.api.platform.IProgressMonitor;

/**
 * CDI Event fired when the project-level services should start to spin up.  Observer's are prioritized
 * to ensure that services start in the proper order.
 *
 * @since 2.0.0
 */
public class StartProjectServices extends ProgressAwareEvent
{
    public StartProjectServices(IProgressMonitor progressMonitor)
    {
        super(progressMonitor);
    }
}
