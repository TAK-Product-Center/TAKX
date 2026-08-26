package gov.takx.core.emergency;

import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.CommoException;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.comms.commo.DeviceDisplayType;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.emergency.EmergencyAlert;
import gov.takx.api.emergency.EmergencyAlertType;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.utils.DirectionType;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.platform.preferences.PreferenceEntry.CALL_SIGN;
import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_DISPLAY_TYPE;

/**
 * Service to manage emergency alerts.
 *
 * @since 5.7
 */
@ApplicationScoped
public class EmergencyAlertManager implements IEmergencyAlertManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMapObjectManager mapObjectManager;
    private final INotificationService notificationService;
    private final ICommonCommoManager commonCommoManager;
    private final UUID nodeUuid;
    private final Event<EmergencyAlertAdded> emergencyAlertAddedEvent;
    private final Event<EmergencyAlertRemoved> emergencyAlertRemovedEvent;

    private final Map<String, EmergencyAlert> alertsByUid = new ConcurrentHashMap<>();

    private final String selfAlertUid;

    @Inject
    EmergencyAlertManager(IMapObjectManager mapObjectManager, INotificationService notificationService,
                          ICommonCommoManager commonCommoManager, @NodeUUID UUID nodeUuid,
                          Event<EmergencyAlertAdded> emergencyAlertAddedEvent, Event<EmergencyAlertRemoved> emergencyAlertRemovedEvent)
    {
        this.mapObjectManager = mapObjectManager;
        this.notificationService = notificationService;
        this.commonCommoManager = commonCommoManager;
        this.nodeUuid = nodeUuid;
        this.emergencyAlertAddedEvent = emergencyAlertAddedEvent;
        this.emergencyAlertRemovedEvent = emergencyAlertRemovedEvent;

        selfAlertUid = nodeUuid + "-9-1-1";
    }

    @Override
    @Nonnull
    public List<EmergencyAlert> getAllAlerts()
    {
        return List.copyOf(alertsByUid.values());
    }

    /**
     * Observe new Map Objects and create an alert if they have the {@link StandardAttributeNames#EMERGENCY} attribute.
     */
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        final var mapObject = event.getMapObject();
        if (mapObject.hasMetaValue(StandardAttributeNames.EMERGENCY) && mapObject instanceof PointMapItem pointMapItem)
        {
            final String type = mapObject.getMetaString(StandardAttributeNames.EMERGENCY, "911");
            final String message = mapObject.getMetaString(StandardAttributeNames.EMERGENCY_MESSAGE, "");
            addAlert(pointMapItem, type, message);
        }
    }

    @Nonnull
    @Override
    public EmergencyAlert addAlert(@Nonnull PointMapItem pointMapItem, @Nonnull String type, @Nonnull String message)
    {
        final var uid = pointMapItem.getUid();
        final var alert = new EmergencyAlert(pointMapItem, getCallsign(pointMapItem), type, message);
        alertsByUid.put(uid, alert);

        emergencyAlertAddedEvent.fire(new EmergencyAlertAdded(alert));

        final var notification = Notification.builder("Emergency: " + alert.message(), Notification.Type.Warning).build();
        notificationService.show(notification);

        return alert;
    }

    @Override
    public void removeAlert(@Nonnull EmergencyAlert alert)
    {
        removeAlert(alert.mapItem());
    }

    @Override
    public void removeAlert(@Nonnull PointMapItem pointMapItem)
    {
        final var uid = pointMapItem.getUid();
        final var removedAlert = alertsByUid.remove(uid);

        if (removedAlert != null)
        {
            mapObjectManager.deleteMapObject(pointMapItem);
            emergencyAlertRemovedEvent.fire(new EmergencyAlertRemoved(removedAlert));
        } else
        {
            logger.warn("Emergency Alert not found: {}", uid);
        }
    }

    @Override
    public void broadcastAlert(@Nonnull EmergencyAlertType alertType, @Nullable String message)
    {
        sendEmergencyCotEvent(alertType, message, false);
    }

    @Override
    public void cancelAlert(@Nonnull EmergencyAlertType alertType)
    {
        sendEmergencyCotEvent(alertType, null, true);
    }

    /**
     * Send an emergency alert CoT event locally and to all TAKServers.
     *
     * @param alertType Alert type to send
     * @param alertMessage Optional message to include in the event
     * @param cancel Pass {@code true} to send a cancel message
     */
    private void sendEmergencyCotEvent(@Nonnull EmergencyAlertType alertType, @Nullable String alertMessage, boolean cancel)
    {
        logger.debug("broadcastAlert: {}, message = '{}' cancel = {}", alertType, alertMessage, cancel);

        final var location = mapObjectManager.getSelfMarker().getLocation();

        if (!location.isValid() && !cancel) // An invalid location is ok when sending a cancellation
        {
            final String message = "Self location is not set; can't send alert CoT message";
            logger.warn(message);
            final var notification = Notification.builder(message, Notification.Type.Error).build();
            notificationService.show(notification);
        } else
        {
            final var cotType = (cancel ? EmergencyAlertType.Cancel : alertType).getCotType();
            final var event = createEmergencyCotEvent(cotType, alertType.getDescription(), alertMessage, location, cancel);

            try
            {
                final var cotMessage = event.toString();
                logger.debug("cotMessage: {}", cotMessage);

                final var message = cancel ? "Canceling Emergency Alert" : ("Broadcasting Emergency Alert: " + alertType.getDescription());
                final var notification = Notification.builder(message, Notification.Type.Success).build();
                notificationService.show(notification);

                commonCommoManager.broadcastCoT(cotMessage);
                commonCommoManager.broadcastCoT(cotMessage, CoTSendMethod.TAK_SERVER);
            } catch (CommoException e)
            {
                final var message = "Failed to broadcast emergency alert CoT message";
                logger.warn(message, e);
                final var notification = Notification.builder(message + e, Notification.Type.Error).build();
                notificationService.show(notification);
            }
        }
    }

    /**
     * Create the CoT event for announcing or canceling an emergency alert.
     *
     * @param cotType The Alert CoT type
     * @param alertType The alert type (description)
     * @param alertMessage Optional message to include in the {@code emergency} detail
     * @param location Location to use in the event
     * @param cancel Pass {@code true} to send a cancel message
     * @return The created event
     */
    @Nonnull
    private CotEvent createEmergencyCotEvent(@Nonnull String cotType, @Nonnull String alertType, @Nullable String alertMessage,
                                             @Nonnull GeoPoint location, boolean cancel)
    {
        final var detail = new CotDetail();

        final var contact = new CotDetail(DetailConstants.CONTACT);
        final var alertCallsign = CALL_SIGN.getStringValue() + "-Alert";
        contact.setAttribute(DetailConstants.ATTR_CALLSIGN, alertCallsign);
        detail.addChild(contact);

        // To mirror ATAK, we only set either the 'type' or 'cancel' attribute.
        final var emergency = new CotDetail(StandardAttributeNames.EMERGENCY);
        if (cancel)
        {
            emergency.setAttribute("cancel", "true");

            // If a message hasn't already been specified for cancellation we should
            // fill it in with the alert callsign so that it doesn't specify show a
            // toast on ATAK "cancelled null".
            if (alertMessage == null) alertMessage = alertCallsign;
        } else
        {
            emergency.setAttribute(DetailConstants.ATTR_TYPE, alertType);
        }
        if (alertMessage != null) emergency.setInnerText(alertMessage);
        detail.addChild(emergency);

        // In a similar manner, cancel requests do not have a link
        if (!cancel)
        {
            final var link = new CotDetail(DetailConstants.LINK);
            link.setAttribute(DetailConstants.ATTR_TYPE, SA_REPORTING_DISPLAY_TYPE.getValueAs(DeviceDisplayType.class).getCotType());
            link.setAttribute(DetailConstants.ATTR_UID, nodeUuid.toString());
            link.setAttribute(DetailConstants.ATTR_RELATION, DetailConstants.LINK_RELATION_PARENT_PRODUCER);
            detail.addChild(link);
        }

        final var now = new CoordinatedTime();

        // If location is invalid, just send ZERO.  This should only happen when sending a cancel message.
        final var locationToUse = location.isValid() ? location : GeoPoint.ZERO_POINT;

        return new CotEvent(selfAlertUid, cotType, CotEvent.VERSION_2_0, locationToUse,
                now, now, now, CotConstants.HOW_HUMAN_ENTERED, detail, null, null, null);
    }

    @Override
    @Nonnull
    public String getRangeAndBearing(@Nonnull EmergencyAlert alert)
    {
        final var mapItem = alert.mapItem();
        final var alertLocation = mapItem.getLocation();
        final var selfMarker = mapObjectManager.getSelfMarker();
        final var selfLocation = selfMarker != null ? selfMarker.getLocation() : GeoPoint.UNKNOWN_POINT;

        if (selfLocation.isValid() && alertLocation.isValid())
        {
            final double bearing = selfLocation.bearingTo(alertLocation);
            final double distance = selfLocation.distanceTo(alertLocation);
            final String direction = DirectionType.getDirectionAbbreviation(bearing);

            if (!Double.isNaN(distance)) return RangeUnits.METERS.format(distance, 0) + ' ' + direction;
        }
        return "(No GPS)";
    }

    @Nonnull
    private static String getCallsign(@Nonnull PointMapItem mapItem)
    {
        final String title = mapItem.getTitle();
        return !StringUtils.isBlank(title) && title.contains("-")
                ? title.substring(0, title.lastIndexOf("-"))
                : title != null ? title : "";
    }
}
