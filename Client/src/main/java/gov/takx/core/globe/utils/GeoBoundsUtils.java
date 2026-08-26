package gov.takx.core.globe.utils;

import com.atakmap.coremap.maps.coords.GeoBounds;
import gov.tak.api.maps.coords.GeoPoint;
import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Contains utilities for working with {@link GeoBounds}, including the required conversions from {@link GeoPoint} to 
 * {@link com.atakmap.coremap.maps.coords.GeoPoint}.
 * 
 * @since 5.3
 */
public class GeoBoundsUtils
{
    /**
     * Creates new {@link GeoBounds bounds} from one or more {@link GeoPoint points}.
     * 
     * @param geoPoints The points
     * @return The new bounds
     * @throws IllegalArgumentException if no points are provided
     */
    @Nonnull
    public static GeoBounds fromGeoPoints(@Nonnull GeoPoint... geoPoints)
    {
        if (geoPoints.length == 0) throw new IllegalArgumentException("At least one point is required");

        // Wrap in ATAK GeoPoints and convert to array
        final com.atakmap.coremap.maps.coords.GeoPoint[] atakGeoPoints =
                Arrays.stream(geoPoints)
                        .map(GeoBoundsUtils::toAtakGeoPoint)
                        .toArray(com.atakmap.coremap.maps.coords.GeoPoint[]::new);

        return GeoBounds.createFromPoints(atakGeoPoints);
    }

    /**
     * Adds a {@link GeoPoint point} to existing {@link GeoBounds bounds}.  
     * 
     * @param geoBounds the bounds to update
     * @param geoPoint the point to add
     * @return New bounds that include the point, or the original bounds if they already contain the point.
     */
    @Nonnull
    public static GeoBounds union(@Nonnull GeoBounds geoBounds, @Nonnull GeoPoint geoPoint)
    {
        if (contains(geoBounds, geoPoint)) return geoBounds;

        final var atakGeoPoint = toAtakGeoPoint(geoPoint);
        final var builder = new GeoBounds.Builder();

        builder.add(geoBounds);
        builder.add(atakGeoPoint.getLatitude(), atakGeoPoint.getLongitude());

        return builder.build();
    }

    /**
     * Checks to see if {@link GeoBounds bounds} contain a {@link GeoPoint point}.
     * 
     * @param geoBounds The bounds to check
     * @param geoPoint The point
     * @return {@code true} if the {@link GeoBounds bounds} contain the {@link GeoPoint point}
     */
    public static boolean contains(@Nonnull GeoBounds geoBounds, @Nonnull GeoPoint geoPoint)
    {
        final var atakGeoPoint = new com.atakmap.coremap.maps.coords.GeoPoint(geoPoint);
        return geoBounds.contains(atakGeoPoint);
    }

    /**
     * Returns the {@link GeoBounds bounds} extents in decimal degrees, ordered: S latitude, W longitude, min altitude,
     * N latitude, E longitude, max altitude.
     * 
     * @param geoBounds The bound to convert
     * @return The bounds' extents in decimal degrees
     */
    @Nonnull
    public static double[] toDegreesAndAlt(@Nonnull GeoBounds geoBounds)
    {
        return new double[]{geoBounds.getSouth(), geoBounds.getWest(), geoBounds.getMinAltitude(), geoBounds.getNorth(),
                geoBounds.getEast(), geoBounds.getMaxAltitude()};
    }

    /**
     * Returns the {@link GeoBounds bounds} defined by the array of decimal degrees. The array must be ordered: 
     * S latitude, W longitude, min altitude, N latitude, E longitude, max altitude.
     *
     * @param degreesAndAlt The decimal degrees and altitudes to convert; the array must be ordered: S latitude,
     *                      W longitude, min altitude, N latitude, E longitude, max altitude
     * @return The bounds defined by the decimal degrees
     * @throws IllegalArgumentException thrown if the {@code degreesAndAlt} is not the correct length
     */
    @Nonnull
    public static GeoBounds fromDegreesAndAlt(@Nonnull double[] degreesAndAlt)
    {
        if (degreesAndAlt.length != 6) throw new IllegalArgumentException("degreesAndAlt must have a length of 6 but was " + degreesAndAlt.length);

        return new GeoBounds(degreesAndAlt[0], degreesAndAlt[1], degreesAndAlt[2], degreesAndAlt[3], degreesAndAlt[4], degreesAndAlt[5]);
    }

    /**
     * Converts a TAK {@link GeoPoint} to an ATAK {@link com.atakmap.coremap.maps.coords.GeoPoint}.
     *
     * @param takGeoPoint A TAK geo point
     * @return An ATAK geo point
     */
    private static com.atakmap.coremap.maps.coords.GeoPoint toAtakGeoPoint(@Nonnull GeoPoint takGeoPoint)
    {
        // Consider switching to MarshalManager if kernel adds a marshaller
        return new com.atakmap.coremap.maps.coords.GeoPoint(takGeoPoint);
    }

    private GeoBoundsUtils()
    {
    }
}
