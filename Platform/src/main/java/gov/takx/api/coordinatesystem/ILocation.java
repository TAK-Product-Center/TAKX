package gov.takx.api.coordinatesystem;

import com.atakmap.map.elevation.ElevationManager;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;

/**
 * Interface describing objects that can represent a location (coordinate) on the globe as defined by a
 * latitude, longitude, and elevation triple.  In general, implementations of this interface allow the elevation
 * to be optional.
 */
public interface ILocation extends IGeoPoint
{
    /**
     * Return the elevation in meters MSL.  If no elevation has been set, then the return value is dependent upon the
     * specific implementing class, but it is usually 0.
     *
     * @return elevation in meters
     */
    double getElevation();

    /**
     * Set the elevation in meters MSL.  After calling this method, subsequent calls to {@link #hasElevation()} will
     * return true.
     *
     * @param elevation New elevation value
     */
    void setElevation(double elevation);

    /**
     * Return if this location has had an elevation value set on it.  Any call to {@link #setElevation(double)}
     * will cause this method to subsequently return true, even if the elevation specified was 0.  Note that an
     * elevation may have been set during construction - see the implementing classes for more details.
     *
     * @return true if an elevation has been specified for this location
     */
    boolean hasElevation();

    /**
     * Get the altitude by converting {@link #getElevation()} from MSL to the altitude reference
     * returned from {@link #getAltitudeReference()}.
     *
     * @return altitude value in meters
     */
    @Override
    default double getAltitude()
    {
        if (!hasElevation()) return Double.NaN;

        double adj = ElevationManager.getGeoidHeight(getLatitude(), getLongitude());
        switch (getAltitudeReference())
        {
            case HAE: // convert MSL to HAE
                break;
            case AGL:
                final double localElevation = ElevationManager.getElevation(getLatitude(), getLongitude(), null);
                if (!Double.isNaN(localElevation)) adj -= localElevation;
                break;
            default:
                throw new IllegalStateException("Unsupported altitude reference");
        }

        return getElevation() + adj; // convert from MSL to the altitude reference
    }

    /**
     * Set the HAE altitude by converting to MSL and calling {@link #setElevation(double)}
     *
     * @param altitude altitude value in meters HAE
     */
    @Override
    default void setAltitude(double altitude)
    {
        setElevation(GeoCalculations.haeToMsl(getLatitude(), getLongitude(), altitude));
    }

    /**
     * @return Always {@link AltitudeReference#HAE}
     */
    @Override
    default AltitudeReference getAltitudeReference()
    {
        return AltitudeReference.HAE;
    }

    /**
     * @return Always {@link IGeoPoint#UNKNOWN}
     */
    @Override
    default double getCE()
    {
        return UNKNOWN;
    }

    /**
     * @return Always {@link IGeoPoint#UNKNOWN}
     */
    @Override
    default double getLE()
    {
        return UNKNOWN;
    }
}
