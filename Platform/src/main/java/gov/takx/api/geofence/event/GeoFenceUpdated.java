package gov.takx.api.geofence.event;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IShapeMapObject;

/**
 * CDI Event fired when a GeoFence is updated.  Ths occurs when an existing {@link GeoFence} is updated via
 * the {@link gov.takx.api.geofence.IGeoFenceManager#addOrUpdateGeoFence(GeoFence)}.
 *
 * @param geoFence The GeoFence that was updated
 * @param shapeMapObject Shape map object defining the GeoFence boundary
 * @see GeoFence
 * @see gov.takx.api.geofence.IGeoFenceManager#addOrUpdateGeoFence(GeoFence)
 * @since 5.3
 */
public record GeoFenceUpdated(GeoFence geoFence, IShapeMapObject<?> shapeMapObject) implements PlatformEvent
{
}
