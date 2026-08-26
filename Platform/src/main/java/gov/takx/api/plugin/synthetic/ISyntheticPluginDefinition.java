package gov.takx.api.plugin.synthetic;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.device.IDevicePluginDefinition;

/**
 * Contract for all "synthetic" plugin definitions.  Synthetic definitions are created for elements like icon
 * resources and drawing tools that can be used to create a Map Object, but are not defined by actual plugins.
 *
 * @since 2.0.0
 */
@Internal
@NoPluginImplementations
public interface ISyntheticPluginDefinition extends IDevicePluginDefinition
{
}
