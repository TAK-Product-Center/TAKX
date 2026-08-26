package gov.takx.core.globe.tak.animation;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.MapRenderer;

/**
 * A factory for creating {@link IAnimation animations} for the globe.
 *
 * @since 3.4.1
 */
@IncubatingApi(since = "3.4.1")
public interface IAnimationFactory
{
    /**
     * Create an {@link IAnimation} that is a composite of other animations running in parallel.
     *
     * @param mapRenderer the map renderer
     * @param lengthMs    the duration of the animation in milliseconds
     * @param animations   the animations to composite
     * @return the composite animation
     */
    IAnimation createComposite(MapRenderer<?> mapRenderer, long lengthMs, IAnimation... animations);

    /**
     * Create an {@link IAnimation} that pans the globe.
     *
     * @param mapRenderer the map renderer
     * @param lat         the latitude to pan the globe to
     * @param lon         the longitude to pan the globe to
     * @param alt         the altitude of the lat/long point to pan the globe to
     * @return the pan animation
     */
    IAnimation createPanTo(MapRenderer<?> mapRenderer, double lat, double lon, double alt);

    /**
     * Create an {@link IAnimation} that zooms the globe.
     *
     * @param mapRenderer the map renderer
     * @param gsd         the gsd to zoom the globe to
     * @return the zoom animation
     */
    IAnimation createZoomTo(MapRenderer<?> mapRenderer, double gsd);

    /**
     * Create an {@link IAnimation} that rotates the globe.
     *
     * @param mapRenderer the map renderer
     * @param heading     the heading to rotate the globe to in degrees
     * @return the heading animation
     */
    IAnimation createHeadingTo(MapRenderer<?> mapRenderer, double heading);

    /**
     * Create an {@link IAnimation} that tilts the globe.
     *
     * @param mapRenderer the map renderer
     * @param tilt        the tilt to change the globe to in degrees
     * @return the tilt animation
     */
    IAnimation createTiltTo(MapRenderer<?> mapRenderer, double tilt);

    /**
     * Create an {@link IAnimation} that flies to a location, zoom level, rotation, and tilt.
     *
     * @param mapRenderer the map renderer
     * @param lat         the latitude to pan the globe to
     * @param lon         the longitude to pan the globe to
     * @param alt         the altitude of the lat/long point to pan the globe to
     * @param gsd         the gsd to zoom the globe to
     * @param heading     the heading to rotate the globe to in degrees
     * @param tilt        the tilt to change the globe to in degrees
     * @return the fly to animation.
     */
    default IAnimation createFlyTo(MapRenderer<?> mapRenderer, double lat, double lon, double alt, double gsd, double heading, double tilt)
    {
        return createFlyTo(mapRenderer, lat, lon, alt, gsd, heading, tilt, true);
    }

    /**
     * Create an {@link IAnimation} that flies to a location, zoom level, rotation, and tilt.
     *
     * @param mapRenderer the map renderer
     * @param lat         the latitude to pan the globe to
     * @param lon         the longitude to pan the globe to
     * @param alt         the altitude of the lat/long point to pan the globe to
     * @param gsd         the gsd to zoom the globe to
     * @param heading     the heading to rotate the globe to in degrees
     * @param tilt        the tilt to change the globe to in degrees
     * @param useMidZoom  Pass {@code true} to use a "mid-altitude" zoom, which causes the camera to move up, then pan,
     *                    then zoom back in.  Passing {@code false} suppresses this additional animation.
     * @return the fly to animation.
     * @since 5.7
     */
    IAnimation createFlyTo(MapRenderer<?> mapRenderer, double lat, double lon, double alt, double gsd, double heading, double tilt, boolean useMidZoom);
}
