package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.structures.IIntersectable;
import gov.takx.api.structures.IIntersectableShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic Terrain operations such as occlusion and Line of sight distance
 *
 * @since 1.35
 */
@Experimental
public class TerrainCalculator implements ITerrainCalculator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ITerrainCalculatorDelegate delegate;
    private Collection<? extends IIntersectable> surfaceStructures = new ArrayList<>();
    private Collection<IIntersectableShape> shapes = new ArrayList<>();
    private final Terrain terrain;

    public TerrainCalculator(Terrain terrain, ITerrainCalculatorDelegate delegate)
    {
        this.terrain = terrain;
        this.delegate = delegate;
    }

    @Override
    public boolean isTerrainOccluding(ILocation locationA, ILocation locationB)
    {
        return isTerrainOccluding(
                locationA.getLatitude(),
                locationA.getLongitude(),
                locationA.getElevation(),
                locationB.getLatitude(),
                locationB.getLongitude(),
                locationB.getElevation());
    }

    @Override
    public boolean isShapeOccluding(ILocation locationA, ILocation locationB)
    {
        // Compute the position of the selected location (incorporate its height).
        final Position positionA = Position.fromDegrees(
                locationA.getLatitude(),
                locationA.getLongitude(),
                locationA.getElevation());

        Vec4 pointAVec4 = terrain.getSurfacePoint(
                positionA.getLatitude(),
                positionA.getLongitude(),
                locationA.getElevation());

        final Position positionB = Position.fromDegrees(
                locationB.getLatitude(),
                locationB.getLongitude(),
                locationB.getElevation());

        return isShapeOccluding(pointAVec4, positionB);
    }

    @Override
    public Collection<IIntersectableShape> getShapes()
    {
        return Collections.unmodifiableCollection(shapes);
    }

    @Override
    public Collection<? extends IIntersectable> getIntersectables()
    {
        return Stream.concat(shapes.stream(), surfaceStructures.stream())
                .filter(IIntersectable::isIntersectable)
                .collect(Collectors.toList());
    }

    /**
     * Sets the structure Renderables to be used to for shape occlusion within the defined Sector.
     *
     * @param sector The sector containing all the renderables of interest
     */
    @Override
    public void updateShapesForSector(Sector sector)
    {
        surfaceStructures = delegate.getSurfaceStructures(sector);
        shapes = delegate.getShapes();
    }

    @Override
    public void updateShapesForArea(ILocation locationA, ILocation locationB)
    {
        updateShapesForSector(Sector.boundingSector(
                LatLon.fromDegrees(locationA.getLatitude(), locationA.getLongitude()),
                LatLon.fromDegrees(locationB.getLatitude(), locationB.getLongitude())));
    }

    @Override
    public double getLineOfSightDistance(ILocation locationA, ILocation locationB)
    {
        return getLineOfSightDistance(locationA.getLatitude(), locationA.getLongitude(), locationA.getElevation(),
                locationB.getLatitude(), locationB.getLongitude(), locationB.getElevation());
    }

    private double getLineOfSightDistance(double pointALatitude,
                                          double pointALongitude,
                                          double pointAHeightAboveSurface,
                                          double pointBLatitude,
                                          double pointBLongitude,
                                          double pointBHeightAboveSurface)
    {
        Position positionA = Position.fromDegrees(pointALatitude, pointALongitude, pointAHeightAboveSurface);
        Vec4 pointA = terrain.getSurfacePoint(positionA);

        Position positionB = Position.fromDegrees(pointBLatitude, pointBLongitude, pointBHeightAboveSurface);
        Vec4 pointB = terrain.getSurfacePoint(positionB);
        return pointB.distanceTo3(pointA);
    }

    private boolean isTerrainOccluding(double pointALatitude,
                                       double pointALongitude,
                                       double pointAEffectiveHeight,
                                       double pointBLatitude,
                                       double pointBLongitude,
                                       double pointBEffectiveHeight)
    {
        // Compute the position of the selected location (incorporate its height).
        final Position positionA = Position.fromDegrees(
                pointALatitude,
                pointALongitude,
                pointAEffectiveHeight);

        Vec4 pointAVec4 = terrain.getSurfacePoint(
                positionA.getLatitude(),
                positionA.getLongitude(),
                pointAEffectiveHeight);

        final Position positionB = Position.fromDegrees(pointBLatitude, pointBLongitude, pointBEffectiveHeight);

        return isTerrainOccluding(positionA, pointAVec4, positionB);
    }

    private boolean isTerrainOccluding(Position positionA, Vec4 pointA, Position positionB)
    {
        // Intersect the line between this grid point and the selected position.
        Intersection[] intersections = terrain.intersect(positionA, positionB);

        if (intersections == null || intersections.length == 0 || positionA.equals(positionB))
        {
            return false;
        }

        // Check whether all intersections are tangent.  If so, there is no real intersection
        int tangentCount = 0;
        for (Intersection intersection : intersections)
        {
            if (intersection.isTangent())
            {
                tangentCount++;
            }
        }
        if (tangentCount == intersections.length)
        {
            return false;
        }

        // Check to see whether the first intersection is beyond the grid point.
        for (Intersection intersection : intersections)
        {
            Vec4 pointB = terrain.getSurfacePoint(positionB);

            // Check to see whether the intersection is beyond the grid point.
            if (isIntersectionOnLineSegment(intersection, pointA, pointB))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isShapeOccluding(Vec4 pointA, Position positionB)
    {
        if (surfaceStructures == null) // && shapes == null)
        {
            return false;
        }

        Vec4 pointB = terrain.getSurfacePoint(positionB);
        Line line = new Line(pointA, pointB.subtract3(pointA));

        for (IIntersectable intersectable : getIntersectables())
        {
            try
            {
                List<Intersection> renderableIntersections = getIntersections(intersectable, line);
                if (renderableIntersections != null)
                {
                    // Filter out intersections beyond the position.
                    for (Intersection intersection : renderableIntersections)
                    {
                        if (isIntersectionOnLineSegment(intersection, pointA, pointB))
                        {
                            return true;
                        }
                    }
                }
            } catch (Exception e)
            {
                handleIntersectionException(e, intersectable);
            }
        }
        return false;
    }

    private boolean isIntersectionOnLineSegment(Intersection intersection, Vec4 pointA, Vec4 pointB)
    {
        double length = pointB.distanceTo3(pointA);
        Vec4 intersectionPoint = intersection.getIntersectionPoint();
        return intersectionPoint.distanceTo3(pointA) <= length && intersectionPoint.distanceTo3(pointB) <= length;
    }

    /**
     * Get a list of intersections between a line and an object (structure or shape).  If there are no intersections
     * or the object doesn't support intersections, {@code null} is returned.
     *
     * @param intersectable object to intersect.
     * @param line          line for intersection calculation.
     * @return list of intersections or {@code null} if none exist.
     */
    private List<Intersection> getIntersections(IIntersectable intersectable, Line line) throws InterruptedException
    {
        return intersectable.isIntersectable()
                ? intersectable.intersect(line, terrain)
                : null;
    }

    /**
     * Handle an intersection exception.  The exception will typically be InterruptedException.
     *
     * @param e             exception to handle.
     * @param intersectable object used in intersection calculation.
     */
    private void handleIntersectionException(Exception e, IIntersectable intersectable)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Unable to intersect: {}", intersectable.getClass().getSimpleName(), e);
        }
    }
}
