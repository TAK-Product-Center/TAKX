package gov.takx.api.globe;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.layer.IRaptorLayer;

/**
 * Common set of globe navigation methods used by various delegates to navigate to a location
 * on the globe, or set the globe heading.
 *
 * @since 1.4
 */
public interface IGlobeNavigationService
{
    /**
     * Provides a means of navigating to somewhere on the globe via a latitude/longitude.
     *
     * @param lat latitude
     * @param lon longitude
     */
    void gotoLocationOnGlobe(double lat, double lon);

    /**
     * Provides a means of navigating to somewhere on the globe via an ILocation.
     *
     * @param location Location on globe
     */
    void gotoLocationOnGlobe(ILocation location);

    /**
     * Provides a means of navigating to somewhere on the globe via a specific camera eye.
     *
     * @param cameraEye Camera eye position
     * @since 1.11
     */
    void gotoLocationOnGlobe(ICameraEye cameraEye);

    /**
     * Navigate the globe to a bookmark. You can create and access bookmarks using the
     * {@link IFavoritesService}.
     *
     * @param bookmark Bookmark to navigate to.
     * @since 1.4
     */
    void gotoLocationOnGlobe(IFavorite bookmark);

    /**
     * Navigate to a {@link Sector} on the globe at a zoom level that will keep the
     * entire sector in view.
     *
     * @param sector sector to zoom to
     */
    void gotoLocationOnGlobe(Sector sector);

    /**
     * Navigate to a {@link IRaptorLayer} on the globe at a zoom level that will keep the
     * entire layer in view.
     *
     * @param layer layer to zoom to
     */
    void gotoLocationOnGlobe(IRaptorLayer layer);

    /**
     * Provides a means for the plugin controller to rotate the globe's heading perspective.  This feature
     * is especially useful and intended to be used by a plugin managing a local GPS feed.  The result is
     * identical to common vehicle GPS boxes where the "Top" always represents your direction of travel, allowing
     * the driver to easily identify that left of his position on the map is left of his actual position, and right is
     * right.
     *
     * @param heading Heading, in degrees
     */
    void setGlobeHeading(double heading);
}
