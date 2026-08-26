package gov.takx.core.geofence;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.IGeoFenceManager;
import gov.takx.api.geofence.event.GeoFenceAdded;
import gov.takx.api.geofence.event.GeoFenceRemoved;
import gov.takx.api.geofence.event.GeoFenceUpdated;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.core.geofence.monitor.GeoFenceMonitorFactory;
import gov.takx.core.geofence.monitor.IGeoFenceMonitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage GeoFences and their associated monitors.
 *
 * @since 5.3
 * @see GeoFence
 */
@ApplicationScoped
public class GeoFenceManager implements IGeoFenceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMapObjectFinder mapObjectFinder;
    private final GeoFenceMonitorFactory geoFenceMonitorFactory;
    private final GlobalLocationTracker globalLocationTracker;
    private final INotificationService notificationService;
    private final AlertManager alertManager;
    private final Event<GeoFenceAdded> geoFenceAddedEvent;
    private final Event<GeoFenceUpdated> geoFenceUpdatedEvent;
    private final Event<GeoFenceRemoved> geoFenceRemovedEvent;

    private final Map<String, IShapeMapObject<?>> knownGeoFences = new ConcurrentHashMap<>();
    private final Map<String, IGeoFenceMonitor> monitors = new ConcurrentHashMap<>();

    @Inject
    GeoFenceManager(IMapObjectFinder mapObjectFinder, GeoFenceMonitorFactory geoFenceMonitorFactory,
                    GlobalLocationTracker globalLocationTracker, INotificationService notificationService,
                    AlertManager alertManager, Event<GeoFenceAdded> geoFenceAddedEvent,
                    Event<GeoFenceUpdated> geoFenceUpdatedEvent, Event<GeoFenceRemoved> geoFenceRemovedEvent)
    {
        this.mapObjectFinder = mapObjectFinder;
        this.geoFenceMonitorFactory = geoFenceMonitorFactory;
        this.globalLocationTracker = globalLocationTracker;
        this.notificationService = notificationService;
        this.alertManager = alertManager;
        this.geoFenceAddedEvent = geoFenceAddedEvent;
        this.geoFenceUpdatedEvent = geoFenceUpdatedEvent;
        this.geoFenceRemovedEvent = geoFenceRemovedEvent;
    }

    @Override
    public void addOrUpdateGeoFence(@Nonnull GeoFence geoFence) throws IllegalArgumentException
    {
        final String shapeUid = geoFence.shapeUid();
        final var shapeMapObject = getAndCheckShapeMapObject(shapeUid);

        if (geoFence.equals(shapeMapObject.getGeoFence()))
        {
            logger.debug("addOrUpdate called with identical GeoFence: {}", geoFence);
        } else
        {
            shapeMapObject.setGeoFence(geoFence);
            shapeMapObject.setPublished(true);
            deleteMonitor(shapeUid);

            notifyAndFireEvents(geoFence, shapeMapObject);
            knownGeoFences.put(shapeUid, shapeMapObject);

            // Only create a monitor if the geoFence is tracking/active
            if (geoFence.tracking()) createMonitor(shapeMapObject);
        }
    }

    /**
     * Notify the user of the geoFence/monitor according to old and new settings.  Fire add/update CDI event.
     *
     * @param geoFence geoFence being processed
     * @param shapeMapObject Shape MO holding the GeoFence
     */
    private void notifyAndFireEvents(GeoFence geoFence, IShapeMapObject<?> shapeMapObject)
    {
        final String shapeUid = geoFence.shapeUid();
        final String message = getNotificationMessage(geoFence, shapeMapObject, shapeUid);

        if (message != null) notificationService.show(Notification.builder(message).build());

        // Fire appropriate event
        if (!knownGeoFences.containsKey(shapeUid))
        {
            geoFenceAddedEvent.fire(new GeoFenceAdded(geoFence, shapeMapObject));
        } else
        {
            geoFenceUpdatedEvent.fire(new GeoFenceUpdated(geoFence, shapeMapObject));
        }
    }

    @Nullable
    private String getNotificationMessage(GeoFence geoFence, IShapeMapObject<?> shapeMapObject, String shapeUid)
    {
        final boolean newMonitor = !monitors.containsKey(shapeUid);

        // Notify the user
        final String message;
        if (newMonitor && geoFence.tracking())
        {
            message = String.format("Monitoring GeoFence '%s' for %s items", shapeMapObject.getName(), geoFence.monitoredType());
        } else if (!newMonitor && !geoFence.tracking())
        {
            message = String.format("Stopped monitoring GeoFence '%s'", shapeMapObject.getName());
        } else
        {
            message = null;
        }
        return message;
    }

    @Override
    public void removeGeoFence(@Nonnull GeoFence geoFence) throws IllegalArgumentException
    {
        final String shapeUid = geoFence.shapeUid();
        final var shapeMapObject = getAndCheckShapeMapObject(shapeUid);

        deleteMonitor(shapeUid);
        knownGeoFences.remove(shapeUid);

        // Don't clear the geofence reference until after the monitor is deleted, which would also clear any alerts
        // and those event handlers are expecting the geofence link to still be valid.
        shapeMapObject.setGeoFence(null);
        shapeMapObject.setPublished(false);

        if (geoFence.tracking())
        {
            final String message = String.format("Stopped monitoring GeoFence '%s'", shapeMapObject.getName());
            notificationService.show(Notification.builder(message).build());
        }

        geoFenceRemovedEvent.fire(new GeoFenceRemoved(geoFence, shapeMapObject));
    }

    /**
     * @return the map object for the given {@code uid}, ensuring it is an {@link IShapeMapObject}.
     * @throws IllegalArgumentException if the referenced MO doesn't exist or is not an {@link IShapeMapObject}
     */
    private IShapeMapObject<?> getAndCheckShapeMapObject(String uid) throws IllegalArgumentException
    {
        if (mapObjectFinder.findMapObjectByUid(uid) instanceof IShapeMapObject<?> shapeMapObject)
        {
            return shapeMapObject;
        } else
        {
            throw new IllegalArgumentException("Geofence MapObject is not a ShapeMapObject: UID=" + uid);
        }
    }

    /**
     * Create a monitor for the geoFence attached to the given {@code shapeMapObject}.
     */
    @VisibleForTesting
    protected void createMonitor(@Nonnull IShapeMapObject<?> shapeMapObject)
    {
        final var geoFence = shapeMapObject.getGeoFence();

        if (geoFence == null)
        {
            logger.warn("ShapeMapObject geoFence is null!: {}", shapeMapObject);
        } else
        {
            final var monitor = geoFenceMonitorFactory.create(shapeMapObject, geoFence);
            if (monitor != null)
            {
                monitors.put(shapeMapObject.getUid(), monitor);
                globalLocationTracker.addMonitor(monitor);
            }
        }
    }

    /**
     * Delete the monitor for the given {@code shapeUid}. Any associated alerts will be cleared.
     */
    @VisibleForTesting
    protected void deleteMonitor(@Nonnull String shapeUid)
    {
        final var monitor = monitors.remove(shapeUid);
        if (monitor != null)
        {
            monitor.stop();
            globalLocationTracker.removeMonitor(monitor);
            alertManager.clearAlerts(shapeUid);
        }
    }

    /**
     * Delete any monitor that may be associated with a shapes geoFence.
     */
    @VisibleForTesting
    protected void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject() instanceof IShapeMapObject<?> shapeMapObject && shapeMapObject.hasGeoFence())
        {
            removeGeoFence(shapeMapObject.requireGeoFence());
        }
    }

    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices ignored)
    {
        // Reload all shapes that have attached geofences
        mapObjectFinder.findMapObjectsOfType(IShapeMapObject.class).stream()
                .filter(IShapeMapObject::hasGeoFence)
                .forEach(shapeMapObject -> {
                    final GeoFence geoFence = shapeMapObject.requireGeoFence();
                    notifyAndFireEvents(geoFence, shapeMapObject);
                    if (geoFence.tracking()) createMonitor(shapeMapObject);
                    knownGeoFences.put(shapeMapObject.getUid(), shapeMapObject);
                });

        // Do the rest in the background
        CompletableFuture.runAsync(globalLocationTracker::start);
    }

    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        monitors.values().forEach(IGeoFenceMonitor::stop);
        monitors.clear();
        knownGeoFences.clear();
        globalLocationTracker.stop();
    }
}
