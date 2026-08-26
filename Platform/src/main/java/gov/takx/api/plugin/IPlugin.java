package gov.takx.api.plugin;

/**
 * Base interface for all TAKX plugin classes. Every plugin must have a "type" that is unique among the
 * plugins of its class (map object, connection, io, etc.).
 *
 * @since 1.3
 */
public interface IPlugin extends IRaptorObject
{
    /**
     * Identifies a human readable plugin name. This name is used in various UI expressions.
     *
     * @return plugin name
     */
    String getName();

    /**
     * Get the version number of this plugin in the form of MAJOR.MINOR.PATCH, each of which are expected to be
     * zero or any positive integer.
     *
     * @return Version string for the plugin (for example, "1.0.0").
     * @since 1.3
     */
    String getVersion();
}
