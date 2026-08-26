package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableIrregularShape;
import gov.takx.api.shapes.IMutablePolyline;

/**
 * Configuration for a polyline (path) shape.
 *
 */
@NoPluginImplementations
public interface IShapePathMapObject<T extends IMutableIrregularShape> extends IShapeMapObject<T>, IMutablePolyline
{
}
