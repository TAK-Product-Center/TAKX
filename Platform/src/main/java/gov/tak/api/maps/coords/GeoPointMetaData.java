package gov.tak.api.maps.coords;

import gov.tak.api.util.AttributeSet;
import gov.takx.dataservices.utils.AttributeSetConverter;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hibernate.Length;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a GeoPoint which exposes additional metadata associated with the point.    When used
 * in conjunction with higher level items such as PointMapItem, the information is stored within
 * the MapObjects attribute set by using the appropriate setPoint(GeoPointMetaData) and getGeoPointMetaData();
 *
 * @since 3.0
 */
@Embeddable
public class GeoPointMetaData
{
    public final static String CHECKSUM = "geopoint.check";
    public final static String GEOPOINT_SOURCE = "geopointsrc";
    public final static String ALTITUDE_SOURCE = "altsrc";

    // Commonly Used Altitude Sources
    public final static String DTED0 = "DTED0";
    public final static String DTED1 = "DTED1";
    public final static String DTED2 = "DTED2";
    public final static String DTED3 = "DTED3";
    public final static String LIDAR = "LIDAR";

    public final static String USER = "USER";
    public final static String UNKNOWN = "???";
    public final static String GPS = "GPS";
    public final static String SRTM1 = "SRTM1";
    public final static String COT = "COT";

    public final static String CALCULATED = "CALC";
    public final static String ESTIMATED = "ESTIMATED";
    public final static String RTK = "RTK";
    public final static String DGPS = "DGPS";
    public final static String PPS = "GPS_PPS";

    public final static String PRECISE_IMAGE_FILE = "PRECISE_IMAGE_FILE";
    public final static String PRECISE_IMAGE_FILE_X = "PRECISE_IMAGE_FILE_X";
    public final static String PRECISE_IMAGE_FILE_Y = "PRECISE_IMAGE_FILE_Y";

    @Embedded
    private GeoPoint geopoint = GeoPoint.UNKNOWN_POINT;

    @Convert(converter = AttributeSetConverter.class)
    @Column(length = Length.LONG32)
    private final AttributeSet metadata = new AttributeSet();

    public GeoPointMetaData()
    {
    }

    /**
     * Copy constructor.
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public GeoPointMetaData(@Nonnull GeoPointMetaData other)
    {
        set(other);
    }

    public GeoPointMetaData(@Nonnull GeoPoint point)
    {
        set(point);
    }

    /**
     * Set the GeoPoint.  If the given GeoPoint is different than the current GeoPoint, then the existing attributes
     * are cleared.
     *
     * @param geoPoint the geopoint that will be associated with metadata.
     * @return the GeoPointMetaData instance for chaining calls.
     */
    @Nonnull
    public GeoPointMetaData set(@Nonnull final GeoPoint geoPoint)
    {
        if (geoPoint.equals(geopoint)) return this;

        geopoint = geoPoint.isMutable() ? new GeoPoint(geoPoint, GeoPoint.Access.READ_ONLY) : geoPoint;

        metadata.clear();
        metadata.setAttribute(CHECKSUM, geoPoint.toStringRepresentation());
        return this;
    }

    /**
     * Obtain the GeoPoint that is described by this GeoPointMetaData instance.
     *
     * @return the geopoint. The resulting GeoPoint is not mutable.
     */
    @Nonnull
    public GeoPoint get()
    {
        return geopoint;
    }

    /**
     * Shortcut for setting the metadata for the altitude source.
     *
     * @param source the altitude source as a string.
     * @return the instance of GeoPointMetaData for chaining
     */
    @Nonnull
    public GeoPointMetaData setAltitudeSource(@Nonnull String source)
    {
        metadata.setAttribute(ALTITUDE_SOURCE, source);
        return this;
    }

    /**
     * Shortcut for setting the metadata for the altitude source.
     *
     * @param source the altitude source as a string.
     * @return the instance of GeoPointMetaData for chaining
     */
    @Nonnull
    public GeoPointMetaData setGeoPointSource(@Nonnull final String source)
    {
        metadata.setAttribute(GEOPOINT_SOURCE, source);
        return this;
    }

    /**
     * Get the associated GeoPoint source information.
     *
     * @return the String representing the source.
     */
    @Nonnull
    public String getGeopointSource()
    {
        return metadata.getStringAttribute(GEOPOINT_SOURCE, UNKNOWN);
    }

    /**
     * Get the associated Altitude source information.
     *
     * @return the String representing the altitude source or if that has not been set, assume it
     * it is the same as the geopoint source.
     */
    @Nonnull
    public String getAltitudeSource()
    {
        return (metadata.containsAttribute(ALTITUDE_SOURCE))
                ? metadata.getStringAttribute(ALTITUDE_SOURCE)
                : getGeopointSource();
    }

    /**
     * @return the attribute set for this point.
     */
    @Nonnull
    public AttributeSet getMetaData()
    {
        return metadata;
    }

    /**
     * Copy the contents of another GeoPointMetaData instance.
     *
     * @param geoPointMetaData the GeoPointMetaData instance.
     * @return updates to the GeoPointMetaData that at least have the keys described by the passed
     * in GeoPointMetaData object but could have more if the GeoPoints are identical.   This is
     * used to aggregate data.
     */
    @Nonnull
    public GeoPointMetaData set(@Nonnull final GeoPointMetaData geoPointMetaData)
    {
        set(geoPointMetaData.get());
        metadata.putAll(geoPointMetaData.getMetaData());
        return this;
    }

    /**
     * Wrap an existing GeoPoint as a GeoPointMetaData.
     *
     * @param geoPoint the geopoint
     * @return an empty GeoPointMetaData
     */
    @Nonnull
    public static GeoPointMetaData wrap(@Nonnull final GeoPoint geoPoint)
    {
        return new GeoPointMetaData(geoPoint);
    }

    /**
     * Wrap an existing GeoPoint as a GeoPointMetaData with a defined geopointSource and altitudeSource.
     *
     * @param geoPoint       the geopoint
     * @param geopointSource the geopoint source
     * @param altitudeSource the altitude source
     * @return GeoPointMetaData with preset geopointSource and altitudeSource values.
     */
    @Nonnull
    public static GeoPointMetaData wrap(@Nonnull final GeoPoint geoPoint, @Nonnull final String geopointSource,
                                        @Nonnull final String altitudeSource)
    {
        return new GeoPointMetaData(geoPoint)
                .setGeoPointSource(geopointSource)
                .setAltitudeSource(altitudeSource);
    }

    /**
     * Wrap an existing GeoPoint array as a GeoPointMetaData array.
     *
     * @param points the geopoint array
     * @return the geopoint array
     */
    @Nonnull
    public static GeoPointMetaData[] wrap(@Nonnull GeoPoint[] points)
    {
        final GeoPointMetaData[] wrapped = new GeoPointMetaData[points.length];

        for (int i = 0, ptsLength = points.length; i < ptsLength; i++) wrapped[i] = new GeoPointMetaData(points[i]);

        return wrapped;
    }

    /**
     * Wrap an existing GeoPoint list as a GeoPointMetaData list.
     *
     * @param points the geopoint list
     * @return the GeoPointMetadata list
     */
    @Nonnull
    public static List<GeoPointMetaData> wrap(@Nonnull List<GeoPoint> points)
    {
        final List<GeoPointMetaData> wrapped = new ArrayList<>(points.size());

        for (GeoPoint pt : points) wrapped.add(new GeoPointMetaData(pt));

        return wrapped;
    }

    /**
     * Given a list of GeoPointMetaData objects, create a list of contained GeoPoints.
     *
     * @param geoPointMetaDataList list of GeoPointMetaData items
     * @return list of GeoPoints
     */
    @Nonnull
    public static List<GeoPoint> unwrap(@Nonnull List<GeoPointMetaData> geoPointMetaDataList)
    {
        final List<GeoPoint> unwrapped = new ArrayList<>(geoPointMetaDataList.size());

        for (GeoPointMetaData pt : geoPointMetaDataList) unwrapped.add(pt.get());

        return unwrapped;
    }

    /**
     * Given an array of GeoPointMetaData objects, create a list of contained GeoPoints.
     *
     * @param geoPointMetaDataArray the array of GeoPointMetaData items.
     * @return the GeoPoints.
     */
    @Nonnull
    public static GeoPoint[] unwrap(@Nonnull GeoPointMetaData[] geoPointMetaDataArray)
    {
        final GeoPoint[] unwrapped = new GeoPoint[geoPointMetaDataArray.length];

        for (int i = 0; i < geoPointMetaDataArray.length; ++i)
        {
            if (geoPointMetaDataArray[i] != null) unwrapped[i] = geoPointMetaDataArray[i].get();
        }
        return unwrapped;
    }

    /**
     * Get a human readable representation of the GeoPoint in the format:
     * {@code latitude, longitude[, altitude]}
     *
     * @return Geo point string
     */
    @Override
    public String toString()
    {
        return geopoint.toString();
    }
}
