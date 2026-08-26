package gov.takx.core.geofence.monitor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.geofence.AlertManager;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * GeoFence monitor to handle rectangular shapes (includes Square).
 *
 * @since 5.3
 */
public class RectangleGeoFenceMonitor extends AGeoFenceMonitor<IRectangle>
{
    private static final List<String> SHAPE_PROPERTIES = List.of(
            ShapeProperties.LOCATION,
            ShapeProperties.ORIENTATION,
            ShapeProperties.HEIGHT,
            ShapeProperties.WIDTH);

    /**
     * @param rectangle The shape defining our geoFence boundary
     * @param geoFence The GeoFence we are monitoring
     * @param calculator Spatial calculator to use for extent and contains calculations
     * @param alertManager Manager to handle any alerts we generate
     */
    public RectangleGeoFenceMonitor(@Nonnull IRectangle rectangle, GeoFence geoFence, GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        super(rectangle, geoFence, calculator, alertManager);
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
        return getShape().getVertices();
    }
}
