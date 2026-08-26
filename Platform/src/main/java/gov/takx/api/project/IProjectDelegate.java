package gov.takx.api.project;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.app.IAppPluginDelegate;

/**
 * Delegate for basic project management.
 * <p>
 * This interface can be accessed via an app plugin delegate by calling {@link IAppPluginDelegate#getProjectDelegate()}.
 *
 * @since 1.4
 */
@NoPluginImplementations
public interface IProjectDelegate
{
    /**
     * Save current project and database.
     *
     * @return boolean success
     */
    boolean saveCurrentProject();

    /**
     * Open an existing project
     *
     * @param name name of project file to open (.projx)
     */
    void openExistingProject(String name);

    /**
     * Get the descriptor interface for the current project
     *
     * @return project descriptor
     */
    IRaptorProjectDescriptor getProjectDescriptor();

    /**
     * Save a snapshot of the globe to the current project directory.
     */
    void saveSnapshot();
}
