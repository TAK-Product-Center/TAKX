package gov.takx.api.mapobject;

import gov.tak.api.maps.PointMapItem;

/**
 * Contract for Map Objects that have a single location and represent a point on the map (as compared to shapes).
 *
 * @since 3.0
 */
public interface IPointMapObject extends PointMapItem
{
    String POINT_TYPE = "PointMapItem";
}
