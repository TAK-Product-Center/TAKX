package gov.takx.api.mapobject;

/**
 * Constants defining well known Map Object group names.
 *
 * @since 2.0.0
 */
public class MapObjectGroupNames
{
    /**
     * Indicates a Map Object defined by a device plugin.
     */
    public static final String DEVICES_GROUP = "Devices";

    /**
     * Indicates a Map Object created from a file.
     */
    public static final String FILES_GROUP = "Files";

    /**
     * Indicates a Map Object representing a drawing tool (such as a shape).
     *
     * @since 3.0 (moved from SyntheticPluginRegistry)
     */
    public static final String MAP_TOOLS = "Map Tools";

    /**
     * Group for routes and waypoint map objects.
     *
     * @since 3.0
     */
    public static final String NAVIGATION_GROUP = "Navigation";
}
