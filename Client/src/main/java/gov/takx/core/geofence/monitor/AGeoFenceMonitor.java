package gov.takx.core.geofence.monitor;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.MonitoredType;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.shapes.IShape;
import gov.takx.core.geofence.AlertManager;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base for {@link GeoFence} monitors.
 *
 * @since 5.3
 */
public abstract class AGeoFenceMonitor<T extends IShape> implements IGeoFenceMonitor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected final IShape shape;
    protected final GeoFenceSpatialCalculator calculator;
    private final AlertManager alertManager;

    private final PropertyChangeListener shapeChangeListener = e -> shapeChanged();
    private final Map<String, PointMapItem> trackedPointMapItems = new ConcurrentHashMap<>();
    private final Set<String> untrackedItemUids = ConcurrentHashMap.newKeySet();
    private final Map<String, Boolean> lastState = new ConcurrentHashMap<>();

    protected GeoFence geoFence;
    protected GeoFenceExtent geoFenceExtent;

    /**
     * @param shape The shape defining our geoFence boundary
     * @param geoFence The GeoFence we are monitoring
     * @param calculator Spatial calculator to use for extent and contains calculations
     * @param alertManager Manager to handle any alerts we generate
     */
    protected AGeoFenceMonitor(IShape shape, GeoFence geoFence, GeoFenceSpatialCalculator calculator, AlertManager alertManager)
    {
        this.shape = shape;
        this.geoFence = geoFence;
        this.calculator = calculator;
        this.alertManager = alertManager;

        shape.addPropertyChangeListener(getShapePropertiesToMonitor(), shapeChangeListener);

        updateGeoFenceExtent();
    }

    /**
     * @return List of shape properties to monitor to determine when our shape extent needs to be updated
     */
    @Nonnull
    protected abstract List<String> getShapePropertiesToMonitor();

    /**
     * @return The vertices defining our shape boundary
     */
    protected abstract List<ILocation> getVertices();

    /**
     * @return Properly typed shape
     */
    @SuppressWarnings("unchecked")
    protected T getShape()
    {
        return (T) shape;
    }

    @Override
    @Nonnull
    public String getShapeUid()
    {
        return geoFence.shapeUid();
    }

    @Override
    @Nonnull
    public GeoFenceExtent getExtent()
    {
        return geoFenceExtent;
    }

    /**
     * Our shape boundary has changed, update our extent and re-process all tracked map items, alarming if the
     * {@link GeoFence} trigger conditions are met.
     */
    private void shapeChanged()
    {
        updateGeoFenceExtent();
        logger.debug("shapeChange, new Extent {}", geoFenceExtent);

        trackedPointMapItems.values().forEach(this::mapItemMoved);
    }

    /**
     * Calculate our {@link GeoFenceExtent}.
     */
    protected void updateGeoFenceExtent()
    {
        final var center = calculator.calculateCenter(shape);
        geoFenceExtent = new GeoFenceExtent(center, calculator.calculateFurthestDistanceFromCenter(shape, center));
        calculator.addOrUpdateGeoFenceBoundary(getShapeUid(), shape.getName(), getVertices());

        logger.debug("New Extent {}", geoFenceExtent);
    }

    @Override
    public void setMapItemState(@Nonnull PointMapItem pointMapItem)
    {
        if (matchesMonitoredType(pointMapItem))
        {
            final boolean contained = contains(pointMapItem);
            lastState.put(pointMapItem.getUid(), contained);

            if (logger.isDebugEnabled()) logger.debug("lastState put {} = {}", pointMapItem.getLocation(), contained);

            // Create an alert for map items that are initially contained with trigger Entry or Both
            final var trigger = geoFence.trigger();
            if (contained && (trigger == Trigger.Entry || trigger == Trigger.Both)) alert(pointMapItem, Trigger.Entry);
        }
    }

    @Override
    public void mapItemMoved(@Nonnull PointMapItem pointMapItem)
    {
        if (!matchesMonitoredType(pointMapItem))
        {
            logger.warn("point {} not matched by {}", pointMapItem.getCotType(), geoFence.monitoredType());
            return; // Not of interest to us
        }

        final var location = pointMapItem.getLocation();
        final boolean contained = geoFenceExtent.contains(location) && contains(pointMapItem);
        Boolean wasContained = lastState.get(pointMapItem.getUid());
        final var trigger = geoFence.trigger();
        boolean newItem = false;

        // If this is a new map item, and our trigger is entry/both, and it is contained, then alert immediately.
        // Do not alert on a new map item that is not contained.

        if (wasContained == null)
        {
            newItem = true;
            wasContained = false;
        }

        logger.debug("test {} new {} contained {} wasContained {} trigger {}", location, newItem, contained, wasContained, trigger);

        if (newItem || contained != wasContained)
        {
            lastState.put(pointMapItem.getUid(), contained);

            if (contained && (newItem || (trigger == Trigger.Entry || trigger == Trigger.Both)))
            {
                alert(pointMapItem, Trigger.Entry);
            } else if (!contained && !newItem && (trigger == Trigger.Exit || trigger == Trigger.Both))
            {
                alert(pointMapItem, Trigger.Exit);
            }
        }
    }

    /**
     * Generate an alert for a boundary crossing.
     *
     * @param mapItem The map item that breached the boundary
     * @param crossingType Type of crossing (entry or exit)
     */
    protected void alert(@Nonnull PointMapItem mapItem, @Nonnull Trigger crossingType)
    {
        alertManager.alert(shape, mapItem, crossingType);
    }

    /**
     * Determine if a given {@code pointMapItem} matches the containment parameters of our {@link GeoFence}.
     * This includes any elevation restrictions.
     *
     * @param pointMapItem map item of interest
     * @return {@code true} if the item is within our GeoFence, {@code false} otherwise
     */
    protected boolean contains(PointMapItem pointMapItem)
    {
        return inElevationBounds(pointMapItem.getLocation()) && inGeographicBounds(pointMapItem);
    }

    /**
     * @return {@code true} if the given {@code location}'s elevation falls within our configured elevation bounds, if any
     */
    protected boolean inElevationBounds(ILocation location)
    {
        if (geoFence.isElevationMonitored())
        {
            double minElev = geoFence.minElevation();
            double maxElev = geoFence.maxElevation();
            double pointElev = location.getElevation();

            logger.debug("checkElevation: {} - {} vs {}", minElev, maxElev, pointElev);
            return (Double.isNaN(minElev) || !(pointElev <= minElev)) && (Double.isNaN(maxElev) || !(pointElev >= maxElev));
        }
        return true;
    }

    /**
     * @return {@code true} if the given {@code pointMapItem}'s location falls within our shape boundary
     */
    protected boolean inGeographicBounds(PointMapItem pointMapItem)
    {
        return calculator.contains(getShapeUid(), pointMapItem.getUid());
    }

    @Override
    public boolean matchesMonitoredType(@Nonnull PointMapItem pointMapItem)
    {
        final String itemUid = pointMapItem.getUid();

        if (trackedPointMapItems.containsKey(itemUid)) return true;
        if (untrackedItemUids.contains(itemUid)) return false;

        final var monitoredType = geoFence.monitoredType();
        final boolean typeMatches = monitoredType.matches(pointMapItem);

        if (typeMatches && (monitoredType != MonitoredType.Custom || geoFence.customUids().contains(itemUid)))
        {
            trackedPointMapItems.put(itemUid, pointMapItem);
            return true;
        } else
        {
            untrackedItemUids.add(itemUid);
            lastState.remove(itemUid);
            return false;
        }
    }

    @Override
    public void stop()
    {
        shape.removePropertyChangeListener(getShapePropertiesToMonitor(), shapeChangeListener);
    }
}
