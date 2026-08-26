package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Envelope utilities.
 *
 * @since 5.8
 */
public final class EnvelopeUtils
{
    // WGS84 authalic radius (equal-area sphere), meters
    private static final double R = 6371007.180918475;
    private static final double METERS_PER_DEG_LAT = 111132.92;

    private EnvelopeUtils()
    {
    }

    /**
     * @param envelope Envelope of an area.
     * @return GeoBounds of the area.
     */
    @Nonnull
    public static GeoBounds toGeoBounds(@Nonnull Envelope envelope)
    {
        double minLat = clampLat(envelope.minY);
        double maxLat = clampLat(envelope.maxY);
        double minLon = wrapLon(envelope.minX);
        double maxLon = wrapLon(envelope.maxX);
        return new GeoBounds(new GeoPoint(minLat, minLon), new GeoPoint(maxLat, maxLon));
    }

    /**
     * @param envelope Envelope of an area.
     * @return Sector of the area.
     */
    @Nonnull
    public static Sector toSector(@Nonnull Envelope envelope)
    {
        return new Sector(Angle.fromDegrees(envelope.minY),
                Angle.fromDegrees(envelope.maxY),
                Angle.fromDegrees(envelope.minX),
                Angle.fromDegrees(envelope.maxX));
    }

    /**
     * @param envelope Envelope to compute area of.
     * @return Area in meters squared.
     */
    public static double areaMetersSquared(@Nullable Envelope envelope)
    {
        if (envelope == null) return Double.NaN;

        double minLat = clampLat(envelope.minY);
        double maxLat = clampLat(envelope.maxY);
        double minLon = wrapLon(envelope.minX);
        double maxLon = wrapLon(envelope.maxX);

        double avgLat = (minLat + maxLat) / 2.0;

        double metersPerDegLon = Math.cos(Math.toRadians(avgLat)) * 2 * Math.PI * R / 360.0;

        double widthMeters = (maxLon - minLon) * metersPerDegLon;
        double heightMeters = (maxLat - minLat) * METERS_PER_DEG_LAT;

        return Math.abs(widthMeters * heightMeters);
    }

    private static double clampLat(double lat)
    {
        if (Double.isNaN(lat)) return Double.NaN;
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    private static double wrapLon(double lon)
    {
        if (Double.isNaN(lon)) return Double.NaN;
        double x = lon % 360.0;
        if (x > 180.0) x -= 360.0;
        if (x <= -180.0) x += 360.0;
        return x;
    }
}
