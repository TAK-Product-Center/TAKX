package gov.takx.core.project;

import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectFactory;

/**
 * This factory generates projects that are "empty" in that they don't actually care about or inspect a
 * project location on disk.  They are simply a shell to provide access to a project session, which contains
 * services, like the persist service, but without any real disk footprint.
 *
 */
public class EmptyProjectFactory implements IProjectFactory
{
    @Override
    public IProject createProject(String projectPath, boolean mostRecentProject)
    {
        return new EmptyProject(projectPath, mostRecentProject);
    }
}
