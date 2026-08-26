package gov.takx.api.event.project;

import gov.takx.api.platform.IProgressMonitor;

/**
 * This event is fired at the beginning of the process to save the currently open project as a package.
 *
 * @since 2.0.0
 */
public class ProjectPackaging extends ProjectSaving
{
    /**
     * Construct the event with the given project name.
     *
     * @param projectName     Name of the project on which this event is being fired
     * @param progressMonitor Progress monitor to report to
     */
    public ProjectPackaging(String projectName, IProgressMonitor progressMonitor)
    {
        super(projectName, progressMonitor);
    }
}
