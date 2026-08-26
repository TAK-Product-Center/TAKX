package gov.takx.api.geofence;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.mapobject.IShapeMapObject;
import jakarta.annotation.Nonnull;

/**
 * Service contract for managing {@link GeoFence} instances.  Geofences are immutable objects created using {@link GeoFence.Builder}.
 * Using the resulting geofence, you can call {@link #addOrUpdateGeoFence(GeoFence)} to install it.
 * <p/>
 * Because they are immutable, in order to modify one, you copy it using {@link GeoFence#builder(GeoFence)}, and then
 * modify any properties you want.  Then pass the geofence obtained from {@link GeoFence.Builder#build()} to
 * {@link #addOrUpdateGeoFence(GeoFence)} to update it.
 *
 * @since 5.3
 */
@InjectableService
public interface IGeoFenceManager
{
    /**
     * Add or update the given {@link GeoFence}, creating/re-creating a monitor for it as needed.  The UID of the shape
     * defining the GeoFence boundary will be used to determine if a previously registered GeoFence should be updated
     * or a new one added.
     *
     * @param geoFence GeoFence to add/update
     * @throws IllegalArgumentException If the shape referenced by the {@code geoFence} is not a {@link IShapeMapObject}
     *         or cannot be found
     * @see GeoFence#shapeUid()
     */
    void addOrUpdateGeoFence(@Nonnull GeoFence geoFence) throws IllegalArgumentException;

    /**
     * Remove the given {@code geoFence} and any associated monitor.
     *
     * @param geoFence GeoFence to remove from processing
     * @throws IllegalArgumentException If the shape referenced by the {@code geoFence} is not an {@link IShapeMapObject}
     */
    void removeGeoFence(@Nonnull GeoFence geoFence) throws IllegalArgumentException;
}
