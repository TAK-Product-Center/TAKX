package gov.takx.api.globe;

import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;

/**
 * A bookmark, or favorite, of a location of the globe. A view
 * bookmark captures the complete state of a view, including heading,
 * pitch, and zoom. View bookmarks support property change notification.
 *
 */
public interface IFavorite extends ILocation, ICameraEye
{
    /**
     * Property change that is fired when the bookmark description is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String DESCRIPTION_PROPERTY = "description";

    /**
     * Property change that is fired when the bookmark latitude is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String LATITUDE_PROPERTY = "latitude";

    /**
     * Property change that is fired when the bookmark longitude is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String LONGITUDE_PROPERTY = "longitude";

    /**
     * Property change that is fired when the bookmark elevation is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String ELEVATION_PROPERTY = "elevation";

    /**
     * Property change that is fired when the bookmark heading is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String HEADING_PROPERTY = "heading";

    /**
     * Property change that is fired when the bookmark pitch is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String PITCH_PROPERTY = "pitch";

    /**
     * Property change that is fired when the bookmark zoom is
     * changed.
     *
     * @see #addPropertyChangeListener
     */
    String ZOOM_PROPERTY = "zoom";

    /**
     * Get the identifier of the object (usually a plugin) that
     * created the bookmark.
     */
    String getOwner();

    /**
     * Get a description of the location.
     *
     * @return Bookmark description
     */
    String getDescription();

    @Override
    double getHeading();

    @Override
    double getPitch();

    @Override
    double getZoom();

    /**
     * Set the description of the location.
     *
     * @param description The description. This string is displayed in
     *                    the user interface as the title of the bookmark.
     */
    void setDescription(String description);

    /**
     * Set the heading of the view.
     *
     * @param heading The new heading setting.
     */
    void setHeading(double heading);

    /**
     * Set the pitch of the view.
     *
     * @param pitch The new pitch setting.
     */
    void setPitch(double pitch);

    /**
     * Set the zoom of the view. The zoom value is the distance between the eye position and the center
     * position. The center position is used as the point about which the heading and pitch rotate. It is defined by the
     * intersection of a ray from the eye position through the center of the viewport with the surface of the globe.
     *
     * @param zoom The new zoom setting.
     */
    void setZoom(double zoom);

    /**
     * Add a property change listener. This listener will be notified
     * when any of the bookmark properties are changed.
     *
     * @param listener New listener
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove a property change listener.
     *
     * @param listener Listener to remove
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);
}
