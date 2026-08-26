package gov.takx.module;

import gov.takx.api.plugin.PluginCategory;

/**
 * Simple holder object for plugin definition data extracted from the jar file (manifest or from annotated classes).
 *
 * @since 2.0.0 (converted to record in 5.7)
 */
public record DiscoveredPlugin(PluginCategory pluginCategory, String pluginClassName)
{
}
