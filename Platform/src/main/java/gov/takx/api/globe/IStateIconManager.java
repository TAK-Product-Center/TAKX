package gov.takx.api.globe;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.platform.globe.CardinalPoint;
import jakarta.annotation.Nonnull;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

/**
 * Manager that allows plugins to configure and interact with state icons for the specific map object with which the
 * manager is associated. A state icon is an icon that will be displayed in one of the cardinal positions (e.g. north,
 * northeast, east, etc.) around the map object icon.
 * <p>
 * The {@code IStateIconManager} for a given map object can be obtained from the delegate in a device plugin using
 * {@link IDevicePluginDelegate#getStateIconManager()}. The data monitor delegate also provides access via
 * {@link IDataMonitorDelegate#getStateIconManager()}.
 *
 */
@NoPluginImplementations
public interface IStateIconManager
{
    /**
     * Has an {@code IStateIcon} been assigned to {@code position}?
     *
     * @param position search constraint to limit context
     * @return true if an icon is found at the designated position, false otherwise
     */
    boolean hasStateIconAtPosition(CardinalPoint position);

    /**
     * Retrieve a particular {@code IStateIcon} using {@code position} as the constraint to find the desired object.
     *
     * @param position constraint
     * @return the state icon found at {@code position} or {@code null}
     */
    IStateIcon getStateIcon(CardinalPoint position);

    /**
     * Construct an {@code IStateIcon} and store it at {@code position} for display using a {@code BufferedImage}.
     *
     * @param position compass point position for display
     * @param image    image used to construct visual indicator
     * @return new instance of IGlobeStateIcon
     */
    IStateIcon addStateIcon(CardinalPoint position, @Nonnull BufferedImage image);

    /**
     * Construct an {@code IGlobeStateIcon} and store it at {@code position} for display using a {@code URL}
     * to obtain the image resource.
     *
     * @param position     compass point position for display
     * @param imagePathURL resource path to image data used to construct visual indicator
     * @return new instance of IGlobeStateIcon
     * @throws IllegalArgumentException if imagePathURL is mal-formed
     */
    IStateIcon addStateIcon(CardinalPoint position, @Nonnull URL imagePathURL);

    /**
     * Construct an {@code IGlobeStateIcon} and store it at {@code position} to display text.
     *
     * @param position compass point position for display
     * @param text     the text to display
     * @return new instance of IGlobeStateIcon
     */
    IStateIcon addStateIcon(CardinalPoint position, String text);

    /**
     * Remove and return the {@link IStateIcon} at the specified position. If none exists,
     * {@code null} will be returned.
     *
     * @param point The point at which to remove the icon.
     * @return The removed {@link IStateIcon}. If no icon exists at the specified location, {@code null} is returned.
     */
    IStateIcon removeStateIcon(CardinalPoint point);

    /**
     * Remove all the state icons currently associated with this manager.
     */
    void removeAllStateIcons();

    /**
     * What positions currently hold or do not hold a state icon? If {@code open} is true,
     * return a list of unfilled positions. If {@code open} is false, return a list of filled
     * positions.
     *
     * @param open unfilled or filled positions
     * @return list of positions
     */
    List<CardinalPoint> findPositions(boolean open);

    /**
     * Is this particular set of icons visible (i.e. drawn) on the globe?
     *
     * @return true if the icons will be drawn; false otherwise
     */
    boolean isVisible();

    /**
     * Set the visibility state (i.e. drawn or not) of the icons set on the globe.
     *
     * @param visible set true to have the icons drawn;
     *                set false to keep the icons from being drawn
     */
    void setVisible(boolean visible);

    /**
     * Set the scale of state icons relative to the primary icon. The value must be > 0.1 (one tenth size),
     * or the value applied will be {@code 0.1}.
     *
     * @param scale The scale (>= 0.1) that state icons should be adjusted to, relative to the primary icon.
     *
     * @apiNote Setting the scale does not affect the size of text used as a state icon.
     * Use {@link IStateIcon#setFont(Font)} to adjust text size.
     */
    void setScale(double scale);

    /**
     * Sets the spacing between the state icon and the center of the map object icon that it is orbiting.
     * <p>
     * E.g. A multiplier of 0.5 will space the state icon 1/2 the height of the map object icon
     * away from (roughly) the map object icon center.
     *
     * @param multiplierFromCenter The value that will be multiplied with the non-selected height to
     *                             determine the gap between the state icon  and the map icon.
     * @throws IllegalArgumentException if the multiplier is less than or equal to 0.
     * @since 1.12
     */
    void setSpacing(double multiplierFromCenter) throws IllegalArgumentException;

    /**
     * Gets the spacing between the state icon and the center of the map object icon that
     * it is orbiting.
     * <p>
     * e.g. A multiplier of 0.5 will space the state icon 1/2 the height of the map object icon
     * away from (roughly) the map object icon center.
     *
     * @return The value that will be multiplied with the non-selected height to
     * determine the gap between the state icon  and the map icon.
     * @since 1.14
     */
    double getSpacing();

    /**
     * Called prior to creating the visual features to render icons to the globe.  All internal properties (such
     * as images) must be updated to ensure proper rendering.
     *
     * @since 3.5
     */
    void updateVisuals();

    /**
     * Determines if the state icons need to be updated. This will be true if the icons are already visible, or if they
     * are "becoming" visible (the MO is visible and the state icons are not).
     *
     * @return true if update is needed
     * @since 5.3
     */
    boolean needsUpdate();
}
