package gov.takx.api.project;

import gov.takx.api.platform.IProgressMonitor;
import gov.takx.core.project.ProjectSelection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The project manager is responsible for naming, locating, and creating TAKX Projects.
 *
 */
public interface IProjectManager extends IProjectInfoProvider
{
    /**
     * Open the named project.
     *
     * @param projectName Name of project to open/create
     * @return {@code true} if opened ok, {@code false} if canceled due to a declined upgrade
     * @since 2.37
     */
    boolean openProject(@Nonnull String projectName) throws Exception;

    /**
     * Open a project as specified in the given projectSelection.  No user interaction is performed.
     *
     * @param projectSelection Selection of project to open/create
     * @param progressMonitor  Progress monitor
     * @return {@code true} if opened ok, {@code false} if canceled due to a declined upgrade
     * @throws IOException If the project open fails
     * @since 2.37
     */
    boolean openProject(@Nonnull ProjectSelection projectSelection, @Nullable IProgressMonitor progressMonitor) throws IOException;

    /**
     * Open a project as specified in the given projectSelection.  This method is strictly here to support the
     * unit test framework which requires custom database setup as part of the project open sequence.
     *
     * @param projectSelection Selection of project to open/create
     * @param progressMonitor  Progress monitor
     * @param dbInitializer    Custom database initialization to run, may be {@code null}
     * @param loadServices     Pass {@code true} to call loadState on the project session, {@code false} to NOT call it
     * @return {@code true} if opened ok, {@code false} if canceled due to a declined upgrade
     * @throws IOException If the project open fails
     * @since 2.37
     */
    boolean openProjectForTest(@Nonnull ProjectSelection projectSelection, @Nullable IProgressMonitor progressMonitor,
                               @Nullable Runnable dbInitializer, boolean loadServices)
            throws IOException;

    /**
     * Save the currently open project, if any.  If called with no open project, nothing is done.
     *
     * @param progressMonitor for reporting progress
     * @since 2.37
     */
    void saveProject(IProgressMonitor progressMonitor);

    /**
     * Close the currently open project, if any.  If called with no open project, nothing is done.
     *
     * @param progressMonitor for reporting progress
     */
    void closeProject(IProgressMonitor progressMonitor);

    /**
     * @return the currently open project, {@code null} if no project is open
     */
    IProject getCurrentOpenProject();

    /**
     * @return {@code true} if there is a currently open project session
     * @since 3.4
     */
    boolean isValidSession();

    /**
     * @return The full path to the directory under which all project directories are created
     */
    File getProjectsDirectory();

    /**
     * Get the full path to the directory holding the given project.
     *
     * @param projectName Name of project
     * @return full path to project directory
     */
    File getProjectDirectory(String projectName);

    /**
     * Get the full path to the project file (.projx) for the given project.
     *
     * @param projectName The name of the project
     * @return the full path to the project file
     */
    String getProjectFilePath(String projectName);

    /**
     * Extract the project name from the path to the project (.projx) file.
     *
     * @param projectFilePath Path to the project file
     * @return project name
     */
    String getProjectNameFromPath(String projectFilePath);

    /**
     * Determines whether or not a given name can be used as a new project name. If a project with that name already
     * exists with the given name or {@code projectName} is {@code null} or empty it returns false
     *
     * @param projectName The name to check for validity
     * @return boolean whether name is valid or not
     */
    boolean isValidNewProjectName(@Nonnull String projectName);

    /**
     * @return {@code true} if the named project directory exists
     * @see #getProjectDirectory(String)
     */
    boolean doesProjectExist(@Nonnull String projectName);

    /**
     * Gets the default name for a new project, the current date/time as a string.
     *
     * @return The default project name (file name only, not path)
     */
    String getDefaultProjectName();

    /**
     * Set the factory to use for generating new project instances.
     *
     * @param projectFactory factory to use
     */
    void setProjectFactory(IProjectFactory projectFactory);

    /**
     * @return the path to the project last opened by the user.  If no project is recorded, then the empty
     * string is returned.
     */
    String getLastOpenedProjectPath();

    /**
     * Clear (set to empty) the last opened project path in the preferences.
     */
    void clearLastOpenedProjectPath() throws IOException;

    /**
     * @return the name of the last opened project if the project directory exists, empty string otherwise
     */
    String getLastOpenProjectNameIfExists();

    /**
     * @return List of File for recently opened projects
     * @since 2.37
     */
    List<File> getRecentProjects();

    /**
     * Save the current project under a new name.  This consists of the following steps:
     * <ol>
     * <li>Close the current project without a backup, marking it to revert</li>
     * <li>Copy the current project to the new name</li>
     * <li>Open the newly copied project</li>
     * </ol>
     *
     * @param newProjectFilePath Full path to the new .projx file
     * @param progressMonitor    Progress monitor
     * @since 2.37
     */
    void saveProjectAs(String newProjectFilePath, IProgressMonitor progressMonitor) throws Exception;

    /**
     * Create a package file (a zip file with a .pkgx suffix) that contains this project, optionally including
     * the main imagery source.
     * <p>
     * The package file is created at the {@code packagePath} in a file named {@code [packageName].pkgx}
     *
     * @param packageName         Name to give the new package
     * @param packagePath         Destination path of package
     * @param includeImageryCache {@code true} if imagery must be included in the package
     * @param progressMonitor     Callback for file zip progress
     * @since 2.37
     */
    void createPackage(String packageName, String packagePath, boolean includeImageryCache, IProgressMonitor progressMonitor);
}
