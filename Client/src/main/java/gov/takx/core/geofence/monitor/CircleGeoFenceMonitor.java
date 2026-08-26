package gov.takx.core.geofence.monitor;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.geofence.AlertManager;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * GeoFence monitor to handle circle shapes.
 *
 * @since 5.3
 */
public class CircleGeoFenceMonitor extends AGeoFenceMonitor<ICircle>
{
    private static final List<String> SHAPE_PROPERTIES = List.of(ShapeProperties.LOCATION, ShapeProperties.RADIUS, ShapeProperties.RING_COUNT);

    /**
     * @param circle The shape defining our geoFence boundary
     * @param geoFence The GeoFence we are monitoring
     * @param calculator Spatial calculator to use for extent and contains calculations
     * @param alertManager Manager to handle any alerts we generate
     */
    public CircleGeoFenceMonitor(@Nonnull ICircle circle, GeoFence geoFence, GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        super(circle, geoFence, calculator, alertManager);
    }

    @Nonnull
    @Override
    protected List<String> getShapePropertiesToMonitor()
    {
        return SHAPE_PROPERTIES;
    }

    @Override
    protected List<ILocation> getVertices()
    {
        return List.of();
    }

    @Override
    protected void updateGeoFenceExtent()
    {
        final var circle = getShape();
        geoFenceExtent = new GeoFenceExtent(circle.getLocation(), calculator.calculateFurthestDistanceFromCenter(shape, circle.getLocation()));
    }

    /**
     * Overridden to perform a simpler distance check using our center and radius.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected boolean inGeographicBounds(PointMapItem pointMapItem)
    {
        return geoFenceExtent.contains(pointMapItem.getLocation());
    }
}
