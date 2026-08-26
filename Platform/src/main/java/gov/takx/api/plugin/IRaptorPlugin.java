package gov.takx.api.plugin;

/**
 * Base interface for all TAKX plugin classes. Every plugin must have a "type" that is unique among the
 * plugins of its class (device, connection, io, etc.)
 *
 * @since 1.3
 */
public interface IRaptorPlugin extends IPlugin, IPluginDescriptor
{
    /**
     * @return The resource path of the icon as defined by the plugin, may be empty
     * @since 3.0
     */
    default String getIconPath()
    {
        return "";
    }
}
