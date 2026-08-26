package gov.takx.api.plugin.app;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.event.app.AppStarted;
import gov.takx.api.event.app.AppStopped;
import gov.takx.api.plugin.IRaptorPlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * The App Plugin Manager provides access to the defined Apps, which in turn provide access to starting and stopping them.
 * <p>
 * To obtain notification of significant App events (start, stop), use the standard {@code @Observes} pattern on the following events:
 * <ul>
 * <li>{@link AppStarted AppStarted}</li>
 * <li>{@link AppStopped AppStopped}</li>
 * </ul>
 *
 * @since 1.24
 */
@InjectableService
@NoPluginImplementations
public interface IAppPluginManager
{
    /**
     * Declares the auto-start handling to be applied to plugins when project services start up.
     * @apiNote All Apps are automatically "installed" as of version 5.7.  This value controls if/how an App is started.
     * @since 5.7
     */
    enum AutoStartMode
    {
        /** Nothing is done, the App is not started. */
        none,

        /** The App is started. */
        start,

        /** The App is started in the background, meaning it is not listed in the Plugins UI. */
        background;

        /**
         * Find the {@link AutoStartMode mode} with the given name.
         *
         * @param name Name to search for
         * @return {@link AutoStartMode mode} with that name, or {@code null} if not found
         */
        @Nullable
        public static AutoStartMode getByValue(String name)
        {
            try
            {
                return valueOf(name);
            } catch (IllegalArgumentException e)
            {
                return null;
            }
        }
    }

    /**
     * @return Collection of all app instances.
     * @since 5.7
     */
    @Nonnull
    Collection<? extends IImmutableAppInstance> getApps();

    /**
     * Get a specific app instance by family/type.
     *
     * @param family  Family of the plugin you're looking for
     * @param type    Type of the plugin you're looking for
     * @return IImmutableAppInstance of the plugin matching inputs, otherwise {@code null}
     * @since 5.7
     */
    @Nullable
    IImmutableAppInstance getApp(@Nonnull String family, @Nonnull String type);

    /**
     * Get all the installed apps.
     *
     * @return Collection of installed apps.
     * @deprecated since 5.7, all apps are automatically installed, use {@link #getApps()} instead
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Nonnull
    default Collection<? extends IImmutableAppInstance> getInstalledApps()
    {
        return getApps();
    }

    /**
     * Get a specific installed app by family/type/version
     *
     * @param family  Family of the plugin you're looking for
     * @param type    Type of the plugin you're looking for
     * @param version Version of the plugin you're looking for
     * @return IImmutableAppInstance of the plugin matching inputs, otherwise {@code null}
     * @since 2.0.0
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Nullable
    default IImmutableAppInstance getInstalledApp(@Nonnull String family, @Nonnull String type, @Nonnull String version)
    {
        return getApp(family, type);
    }

    /**
     * @param appInstance The App instance to update
     * @param autoStartMode The auto-start mode to use for this App
     * @since 5.7
     */
    @SuppressWarnings("unused")
    void setAutoStartMode(@Nonnull IImmutableAppInstance appInstance, @Nonnull AutoStartMode autoStartMode);

    /**
     * Get a specific app that is not installed
     *
     * @param family  Family of the plugin you're looking for
     * @param type    Type of the plugin you're looking for
     * @param version Version of the plugin you're looking for
     * @return Always {@code null}
     * @deprecated since 5.7, no replacement, all apps are automatically installed
     */
    @SuppressWarnings("unused")
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Nullable
    default IAppPlugin getUninstalledApp(@Nonnull String family, @Nonnull String type, @Nonnull String version)
    {
        return null;
    }

    /**
     * Get an installed app instance given the class of the app plugin.
     *
     * @param appClass App plugin class
     * @return App instance, if it is installed, {@code null} otherwise
     * @since 1.25
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Nullable
    IImmutableAppInstance getInstalledAppByClass(@Nonnull Class<?> appClass);

    /**
     * Get a list of all the uninstalled apps.
     *
     * @return Empty collection
     * @since 2.0.0
     * @deprecated since 5.7, no replacement, all apps are automatically installed
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Nonnull
    default Collection<? extends IRaptorPlugin> getUninstalledApps()
    {
        return Collections.emptyList();
    }

    /**
     * Install an app.
     *
     * @param family  App/Plugin family.
     * @param type    App/plugin type.
     * @param version App/plugin version (ignored).
     * @return Instance of the app.
     * @throws InstantiationException If the app class cannot be instantiated.
     * @since 2.0.0
     * @deprecated since 5.7, no replacement, all apps are automatically installed
     */
    @SuppressWarnings({"UnusedDeclaration", "RedundantThrows"})
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Nullable
    default IImmutableAppInstance installApp(@Nonnull String family, @Nonnull String type, @Nonnull String version) throws IllegalArgumentException, InstantiationException
    {
        return getApp(family, type);
    }

    /**
     * Install an app, and optionally set it to auto-start.
     *
     * @param family     App/Plugin family.
     * @param type       App/plugin type.
     * @param version    App/plugin version.
     * @param autoStart  true if the app should auto-start.
     * @param background true if the app should run in the background.
     * @return Instance of the app.
     * @throws InstantiationException If the app class cannot be instantiated.
     * @since 2.0.0
     * @deprecated since 5.7, no replacement, all apps are automatically installed. To configure auto-start,
     *             use {@link #setAutoStartMode(IImmutableAppInstance, AutoStartMode)} instead
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Nullable
    IImmutableAppInstance installApp(@Nonnull String family, @Nonnull String type, @Nonnull String version, boolean autoStart, boolean background) throws IllegalArgumentException, InstantiationException;

    /**
     * Add a listener for app lifecycle events.
     *
     * @param listener The listener.
     */
    void addListener(@Nonnull IAppListener listener);

    /**
     * Remove a listener.
     *
     * @param listener The listener to remove.
     */
    void removeListener(@Nonnull IAppListener listener);
}
