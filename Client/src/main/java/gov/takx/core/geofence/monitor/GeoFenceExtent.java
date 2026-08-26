package gov.takx.core.geofence.monitor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.api.geofence.GeoFence;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Holds the values of a bounding circle for a {@link GeoFence}, supporting quick in/out containment tests.
 *
 * @param center Center point
 * @param furthestPoint Most distant boundary point from center (radius of bounding circle)
 */
public record GeoFenceExtent(@Nonnull IGeoPoint center, double furthestPoint)
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Quick calculation to see if a point lies within the spatial extent of a GeoFence.  This is not a definitive
     * containment test.  The test is just seeing if it is within the bounding circle of all the boundary points
     * for a GeoFence.
     *
     * @param location Location to test
     * @return {@code true} if the given point is within this extent
     */
    public boolean contains(@Nonnull IGeoPoint location)
    {
        final boolean contained = GeoCalculations.distance(center, location) <= furthestPoint;
        logger.debug("{} contains {} = {}", this, location, contained);
        return contained;
    }
}
