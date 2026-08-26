package gov.takx.core.geofence;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.GeoFenceAlert;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.geofence.event.GeoFenceAlertAdded;
import gov.takx.api.geofence.event.GeoFenceAlertRemoved;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.shapes.IShape;
import gov.takx.platform.utils.ListTimeBatchedQueue;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class may be removed/refactored when the actual alert handling is implemented in TAKX-3238.
 * For now, it is used to support test verification that alerts are being generated in various scenarios.
 * 
 * @since 5.3
 */
@ApplicationScoped
public class AlertManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final INotificationService notificationService;

    private final Event<GeoFenceAlertAdded> alertAddedEvent;
    private final Event<GeoFenceAlertRemoved> alertRemovedEvent;
    private final Map<String, List<GeoFenceAlert>> alertsByUid = new ConcurrentHashMap<>();

    private final NotificationQueue notificationQueue = new NotificationQueue();

    @Inject
    AlertManager(INotificationService notificationService, Event<GeoFenceAlertAdded> alertAddedEvent, Event<GeoFenceAlertRemoved> alertRemovedEvent)
    {
        this.notificationService = notificationService;
        this.alertAddedEvent = alertAddedEvent;
        this.alertRemovedEvent = alertRemovedEvent;
    }

    /**
     * Create and add a new {@link GeoFenceAlert alert}. A {@link GeoFenceAlertAdded} event is fired for the new alert.
     * A user notification is posted with the alert information.
    
     * @param shape The shape defining the GeoFence boundary
     * @param mapItem The {@link PointMapItem} that crossed the boundary
     * @param crossingType The type of crossing (entry/exit)
     * @return The created {@link GeoFenceAlert}
     * @throws IllegalArgumentException If the shape does not have an associated map object ({@link IShape#getShapeMapObject()})
     *         or if the shape map object does not have a {@link GeoFence} ({@link IShapeMapObject#getGeoFence()})
     */
    @Nonnull
    public GeoFenceAlert alert(@Nonnull IShape shape, @Nonnull PointMapItem mapItem, @Nonnull Trigger crossingType)
    {
        final var shapeMapObject = shape.getShapeMapObject();
        if (shapeMapObject == null) throw new IllegalArgumentException("ShapeMapObject is null for shape: " + shape);

        final var geoFence = shapeMapObject.getGeoFence();
        if (geoFence == null) throw new IllegalArgumentException("geoFence is null for shape: " + shape);

        final var alert = new GeoFenceAlert(shape, geoFence, mapItem, crossingType, new CoordinatedTime());
        logger.debug("Add {}", alert);

        final String uid = shapeMapObject.getUid();
        alertsByUid.computeIfAbsent(uid, k -> new ArrayList<>()).add(alert);

        addNotification(alert);
        alertAddedEvent.fire(new GeoFenceAlertAdded(alert));

        return alert;
    }

    /**
     * Add a notification to the queue.  This is only here to support testing so it can verify that notifications are
     * being queued and not have to deal with the time-based nature of the {@code notificationQueue}.
     */
    @VisibleForTesting
    protected void addNotification(@Nonnull GeoFenceAlert alert)
    {
        notificationQueue.add(alert);
    }

    /**
     * @return The total number of alerts recorded across all GeoFences.
     */
    public int getTotalAlertCount()
    {
        return alertsByUid.values().stream().mapToInt(List::size).sum();
    }

    /**
     * Get the list of all {@link GeoFenceAlert alerts} recorded.
     *
     * @return List of {@link GeoFenceAlert alerts}, may be empty, never {@code null}
     */
    @Nonnull
    public List<GeoFenceAlert> getAllAlerts()
    {
        return alertsByUid.values().stream().flatMap(Collection::stream).toList();
    }

    /**
     * Get the number of alerts recorded for the {@link GeoFence} with the given UID.
     *
     * @param uid UID of GeoFence for which to retrieve the alert count
     * @return The number of {@link GeoFenceAlert alerts}, 0 if none have been recorded
     * @see GeoFence#shapeUid()
     */
    public int getAlertCount(@Nonnull String uid)
    {
        return alertsByUid.containsKey(uid) ? alertsByUid.get(uid).size() : 0;
    }

    /**
     * Get the list of {@link GeoFenceAlert alerts} recorded for the {@link GeoFence} with the given UID.
     *
     * @param uid UID of GeoFence for which to retrieve alerts
     * @return List of {@link GeoFenceAlert alerts}, may be empty, never {@code null}
     * @see GeoFence#shapeUid()
     */
    @Nonnull
    public List<GeoFenceAlert> getAlerts(@Nonnull String uid)
    {
        return List.copyOf(alertsByUid.getOrDefault(uid, List.of()));
    }

    /**
     * Remove the given alert.  A {@link GeoFenceAlertRemoved} event is fired for the removed alert.
     * No action is taken if the alert is not in the collection associated with its UID.
     * 
     * @param alert The alert to remove
     */
    public void removeAlert(@Nonnull GeoFenceAlert alert)
    {
        logger.debug("Remove {}", alert);

        final String uid = alert.shapeUid();
        if (alertsByUid.containsKey(uid))
        {
            alertsByUid.get(uid).remove(alert);
            alertRemovedEvent.fire(new GeoFenceAlertRemoved(alert));
        }
    }

    /**
     * Remove all alerts recorded for the {@link GeoFence} with the given UID.
     * A {@link GeoFenceAlertRemoved} event is fired for each alert removed, if any.
     *
     * @param uid UID of GeoFence for which to clear alerts
     * @see GeoFence#shapeUid()
     */
    public void clearAlerts(@Nonnull String uid)
    {
        logger.debug("Clear alerts for {}", uid);

        final var removed = alertsByUid.remove(uid);
        if (removed != null) removed.forEach(alert -> alertRemovedEvent.fire(new GeoFenceAlertRemoved(alert)));
    }

    @VisibleForTesting
    void onProjectClosing(@Observes ProjectClosing ignored)
    {
        alertsByUid.clear();
    }

    /**
     * Queue to collect alerts that should be shown as notifications to the user.  If more than one alert for a given
     * GeoFence is collected, then the message will just show the number of breaches in a single notification instead
     * of producing multiple notifications.  This is useful when a GeoFence is initially created (or restored), and
     * it contains a number of map items that trigger the geoFence.
     */
    private class NotificationQueue extends ListTimeBatchedQueue<GeoFenceAlert>
    {
        NotificationQueue()
        {
            super("Alert notification Queue", 100, 500);
        }

        @Override
        protected void runBatch(List<GeoFenceAlert> batch)
        {
            if (batch.size() > 1)
            {
                final Map<String, List<GeoFenceAlert>> alertsByUid = new HashMap<>();
                batch.forEach(alert -> alertsByUid.computeIfAbsent(alert.shapeUid(), k -> new ArrayList<>()).add(alert));

                alertsByUid.forEach((uid, alerts) -> {
                    if (alerts.size() > 1)
                    {
                        notify(String.format("%d items breached geoFence '%s'", batch.size(), alerts.get(0).shape().getName()));
                    } else
                    {
                        notify(alerts.get(0).message());
                    }
                });
            } else
            {
                notify(batch.get(0).message());
            }
        }

        private void notify(String message)
        {
            notificationService.show(Notification.builder(message).setType(Notification.Type.Warning).build());
        }
    }
}
