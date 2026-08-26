package gov.takx.core.geofence.monitor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.geofence.AlertManager;
import gov.takx.dataservices.shapes.Ellipse;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * GeoFence monitor to handle ellipse shapes.
 *
 * @since 5.3
 */
public class EllipseGeoFenceMonitor extends AGeoFenceMonitor<IEllipse>
{
    private static final List<String> SHAPE_PROPERTIES = List.of(
            ShapeProperties.LOCATION,
            ShapeProperties.ORIENTATION,
            ShapeProperties.SEMI_MAJOR_AXIS,
            ShapeProperties.SEMI_MINOR_AXIS,
            ShapeProperties.RING_COUNT);

    private List<ILocation> outerRingVertices;

    /**
     * @param ellipse The shape defining our geoFence boundary
     * @param geoFence The GeoFence we are monitoring
     * @param calculator Spatial calculator to use for extent and contains calculations
     * @param alertManager Manager to handle any alerts we generate
     */
    public EllipseGeoFenceMonitor(@Nonnull IEllipse ellipse, GeoFence geoFence, GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        super(ellipse, geoFence, calculator, alertManager);
    }

    @Nonnull
    @Override
    protected List<String> getShapePropertiesToMonitor()
    {
        return SHAPE_PROPERTIES;
    }

    @Override
    protected void updateGeoFenceExtent()
    {
        constructOuterRingVertices();
        super.updateGeoFenceExtent();
    }

    /**
     * Construct the vertices according to the outer ring of this ellipse.
     * @since 5.4
     */
    private void constructOuterRingVertices()
    {
        final var ellipse = getShape();
        final int ringCount = ellipse.getRingCount();
        final var outerEllipse = new Ellipse(ellipse.getName() + " monitor", ellipse.getLocation(), ellipse.getOrientation(),
                ellipse.getSemiMajorAxis() * ringCount, ellipse.getSemiMinorAxis() * ringCount);

        outerRingVertices = outerEllipse.getVertices();
    }

    @Override
    protected List<ILocation> getVertices()
    {
        return outerRingVertices != null ? outerRingVertices : getShape().getVertices();
    }
}
