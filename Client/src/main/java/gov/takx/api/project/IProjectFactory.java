package gov.takx.api.project;

/**
 * Factory mechanism for creating project instances.
 *
 */
public interface IProjectFactory
{
    /**
     * Create a project instance on the given project path.
     *
     * @param projectPath       Project path
     * @param mostRecentProject Pass {@code true} if this is the same path as the most recently opened project
     * @return new project instance
     * @since 2.37, added {@code mostRecentProject} parameter
     */
    IProject createProject(String projectPath, boolean mostRecentProject);
}
