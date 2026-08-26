package gov.takx.api.event;

import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.platform.IProgressMonitor;

/**
 * CDI event fired internally to the core to initiate the shutdown of the project-related services.
 * <p>
 * This event is fired after the {@link ProjectClosing} event since that event
 * is visible to plugins and is the notification that services are about to go away, but they are still available
 * at the time of the event.  The StopProjectServices event is fired to actually start the service shutdown.  Many
 * of the observers use a priority setting to ensure that services shutdown in the order needed.
 *
 * @since 2.0.0
 */
public class StopProjectServices extends ProgressAwareEvent
{
    public StopProjectServices(IProgressMonitor progressMonitor)
    {
        super(progressMonitor);
    }
}
