package gov.takx.api.event.project;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.platform.IProgressMonitor;

/**
 * This is the base class for all events relating to the project lifecycle.
 * Events are fired as an observable event, using {@link jakarta.enterprise.event.Observes}.
 * <p>
 * The sequence of project events is as follows (some sub-sequences can be fired repeatedly):
 * <ul>
 * <li>ProjectOpening</li>
 * <li>Opened</li>
 * <li>Saving</li>
 * <li>Saved</li>
 * <li>Packaging</li>
 * <li>Packaged</li>
 * <li>Closing</li>
 * <li>Closed</li>
 * </ul>
 *
 * @since 2.0.0
 */
public class ProjectEvent implements PlatformEvent
{
    private final String projectName;
    private final IProgressMonitor progressMonitor;

    /**
     * Construct the event with the given project name.
     *
     * @param projectName     Name of the project on which this event is being fired
     * @param progressMonitor Progress monitor to report to
     */
    protected ProjectEvent(String projectName, IProgressMonitor progressMonitor)
    {
        this.projectName = projectName;
        this.progressMonitor = progressMonitor;
    }

    /**
     * @return Project name
     */
    public String getProjectName()
    {
        return projectName;
    }

    /**
     * @return progress monitor to report to
     */
    public IProgressMonitor getProgressMonitor()
    {
        return progressMonitor;
    }

    /**
     * Override this to provide subclass-specific toString details.
     *
     * @return {@code null}
     */
    protected String getToStringDetails()
    {
        return null;
    }

    public String toString()
    {
        final String details = getToStringDetails();

        return getClass().getSimpleName() + "[" + projectName + (details != null ? ("," + details) : "") + "]";
    }
}
