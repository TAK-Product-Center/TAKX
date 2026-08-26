package gov.takx.core.geofence;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.geofence.monitor.GeoFenceSpatialCalculator;
import gov.takx.core.geofence.monitor.IGeoFenceMonitor;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Central location tracker for all point map items.  This avoids every monitor from having to add a listener
 * on every map item.
 */
@ApplicationScoped
class GlobalLocationTracker
{
    // TODO monitor for CoT tpe changes (not handled in ATAK or WinTAK)

    // Filter to prevent inspecting point map items unless they are "self", have a valid CoT type, or have a MIL_SYM attached
    private static final Predicate<PointMapItem> INITIAL_FILTER = mapItem -> {
        final String cotType = mapItem.getCotType();
        return CotTypes.SELF.equals(cotType)
                || (cotType.length() > 2 && cotType.charAt(1) == '-') // Valid CoT type
                || mapItem.hasMetaValue(StandardAttributeNames.MIL_SYM);
    };

    private final GeoFenceSpatialCalculator calculator;
    private final IMapObjectFinder mapObjectFinder;

    private final Map<String, IGeoFenceMonitor> monitors = new ConcurrentHashMap<>();
    private final PropertyChangeListener locationChangeListener = e -> mapItemMoved((PointMapItem) e.getSource());

    private volatile boolean started;

    @Inject
    GlobalLocationTracker(IMapObjectFinder mapObjectFinder, GeoFenceSpatialCalculator calculator)
    {
        this.mapObjectFinder = mapObjectFinder;
        this.calculator = calculator;
    }

    /**
     * Start processing.  Have all our monitors install the initial state for tracked {@link PointMapItem map items}.
     */
    void start()
    {
        started = true;
        setInitialState(monitors.values());
    }

    /**
     * Stop processing and release resources.
     */
    void stop()
    {
        started = false;
        monitors.clear();
    }

    /**
     * @param monitor Monitor to add to our processing
     */
    public void addMonitor(@Nonnull IGeoFenceMonitor monitor)
    {
        monitors.put(monitor.getShapeUid(), monitor);

        if (started) setInitialState(List.of(monitor));
    }

    /**
     * @param monitor Monitor to remove from our processing
     */
    public void removeMonitor(@Nonnull IGeoFenceMonitor monitor)
    {
        final String shapeUid = monitor.getShapeUid();
        final var removedMonitor = monitors.remove(shapeUid);

        if (removedMonitor != null)
        {
            calculator.removeGeoFenceBoundary(shapeUid);
            if (started) rescanPointMapItems();
        }
    }

    /**
     * Handle a new Map Object.  If it is a {@link PointMapItem}, and it matches any of our GeoFences
     * monitored types, add it to our tracked items.
     */
    @VisibleForTesting
    protected void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (!started) return;

        if (event.getMapObject() instanceof PointMapItem pointMapItem && anyGeoFenceMatches(pointMapItem))
        {
            if (!calculator.isPointTracked(pointMapItem.getUid())) addTrackedMapItem(pointMapItem);
            mapItemMoved(pointMapItem);
        }
    }

    /**
     * Have each monitor in {@code monitorsToScan} install the initial state for every matching {@link PointMapItem}.
     */
    private void setInitialState(@Nonnull Collection<IGeoFenceMonitor> monitorsToScan)
    {
        getPointMapItems().forEach(pointMapItem -> {
            boolean tracking = calculator.isPointTracked(pointMapItem.getUid());

            for (var monitor : monitorsToScan)
            {
                if (monitor.matchesMonitoredType(pointMapItem))
                {
                    if (!tracking)
                    {
                        addTrackedMapItem(pointMapItem);
                        tracking = true;
                    }

                    monitor.setMapItemState(pointMapItem);
                }
            }
        });
    }

    /**
     * Rescan every {@link PointMapItem} being tracked, removing those that are no longer matched by any monitor.
     */
    private void rescanPointMapItems()
    {
        getPointMapItems()
                .filter(pointMapItem -> !anyGeoFenceMatches(pointMapItem))
                .forEach(this::removeTrackedMapItem);
    }

    /**
     * @return All {@link PointMapItem} instances that can be valid geoFence inputs.  They must have a CoT type
     * satisfying {@link #INITIAL_FILTER}.
     */
    private Stream<PointMapItem> getPointMapItems()
    {
        return mapObjectFinder.findMapObjectsOfType(PointMapItem.class).stream().filter(INITIAL_FILTER);
    }

    /**
     * @return {@code true} if the given {@code pointMapItem} matches any of our GeoFences monitored types
     */
    private boolean anyGeoFenceMatches(@Nonnull PointMapItem pointMapItem)
    {
        return monitors.values().stream().anyMatch(m -> m.matchesMonitoredType(pointMapItem));
    }

    /**
     * Add the given {@code pointMapItem} to our tracked set, add listeners/handlers and add to the calculator.
     */
    private void addTrackedMapItem(@Nonnull PointMapItem pointMapItem)
    {
        pointMapItem.addPropertyChangeListener(IMapObject.LOCATION, locationChangeListener);
        pointMapItem.addDeletionHandler(item -> removeTrackedMapItem(pointMapItem));
        calculator.addOrUpdatePoint(pointMapItem);
    }

    /**
     * Handle a change in the location of the {@code pointMapItem}, updating the calculator and monitors.
     */
    private void mapItemMoved(@Nonnull PointMapItem pointMapItem)
    {
        if (calculator.addOrUpdatePoint(pointMapItem))
        {
            monitors.values().stream()
                    .filter(m -> m.matchesMonitoredType(pointMapItem))
                    .forEach(monitor -> monitor.mapItemMoved(pointMapItem));
        }
    }

    /**
     * Remove the given {@code pointMapItem} from tracking.
     */
    private void removeTrackedMapItem(@Nonnull PointMapItem pointMapItem)
    {
        pointMapItem.removePropertyChangeListener(IMapObject.LOCATION, locationChangeListener);
        calculator.removePoint(pointMapItem);
    }
}
