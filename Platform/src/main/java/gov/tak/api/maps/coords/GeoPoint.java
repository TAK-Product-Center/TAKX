package gov.tak.api.maps.coords;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import jakarta.persistence.Embeddable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Description of a geospatial point latitude, longitude and altitude with the associated error ellipses.
 *
 * @since 3.0
 */
@Embeddable
public class GeoPoint implements ILocation
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        MarshalManager.registerMarshal(new IMarshal()
        {
            @Override
            public <T, V> T marshal(V in)
            {
                if (in == null)
                    return null;
                final var src = (com.atakmap.coremap.maps.coords.GeoPoint) in;
                AltitudeReference altitudeReference = MarshalManager.marshal(src.getAltitudeReference(), com.atakmap.coremap.maps.coords.GeoPoint.AltitudeReference.class, AltitudeReference.class);
                //noinspection unchecked
                return (T) new GeoPoint(src.getLatitude(), src.getLongitude(), src.getAltitude(), altitudeReference, src.getCE(), src.getLE());
            }
        }, com.atakmap.coremap.maps.coords.GeoPoint.class, ILocation.class);
    }

    public enum Access
    {
        READ_ONLY,
        READ_WRITE
    }

    public static final GeoPoint ZERO_POINT = new GeoPoint(0d, 0d);
    public static final GeoPoint UNKNOWN_POINT = new GeoPoint(UNKNOWN, UNKNOWN);

    private double latitude;
    private double longitude;
    private double altitude;
    private AltitudeReference altitudeReference;
    private double ce90;
    private double le90;
    private final Access access;

    public GeoPoint()
    {
        this(UNKNOWN, UNKNOWN);
    }

    /**
     * Constructs a read only geopoint with all of the parameters.
     *
     * @param latitude          the latitude position in decimal degrees [-90, 90]
     * @param longitude         the longitude position in decimal degrees [-180, 180]
     * @param altitude          the altitude in meters
     * @param altitudeReference the altitude reference in either AltitudeReference.HAE or AltitudeReference.AGL
     * @param ce90              circular error of 90 percent for the horizontal. A CE90 value is the minimum
     *                          diameter of the horizontal circle that can be centered on all photo-identifiable
     *                          Ground Control Points (GCPs) and also contain 90% of their respective twin
     *                          counterparts acquired in an independent geodetic survey.
     * @param le90              linear error of 90 percent for the vertical. A LE90 value represents the linear
     *                          vertical distance that 90% of control points and their respective twin matching
     *                          counterparts acquired in an independent geodetic survey should be found from each
     *                          other.
     * @since 3.4 ({@link AltitudeReference} moved from {@link GeoPoint} to {@link IGeoPoint}, changing method signature)
     */
    public GeoPoint(double latitude, double longitude, double altitude, AltitudeReference altitudeReference,
                    double ce90, double le90)
    {
        this(latitude, longitude, altitude, altitudeReference, ce90, le90, Access.READ_ONLY);
    }

    /**
     * Constructs a read only geopoint with the altitude specified as {@link AltitudeReference#HAE}.
     *
     * @param latitude  the latitude position in decimal degrees [-90, 90]
     * @param longitude the longitude position in decimal degrees [-180, 180]
     * @param altitude  the altitude in HAE METERS
     */
    public GeoPoint(double latitude, double longitude, double altitude, double ce90, double le90)
    {
        this(latitude, longitude, altitude, AltitudeReference.HAE, ce90, le90, Access.READ_ONLY);
    }

    /**
     * Constructs a read-only GeoPoint with altitude, CE90, LE90 set to {@link GeoPoint#UNKNOWN}, and
     * altitude reference of {@link AltitudeReference#HAE}
     *
     * @param latitude  the latitude position in decimal degrees [-90, 90]
     * @param longitude the longitude position in decimal degrees [-180, 180]
     */
    public GeoPoint(double latitude, double longitude)
    {
        this(latitude, longitude, UNKNOWN, AltitudeReference.HAE, UNKNOWN, UNKNOWN, Access.READ_ONLY);
    }

    /**
     * @param latitude  the latitude position in decimal degrees [-90, 90]
     * @param longitude the longitude position in decimal degrees [-180, 180]
     * @param altitude  the altitude in HAE METERS
     */
    public GeoPoint(double latitude, double longitude, double altitude)
    {
        this(latitude, longitude, altitude, AltitudeReference.HAE, UNKNOWN, UNKNOWN, Access.READ_ONLY);
    }

    /**
     * @param latitude          the latitude position in decimal degrees [-90, 90]
     * @param longitude         the longitude position in decimal degrees [-180, 180]
     * @param altitude          the altitude in METERS specified by the altitudeReference
     * @param altitudeReference the altitude reference
     * @since 3.4 ({@link AltitudeReference} moved from {@link GeoPoint} to {@link IGeoPoint}, changing method signature)
     */
    public GeoPoint(double latitude, double longitude, double altitude, AltitudeReference altitudeReference)
    {
        this(latitude, longitude, altitude, altitudeReference, UNKNOWN, UNKNOWN, Access.READ_ONLY);
    }

    /**
     * Full constructor for a GeoPoint.
     *
     * @param latitude          the latitude position in decimal degrees [-90, 90]
     * @param longitude         the longitude position in decimal degrees [-180, 180]
     * @param altitude          the altitude in meters
     * @param altitudeReference the altitude reference in either AltitudeReference.HAE or AltitudeReference.AGL
     * @param ce90              circular error of 90 percent for the horizontal. A CE90 value is the minimum
     *                          diameter of the horizontal circle that can be centered on all photo-identifiable
     *                          Ground Control Points (GCPs) and also contain 90% of their respective twin
     *                          counterparts acquired in an independent geodetic survey.
     * @param le90              linear error of 90 percent for the vertical. A LE90 value represents the linear
     *                          vertical distance that 90% of control points and their respective twin matching
     *                          counterparts acquired in an independent geodetic survey should be found from each
     *                          other.
     * @param access            the access for this geopoint as either Access.READ_ONLY or Access.READ_WRITE
     * @since 3.4 ({@link AltitudeReference} moved from {@link GeoPoint} to {@link IGeoPoint}, changing method signature)
     */
    public GeoPoint(double latitude, double longitude, double altitude, AltitudeReference altitudeReference,
                    double ce90, double le90, Access access)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.altitudeReference = altitudeReference;
        this.ce90 = ce90;
        this.le90 = le90;
        this.access = access;
    }

    /**
     * Copy constructor for a GeoPoint which preserves the access permissions of the original geopoint.
     *
     * @param other the geopoint to copy.
     */
    public GeoPoint(GeoPoint other)
    {
        this(other, other.access);
    }

    /**
     * Copy constructor for a GeoPoint which allows for modifying the access of the copy.
     *
     * @param other  the geopoint to copy.
     * @param access the access for the GeoPoint of either Access.READ_ONLY or Access.READ_WRITE
     */
    public GeoPoint(GeoPoint other, Access access)
    {
        this(other.latitude, other.longitude, other.altitude, other.altitudeReference, other.ce90, other.le90, access);
    }

    /**
     * Constructs a read-only GeoPoint with CE90, LE90 set to {@link GeoPoint#UNKNOWN}, and
     * altitude reference of {@link AltitudeReference#HAE}.
     *
     * @param location the location
     */
    public GeoPoint(ILocation location)
    {
        this(location.getLatitude(), location.getLongitude(), location.getAltitude());
    }

    /**
     * Constructs a read-only GeoPoint with altitude reference of {@link AltitudeReference#HAE}
     *
     * @param other the IGeoPoint describing a location with latitude and longitude
     * @since 5.1
     */
    public GeoPoint(IGeoPoint other)
    {
        this(other.getLatitude(), other.getLongitude(), other.getAltitude(), other.getCE(), other.getLE());
    }

    /**
     * Creates a mutable Geopoint which is initially invalid.
     *
     * @return the mutable geopoint.
     */
    public static GeoPoint createMutable()
    {
        return new GeoPoint(UNKNOWN_POINT, Access.READ_WRITE);
    }

    /**
     * Checks the validity of the GeoPoint
     *
     * @return returns true if the GeoPoint is valid.
     */
    public boolean isValid()
    {
        return isValid(latitude, longitude);
    }

    /**
     * Checks the validity of the provided latitude and longitude.
     *
     * @param latitude  the latitude as specified in degrees.
     * @param longitude the longitude as specified in degrees.
     * @return returns true if the latitude and longitude are valid.
     */
    public static boolean isValid(double latitude, double longitude)
    {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude)
                && latitude >= -90d && longitude >= -180d && latitude <= 90d && longitude <= 180d;
    }

    /**
     * Checks the validity of the GeoPoints altitude. It is invalid if it is {@link Double#NaN}.
     *
     * @return returns true if the Altitude is valid.
     * @see #isAltitudeValid(double)
     */
    public boolean isAltitudeValid()
    {
        return isAltitudeValid(altitude);
    }

    /**
     * Checks the validity of a raw altitude.  It is invalid if it is {@link Double#NaN}.
     *
     * @return returns true if the Altitude is valid.
     * @see Double#isNaN(double)
     */
    public static boolean isAltitudeValid(final double altitude)
    {
        return !Double.isNaN(altitude);
    }

    /**
     * Is the geopoint capable of being changed with the appropriate set methods.
     *
     * @return true if it is mutable.
     */
    public boolean isMutable()
    {
        return access == Access.READ_WRITE;
    }

    /**
     * Calculate the distance from this geopoint to another geopoint.
     *
     * @param p the point to measure to
     * @return the distance in meters
     */
    public double distanceTo(final GeoPoint p)
    {
        return GeoCalculations.distance(this, p);
    }

    /**
     * Convenience method to calculate a bearing from this geopoint to another geopoint.
     *
     * @param p the point to measure to
     * @return the bearing from p1 to p2
     */
    public double bearingTo(final GeoPoint p)
    {
        return GeoCalculations.bearing(this, p);
    }

    /**
     * @return latitude in decimal degrees
     */
    @Override
    public double getLatitude()
    {
        return latitude;
    }

    /**
     * @return longitude in decimal degrees
     */
    @Override
    public double getLongitude()
    {
        return longitude;
    }

    /**
     * @return altitude in meters
     */
    @Override
    public double getAltitude()
    {
        return altitude;
    }

    /**
     * @return the altitude reference
     */
    @Override
    public AltitudeReference getAltitudeReference()
    {
        return altitudeReference;
    }

    /**
     * @return the circular error of 90%, {@link Double#NaN} if invalid
     */
    @Override
    public double getCE()
    {
        return ce90;
    }

    /**
     * @return the linear error of 90%, {@link Double#NaN} if invalid
     */
    @Override
    public double getLE()
    {
        return le90;
    }

    /**
     * Get a human readable representation of the GeoPoint
     * <p>
     * Valid formats include:
     * <ul>
     * <li>lat,lng</li>
     * <li>lat,lng,alt(HAE)</li>
     * <li>lat,lng,alt,altReference</li>
     * <li>lat,lng,alt,altReference,CE</li>
     * <li>lat,lng,alt,altReference,CE,LE</li>
     * </ul>
     * <p>
     * There are tools that currently make use of this format for machine to machine communications
     *
     * @param numArgs Number of arguments (2 - 6)
     * @return Geo point string
     */
    public String toString(int numArgs)
    {
        if (numArgs < 2 || numArgs > 6) return "";

        StringBuilder sb = new StringBuilder();
        sb.append(latitude).append(",").append(longitude);

        if (numArgs >= 3)
        {
            if (numArgs == 3 && altitudeReference != AltitudeReference.HAE)
            {
                logger.error("cannot convert from AGL to HAE, returning raw altitude: {}", altitude, new Exception());
            }
            sb.append(",").append(altitude);
        }

        if (numArgs >= 4) sb.append(",").append(altitudeReference);
        if (numArgs >= 5) sb.append(",").append(ce90);
        if (numArgs >= 6) sb.append(",").append(le90);

        return sb.toString();
    }

    /**
     * Get a human readable representation of the GeoPoint in the format:
     * latitude, longitude, altitude (HAE)
     */
    @Override
    public String toString()
    {
        return toString(isAltitudeValid() ? 3 : 2);
    }

    /**
     * Get a human readable representation of the GeoPoint in the format:
     * latitude,longitude,altitude,altitudeReference,ce90,le90
     */
    public String toStringRepresentation()
    {
        return toString(6);
    }

    /**
     * Given a String of format lat, lon or lat, lon, alt where all values are stored in the double
     * type, this method returns a GeoPoint instance.
     *
     * @param str A String containing a coordinate pair or a coordinate pair plus altitude. All
     *            values are to be delimited by a comma. Example: "34.831, -112.243" or
     *            "34.831, -112.243, 543.2" are valid values. The ALTITUDE, if present is in HAE /
     *            METERS.
     *            <p>
     *            There is also a 6 parameter serialization produced by the toString method of this class.
     * @return A GeoPoint instance or {@code null} if the String cannot be parsed.
     */
    public static GeoPoint parseGeoPoint(String str)
    {
        if (str == null) return null;

        final String[] parts = str.split(",");

        // for some reason the point has an invalid format
        if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty())
        {
            logger.error("unable to parse: {}", str);
            return null;
        }

        double lat;
        double lon;
        double altitude = UNKNOWN;
        AltitudeReference altitudeReference = AltitudeReference.HAE;
        double ce = UNKNOWN;
        double le = UNKNOWN;

        try
        {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());

            if (parts.length > 2) altitude = Double.parseDouble(parts[2].trim());
            if (parts.length > 3) altitudeReference = AltitudeReference.valueOf(parts[3].trim());
            if (parts.length > 4) ce = Double.parseDouble(parts[4].trim());
            if (parts.length > 5) le = Double.parseDouble(parts[5].trim());
        } catch (Exception e)
        {
            logger.error("Unable to parse: {}", str, e);
            return null;
        }

        return new GeoPoint(lat, lon, altitude, altitudeReference, ce, le);
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param location RaptorLocation from which to obtain the latitude, longitude, and elevation
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(RaptorLocation location)
    {
        if (access == Access.READ_ONLY) return null;

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude(); // use getAltitude to get HAE
        ce90 = UNKNOWN;
        le90 = UNKNOWN;
        return this;
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param latitude          the latitude of the geopoint.
     * @param longitude         the longitude of the geopoint.
     * @param altitude          the altitude in as specified by the altitude reference
     * @param altitudeReference the altitude reference for the provided altitude.
     * @param ce90              the circular error
     * @param le90              the linear error.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     * @since 3.4 ({@link AltitudeReference} moved from {@link GeoPoint} to {@link IGeoPoint}, changing method signature)
     */
    public GeoPoint set(final double latitude, final double longitude, final double altitude,
                        final AltitudeReference altitudeReference, final double ce90, final double le90)
    {
        if (access == Access.READ_ONLY) return null;

        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.altitudeReference = altitudeReference;
        this.ce90 = ce90;
        this.le90 = le90;
        return this;
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param gp the geopoint to use as the source to copy from.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(GeoPoint gp)
    {
        if (access == Access.READ_ONLY) return null;

        latitude = gp.getLatitude();
        longitude = gp.getLongitude();
        altitude = gp.getAltitude();
        altitudeReference = gp.getAltitudeReference();
        ce90 = gp.getCE();
        le90 = gp.getLE();
        return this;
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param latitude  the latitude of the geopoint.
     * @param longitude the longitude of the geopoint.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(final double latitude, final double longitude)
    {
        if (access == Access.READ_ONLY) return null;

        this.latitude = latitude;
        this.longitude = longitude;
        ce90 = UNKNOWN;
        le90 = UNKNOWN;
        return this;
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param latitude  the latitude of the geopoint.
     * @param longitude the longitude of the geopoint.
     * @param altitude  the altitude in HAE.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(final double latitude, final double longitude, final double altitude)
    {
        return set(latitude, longitude, altitude, AltitudeReference.HAE, UNKNOWN, UNKNOWN);
    }

    /**
     * Sets the parameters of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param latitude  the latitude of the geopoint.
     * @param longitude the longitude of the geopoint.
     * @param altitude  the altitude in HAE.
     * @param ce90      the circular error
     * @param le90      the linear error.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(final double latitude, final double longitude, final double altitude,
                        final double ce90, final double le90)
    {
        return set(latitude, longitude, altitude, AltitudeReference.HAE, ce90, le90);
    }

    /**
     * Sets the altitude of the GeoPoint provided the GeoPoint access is READ_WRITE.
     *
     * @param altitude the altitude in HAE.
     * @return a reference to the same GeoPoint, {@code null} if the GeoPoint was not READ_WRITE
     */
    public GeoPoint set(final double altitude)
    {
        return setAltitude(altitude, AltitudeReference.HAE);
    }

    /**
     * Set the altitude and altitude reference.
     *
     * @param altitude          new altitude
     * @param altitudeReference the altitude reference for the provided altitude
     * @return this for chaining, or {@code null} if this instance is read-only
     * @since 3.4 ({@link AltitudeReference} moved from {@link GeoPoint} to {@link IGeoPoint}, changing method signature)
     */
    public GeoPoint setAltitude(double altitude, AltitudeReference altitudeReference)
    {
        if (access == Access.READ_ONLY) return null;

        this.altitude = altitude;
        this.altitudeReference = altitudeReference;
        ce90 = UNKNOWN;
        le90 = UNKNOWN;
        return this;
    }

    @Override
    public void setLatitude(double latitude)
    {
        set(latitude, longitude);
    }

    @Override
    public void setLongitude(double longitude)
    {
        set(latitude, longitude);
    }

    @Override
    public double getElevation()
    {
        return isValid() ? GeoCalculations.haeToMsl(latitude, longitude, getAltitude()) : Double.NaN;
    }

    @Override
    public void setElevation(double elevation)
    {
        if (isValid())
        {
            set(GeoCalculations.mslToHae(latitude, longitude, elevation));
        } else
        {
            logger.error("Cannot calculate MSL for elevation {}; lat/lon are not valid", elevation);
        }
    }

    @Override
    public boolean hasElevation()
    {
        return !Double.isNaN(altitude);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null) return false;

        if (getClass() == o.getClass())
        {
            GeoPoint geoPoint = (GeoPoint) o;

            if (Double.compare(geoPoint.latitude, latitude) != 0) return false;
            if (Double.compare(geoPoint.longitude, longitude) != 0) return false;
            if (Double.compare(geoPoint.altitude, altitude) != 0) return false;
            if (Double.compare(geoPoint.ce90, ce90) != 0) return false;
            if (Double.compare(geoPoint.le90, le90) != 0) return false;
            return altitudeReference == geoPoint.altitudeReference;
        } else if (o instanceof ILocation)
        {
            return sameAs((ILocation) o);
        } else
        {
            return false;
        }
    }

    /**
     * Indicates whether the given {@link ILocation} is "equal to" this one, by comparing the latitude, longitude,
     * and altitude values. This ignores the other values here (e.g., ce90, le90, and altitude source).
     *
     * @param location Location to compare against
     * @return {@code true} if the latitude, longitude, and altitude values are equal
     */
    public boolean sameAs(ILocation location)
    {
        if (location == null) return false;

        if (location instanceof GeoPoint) return equals(location);

        if (Double.compare(location.getLatitude(), latitude) != 0) return false;
        if (Double.compare(location.getLongitude(), longitude) != 0) return false;

        final boolean altitudeValid = isAltitudeValid();

        if (altitudeValid != location.hasElevation()) return false;

        return !altitudeValid || Double.compare(altitude, location.getAltitude()) == 0;
    }

    @Override
    public int hashCode()
    {
        int result;
        result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        result = 31 * result + Double.hashCode(altitude);
        result = 31 * result + altitudeReference.hashCode();
        result = 31 * result + Double.hashCode(ce90);
        result = 31 * result + Double.hashCode(le90);

        return result;
    }
}
