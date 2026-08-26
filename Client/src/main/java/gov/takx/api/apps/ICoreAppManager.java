package gov.takx.api.apps;

import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginManager;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Contract for the core app manager.
 *
 * @since 2.0.0
 */
public interface ICoreAppManager extends IAppPluginManager
{
    /**
     * @return Collection of all apps.
     * <p>
     * Covariant return to yield {@link IAppInstance} instead of {@link IImmutableAppInstance}
     * @since 5.7
     */
    @Override
    @Nonnull
    Collection<IAppInstance> getApps();

    /**
     * Get the app Instance for the given app plugin, if any.
     *
     * @param app The app plugin of interest
     * @return App instance, or {@code null} if not installed
     */
    @Nullable
    IAppInstance getAppInstance(@Nonnull IAppPlugin app);

    /**
     * Write the app instance's auto-start mode to preferences.
     *
     * @param appInstance The App instance to process
     * @since 5.7
     */
    void saveAutoStartMode(@Nonnull IAppInstance appInstance);
}
