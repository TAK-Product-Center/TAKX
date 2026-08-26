package gov.takx.api.emergency;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.emergency.event.EmergencyAlertAdded;
import gov.takx.api.emergency.event.EmergencyAlertRemoved;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Contract for the service that manages Emergency Alerts.
 *
 * @since 5.7
 */
@InjectableService
@IncubatingApi(since = "5.7")
@NoPluginImplementations
public interface IEmergencyAlertManager
{
    /**
     * Add an alert on the given Map Item.  This will fire an {@link EmergencyAlertAdded} event, resulting in an alert
     * being added to Overlays, a Notification, and a globe widget showing the event data.
     *
     * @param pointMapItem Alert map item
     * @param type Alert type
     * @param message Alert message
     * @return The alert created
     */
    @Nonnull
    EmergencyAlert addAlert(@Nonnull PointMapItem pointMapItem, @Nonnull String type, @Nonnull String message);

    /**
     * Remove an alert for the given Map Item. This will fire an {@link EmergencyAlertRemoved} event,
     * resulting in the alert being removed from the Overlays.
     *
     * @param pointMapItem Alert map item
     */
    void removeAlert(@Nonnull PointMapItem pointMapItem);

    /**
     * Remove the given {@code emergencyAlert}.
     * @see #removeAlert(PointMapItem)
     */
    void removeAlert(@Nonnull EmergencyAlert emergencyAlert);

    /**
     * @return List of all {@link EmergencyAlert alerts} recorded, may be empty, never {@code null}
     */
    @Nonnull
    List<EmergencyAlert> getAllAlerts();

    /**
     * @return A string describing the range and bearing from the Self Location marker to the given alert.
     *         If either location is invalid, then {@code (No GPS)} is returned.
     */
    @Nonnull
    String getRangeAndBearing(@Nonnull EmergencyAlert alert);

    /**
     * Send an emergency alert CoT event locally and to all TAKServers.
     *
     * @param alertType Alert type to send
     * @param message Optional message to include in the event
     */
    void broadcastAlert(@Nonnull EmergencyAlertType alertType, @Nullable String message);

    /**
     * Send an emergency alert cancellation event locally and to all TAKServers.
     *
     * @param alertType Alert type to cancel
     */
    void cancelAlert(@Nonnull EmergencyAlertType alertType);
}
