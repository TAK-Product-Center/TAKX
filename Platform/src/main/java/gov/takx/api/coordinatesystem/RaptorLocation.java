package gov.takx.api.coordinatesystem;

import gov.tak.api.maps.coords.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/**
 */
@Embeddable
public class RaptorLocation implements ILocation, Serializable
{
    @Column
    protected double latitude;
    @Column
    protected double longitude;
    @Column
    protected double elevation;
    @Column
    protected boolean hasElevation = false;

    public RaptorLocation()
    {
    }

    /**
     * Copy constructor.
     *
     * @param source Source from which to copy
     * @since 2.29
     */
    public RaptorLocation(ILocation source)
    {
        this(source.getLatitude(), source.getLongitude(), source.hasElevation(), source.getElevation());
    }

    /**
     * Construct using the given values.  If {@code hasElevation} is {@code false}, then this is the same as
     * calling {@link #RaptorLocation(double, double)}.
     *
     * @param latitude     Latitude
     * @param longitude    Longitude
     * @param hasElevation Pass {@code true} if this location has a valid elevation, {@code false} otherwise
     * @param elevation    Elevation, ignored if {@code hasElevation} is {@code false}
     */
    public RaptorLocation(double latitude, double longitude, boolean hasElevation, double elevation)
    {
        this.latitude = wrapLatitude(latitude);
        this.longitude = wrapLongitude(longitude);

        this.hasElevation = hasElevation;
        if (hasElevation) this.elevation = elevation;
    }

    public RaptorLocation(double latitude, double longitude, double elevation)
    {
        this(latitude, longitude, true, elevation);
    }

    public RaptorLocation(double latitude, double longitude)
    {
        this(latitude, longitude, false, 0);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null) return false;

        if (getClass() == o.getClass())
        {
            final RaptorLocation that = (RaptorLocation) o;

            if (Double.compare(that.latitude, latitude) != 0) return false;
            if (Double.compare(that.longitude, longitude) != 0) return false;
            if (Double.compare(that.elevation, elevation) != 0) return false;
            return hasElevation == that.hasElevation;
        } else if (o instanceof GeoPoint)
        {
            return ((GeoPoint) o).sameAs(this);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int result = Double.hashCode(latitude);
        result = 31 * result + Double.hashCode(longitude);
        result = 31 * result + Double.hashCode(elevation);
        result = 31 * result + (hasElevation ? 1 : 0);
        return result;
    }

    @Override
    public void setElevation(double elevation)
    {
        this.elevation = elevation;
        hasElevation = true;
    }

    @Override
    public void setLatitude(double latitude) throws IllegalArgumentException
    {
        this.latitude = wrapLatitude(latitude);
    }

    @Override
    public void setLongitude(double longitude) throws IllegalArgumentException
    {
        this.longitude = wrapLongitude(longitude);
    }

    /**
     * Adjust the given latitude into the range {@code (-90, 90)}.
     *
     * @param latitude Value to adjust
     * @return the given value in the expected range
     * @since 5.0
     */
    private double wrapLatitude(double latitude)
    {
        latitude %= 180;
        if (latitude < -90) latitude += 180;
        return latitude;
    }

    /**
     * Adjust the given longitude into the range {@code (-180, 180)}.
     *
     * @param longitude Value to adjust
     * @return the given value in the expected range
     * @since 5.0
     */
    private double wrapLongitude(double longitude)
    {
        longitude %= 360;

        if (longitude > 180)
        {
            longitude = -180 + (longitude % 180);
        } else if (longitude < -180)
        {
            longitude = 180 + (longitude % 180);
        }
        return longitude;
    }

    @Override
    public boolean hasElevation()
    {
        return hasElevation;
    }

    @Override
    public double getLatitude()
    {
        return latitude;
    }

    @Override
    public double getLongitude()
    {
        return longitude;
    }

    @Override
    public double getElevation()
    {
        return elevation;
    }

    @Override
    public String toString()
    {
        return CoordinateSystem.getLocationString(this, CoordinateSystem.DECIMAL_DEGREES);
    }

    public String toString(CoordinateSystem cs)
    {
        return CoordinateSystem.getLocationString(this, cs);
    }
}
