package gov.takx.api.registry;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;

/**
 */
@InjectableService
@NoPluginImplementations
public interface IDevicePluginRegistry extends IPluginRegistry<IDevicePlugin, IDevicePluginDefinition>
{
}
