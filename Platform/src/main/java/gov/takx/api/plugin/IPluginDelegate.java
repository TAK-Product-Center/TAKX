package gov.takx.api.plugin;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.globe.IGlobeNavigationService;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.platform.IExperimentalServices;
import gov.takx.api.project.IProjectAssistant;
import gov.takx.api.propertymanager.ISystemPropertyManager;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.tasks.ITaskRunnable;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.toolbar.IToolbarManager;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.platform.utils.ILengthMeasurer;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Common ancestor of all plugin delegate classes. This interface defines services that all plugins have access to.
 *
 */
@SuppressWarnings("unused") // called from plugins
@NoPluginImplementations
public interface IPluginDelegate extends IGlobeNavigationService
{
    /**
     * @return The current project's name.
     */
    String getProjectName();

    /**
     * The current platform version. The {@link PlatformVersion} instance only populates MAJOR.MINOR.RELEASE and does
     * not represent the build.
     *
     * @return The platform version
     * @since 5.0
     */
    PlatformVersion getPlatformVersion();

    /**
     * Get the system property manager.
     *
     * @return system property manager
     * @deprecated Use {@link gov.takx.platform.preferences.PreferenceEntry} and
     * {@link gov.takx.platform.preferences.UserPreferenceEntry} directly instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    ISystemPropertyManager getSystemPropertyManager();

    /**
     * Access read-only metadata about installed TAKX plugins. This service does not provide any means of editing
     * any existing plugin data, nor does it provide direct access to the plugin. Similarly, it is not possible to
     * dynamically install or uninstall plugins via the plugin manager.
     * <p>
     * Every {@link IRaptorPlugin} data returned from the IPluginManager relies on composition, wrapping and protecting the
     * actual plugin to ensure these contractual obligations are enforced.
     *
     * @return Plugin Manager
     * @since 1.4
     */
    IPluginInspector getPluginInspector();

    /**
     * Access read-only metadata about plugin definitions within TAKX. The inspector returned will have been
     * configured to only return definitions from the specified plugin categories.
     *
     * @param pluginCategories Plugin categories to access
     * @return Initialized plugin inspector
     * @since 2.0.0
     */
    IPluginInspector getPluginInspector(PluginCategory... pluginCategories);

    /**
     * Obtains a length measurement utility that is useful for measuring distances.  The distance can be measured
     * as line of sight or terrain following.
     *
     * @return Length Measurer Tool
     * @since 1.5
     */
    ILengthMeasurer getLengthMeasurer();

    /**
     * Get a reference to the virtual file system (VFS) associated with this delegate's plugin.
     *
     * @return a file system object.
     * @since 1.3
     */
    IVirtualFileSystem getVirtualFileSystem();

    /**
     * Get a reference to the virtual file system (VFS) associated with this delegate's plugin.
     *
     * @param scope The scope of the requested VFS (Project, User, or Global)
     * @return a file system object.
     * @since 1.16
     */
    IVirtualFileSystem getVirtualFileSystem(VFSScope scope);

    /**
     * Determines if the plugin is operating within a valid project session. Generally, only an App Plugin would
     * require this information since apps are long-lived and may be started prior to establishing a session, and run
     * after a project session has been closed.
     *
     * @return {@code true}, if a valid TAKX project session exists.
     * @since 1.13
     */
    boolean isValidProjectSession();

    /**
     * Get a reference to the Favorites manager. A Favorite is like a bookmark; it is visible in the user interface
     * under the Favorites tab on the right side of the app frame.  A favorite allows a user to quickly navigate
     * the globe to a very precise view.  The Favorites Manager allows a plugin to define, remove, and list favorites.
     *
     * @return Favorites manager
     */
    IFavoritesService getFavoritesService();

    /**
     * Get the geo ping service. A geo ping is a temporary visual cue on the globe that is used to draw a
     * user's attention to a location. The ping will disappear after a configured amount of time has passed.
     *
     * @return The ping service.
     */
    IGeoPingService getGeoPingService();

    /**
     * Get the timer service.
     *
     * @return Timer service
     * @since 1.3
     */
    ITimerService getTimerService();

    /**
     * Get the Media Manager.
     *
     * @return The Media Manager.
     * @since 2.7
     */
    IMediaManager getMediaManager();

    /**
     * Obtain the TAKX Executor service.  Contains internal methods to do things like cleaning up and closing
     * the persist service for the thread after execution.
     *
     * @return the executor service
     * @since 1.49
     */
    ScheduledExecutorService getExecutorService();

    /**
     * Gets the toolbar manager.
     *
     * @return Toolbar manager
     * @since 1.14
     */
    IToolbarManager getToolbarManager();

    /**
     * Returns a new {@link IProjectAssistant} object that is capable of inspecting the metadata
     * included with projects (or packaged projects) and importing map objects into the active project.
     *
     * @return A class containing the extracted project metadata.
     * @since 1.16
     */
    IProjectAssistant getProjectAssistant();

    /**
     * Return the directory where program data is stored. This is, in order of priority, the directory either chosen at
     * runtime, inferred from a previous installation or set via environment variable.
     *
     * @return The directory where data is stored.
     * @since 1.14
     */
    String getProgramDataDirectory();

    /**
     * Return the experimental services available.  These services are not guaranteed to be stable and should not be
     * used in a production environment.  The services are not guaranteed to be present in the next version.
     *
     * @return IExperimentalServices
     * @since 1.35
     * @deprecated Inject services directly instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    IExperimentalServices getExperimentalServices();

    /**
     * Register a process that should be killed if it's orphaned after an application crash.
     *
     * @param pid The process' id.
     */
    void registerExternalProcess(int pid);

    /**
     * Get the directory at which the contents of the plugin's jar file have been mounted (exploded).  The directory
     * will be the equivalent of the jar file, with the same file/directory structure.
     * <p>
     * Note that this is only relevant for plugin jar files that contain a nested {@code "lib"} or {@code "native"}
     * directory. For all others, this will return {@code null}.
     *
     * @return Jar mount point, {@code null} if not mounted
     */
    File getJarMountPoint();

    /**
     * Submit a task to be run.  May or may not be run immediately depending on how many tasks are currently in
     * the queue.  {@link ITaskRunnable#getName()} must return a non-null string.
     *
     * @param runnable the task runnable
     * @since 2.14
     */
    void submitTask(ITaskRunnable runnable);

    /**
     * Invokes the plugin's {@link IPersistableState#store()} method (if the plugin implements {@link IPersistableState}).
     * <p>
     * This method provides a mechanism for plugins to have their saved state string updated in the database
     * whenever desired. It is best utilized during major changes in a plugin's state, so it is completely up to the
     * plugin when to trigger the database operation to save.
     * <p>
     * Since a call to this method will result in a call to the plugin's {@code store()} method, the developer
     * needs to balance the cost of saving state with the frequency of such save operations.
     *
     * @since 2.36
     */
    default void doStore()
    {
    }
}
