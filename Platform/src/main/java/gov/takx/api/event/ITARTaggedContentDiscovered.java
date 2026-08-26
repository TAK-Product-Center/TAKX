package gov.takx.api.event;

import gov.takx.api.plugin.IPluginDefinition;

/**
 * Event fired when a plugin has been tagged with metadata indicating it is ITAR (Export) controlled.
 *
 * @since 2.0.0
 */
public class ITARTaggedContentDiscovered implements PlatformEvent
{
    private final IPluginDefinition pluginDefinition;

    public ITARTaggedContentDiscovered(IPluginDefinition pluginDefinition)
    {
        this.pluginDefinition = pluginDefinition;
    }

    public IPluginDefinition getPluginDefinition()
    {
        return pluginDefinition;
    }

    public String toString()
    {
        return getClass().getSimpleName() + "[" + pluginDefinition.getUniqueKey() + "]";
    }
}
