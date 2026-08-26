package gov.takx.core.registry;

import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IDevicePluginRegistry;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry for all Device plugins.
 *
 */
@ApplicationScoped
public class DevicePluginRegistry extends APluginRegistry<IDevicePlugin, IDevicePluginDefinition> implements IDevicePluginRegistry
{
    /**
     * Construct the Device plugin registry.
     */
    public DevicePluginRegistry()
    {
        super(PluginCategory.DevicePlugin);
    }
}
