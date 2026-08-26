package gov.takx.core.project;

/**
 * This class holds the data from a user's selection of a project, either new or re-selected, to be opened.
 * For new projects, we hold the project descriptor data as well.
 *
 * @since 1.25
 */
public class ProjectSelection
{
    private String projectPath;
    private ProjectMetadata projectMetadata;
    private boolean newProject = false;
    private boolean canceled = false;
    private Boolean wmsOffline = null;

    /**
     * Factory method to make a project selection indicating the user canceled the operation.
     *
     * @return properly configured ProjectSelection
     */
    public static ProjectSelection makeCanceled()
    {
        ProjectSelection ps = new ProjectSelection();
        ps.canceled = true;
        return ps;
    }

    /**
     * Factory method to make a project selection indicating the user wants to create a new project.
     *
     * @param projectPath     Path to the project to create
     * @param projectMetadata ProjectMetadata for the new project
     * @param wmsOffline      {@code true} if the network access to WMS servers should be disabled
     * @return properly configured ProjectSelection
     */
    public static ProjectSelection makeNew(String projectPath, ProjectMetadata projectMetadata, boolean wmsOffline)
    {
        ProjectSelection ps = new ProjectSelection();
        ps.newProject = true;
        ps.projectPath = projectPath;
        ps.projectMetadata = projectMetadata;
        ps.wmsOffline = wmsOffline;

        return ps;
    }

    /**
     * Factory method to make a project selection indicating the user wants to open an existing project.
     * The "WMS offline" mode in the existing project's preferences will be used.
     *
     * @param projectPath Path to the project selected
     * @return properly configured ProjectSelection
     * @since 3.3
     */
    public static ProjectSelection makeExisting(String projectPath)
    {
        ProjectSelection ps = new ProjectSelection();
        ps.projectPath = projectPath;

        return ps;
    }

    /**
     * Factory method to make a project selection indicating the user wants to open an existing project.
     *
     * @param projectPath Path to the project selected
     * @param wmsOffline  {@code true} if the network access to WMS servers should be disabled
     * @return properly configured ProjectSelection
     */
    public static ProjectSelection makeExisting(String projectPath, boolean wmsOffline)
    {
        ProjectSelection ps = new ProjectSelection();
        ps.projectPath = projectPath;
        ps.wmsOffline = wmsOffline;

        return ps;
    }

    /**
     * @return {@code true} if the user canceled the project selection operation
     */
    public boolean isCanceled()
    {
        return canceled;
    }

    /**
     * @return The project metadata (as filled out by the user)
     */
    public ProjectMetadata getProjectMetadata()
    {
        return projectMetadata;
    }

    /**
     * @return The path to the project to open/create
     */
    public String getProjectPath()
    {
        return projectPath;
    }

    /**
     * @return {@code true} if this is a new project to create, {@code false} if it is an existing project
     */
    public boolean isNewProject()
    {
        return newProject;
    }

    /**
     * This is only relevant if a value was specified at construction.
     *
     * @return {@code true} if access to WMS servers should be disabled
     * @see #isWmsOfflineSet()
     */
    public boolean isWmsOffline()
    {
        return wmsOffline;
    }

    /**
     * @return {@code true} if a value for {@code wmsOffline} was specified.
     * @since 3.3
     */
    public boolean isWmsOfflineSet()
    {
        return wmsOffline != null;
    }
}
