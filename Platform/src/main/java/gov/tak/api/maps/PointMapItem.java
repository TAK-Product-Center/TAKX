package gov.tak.api.maps;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

/**
 * Contract for an item (Map Object) at a specific {@link GeoPoint} on the globe.
 * <p>
 * Bridge interface to attempt to harmonize the TAKX code with the ATAK code (where PointMapItem is a concrete class).
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface PointMapItem extends IMapObject
{
    /**
     * Set the GeoPoint and associated metadata for this map item from the given geoPointMetaData.
     *
     * @param geoPointMetaData Source to copy in
     */
    void setPoint(@Nonnull GeoPointMetaData geoPointMetaData);

    /**
     * @return GeoPoint for the map item
     */
    GeoPoint getPoint();

    /**
     * Set the GeoPoint for this map item.
     *
     * @param geoPoint new GeoPoint
     */
    void setPoint(@Nonnull GeoPoint geoPoint);
}
