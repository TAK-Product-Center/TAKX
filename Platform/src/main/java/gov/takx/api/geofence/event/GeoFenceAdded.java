package gov.takx.api.geofence.event;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.mapobject.IShapeMapObject;

/**
 * CDI Event fired when a GeoFence is added.  Ths occurs when a {@link gov.takx.api.geofence.GeoFence} is added via
 * the {@link gov.takx.api.geofence.IGeoFenceManager}.  It only occurs on the first addition of the GeoFence.  it is
 * not fired again (in the case of an update) unless the GeoFence is removed and then re-added.
 *
 * @param geoFence The GeoFence that was added
 * @param shapeMapObject Shape map object defining the GeoFence boundary
 * @see GeoFence
 * @see gov.takx.api.geofence.IGeoFenceManager#addOrUpdateGeoFence(GeoFence)
 * @since 5.3
 */
public record GeoFenceAdded(GeoFence geoFence, IShapeMapObject<?> shapeMapObject) implements PlatformEvent
{
}
