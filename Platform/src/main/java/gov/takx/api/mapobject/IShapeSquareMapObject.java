package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableSquare;

/**
 * Configuration for a square shape.
 *
 */
@NoPluginImplementations
public interface IShapeSquareMapObject extends IShapeMapObject<IMutableSquare>, IMutableSquare
{
}
