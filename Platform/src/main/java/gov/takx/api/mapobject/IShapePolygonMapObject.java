package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutablePolygon;

/**
 * Contract for a Map Object defined by a Polygon model.
 */
@NoPluginImplementations
public interface IShapePolygonMapObject extends IShapePathMapObject<IMutablePolygon>, IMutablePolygon
{
    /**
     * Restore the holes from persistence.
     *
     * @since 3.3
     */
    void restoreHoles();
}
