package gov.takx.api.tracks;

import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.Groupability;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.TrackStyleDefinitionBuilder;
import gov.takx.platform.tracks.VerticalAlignment;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Collection of attributes that wholly represent a SmartTrack style definition. Track styles may be cloned
 * via {@link TrackStyleDefinitionBuilder} and modifying properties accordingly before invoking
 * {@link TrackStyleDefinitionBuilder#build()}.
 * <p>
 * When track style definitions are applied to determine the final rendering characteristics of a track point (RDM),
 * there is an implicit inheritance hierarchy at work.  The TAKX core is completely responsible for handling this
 * hierarchy. Plugin developers need to be aware of how the hierarchy is used in order to fully understand how the
 * track style definitions interact. When an {@link ITrackStyleDefinition} is assigned to an
 * {@link IPrePersistRaptorDataMessage}, the track style definition at the RDM takes precedence
 * over one assigned to the {@link IIconMapObject}. Similarly, a track style definition
 * assigned to the {@link IIconMapObject} takes precedence over the global track style definition.
 * See {@link ITrackStylingContext} for more details on inheritance.
 * <p>
 * Individual attributes are served up from the track style definition hierarchy. In other words, the track style
 * definition at the RDM level might only define Track Shape and Track Shape Color, deferring all of the other
 * track style attributes to its immediate parent. Thus, when calling {@link #getTrackShape()} the caller really
 * has no notion of which track style definition explicitly assigned the Track Shape- it could have been the RDM, the
 * Map Object, or the global track style definition.
 * <p>
 * To determine if the locally assigned track style definition explicitly defines a property, use any one of the
 * {@code hasDefinedXXX()} methods, e.g. {@link #hasDefinedTrackShape()}. If a track style wants to revert its
 * explicit override, opting to delegate the attribute's definition to its immediate parent, the developer must call
 * one of the {@code clearDefinedXXX()} methods, e.g. {@link #clearDefinedTrackShape()}.
 * <p>
 * Property change listeners registered with the track style definition will be notified whenever a property changes,
 * and whenever a property is expressly cleared <i>on the specific track style definition</i>. For example, registering
 * as a track style definition property change listener for the RDM's {@code ITrackStyleDefinition} would result in
 * the following behavior:
 * <p>
 * First, whenever an explicit setting is invoked on the RDM's track style definition, a property change event is
 * triggered that provides both the old value and the new value. In this instance, the old value may be {@code null}, which
 * would indicate that the value was previously pulled from its immediately parent, not the track definition itself.
 * <p>
 * Second, whenever an explicit clearDefinedXXX() is invoked on the RDM's track style definition, two property change
 * events will be triggered. The first is a
 *
 * @see TrackStyleDefinitionBuilder#TrackStyleDefinitionBuilder(String, String, String, ITrackStyleDefinition)
 * @since 1.12
 */
@SuppressWarnings("unused")
public interface ITrackStyleDefinition extends ITrackStyle, IRaptorObject, IPersistable
{
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
    void setDisplayName(@Nonnull String displayName);

    /**
     * Determines if this track style was defined by the user at run time or by a plugin
     *
     * @return true, if user defined
     */
    boolean isUserDefined();

    /**
     * Defines if tracks fade over time. When true, a valid, positive time threshold in millis must be defined.
     *
     * @param fadedOverTimeFlag true, if tracks should fade over time
     * @see #setTimeThreshold(long)
     */
    void setFadedOverTime(boolean fadedOverTimeFlag);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedFadedOverTime();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedFadedOverTime();

    /**
     * Defines if the track point shows a glyph to indicate the heading of the map object at that track point. This
     * glyph is separate from the overlay glyph and the track shape; it is dynamically positioned and rendered as part
     * of the overall track point.
     *
     * @param showHeading true, if heading should be displayed
     */
    void setShowHeading(boolean showHeading);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedShowHeading();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedShowHeading();

    /**
     * Defines the time threshold, in millis, used to mask or filter out stale track points. For example, if the time
     * threshold is set to 5 minutes (1000 * 60 * 5 in millis), then tracks older than 5 minutes will not be displayed
     * on the globe.
     *
     * @param threshold threshold, in millis
     * @throws IllegalArgumentException if the millis is less than zero
     */
    void setTimeThreshold(long threshold);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedTimeThreshold();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedTimeThreshold();

    /**
     * Defines if the glyph size should be explicitly linked to the track size.
     *
     * @param linkGlyphSizeToTrackSize true, if glyph size is explicitly linked to track size
     */
    void setLinkGlyphSizeToTrackSize(boolean linkGlyphSizeToTrackSize);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLinkGlyphSizeToTrackSize();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLinkGlyphSizeToTrackSize();

    /**
     * Defines a constant glyph size. Developers are strongly encouraged to use one of the
     * {@link ISize} constants.
     * <p/>
     * The default is {@link ISize#NORMAL}
     *
     * @param constantGlyphSize Constant glyph size in the range of 1 to 10, inclusive, with 1 being the smallest and
     *                          10 being the largest.
     * @see ISize
     */
    void setGlyphConstantSize(int constantGlyphSize);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGlyphConstantSize();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGlyphConstantSize();

    /**
     * Defines if the glyph's opacity is explicitly linked to the track opacity.
     *
     * @param linkGlyphOpacityToTrackOpacity true, if glyph opacity is explicitly linked to the track opacity
     */
    void setLinkGlyphOpacityToTrackOpacity(boolean linkGlyphOpacityToTrackOpacity);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLinkGlyphOpacityToTrackOpacity();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLinkGlyphOpacityToTrackOpacity();

    /**
     * Defines if the glyph's heading is explicitly linked to the track's heading.
     *
     * @param linkGlyphToHeading true, if the glyph should be rotated the same number of degrees as the associated track
     *                           heading.
     */
    void setLinkGlyphToHeading(boolean linkGlyphToHeading);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLinkGlyphToHeading();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLinkGlyphToHeading();

    /**
     * Defines if the track is snapped to the surface, effectively ignoring the elevation attribute of the track.
     *
     * @param snapToSurface true, if track elevation is ignored and tracks are snapped to the surface
     */
    void setSnapToSurface(boolean snapToSurface);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedSnapToSurface();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedSnapToSurface();

    /**
     * Defines a three-dimensional OpenGL track shape used to represent the track on the globe. The track shape may be
     * complimented by overlaying a custom glyph icon to the track shape.
     *
     * @param trackShape Track shape used to represent the track point
     */
    void setTrackShape(TrackShape trackShape);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedTrackShape();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedTrackShape();

    /**
     * Defines if the track shape is automatically oriented based on the track heading.
     *
     * @param linkTrackShapeToHeading true, if the track shape should be rotated the same number of degrees as the heading
     *                                associated with the track point
     */
    void setLinkTrackShapeToHeading(boolean linkTrackShapeToHeading);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedLinkTrackShapeToHeading();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedLinkTrackShapeToHeading();

    /**
     * Defines a constant track shape color.
     *
     * @param constantColorValue Constant track shape color
     */
    void setTrackShapeConstantColor(Color constantColorValue);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedTrackShapeConstantColor();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedTrackShapeConstantColor();

    /**
     * Defines a constant track shape opacity.
     *
     * @param trackShapeConstantOpacity Constant track shape opacity
     */
    void setTrackShapeConstantOpacity(float trackShapeConstantOpacity);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedTrackShapeConstantOpacity();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedTrackShapeConstantOpacity();

    /**
     * Defines a constant track shape size. Developers are strongly encouraged to use one of the
     * {@link ISize} constants.
     *
     * @param trackShapeConstantSize Constant glyph size in the range of 1 to 10, inclusive, with 1 being the smallest and
     *                               10 being the largest.
     * @see ISize
     */
    void setTrackShapeConstantSize(int trackShapeConstantSize);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedTrackShapeConstantSize();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedTrackShapeConstantSize();

    /**
     * Defines a valid resource path to a .png file within a .jar resource bundle. The resource is an overlay glyph that
     * will be rendered with the track shape.
     * <p/>
     * Example: gov/takx/acme/glyphs/RadOverlay.png
     * <p/>
     * Developers are strongly encouraged to keep the size of their glyph resources as small as possible by using a
     * flattened .png file.
     * <p/>
     * Glyphs are <i>never</i> transformed for perspective when the globe is tilted.
     * <p/>
     * The default is no glyph defined, {@code null}.
     *
     * @param resourcePath Path to a valid glyph resource located within a .jar file, fully qualified but without the
     *                     leading slash.
     */
    void setGlyphResource(String resourcePath);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGlyphResource();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGlyphResource();

    /**
     * Defines the glyph's opacity within the range of 0.0f through 1.0f, inclusive.
     *
     * @param glyphOpacity Glyph opacity
     */
    void setGlyphConstantOpacity(float glyphOpacity);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGlyphConstantOpacity();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGlyphConstantOpacity();

    /**
     * Defines the vertical (Z-order) rendering alignment of the glyph. Glyphs positioned on the top will occlude the
     * track shape when the user is viewing the track from nadir. Glyphs positioned on the bottom will always be
     * occluded by the track shape when the user is viewing the track from nadir.
     * <p/>
     * Glyphs are <i>never</i> transformed for perspective when the globe is tilted.
     *
     * @param glyphVerticalAlignment glyph alignment
     */
    void setGlyphVerticalAlignment(VerticalAlignment glyphVerticalAlignment);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGlyphVerticalAlignment();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGlyphVerticalAlignment();

    /**
     * Set the vertical offset, in meters, to apply to the glyph position after it is determined according to the
     * specified vertical position.  The offset may be positive or negative.  A value of 0 indicates no offset.
     *
     * @param offset The vertical offset in meters
     */
    void setGlyphVerticalOffset(double offset);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGlyphVerticalOffset();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGlyphVerticalOffset();

    /**
     * Defines the overall visibility of tracks defined by this track style definition.
     *
     * @param isVisible When false, no tracks are rendered regardless of the other track style definition values
     */
    void setVisible(boolean isVisible);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedVisible();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedVisible();

    /**
     * Defines a strict elevation, in meters and relative to mean sea level, such that when the camera eye is above the
     * threshold the tracks are not rendered. For example, it is possible to define a track style such that the tracks
     * are only visible when the camera eye is at an elevation of 1000 meters mean sea level or below.
     *
     * @param elevationThreshold Viewable threshold elevation, or 0 if no threshold should be defined (always visible)
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
     * Defines the bucket mode of the track style for aggregating tracks together based on a well-known attribute.
     * The bucket mode overrides the standard color and marker shape in order to show
     * track points according to "buckets" or categories like "hour of day" or "day of week."  This provides
     * a distinct rendering style to every track point that falls into one of the defined buckets and aids in
     * track analysis.  A bucket mode of {@link BucketMode#None} means that the standard color and track shape
     * are used.
     *
     * @param bucketMode Bucket mode to apply to the track style.
     */
    void setBucketMode(@Nonnull BucketMode bucketMode);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedBucketMode();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedBucketMode();

    /**
     * Defines the groupability of the track style for consolidating tracks when the camera eye zooms out. Groupability
     * effectively provides a means to reduce globe clutter by aggregating multiple tracks, perhaps disparate tracks,
     * into a single icon.
     *
     * @param groupability Groupability of the track style
     */
    void setGroupability(Groupability groupability);

    /**
     * Indicates the track style has defined a local value for this property when true; otherwise, if the value
     * is false than the corresponding property value is implicitly obtained by reaching back to the track style
     * definition parent.
     *
     * @return true, if the track style has a locally defined value for this property
     */
    boolean hasDefinedGroupability();

    /**
     * Clears the locally defined value, ensuring that the corresponding property value is obtained implicitly by
     * reaching back to the track style parent.
     */
    void clearDefinedGroupability();

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
