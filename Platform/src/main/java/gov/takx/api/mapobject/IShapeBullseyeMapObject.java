package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableBullseye;

/**
 * Interface defining the Bullseye shape map object.
 * @since 5.5
 */
@NoPluginImplementations
public interface IShapeBullseyeMapObject extends IShapeRingMapObject<IMutableBullseye>, IMutableBullseye
{
}
