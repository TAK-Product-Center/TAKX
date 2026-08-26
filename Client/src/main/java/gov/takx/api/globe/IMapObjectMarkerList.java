package gov.takx.api.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.mapdata.MapPoint;

/**
 * Contract for classes that provide an iterable set of map points for a specific map object.
 *
 * @since 1.13
 */
public interface IMapObjectMarkerList extends Iterable<MapPoint>
{
    /**
     * @return The Map Object whose points are contained in this iterable
     */
    public IMapObject getMapObject();

    /**
     * @return The number of points in this iterable
     */
    public int size();
}
