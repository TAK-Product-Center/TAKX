package gov.takx.api.apps;

import gov.takx.api.messages.IPluginState;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.core.apps.AppPluginDelegate;
import jakarta.annotation.Nonnull;

/**
 * A runnable instance of an app plugin.
 */
public interface IAppInstance extends IImmutableAppInstance
{
    /**
     * Get the app wrapped by this instance.
     *
     * @return the app.
     */
    @Nonnull
    IAppPlugin getApp();

    /**
     * @return The definition for our app plugin.
     * @since 5.7
     */
    @Nonnull
    IAppPluginDefinition getPluginDefinition();

    /**
     * @return the app plugin delegate tied to this app instance.
     */
    @Nonnull
    AppPluginDelegate getAppDelegate();

    /**
     * @param autoStartMode The auto-start mode to use for this App.
     * @since 5.7
     */
    void setAutoStartMode(@Nonnull AutoStartMode autoStartMode);

    /**
     * @return  The auto-start mode to use for this App
     * @since 5.7
     */
    @Nonnull
    AutoStartMode getAutoStartMode();

    /**
     * Set the app to run in the background or not. If the app is running in the background it will not appear
     * in the app manager, and cannot be started or stopped using the user interface.
     *
     * @param background True if the app should run in the background.
     */
    void setBackground(boolean background);

    /**
     * @return Unique key for this app instance, includes family, type, and version. Useful for sorting.
     */
    @Nonnull
    String getUniqueKey();

    /**
     * Restore the app state from a previously saved copy.
     *
     * @param state State to restore.
     * @since 5.7
     */
    void restoreState(@Nonnull IPluginState state);
}
