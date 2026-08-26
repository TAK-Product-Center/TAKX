package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableRectangle;

/**
 * Configuration for a rectangle shape.
 *
 */
@NoPluginImplementations
public interface IShapeRectangleMapObject extends IShapeMapObject<IMutableRectangle>, IMutableRectangle
{
}
