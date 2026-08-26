package gov.takx.api.structures;

import gov.takx.api.mapobject.IShapeMapObject;

/**
 * Contract for {@link IShapeMapObject} to provide metadata and 3D intersection
 * capabilities.
 *
 */
public interface IIntersectableShape extends IIntersectable
{
    IShapeMapObject getShapeMapObject();
}
