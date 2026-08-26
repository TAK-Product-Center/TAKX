package gov.takx.api.geofence.event;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IShapeMapObject;

/**
 * CDI Event fired when a GeoFence is removed.  Ths occurs when a {@link GeoFence} is removed via
 * the {@link gov.takx.api.geofence.IGeoFenceManager}.
 *
 * @param geoFence The GeoFence that was removed
 * @param shapeMapObject Shape map object defining the GeoFence boundary
 * @see GeoFence
 * @see gov.takx.api.geofence.IGeoFenceManager#removeGeoFence(GeoFence)
 * @since 5.3
 */
public record GeoFenceRemoved(GeoFence geoFence, IShapeMapObject<?> shapeMapObject) implements PlatformEvent
{
}
