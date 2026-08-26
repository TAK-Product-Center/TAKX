package gov.takx.api.geofence.event;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.geofence.GeoFenceAlert;

/**
 * CDI Event fired when a GeoFence alert is removed.  This happens when a user dismisses a single alert, or clears
 * all alerts for a given {@link gov.takx.api.geofence.GeoFence}
 *
 * @param alert The alert that was removed
 * @since 5.3
 */
public record GeoFenceAlertRemoved(GeoFenceAlert alert) implements PlatformEvent
{
}
