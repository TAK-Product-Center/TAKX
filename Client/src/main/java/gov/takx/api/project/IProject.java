package gov.takx.api.project;

import gov.takx.api.platform.IProgressMonitor;
import gov.takx.commons.file.IZipFileProgress;

import java.io.IOException;
import java.util.UUID;

/**
 * Interface specification for TAKX projects. A project essentially represents the location of the project
 * files and information from those files (like version).
 * @since 3.4 (renamed from IRaptorProject)
 */
public interface IProject extends IRaptorProjectDescriptor
{
    /**
     * Open the project.  If this project is a "packaged" project, then
     * the package will be expanded and the resulting project will be opened.
     * <p>
     * Note: this method is not for general use, only the project manager should use it.  However, it needs to
     * be in the interface for the factory pattern to work.
     *
     * @throws IOException If any IO failure occurs
     */
    void open() throws IOException;

    /**
     * Close this project, release any resources (like the log file),
     *
     * @param progressMonitor for reporting progress, may be {@code null}
     */
    void close(IProgressMonitor progressMonitor);

    /**
     * Create a package file (a zip file with a .pkgx suffix) that contains this project, optionally including
     * the main imagery source.
     * <p>
     * The package file is created at the {@code packagePath} in a file named projectName.pkgx
     *
     * @param packageName         Name to give the new package.
     * @param packagePath         Destination path of package
     * @param includeImageryCache {@code true} if imagery must be included in the package.
     * @param zipFileProgress     Callback for file zip progress.
     * @since 1.19 (moved here from IPersistService)
     */
    void createPackage(String packageName, String packagePath, boolean includeImageryCache, IZipFileProgress zipFileProgress);

    /**
     * @return the version string from the project preferences.
     */
    String getProjectVersion();

    /**
     * @return The UUID of the project, assigned at project creation
     * @since 2.0.0
     */
    UUID getProjectUUID();

    void saveProjectPreferences();

    boolean isNew();

    void setNew(boolean val);

    /**
     * @return Was this project created by extracting a package.
     * @since 3.0
     */
    boolean isFromPackage();

    /**
     * Set the <em>offline mode</em> for WMS network access. If {@code offlineMode} is {@code true}, then NO
     * network traffic will be allowed for WMS requests. If {@code false}, then normal WMS traffic is allowed.
     *
     * @param offlineMode New offline mode
     */
    void setWMSOfflineMode(boolean offlineMode);

    /**
     * @return The current offline mode setting
     */
    boolean isWMSOfflineMode();

    /**
     * Initialize the preferences on a new project, taking values from the given project metadata.
     *
     * @param projectMetadata Project metadata
     */
    void initializeNewProjectPreferences(IProjectMetadata projectMetadata);
}
