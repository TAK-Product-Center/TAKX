package gov.takx.api.plugin.app;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import jakarta.annotation.Nonnull;

/**
 * Contract for App Plugin definitions.  This includes those attributes that are unique to app plugins.
 *
 * @see IPluginDefinition
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IAppPluginDefinition extends IPluginDefinition<IAppPlugin>
{
    /**
     * @return {@code true} If this plugin can run, and should be started, when running headless
     * @since 2.25
     * @deprecated No longer used as TAKX cannot run headless
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    default boolean canRunHeadless()
    {
        return true;
    }

    /**
     * @return The auto-start mode of this plugin
     * @since 5.7
     */
    @Nonnull
    AutoStartMode getAutoStartMode();
}
