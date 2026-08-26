package gov.takx.api.globe;

import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.geom.Sector;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.layer.IRaptorLayer;
import jakarta.annotation.Nonnull;

/**
 * Contract for service controlling a globe's view parameters.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
@NoPluginImplementations
public interface IGlobeViewController
{
    /**
     * @return Camera eye defining the current globe view
     */
    @Nonnull
    ICameraEye getCurrentCameraEye();

    /**
     * Set the view heading.
     *
     * @param heading in degrees
     * @param animated Pass {@code true} to animate the change, {@code false} to make the change immediate
     */
    void setHeading(double heading, boolean animated);

    /**
     * Set the view tilt.
     *
     * @param tilt the tilt in degrees, range 0 = nadir (straight down) to 89
     * @param animated Pass {@code true} to animate the change, {@code false} to make the change immediate
     */
    void setTilt(double tilt, boolean animated);

    /**
     * Set the view camera eye altitude.
     *
     * @param eyeAltitude the camera eye altitude in meters
     * @param animated Pass {@code true} to animate the change, {@code false} to make the change immediate
     */
    void setEyeAltitude(double eyeAltitude, boolean animated);

    /**
     * Reset the view to show the whole globe. Heading and tilt are set to 0.
     */
    void resetWorldView();

    /**
     * Reset the view heading to 0 (North up).  This change is animated.
     */
    void resetHeading();

    /**
     * Reset the view tilt to 0 (nadir).  This change is animated.
     */
    void resetTilt();

    /**
     * Reset the view heading and tilt to 0.  This change is animated.
     */
    void resetHeadingAndTilt();

    /**
     * Navigate the globe view to the given lat/lon coordinates.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param lat Latitude on which to center
     * @param lon Longitude on which to center
     */
    default void goTo(double lat, double lon)
    {
        goTo(lat, lon, true);
    }

    /**
     * Navigate the globe view to the given lat/lon coordinates.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param lat  Latitude on which to center
     * @param lon  Longitude on which to center
     * @param zoom Pass {@code true} to zoom in on the location, {@code false} to keep current eye altitude
     */
    void goTo(double lat, double lon, boolean zoom);

    /**
     * Navigate the globe view to the given lat/lon coordinates with the camera at the specified {@code eyeAltitude}.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param lat Latitude on which to center
     * @param lon Longitude on which to center
     * @param eyeAltitude Camera eye altitude in meters
     */
    void goTo(double lat, double lon, double eyeAltitude);

    /**
     * Navigate the globe view to the given location.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param location Location on which to center
     */
    default void goTo(@Nonnull ILocation location)
    {
        goTo(location, true);
    }

    /**
     * Navigate the globe view to the given location.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param location Location on which to center
     * @param zoom Pass {@code true} to zoom in on the location, {@code false} to keep current eye altitude
     */
    default void goTo(@Nonnull ILocation location, boolean zoom)
    {
        goTo(location.getLatitude(), location.getLongitude(), zoom);
    }

    /**
     * Change the globe view to the view settings in the given camera eye.  This change is animated.
     *
     * @param cameraEye View settings to install
     */
    default void goTo(@Nonnull ICameraEye cameraEye)
    {
        goTo(cameraEye, true);
    }

    /**
     * Change the globe view to the view settings in the given camera eye.  This change is animated.
     *
     * @param cameraEye View settings to install
     * @param useMidZoom  Pass {@code true} to use a "mid-altitude" zoom, which causes the camera to move up, then pan,
     *                    then zoom back in.  Passing {@code false} suppresses this additional animation.
     * @since 5.7
     */
    void goTo(@Nonnull ICameraEye cameraEye, boolean useMidZoom);

    /**
     * Navigate the globe view to show the given envelope.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param envelope Envelope whose extent should be shown in the view
     */
    void goTo(@Nonnull Envelope envelope);

    /**
     * Navigate the globe view to show the given sector.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param sector Sector whose extent should be shown in the view
     */
    void goTo(@Nonnull Sector sector);

    /**
     * Navigate the globe view to show the given layer.
     * The heading and tilt are reset to 0.  This change is animated.
     *
     * @param layer Layer whose extent should be shown in the view
     */
    void goTo(@Nonnull IRaptorLayer layer);

    /**
     * Center the globe view on the given lat/lon, optionally with animation.  No other view parameters are modified.
     *
     * @param latitude the latitude on which to center
     * @param longitude the longitude on which to center
     * @param eyeAltitude Camera eye altitude in meters
     * @param animated Pass {@code true} to animate the change, {@code false} to make the change immediate
     */
    void centerOn(double latitude, double longitude, double eyeAltitude, boolean animated);
}
