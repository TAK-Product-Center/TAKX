package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.structures.IIntersectableShape;
import gov.takx.api.structures.ISurfaceStructure;

import java.util.Collection;

/**
 * A delegate for implementations of ITerrainCalculator which provides access to core functionality.
 *
 * @since 1.35
 */
@Experimental
public interface ITerrainCalculatorDelegate
{
    /**
     * Get all intersectable shapes.
     *
     * @return a collection of intersectable shapes to be used in occlusion calculations
     */
    Collection<IIntersectableShape> getShapes();

    /**
     * Get surface structures for a Sector of interest.
     *
     * @param sector Sector of interest
     * @return all surface structures found within the sector of interest to be used in occlusion calculations.
     */
    Collection<ISurfaceStructure> getSurfaceStructures(Sector sector);
}
