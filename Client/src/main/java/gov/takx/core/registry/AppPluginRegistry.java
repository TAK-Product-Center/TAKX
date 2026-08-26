package gov.takx.core.registry;

import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.registry.IAppPluginRegistry;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry for all App plugins.
 *
 */
@ApplicationScoped
public class AppPluginRegistry extends APluginRegistry<IAppPlugin, IAppPluginDefinition> implements IAppPluginRegistry
{
    /**
     * Create the app plugin registry and manager.
     */
    public AppPluginRegistry()
    {
        super(PluginCategory.AppPlugin, false);
    }
}
