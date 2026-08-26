package gov.takx.core.project;

import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectFactory;

/**
 * Default (standard) implementation of the project factory. This implementation generates instances of
 * {@link Project}.
 *
 */
public class DefaultProjectFactory implements IProjectFactory
{
    @Override
    public IProject createProject(String projectPath, boolean mostRecentProject)
    {
        return new Project(projectPath);
    }
}
