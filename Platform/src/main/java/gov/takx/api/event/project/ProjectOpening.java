package gov.takx.api.event.project;

import gov.takx.api.platform.IProgressMonitor;

/**
 * This event is fired at the start of the process to open a project.
 *
 * @since 2.0.0
 */
public class ProjectOpening extends ProjectEvent
{
    /**
     * Construct the event with the given project name.
     *
     * @param projectName     Name of the project on which this event is being fired
     * @param progressMonitor Progress monitor to report to
     */
    public ProjectOpening(String projectName, IProgressMonitor progressMonitor)
    {
        super(projectName, progressMonitor);
    }
}
