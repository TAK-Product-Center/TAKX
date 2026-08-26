package gov.takx.api.tracks;

import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.Groupability;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.VerticalAlignment;

import java.awt.Color;

/**
 * Getter methods for retrieving attribute values of a track style.
 *
 * @since 1.12
 */
public interface ITrackStyle extends ISize
{
    String CLEARED = "Cleared";
    String FADED_OVER_TIME = "fadedOverTime";
    String TIME_THRESHOLD = "timeThreshold";
    String DISPLAY_NAME = "displayName";
    String GLYPH_LINKED_TRACK_SIZE = "glyphLinkedTrackSize";
    String GLYPH_LINKED_TRACK_OPACITY = "glyphLinkedTrackOpacity";
    String GLYPH_LINKED_TRACK_HEADING = "glyphLinkedTrackHeading";
    String GLYPH_RESOURCE = "glyphResourcePath";
    String GLYPH_CONSTANT_SIZE = "glyphConstantSize";
    String GLYPH_CONSTANT_OPACITY = "glyphConstantOpacity";
    String GLYPH_VERTICAL_ALIGNMENT = "glyphVerticalAlignment";
    String GLYPH_VERTICAL_OFFSET = "glyphVerticalOffset";
    String SNAP_TO_SURFACE = "snapToSurface";
    String TRACK_SHAPE = "trackShape";
    String TRACK_SHAPE_COLOR = "trackShapeColor";
    String TRACK_SHAPE_OPACITY = "trackShapeOpacity";
    String TRACK_SHAPE_SIZE = "trackShapeSize";
    String TRACK_SHAPE_TRACK_HEADING = "trackShapeLinkedTrackHeading";
    String VISIBILITY = "visibility";
    String ELEVATION_THRESHOLD = "elevationThreshold";
    String GROUPABILITY = "groupability";
    String BUCKET_MODE = "bucketMode";
    String SHOW_HEADING = "showHeading";

    /**
     * Indicates if tracks fade over time.
     * <p/>
     * Defaults to false
     *
     * @return true, if tracks fade
     */
    boolean isFadedOverTime();

    /**
     * Determine if the heading is displayed on the track points rendered using this track style definition.
     * <p/>
     * Defaults to true
     *
     * @return true, if heading is displayed
     */
    boolean isShowingHeading();

    /**
     * Indicates a time threshold, in millis, used to mask or filter out stale track points. For example, if the time
     * threshold is set to 5 minutes (1000 * 60 * 5 in millis), then tracks older than 5 minutes will not be displayed
     * on the globe.
     * <p/>
     * Default is no time threshold, 0.
     *
     * @return time threshold in millis
     */
    long getTimeThreshold();

    /**
     * Convenience method to determine if a time threshold has been defined for the track style. This merely checks
     * to see if the time threshold millis are greater than zero.
     *
     * @return true, if time threshold millis value is greater than zero
     */
    boolean hasTimeThreshold();

    /**
     * Determines if the track glyph's size is linked to the track's size.
     * <p/>
     * Defaults to false
     *
     * @return true, if linked
     */
    boolean isGlyphLinkedToTrackSize();

    /**
     * Constant size on a scale of 1 to 10 for a glyph overlay, with 5 being the default scaling. Developers are
     * strongly encouraged to use the constants on {@link ISize} instead of literal values.
     * <p/>
     * Defaults to {@link ISize#NORMAL}
     *
     * @return Constant glyph size on a scale of 1 to 10
     */
    int getGlyphConstantSize();

    /**
     * Determines if the track glyph's opacity is linked to the track's opacity.
     * <p/>
     * Defaults to true
     *
     * @return true, if linked
     */
    boolean isGlyphLinkedToTrackOpacity();

    /**
     * Determines if the glyph's orientation is linked to the track heading value.
     * <p/>
     * Defaults to false
     *
     * @return true, if linked
     */
    boolean isLinkGlyphToHeading();

    /**
     * Determines if the track elevation is ignored, snapping the track to the globe surface.
     * <p/>
     * Defaults to false.
     *
     * @return true, if track elevation is ignored
     */
    boolean isSnappedToSurface();

    /**
     * Determines which single track shape is used to represent the track on the globe. The track shape may be
     * complimented by overlaying a custom glyph icon to the track shape.
     * <p/>
     * Default is {@link TrackShape#Sphere}
     *
     * @return Track shape used to represent the track point
     */
    TrackShape getTrackShape();

    /**
     * Determines if the track shape is oriented based on the track heading.
     * <p/>
     * Defaults to false
     *
     * @return true, if the track shape is oriented with the track heading
     */
    boolean isTrackShapeLinkedToHeading();

    /**
     * Constant color assigned to the track shape.
     * <p/>
     * Default is {@code Color.WHITE}.
     *
     * @return Constant track shape color
     */
    Color getTrackShapeConstantColor();

    /**
     * Constant opacity assigned to the track shape.
     * <p/>
     * Defaults to fully opaque, 1.0f
     *
     * @return Constant track shape opacity
     */
    float getTrackShapeConstantOpacity();

    /**
     * Constant size assigned to the track shape.
     * <p/>
     * Defaults to {@link ISize#NORMAL}
     *
     * @return Constant track shape size
     */
    int getTrackShapeConstantSize();

    /**
     * Valid path to a .jar resource bundle .png file that defines an overlay glyph that will be rendered with the
     * track shape.
     * <p/>
     * Example: gov/takx/acme/glyphs/RadOverlay.png
     * <p/>
     * Developers are strongly encouraged to keep the size of their glyph resources as small as possible by using a
     * flattened .png file.
     * <p/>
     * Defaults to no glyph, {@code null}.
     *
     * @return Path to a valid glyph resource located within a .jar file
     */
    String getGlyphResource();

    /**
     * Constant value of the glyph's opacity within the range of 0.0f through 1.0f, inclusive.
     * <p/>
     * The default value is fully opaque, 1.0f.
     *
     * @return Glyph opacity
     */
    float getGlyphConstantOpacity();

    /**
     * Identifies where the glyph used to decorate the track shape should be positioned, top, middle, or bottom. Glyphs
     * positioned on the top will occlude the track shape when the user is viewing the track from nadir. Glyphs
     * positioned on the bottom will always be occluded by the track shape when the user is viewing the track from
     * nadir.
     * <p/>
     * The default value is {@link VerticalAlignment#DEFAULT}
     *
     * @return Vertical alignment for the glyph with respect to the track shape
     */
    VerticalAlignment getGlyphVerticalAlignment();

    /**
     * Specifies a vertical offset, in meters, to apply to the glyph position after it is determined according to the
     * specified vertical position.  The offset may be positive or negative.  A value of 0 indicates no offset.
     *
     * @return The vertical offset in meters
     */
    double getGlyphVerticalOffset();

    /**
     * Determines if the tracks are visible in this style. A false value here means that no track attributes whatsoever
     * will be rendered on the globe.
     * <p/>
     * Defaults to true
     *
     * @return true, if the track attributes contained within this track style are rendered on the globe
     */
    boolean isVisible();

    /**
     * A strict elevation, in meters and relative to mean sea level, such that when the camera eye is above the
     * threshold the tracks are not rendered. For example, it is possible to define a track style such that the tracks
     * are only visible when the camera eye is at an elevation of 1000 meters mean sea level or below.
     * <p/>
     * The default is no threshold, 0.
     *
     * @return Viewable threshold elevation, or 0 if no threshold is defined
     */
    double getElevationThreshold();

    /**
     * Get the "bucket mode."  The bucket mode overrides the standard color and marker shape in order to show
     * track points according to "buckets" or categories like "hour of day" or "day of week."  This provides
     * a distinct rendering style to every track point that falls into one of the defined buckets and aids in
     * track analysis.  A bucket mode of {@link BucketMode#None} means that the standard color and track shape
     * are used.
     *
     * @return The current bucket mode
     */
    public BucketMode getBucketMode();

    /**
     * Determines the groupability of the track style for consolidating tracks when the camera eye zooms out. Groupability
     * effectively provides a means to reduce globe clutter by aggregating multiple tracks into a single marker.
     * <p/>
     * The default is {@link Groupability#SimilarOnly}
     *
     * @return Groupability of the track style
     */
    Groupability getGroupability();
}
