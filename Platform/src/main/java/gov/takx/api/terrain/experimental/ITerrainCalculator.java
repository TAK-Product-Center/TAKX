package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.structures.IIntersectable;
import gov.takx.api.structures.IIntersectableShape;

import java.util.Collection;

/**
 * This interface defines a terrain calculator. It allows terrain related calculations such as terrain occlusion,
 * line of sight distance, etc.
 *
 * @since 1.35
 */
@Experimental
public interface ITerrainCalculator
{
    /**
     * Determines if locationA and locationB have any terrain occluding their line of sight.  This assumes
     * the location elevations are heights above the surface.
     *
     * @param locationA location with elevation being the height above the surface in meters
     * @param locationB location with elevation being the height above the surface in meters
     * @return {@code true} if there is terrain occluding their line of sight, or {@code false}if they have
     * direct line of sight.
     * @throws Exception Thrown if there is an issue retrieving the terrain information.
     */
    boolean isTerrainOccluding(ILocation locationA,
                               ILocation locationB)
            throws Exception;

    /**
     * Determines if locationA and locationB have any shapes occluding their line of sight.  This assumes the location
     * elevations are heights above the surface.
     *
     * @param locationA location with elevation being the height above the surface in meters
     * @param locationB location with elevation being the height above the surface in meters
     * @return {@code true} if there are shapes occluding their line of sight, or {@code false}if they have
     * direct line of sight.
     * @throws Exception
     */
    boolean isShapeOccluding(ILocation locationA, ILocation locationB) throws Exception;

    /**
     * Updates the calculator with shapes within an area of interest to be used in occlusion calculations.
     *
     * @param locationA first corner of area of interest
     * @param locationB opposite corner of area of interest
     */
    void updateShapesForArea(ILocation locationA, ILocation locationB);

    /**
     * Updates the calculator with shapes within a Sector of interest to be used in occlusion calculations.
     */
    void updateShapesForSector(Sector sector);

    /**
     * @return All intersectable shapes/structures in the current sector of interest
     * @since 3.0
     */
    Collection<? extends IIntersectable> getIntersectables();

    /**
     * Get intersectable shapes.
     *
     * @return intersectable shapes.
     */
    Collection<IIntersectableShape> getShapes();

    /**
     * Gets the distance between two points (with height above surface in meters).
     * This assumes line of sight distance.
     *
     * @param locationA location with elevation being the height above the surface in meters
     * @param locationB location with elevation being the height above the surface in meters
     * @return The distance in meters between the two locations
     */
    double getLineOfSightDistance(ILocation locationA, ILocation locationB);
}
