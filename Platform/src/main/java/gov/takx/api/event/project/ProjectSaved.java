package gov.takx.api.event.project;

import gov.takx.api.platform.IProgressMonitor;

/**
 * This event is fired at the end of the process to save the currently open project.
 *
 * @since 2.0.0
 */
public class ProjectSaved extends ProjectEvent
{
    /**
     * Construct the event with the given project name.
     *
     * @param projectName     Name of the project on which this event is being fired
     * @param progressMonitor Progress monitor to report to
     */
    public ProjectSaved(String projectName, IProgressMonitor progressMonitor)
    {
        super(projectName, progressMonitor);
    }
}
