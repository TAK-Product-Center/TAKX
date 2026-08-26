package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableEllipse;

/**
 * Configuration of an ellipse shape.
 *
 */
@NoPluginImplementations
public interface IShapeEllipseMapObject extends IShapeRingMapObject<IMutableEllipse>, IMutableEllipse
{
}
