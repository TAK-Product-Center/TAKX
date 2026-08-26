package gov.takx.api.plugin.synthetic;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IRaptorPlugin;

/**
 * Contract for all "synthetic" plugins.  Map tools, icon resources, etc.
 *
 * @since 2.0.0
 */
@Internal
@NoPluginImplementations
public interface ISyntheticPlugin extends IRaptorPlugin
{
}
