package gov.takx.api.plugin.app;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Listener interface for plugins that monitor App lifecycle events.
 *
 * @since 1.24
 */
public interface IAppListener
{
    /**
     * Called when an app is installed.  This is only called when an App is hot deployed.
     *
     * @param app The app that was installed
     * @since 3.4 (default implementation added)
     */
    default void onAppInstalled(IImmutableAppInstance app)
    {
    }

    /**
     * Never called.
     *
     * @param app The app that was uninstalled
     * @since 3.4 (default implementation added)
     * @deprecated since 5.7, no replacement, apps cannot be uninstalled
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    default void onAppUninstalled(IImmutableAppInstance app)
    {
    }

    /**
     * Called when an app is started.
     *
     * @param app The app that was started
     * @since 3.4 (default implementation added)
     */
    default void onAppStarted(IImmutableAppInstance app)
    {
    }

    /**
     * Called when an app is stopped.
     *
     * @param app The app that was stopped
     * @since 3.4 (default implementation added)
     */
    default void onAppStopped(IImmutableAppInstance app)
    {
    }
}
