package gov.takx.core.geofence.monitor;

import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.core.geofence.AlertManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Factory for creating appropriate monitor instances based on a shape type.
 *
 * @since 5.3
 */
@ApplicationScoped
public class GeoFenceMonitorFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GeoFenceSpatialCalculator calculator;
    private final AlertManager alertManager;

    @Inject
    GeoFenceMonitorFactory(GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        this.calculator = calculator;
        this.alertManager = alertManager;
    }

    /**
     * Create a new monitor based on the shape type of the given {@code shapeMapObject}.
     * 
     * @param shapeMapObject Shape for which to construct a monitor
     * @param geoFence {@link GeoFence} controlling the monitor's operation
     * @return new monitor instance, {@code null} if the given shape is not of a supported type
     */
    @Nullable
    public IGeoFenceMonitor create(@Nonnull IShapeMapObject<?> shapeMapObject, @Nonnull GeoFence geoFence)
    {
        if (shapeMapObject instanceof ICircle circle)
        {
            return new CircleGeoFenceMonitor(circle, geoFence, calculator, alertManager);
        } else if (shapeMapObject instanceof IEllipse ellipse)
        {
            return new EllipseGeoFenceMonitor(ellipse, geoFence, calculator, alertManager);
        } else if (shapeMapObject instanceof IRectangle rectangle)
        {
            return new RectangleGeoFenceMonitor(rectangle, geoFence, calculator, alertManager);
        } else if (shapeMapObject instanceof IPolygon polygon)
        {
            return new PolygonGeoFenceMonitor(polygon, geoFence, calculator, alertManager);
        } else
        {
            logger.warn("Unsupported shape type for GeoFence: {}", shapeMapObject.getClass().getName());
            return null;
        }
    }
}
