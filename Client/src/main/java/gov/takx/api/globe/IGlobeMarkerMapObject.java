package gov.takx.api.globe;

import gov.nasa.worldwind.drag.Draggable;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Interface for representing an icon/marker map object on the globe.
 *
 * @since 3.0 (major rework in 5.2)
 */
public interface IGlobeMarkerMapObject extends IGlobeMapObject, IPositionOverlayOverride, Draggable
{
    /**
     * Refresh all the icons associated with this map object. This usually results from a change that affects the icon
     * image.
     */
    void refreshIcons();

    /**
     * @return the current size of the icon image on the globe
     */
    Dimension getCurrentImageSize();

    /**
     * @return {@code true} if this icon is being rendered "on the surface".  If so, then we need to return a WW
     * position with a 0 elevation to ensure we actually render on the surface.
     */
    boolean isRenderingOnSurface();

    /**
     * Set if this icon is being rendered on the surface.  This will be determined on each rendering cycle based
     * on the Snap To Surface settings for the MO and the elevation of the MO.  We force any icon that would render
     * below the terrain to render on the surface (so the user doesn't "lose" them).
     *
     * @param renderingOnSurface Set {@code true} to force an elevation of 0
     */
    void setRenderingOnSurface(boolean renderingOnSurface);

    /**
     * Indicates whether the icon is always to be displayed "on top" of all other ordered renderables.
     *
     * @return true if the icon has visual priority, otherwise false, the default.
     */
    boolean isAlwaysOnTop();

    /**
     * Indicates whether the icon is always to be displayed "on top" of all other ordered renderables.
     *
     * @param alwaysOnTop true if the icon has visual priority, otherwise false, the default.
     */
    void setAlwaysOnTop(boolean alwaysOnTop);

    /**
     * @return The image source in use by this icon
     * @since 5.2
     */
    Object getImageSource();

    /**
     * Returns the icon's background image source, if any.
     *
     * @return the icon's background image source. The default is null.
     * @since 5.2
     */
    Object getBackgroundImage();

    /**
     * Specifies the icon's background image source. If non-null, the image is displayed centered and behind the icon.
     *
     * @param background the background image source. The default is null.
     * @since 5.2
     */
    void setBackgroundImage(Object background);

    /**
     * Indicates the relative screen size of the background image, if specified. The scale indicates the degree of
     * expansion or shrinkage from the image's source size.
     *
     * @return the background image scale.
     * @since 5.2
     */
    double getBackgroundScale();

    /**
     * Indicates the relative screen size of the background image, if specified. The scale indicates the degree of
     * expansion or shrinkage from the image's source size.
     *
     * @param backgroundScale the background image scale.
     * @since 5.2
     */
    void setBackgroundScale(double backgroundScale);

    /**
     * Indicates whether the icon's tool tip, if any, is displayed with the icon.
     *
     * @return true if the tool tip is displayed when the icon is rendered, otherwise false, the default.
     * @since 5.2
     */
    boolean isShowToolTip();

    /**
     * Indicates whether the icon's tool tip, if any, is displayed with the icon.
     *
     * @param showToolTip true if the tool tip is displayed when the icon is rendered, otherwise false, the default.
     * @since 5.2
     */
    void setShowToolTip(boolean showToolTip);

    /**
     * @return {@code true} if the tooltip visibility is being forced to a specific state due to program preferences
     *         or via the API, {@code false} otherwise
     *
     * @see IMapObject#setTooltipVisibility(IMapObject.TooltipVisibility)
     * @since 5.6.1
     */
    boolean isTooltipVisibilityForced();

    /**
     * Returns the icon's current tool tip text.
     *
     * @return the current tool tip text.
     * @since 5.2
     */
    String getToolTipText();

    /**
     * Specifies the text string to display as the icon's tool tip.
     *
     * @param toolTipText the tool tip text. May be null, the default, to indicate no tool tip is displayed.
     * @since 5.2
     */
    void setToolTipText(String toolTipText);

    /**
     * Recalculate the tooltip (label) state of this marker.  This is invoked in response to a change to the
     * {@link gov.takx.platform.preferences.PreferenceEntry#MAP_OBJECT_TOOLTIPS} preference.
     * @since 5.7
     */
    void recalculateTooltipState();

    /**
     * @return {@code true} if only the label/tooltip should be rendered.  If so, the icon should not be rendered,
     * and the tooltip should be centered on the marker's location.
     * @since 5.2
     */
    boolean isLabelOnly();

    /**
     * Add a property change listener on the given list of properties of this object.  To unregister, use
     * {@link #removePropertyChangeListener(PropertyChangeListener, List)}.
     *
     * @param listener   Listener to register
     * @param properties List of properties of interest
     * @since 5.2
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties);

    /**
     * Remove the listener from the specified properties.
     *
     * @param listener   Listener to remove
     * @param properties list of properties to unregister
     * @since 5.2
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties);
}
