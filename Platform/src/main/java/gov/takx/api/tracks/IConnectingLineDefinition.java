package gov.takx.api.tracks;

import gov.takx.api.persist.IPersistable;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.platform.tracks.ConnectingLineDefinitionBuilder;
import gov.takx.platform.tracks.LineStyle;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Collection of immutable attributes that represent how a SmartTrack-enabled Map Object visually represents the flow
 * between its set of track points. Connecting lines are changed by optionally cloning a connecting line definition
 * via the {@code ConnectingLineBuilder} and modifying properties accordingly before invoking
 * {@link ConnectingLineDefinitionBuilder#build()}.
 *
 * @see ConnectingLineDefinitionBuilder#ConnectingLineDefinitionBuilder(IConnectingLineDefinition, String)
 * @since 1.12
 */
public interface IConnectingLineDefinition extends IRaptorObject, IPersistable, ISize
{
    String CLEARED = "Cleared";

    String PARENT = "parent";
    String DIRECTIONAL_ARROW = "directionArrow";
    String LINE_COLOR = "lineColor";
    String LINE_OPACITY = "lineOpacity";
    String LINE_WIDTH = "lineWidth";
    String LINE_STYLE = "lineStyle";
    String ELEVATION_THRESHOLD = "elevationThreshold";

    /**
     * Human readable name assigned to the track style. This value is not used as a part of the key, nor can track
     * styles be located within the API using the display name.
     *
     * @return Display name assigned to the track style (guaranteed to be non-null)
     */
    String getDisplayName();

    /**
     * Defines a human readable name assigned to the track style. This value is not used as a part of the key, nor can
     * track styles be located within the API using the display name.
     *
     * @param displayName Display name assigned to the track style
     * @throws IllegalArgumentException when the display name is {@code null}
     */
    void setDisplayName(String displayName);

    /**
     * Identifies the parent track style definition deferred to for styling attribute values when this style definition
     * explicit does not define a value for the attribute.
     * <p/>
     * <b>Note:</b>Plugin developers should avoid using getParent() and setParent() unless there is an express reason to
     * do so and they've consulted with the TAKX team. The SmartTrack design is such that plugin developers should
     * be fully buffered from "where" a track style attribute comes from.
     *
     * @return parent, or {@code null} if no parent is defined
     */
    IConnectingLineDefinition getParent();

    /**
     * Defines a parent track style definition for deferring.
     *
     * @param parent parent definition
     */
    void setParent(IConnectingLineDefinition parent);

    /**
     * Determines if this track style was defined by the user at run time or by a plugin
     *
     * @return true, if user defined
     */
    boolean isUserDefined();

    /**
     * Determines if each connecting line between track points is rendered with a fixed style directional arrow.
     * <p>
     * The default is {@code false}.
     *
     * @return true, if the directional arrow should be drawn on the connecting line
     */
    boolean showDirectionalArrow();

    /**
     * Defines if the connecting line includes a direction arrow or not.
     *
     * @param showDirectionalArrow true, if the connecting line should include a direction arrow
     */
    void setDirectionalArrow(boolean showDirectionalArrow);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedDirectionalArrow();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedDirectionalArrow();

    /**
     * Static color used to render the connecting line.
     * <p>
     * The default is {@code Color.WHITE}
     *
     * @return Color used to render the connecting line
     */
    Color getLineColor();

    /**
     * Defines a static color used to render the connecting line.
     *
     * @param lineColor Line color
     */
    void setLineColor(@Nonnull Color lineColor);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLineColor();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLineColor();

    /**
     * Static opacity used to render the connecting line.
     * <p>
     * The default is 1.0f, fully opaque.
     *
     * @return Connecting line opacity
     */
    float getLineOpacity();

    /**
     * Defines the static line opacity used to render the connecting line.
     *
     * @param lineOpacity line opacity
     */
    void setLineOpacity(float lineOpacity);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLineOpacity();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLineOpacity();

    /**
     * Width of the connecting line. The values within the field specified <b>must</b> within the valid
     * range of 1 through 10, inclusive.
     * <p>
     * The default value is {@link ISize#NORMAL}.
     *
     * @return Connecting line width
     * @see ISize
     */
    int getLineWidth();

    /**
     * Defines the width of the connecting line on a scale of 1 to 10, with 5 being the default width. Developers are
     * strongly encouraged to use the constants on {@link ISize} instead of literal values.
     * <p/>
     * Defaults to {@link ISize#NORMAL}
     *
     * @param lineWidth Connecting line width
     */
    void setLineWidth(int lineWidth);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLineWidth();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLineWidth();

    /**
     * Style of the connecting line.
     * <p/>
     * The default line style is {@link LineStyle#Solid}.
     *
     * @return Line style
     */
    LineStyle getLineStyle();

    /**
     * Defines the connecting line style. Only {@link LineStyle#Solid} and {@link LineStyle#None} are supported.
     * The other line styles are deprecated and will result in an UnsupportedOperationException.
     *
     * @param lineStyle Line style
     */
    void setLineStyle(LineStyle lineStyle);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLineStyle();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLineStyle();

    /**
     * Strict elevation, in meters and relative to mean sea level, such that when the camera eye is above the
     * threshold the connecting lines are not rendered. For example, it is possible to define a connecting line
     * definition such that the lines are only visible when the camera eye is at an elevation of 1000 feet mean sea
     * level or below.
     * <p/>
     * The default is 0, no threshold defined.
     *
     * @return Viewable threshold elevation, or 0 if no threshold is defined
     */
    double getElevationThreshold();

    /**
     * Defines a strict elevation, in meters and relative to mean sea level, such that when the camera eye is above the
     * threshold the connecting lines are not rendered. For example, it is possible to define a connecting line
     * definition such that the lines are only visible when the camera eye is at an elevation of 1000 feet mean sea
     * level or below. The default is 0, no threshold defined.
     *
     * @param elevationThreshold elevation threshold, in meters and relative to mean sea level, or 0 if no threshold
     *                           is defined
     */
    void setElevationThreshold(double elevationThreshold);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedElevationThreshold();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedElevationThreshold();

    /**
     * Add property change listeners for the named list of properties.
     *
     * @param listener   Listener to add
     * @param properties List of properties to listen for
     * @throws IllegalArgumentException When the {@code properties} list is empty.
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties);

    /**
     * Add property change listeners for the named property.
     *
     * @param listener Listener to add
     * @param property Property to listen for
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property);

    /**
     * Add a property change listener on ALL properties of this object.  To unregister, use
     * {@link #removePropertyChangeListener(java.beans.PropertyChangeListener)}.
     *
     * @param listener Listener to add
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove the listener from the specified properties.
     *
     * @param listener   Listener to remove
     * @param properties list of properties to unregister
     * @throws IllegalArgumentException When the {@code properties} list is empty.
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(String, java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull List<String> properties);

    /**
     * Remove the listener from the specified property.
     *
     * @param listener Listener to remove
     * @param property Property to unregister
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(String, java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull String property);

    /**
     * Remove the listener. This method can only be used if the listener was originally registered using
     * {@link #addPropertyChangeListener(java.beans.PropertyChangeListener)}
     *
     * @param listener Listener to remove
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(java.beans.PropertyChangeListener)
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    @Nonnull
    @Override
    default Set<String> getMetadataKeys()
    {
        return Collections.emptySet();
    }

    @Override
    default String getMetadataValue(String key)
    {
        return null;
    }
}
