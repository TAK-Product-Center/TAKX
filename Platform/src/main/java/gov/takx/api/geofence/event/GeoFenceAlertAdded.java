package gov.takx.api.geofence.event;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.geofence.GeoFenceAlert;

/**
 * CDI Event fired when a GeoFence alert is added.  Ths occurs when a monitored map item triggers a monitor by crossing
 * into or out of a {@link gov.takx.api.geofence.GeoFence} perimeter.
 *
 * @param alert The alert that was added
 * @since 5.3
 */
public record GeoFenceAlertAdded(GeoFenceAlert alert) implements PlatformEvent
{
}
