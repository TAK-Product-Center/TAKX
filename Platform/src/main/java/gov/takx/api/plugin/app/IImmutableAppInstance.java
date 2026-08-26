package gov.takx.api.plugin.app;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * The basic interrogation and control operations for an App Instance.  The app can be started, stopped, and shutdown,
 * but its properties may not be changed.
 *
 * @since 1.24
 */
public interface IImmutableAppInstance
{
    /**
     * Get the app name.
     *
     * @return The app's name.
     */
    String getName();

    /**
     * Get the app version.
     *
     * @return The app's version string.
     */
    String getVersion();

    /**
     * Get the app's description string.
     *
     * @return The app description.
     */
    String getDescription();

    /**
     * Get the app's family string
     *
     * @return The app's family
     */
    String getFamily();

    /**
     * Get the app's type string
     *
     * @return The app's type
     */
    String getType();

    /**
     * Is the instance running?
     *
     * @return true if the instance is running.
     */
    boolean isRunning();

    /**
     * Start the app. This method may throw an exception if the app is not able to start.
     *
     * @throws Exception If the app is unable to start. The message in the exception will be
     *                   displayed to the user in an error window.
     */
    void start() throws Exception;

    /**
     * Stop the app. This method may throw an exception if the app is not able to stop.
     *
     * @throws Exception If the app is unable to stop. The message in the exception will be
     *                   displayed to the user in an error window.
     */
    void stop() throws Exception;

    /**
     * Uninstall the app.
     *
     * @deprecated since 5.7, no replacement, Apps can no longer be uninstalled
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    void uninstall() throws Exception;

    /**
     * Shutdown the app.
     *
     * @throws Exception If anything goes wrong.
     */
    void shutdown() throws Exception;

    /**
     * Does the app auto-start?
     *
     * @return true if the app auto-starts.
     */
    boolean isAutoStart();

    /**
     * Is the app running in the background? A background app is not visible in the app manager, and
     * cannot be started and stopped using the user interface.
     *
     * @return True if the app is running in the background.
     */
    boolean isBackground();
}
