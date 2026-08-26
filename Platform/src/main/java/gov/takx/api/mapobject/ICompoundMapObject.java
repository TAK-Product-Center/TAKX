package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for map objects that are defined by their relationship with other map objects (e.g. a route and its
 * waypoints or a shape with holes).
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface ICompoundMapObject extends IMapObject
{
    /**
     * A set of map object options to use for the component map objects as needed to ensure they don't show up in the
     * explorer or negatively affect performance.
     */
    MapObjectOptions COMPONENT_MO_OPTIONS = new MapObjectOptions(MapObjectOption.ExcludeFromExplorer,
            MapObjectOption.NoAlarms, MapObjectOption.NoStyledTracks, MapObjectOption.NoEventLogs,
            MapObjectOption.NoPluginState);
}
