package gov.takx.api.globe;

import java.util.List;

/**
 * Service that allows plugins to create bookmarks (favorites) to
 * locations on the globe. A view bookmark is like a camera position
 * looking at the map; includes heading, pitch, and zoom settings. The
 * bookmarks are displayed in the user interface, and the user can use
 * them to quickly navigate the globe to different locations.
 *
 */
public interface IFavoritesService
{
    /**
     * Creates a new favorite based on the current camera eye position.
     *
     * @return Favorite based on the current eye position.
     * @since 1.11
     */
    IFavorite add();

    /**
     * Creates a new favorite based on the current camera eye position.
     *
     * @param description Description of this view.
     * @return Favorite based on the current eye position.
     * @since 1.14
     */
    IFavorite add(String description);

    /**
     * Create a new favorite. A favorite is the position of a camera looking at the map.
     * <p/>
     * The zoom value is the distance between the eye position and the center
     * position. The center position is used as the point about which the heading and pitch rotate. It is defined by the
     * intersection of a ray from the eye position through the center of the viewport with the surface of the globe.
     *
     * @param description Description of this view.
     * @param latitude    Latitude of the view.
     * @param longitude   Longitude of the view.
     * @param elevation   Elevation of the view.
     * @param heading     Heading of the view.
     * @param pitch       Pitch of the view.
     * @param zoom        Zoom of the view.
     * @return The new favorite. The bookmark may be edited after creation.
     */
    IFavorite add(String description, double latitude, double longitude,
                  double elevation, double heading, double pitch, double zoom);

    /**
     * Creates a new favorite based on the provided camera eye position.
     *
     * @param description Description of this view
     * @param cameraEye   Precise camera eye location
     * @return The new favorite. The bookmark may be edited after creation.
     */
    IFavorite add(String description, ICameraEye cameraEye);

    /**
     * Remove an existing view bookmark.
     *
     * @param favorite Bookmark to remove.
     */
    void remove(IFavorite favorite);

    /**
     * Get all favorites created by the current plugin.
     *
     * @return All favorite created by this plugin. The plugin cannot
     * access favorite created by the user or by other
     * plugins.
     */
    List<IFavorite> getFavorites();
}
