package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableRingedShape;

/**
 * Interface defining map objects that can render concentric rings.
 *
 * @since 3.0
 */
@NoPluginImplementations
public interface IShapeRingMapObject<T extends IMutableRingedShape> extends IMutableRingedShape, IShapeMapObject<T>
{
}
