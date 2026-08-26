package gov.takx.core.geofence.monitor;

import com.atakmap.spatial.SpatialCalculator;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static gov.tak.api.maps.coords.GeoPoint.UNKNOWN_POINT;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to handle an in-memory instance of {@link SpatialCalculator} ensuring thread safety.  Both {@link PointMapItem}
 * and polygons (representing GeoFence boundary shapes) can be stored, and containment tests performed.
 *
 * @see #addOrUpdatePoint(PointMapItem)
 * @see #addOrUpdateGeoFenceBoundary(String, String, List)
 * @see #contains(String, String)
 */
@ApplicationScoped
public class GeoFenceSpatialCalculator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final PointHandle UNKNOWN = new PointHandle(UNKNOWN_POINT, -1); // Placeholder for an unknown state of a map item

    private final Supplier<SpatialCalculator> calculatorSupplier;
    private SpatialCalculator calculator;

    private final Map<String, PointHandle> pointHandles = new ConcurrentHashMap<>();
    private final Set<String> badPoints = ConcurrentHashMap.newKeySet(); // Points that failed to add to the calculator

    private final Map<String, Long> shapeHandles = new ConcurrentHashMap<>();
    private final Set<String> badShapes = ConcurrentHashMap.newKeySet(); // Shapes that failed to add to the calculator

    private final Set<String> reportedErrorUids = ConcurrentHashMap.newKeySet();

    GeoFenceSpatialCalculator()
    {
        this(() -> new SpatialCalculator.Builder().inMemory().build());
    }

    /**
     * Strictly for testing use.
     *
     * @param calculatorSupplier Supplier of spatial calculator to use
     */
    @VisibleForTesting
    GeoFenceSpatialCalculator(Supplier<SpatialCalculator> calculatorSupplier)
    {
        this.calculatorSupplier = calculatorSupplier;
        calculator = calculatorSupplier.get();
    }

    /**
     * Add or update a {@link PointMapItem}.  The location of the given item will be added/updated in the spatial
     * calculator.  If any error occurs doing this, the item will be removed, and not included in any further
     * calculations.
     *
     * @param pointMapItem Item to add
     * @return {@code true} if the item's location was successfully/added updated
     */
    public synchronized boolean addOrUpdatePoint(PointMapItem pointMapItem)
    {
        final String itemUid = pointMapItem.getUid();

        if (badPoints.contains(itemUid))
        {
            logger.debug("Skipping bad point {} : {}", itemUid, pointMapItem.getName());
            return false;
        }

        final var pointAndHandle = pointHandles.getOrDefault(itemUid, UNKNOWN);
        final var location = pointMapItem.getLocation();
        long handle = pointAndHandle.handle();

        try
        {
            // If it is new or different, add/update the calculator
            if (handle <= 0 || !location.equals(pointAndHandle.geoPoint))
            {
                handle = createOrUpdatePoint(handle, location);
                pointHandles.put(itemUid, new PointHandle(location, handle));
            }
        } catch (Exception e)
        {
            handle = -1;
            logger.error("Failed to add or update point for spatial calculations: {}", itemUid, e);
            pointHandles.remove(itemUid);
            badPoints.add(itemUid);
        }

        return handle > 0;
    }

    /**
     * Remove the given {@code pointMapItem} from the calculator.
     *
     * @param pointMapItem to remove
     */
    public synchronized void removePoint(@Nonnull PointMapItem pointMapItem)
    {
        try
        {
            final var removed = pointHandles.remove(pointMapItem.getUid());
            if (removed != null) calculator.deleteGeometry(removed.handle());
        } catch (Exception e)
        {
            logger.error("Failed to remove point from spatial calculations: {}", pointMapItem.getUid(), e);
        }
    }

    /**
     * Add/update a shape boundary to the calculator. If any error occurs doing this, the boundary will be removed,
     * and not included in any further calculations.
     *
     * @param geoFenceUid UID of the shape representing the GeoFence boundary
     * @param shapeName   The name of the shape, for use in error reporting
     * @param locations   The vertices of the boundary shape
     */
    public synchronized void addOrUpdateGeoFenceBoundary(@Nonnull String geoFenceUid, @Nonnull String shapeName, @Nonnull List<? extends IGeoPoint> locations)
    {
        if (badShapes.contains(geoFenceUid))
        {
            logger.debug("Skipping bad shape {} : {}", geoFenceUid, shapeName);
            return;
        }

        final var geoPoints = locations.toArray(IGeoPoint[]::new);
        Long handle = shapeHandles.get(geoFenceUid);

        try
        {
            if (handle == null || handle <= 0)
            {
                handle = calculator.createPolygon(geoPoints);
                shapeHandles.put(geoFenceUid, handle);
            } else
            {
                calculator.updatePolygon(handle, geoPoints);
            }
        } catch (Exception e)
        {
            reportBoundaryError(geoFenceUid, shapeName);
            logger.error("Failed to add or update shape for spatial calculations: {}", geoFenceUid, e);
            shapeHandles.remove(geoFenceUid);
            badShapes.add(geoFenceUid);
        }
    }

    /**
     * Remove the boundary shape for the given {@code geoFenceUid} from the calculator.
     *
     * @param geoFenceUid UID of the shape boundary to remove
     */
    public synchronized void removeGeoFenceBoundary(@Nonnull String geoFenceUid)
    {
        final Long handle = shapeHandles.remove(geoFenceUid);
        if (handle != null) calculator.deleteGeometry(handle);
    }

    private void reportBoundaryError(@Nonnull String shapeUid, @Nonnull String shapeName)
    {
        if (reportedErrorUids.contains(shapeUid)) return;

        reportedErrorUids.add(shapeUid);

        // TODO - add user notification TAKX-3238
        final String msg = "Failed to add or update shape for spatial calculations: " + shapeName;
    }

    /**
     * @return {@code true} if we have a valid calculator entry for the given {@code pointMapItemUid}
     */
    public boolean isPointTracked(@Nonnull String pointMapItemUid)
    {
        return pointHandles.containsKey(pointMapItemUid);
    }

    /**
     * @return {@code true} if we have a valid calculator entry for the given GeoFence {@code shapeUid}
     */
    public boolean isGeoFenceTracked(@Nonnull String shapeUid)
    {
        return shapeHandles.containsKey(shapeUid);
    }

    /**
     * Perform a spatial calculation to determine if the point is within the boundary of a shape boundary.
     *
     * @param shapeUid UID of the registered shape (polygon)
     * @param pointMapItemUid UID of the registered point to test
     * @return {@code true} if the point is within the geographic bounds of the shape
     * @see #addOrUpdatePoint(PointMapItem)
     * @see #addOrUpdateGeoFenceBoundary(String, String, List)
     */
    public synchronized boolean contains(@Nonnull String shapeUid, @Nonnull String pointMapItemUid)
    {
        if (badShapes.contains(shapeUid) || badPoints.contains(pointMapItemUid))
        {
            logger.debug("Skipping contains test on {} : {}", shapeUid, pointMapItemUid);
            return false;
        }

        try
        {
            return calculator.contains(shapeHandles.get(shapeUid), pointHandles.get(pointMapItemUid).handle());
        } catch (Exception e)
        {
            logger.error("Failed to perform contains test in spatial calculations: shape {} point {}", shapeUid, pointMapItemUid, e);
            return false;
        }
    }

    /**
     * Calculate the center location of a given shape.
     *
     * @param shape Shape of interest
     * @return center location, direct or calculated (for polygonal shapes)
     */
    public IGeoPoint calculateCenter(IShape shape)
    {
        return shape instanceof IPolygon polygon
                ? GeoCalculations.computeAverage(polygon.getVertices().toArray(IGeoPoint[]::new))
                : shape.getLocation();
    }

    /**
     * @param shape Shape to process
     * @param center center location
     * @return the distance from the given {@code center} location to the furthest point on the shape boundary
     */
    public double calculateFurthestDistanceFromCenter(IShape shape, IGeoPoint center)
    {
        if (shape instanceof ICircle circle)
        {
            return circle.getRadius() * circle.getRingCount();
        } else if (shape instanceof IEllipse ellipse)
        {
            return Math.max(ellipse.getSemiMajorAxis(), ellipse.getSemiMinorAxis()) * ellipse.getRingCount();
        } else if (shape instanceof IRectangle rectangle)
        {
            return calculateFurthestDistanceFromCenter(center, rectangle.getVertices());

        } else if (shape instanceof IPolygon polygon)
        {
            return calculateFurthestDistanceFromCenter(center, polygon.getVertices());
        } else
        {
            logger.error("Can't calculate distances for shape: {}", shape.getClass().getName());
            return 0;
        }
    }

    /**
     * @param center center location
     * @param locations Locations to test
     * @return the distance from the given {@code center} location to the furthest point in {@code locations}
     */
    private double calculateFurthestDistanceFromCenter(IGeoPoint center, List<? extends IGeoPoint> locations)
    {
        return locations.stream()
                .mapToDouble(location -> GeoCalculations.distance(center, location))
                .max().orElse(0);
    }

    /**
     * Create or update the point in the spatial calculator.
     *
     * @param handle Existing handle for update, {@code <= 0} for a new point
     * @param location Point's location
     * @return handle for point
     */
    private synchronized long createOrUpdatePoint(long handle, IGeoPoint location)
    {
        if (handle <= 0)
        {
            return calculator.createPoint(location);
        } else
        {
            calculator.updatePoint(handle, location);
            return handle;
        }
    }

    @VisibleForTesting
    protected void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        pointHandles.clear();
        badPoints.clear();
        shapeHandles.clear();
        badShapes.clear();

        calculator.dispose();
        calculator = calculatorSupplier.get();
    }

    /**
     * Holds the location of a map item and its associated handle in the spatial calculator.
     */
    private record PointHandle(@Nonnull IGeoPoint geoPoint, long handle)
    {
    }
}
