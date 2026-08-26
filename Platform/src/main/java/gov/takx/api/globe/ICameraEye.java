package gov.takx.api.globe;

/**
 * Data about the current camera eye location. The camera eye is the notion of a virtual camera positioned above the
 * earth, looking down at a region. The heading, pitch, and zoom level controls the field of view over the given
 * location.
 *
 * @since 1.11
 */
public interface ICameraEye
{
    /**
     * Latitude, in degrees
     *
     * @return the latitude
     */
    double getLatitude();

    /**
     * Longitude in degrees
     *
     * @return the longitude
     */
    double getLongitude();

    /**
     * Return the elevation.  If no elevation has been set, then the return value is dependent upon the
     * specific implementing class, but it is usually 0.
     *
     * @return elevation
     */
    double getElevation();

    /**
     * Get the heading of the camera's current eye position, in degrees.
     *
     * @return View heading
     */
    double getHeading();

    /**
     * Get the pitch of the camera's current eye position, in degrees.
     *
     * @return View pitch
     */
    double getPitch();

    /**
     * Get the zoom setting for this view. The zoom value is the distance between the eye position and the center
     * position. The center position is used as the point about which the heading and pitch rotate. It is defined by the
     * intersection of a ray from the eye position through the center of the viewport with the surface of the globe.
     *
     * @return View zoom
     */
    double getZoom();
}
