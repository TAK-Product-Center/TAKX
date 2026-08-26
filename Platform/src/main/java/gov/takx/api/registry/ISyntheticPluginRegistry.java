package gov.takx.api.registry;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.synthetic.ISyntheticPlugin;
import gov.takx.api.plugin.synthetic.ISyntheticPluginDefinition;

/**
 * Registry for synthetic plugins.  These are not actual plugin instances, they are defined by external resources
 * like icons and map tools.
 *
 * @since 2.0.0
 */
@InjectableService
@NoPluginImplementations
public interface ISyntheticPluginRegistry extends IPluginRegistry<ISyntheticPlugin, ISyntheticPluginDefinition>
{
    /**
     * Generate all the synthetic plugin definitions.  These come from Icon resources provided by App and Device
     * plugins (previously loaded) and from icon resources located in the "resources" directory.
     */
    void generateSyntheticPlugins();
}
