package gov.takx.core.geofence.monitor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.geofence.AlertManager;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * GeoFence monitor to handle polygon shapes.
 * 
 * @since 5.3
 */
public class PolygonGeoFenceMonitor extends AGeoFenceMonitor<IPolygon>
{
    private static final List<String> SHAPE_PROPERTIES = List.of(ShapeProperties.LOCATIONS);

    /**
     * @param polygon The shape defining our geoFence boundary
     * @param geoFence The GeoFence we are monitoring
     * @param calculator Spatial calculator to use for extent and contains calculations
     * @param alertManager Manager to handle any alerts we generate
     */
    public PolygonGeoFenceMonitor(@Nonnull IPolygon polygon, GeoFence geoFence, GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        super(polygon, geoFence, calculator, alertManager);
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
