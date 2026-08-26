package gov.takx.core.globe.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerAttributes;
import gov.nasa.worldwind.render.markers.BasicMarkerShape;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.tracks.ISize;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.api.url.Schemes;
import gov.takx.commons.date.DateFormatter;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.module.IPluginModuleManager;
import gov.takx.platform.tracks.BucketMode;
import gov.takx.platform.tracks.TrackShape;
import gov.takx.platform.tracks.VerticalAlignment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class holds/manages the marker attributes for a single TrackStyleDefinition.  Each instance is shared
 * by every object that uses the same TSD.
 * <p>
 * MapPoint and MapObject highlighting is also managed here (invoked when the user hovers over a map point or map object).
 *
 * @see ITrackStyleDefinition
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeMarkerAttributes extends BasicMarkerAttributes implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int NO_MO_ID = -1;   // No selection indicator
    private static final double NO_FADE = -1; // No fade opacity in use

    protected static int highlightedMoId = NO_MO_ID;
    protected static MapPoint highlightedMapPoint;

    // Color scales (shared with TrackColorScaleWidget)
    public static final Color[] COLOR_SCALE_HOUR_OF_DAY = new Color[24];
    public static final Color[] COLOR_SCALE_DAY_OF_WEEK = new Color[7];
    public static final Color[] COLOR_SCALE_MONTH_OF_YEAR = new Color[12];

    // Fade over time fields
    private static final int RAMP_VALUES = 30;
    private static final float[] OPACITY_RAMP = new float[RAMP_VALUES];

    // Custom colors for time buckets
    private static final Color BRICK_RED = new Color(0x8C, 0x17, 0x17);
    private static final Color ORANGE = new Color(0xFF, 0x7F, 0x00);
    private static final Color GREEN_YELLOW = new Color(0xAD, 0xFF, 0x2F);
    private static final Color DARK_GREEN = new Color(0x22, 0x8B, 0x22);
    private static final Color SEA_GREEN = new Color(0x5F, 0x9F, 0x9F);
    private static final Color TURQUOISE = new Color(0x00, 0xFF, 0xFF);
    private static final Color PURPLE = new Color(0x7D, 0x26, 0xCD);
    private static final Color BRIGHT_PINK = new Color(0xFF, 0x00, 0xFF);

    static
    {
        // Day of Week colors
        for (int i = 0; i < 7; i++)
        {
            // Bias ratio to avoid looping back to red on saturday
            Color c = Color.getHSBColor(i / 6.5f, 1f, 1f);
            COLOR_SCALE_DAY_OF_WEEK[i] = c;
        }

        // Hour of day colors 0 - 23 (AM and PM colors are the same)
        COLOR_SCALE_HOUR_OF_DAY[0] = COLOR_SCALE_HOUR_OF_DAY[12] = BRICK_RED;
        COLOR_SCALE_HOUR_OF_DAY[1] = COLOR_SCALE_HOUR_OF_DAY[13] = Color.red;
        COLOR_SCALE_HOUR_OF_DAY[2] = COLOR_SCALE_HOUR_OF_DAY[14] = ORANGE;
        COLOR_SCALE_HOUR_OF_DAY[3] = COLOR_SCALE_HOUR_OF_DAY[15] = Color.yellow;
        COLOR_SCALE_HOUR_OF_DAY[4] = COLOR_SCALE_HOUR_OF_DAY[16] = GREEN_YELLOW;
        COLOR_SCALE_HOUR_OF_DAY[5] = COLOR_SCALE_HOUR_OF_DAY[17] = Color.green;
        COLOR_SCALE_HOUR_OF_DAY[6] = COLOR_SCALE_HOUR_OF_DAY[18] = DARK_GREEN;
        COLOR_SCALE_HOUR_OF_DAY[7] = COLOR_SCALE_HOUR_OF_DAY[19] = SEA_GREEN;
        COLOR_SCALE_HOUR_OF_DAY[8] = COLOR_SCALE_HOUR_OF_DAY[20] = TURQUOISE;
        COLOR_SCALE_HOUR_OF_DAY[9] = COLOR_SCALE_HOUR_OF_DAY[21] = Color.blue;
        COLOR_SCALE_HOUR_OF_DAY[10] = COLOR_SCALE_HOUR_OF_DAY[22] = PURPLE;
        COLOR_SCALE_HOUR_OF_DAY[11] = COLOR_SCALE_HOUR_OF_DAY[23] = BRIGHT_PINK;

        // Month of the year colors (use same colors as hour of day)
        System.arraycopy(COLOR_SCALE_HOUR_OF_DAY, 0, COLOR_SCALE_MONTH_OF_YEAR, 0, 12);

        for (int i = 0; i < RAMP_VALUES; i++) OPACITY_RAMP[i] = Math.max(1f - (float) i / RAMP_VALUES, .1f);
    }

    @Inject
    private IPluginModuleManager pluginModuleManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    IVirtualFileSystemManager vfsManager;

    @Inject
    private IPlaybackManager playbackManager;

    private final IMapObject mapObject;
    private double fadeOpacity = 1d; // When fade over time is in use, this is the opacity to use

    private String amShape;
    private String pmShape;

    protected ITrackStylingContext stylingContext;

    private long frameTimeStamp;

    // Values calculated at most once per frame
    private long now;

    // TSC attributes are cached as values here to avoid the cost of getting them via the TSC on each rendering cycle
    private boolean snapToSurface;
    private boolean fadeOverTime;
    private long timeThreshold;
    private BucketMode bucketMode = BucketMode.None;
    private int size = ISize.NORMAL;
    private boolean hasGlyph;
    private VerticalAlignment glyphVerticalAlignment = VerticalAlignment.DEFAULT;
    protected boolean linkGlyphToHeading;
    private boolean linkGlyphToTrackOpacity = true;
    private TrackShape trackShape = TrackShape.Sphere;
    private double glyphVerticalOffset;
    private float glyphOpacity = 1.0f;
    private Color color;

    // Attributes for rendering our glyph (if any), we use a PointPlacemark to actually render the glyph
    protected PointPlacemarkAttributes glyphAttributes = new PointPlacemarkAttributes();

    /**
     * Construct on the given map Object.  Our rendering attributes are pulled from the TrackStylingContext provided,
     * which may be specific to an RDM for the MapObject or just the MapObject if no RDM-level TSD was assigned.
     *
     * @param mapObject Map Object we are handling
     * @param tsc       The Track Styling Context associated with this MO
     */
    public GlobeMarkerAttributes(@Nonnull IMapObject mapObject, @Nonnull ITrackStylingContext tsc)
    {
        this.mapObject = mapObject;
        stylingContext = tsc;

        stylingContext.addPropertyChangeListener(this); // And watch for changes later

        setMinMarkerSize(.01d);
        setMarkerPixels(5d);
    }

    @PostConstruct
    private void postConstruct()
    {
        initializeFromStylingContext();
    }

    /**
     * Initialize all our control variables and super-class attributes from the styling context.
     */
    private void initializeFromStylingContext()
    {
        setColor(stylingContext.getTrackShapeConstantColor());
        setOpacity(stylingContext.getTrackShapeConstantOpacity());
        size = stylingContext.getTrackShapeConstantSize();
        snapToSurface = stylingContext.isSnappedToSurface();
        fadeOverTime = stylingContext.isFadedOverTime();
        timeThreshold = stylingContext.getTimeThreshold();
        bucketMode = stylingContext.getBucketMode();

        setMarkerShapeFromTSD();
        setGlyphPropertiesFromTSD();
    }

    /**
     * @return The styling context being used
     */
    public ITrackStylingContext getTrackStylingContext()
    {
        return stylingContext;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propName = evt.getPropertyName();

        switch (propName)
        {
            case ITrackStylingContext.TRACK_STYLE_DEFINITION:
                initializeFromStylingContext(); // Full re-initialize when the TSD is changed out
                break;

            case ITrackStylingContext.TRACK_SHAPE_OPACITY:
                setOpacity(stylingContext.getTrackShapeConstantOpacity());
                break;

            case ITrackStylingContext.TRACK_SHAPE_SIZE:
                size = stylingContext.getTrackShapeConstantSize();
                break;

            case ITrackStylingContext.TRACK_SHAPE_COLOR:
                setColor(stylingContext.getTrackShapeConstantColor());
                break;

            case ITrackStylingContext.TRACK_SHAPE:
            case ITrackStylingContext.SHOW_HEADING:
                setMarkerShapeFromTSD();
                break;

            case ITrackStylingContext.BUCKET_MODE:
                bucketMode = stylingContext.getBucketMode();

                setMarkerShapeFromTSD();

                // If we are back to "None" then reset the color of the track
                if (bucketMode == BucketMode.None)
                {
                    setColor(stylingContext.getTrackShapeConstantColor());
                }
                break;

            case ITrackStylingContext.GLYPH_RESOURCE:
            case ITrackStylingContext.GLYPH_CONSTANT_OPACITY:
            case ITrackStylingContext.GLYPH_CONSTANT_SIZE:
            case ITrackStylingContext.GLYPH_LINKED_TRACK_HEADING:
            case ITrackStylingContext.GLYPH_LINKED_TRACK_OPACITY:
            case ITrackStylingContext.GLYPH_LINKED_TRACK_SIZE:
            case ITrackStylingContext.GLYPH_VERTICAL_ALIGNMENT:
            case ITrackStylingContext.GLYPH_VERTICAL_OFFSET:
                setGlyphPropertiesFromTSD();
                break;

            case ITrackStylingContext.FADED_OVER_TIME:
                fadeOverTime = stylingContext.isFadedOverTime();
                break;

            case ITrackStylingContext.TIME_THRESHOLD:
                timeThreshold = stylingContext.getTimeThreshold();
                break;

            case ITrackStylingContext.SNAP_TO_SURFACE:
                snapToSurface = stylingContext.isSnappedToSurface();
                break;
        }
    }

    /**
     * Determine if the given map point is the primary selected map point.
     *
     * @param mapPoint Map point to test
     * @return true if highlighted, false otherwise
     */
    public boolean isHighlighted(MapPoint mapPoint)
    {
        return highlightedMapPoint != null && (highlightedMapPoint == mapPoint || highlightedMapPoint.getRdmId() == mapPoint.getRdmId());
    }

    /**
     * Set the primary and secondary highlighting.
     *
     * @param primary   The map point that has the primary highlight, {@code null} to if none
     * @param secondary The id of the map object that has the secondary highlight, {@code null} if none
     */
    public static void setHighlighted(MapPoint primary, Integer secondary)
    {
        highlightedMapPoint = primary;
        highlightedMoId = secondary == null ? NO_MO_ID : secondary;
    }

    /**
     * Determine if the given map object (by id) has a secondary highlight.  This occurs when any
     * one of the map points associated with the map object is highlighted.
     *
     * @param moId ID of the MapObject to test
     * @return true if secondary highlighted, false otherwise
     */
    public boolean isSecondaryHighlighted(int moId)
    {
        return highlightedMoId == moId;
    }

    /**
     * Set the secondary highlighting.
     *
     * @param moId The id of the map object that has the secondary highlight, {@code null} if none
     */
    public static void setSecondaryHighlight(Integer moId)
    {
        highlightedMoId = moId == null ? NO_MO_ID : moId;
    }

    /**
     * Get the marker color.
     * @return the marker color
     * @since 5.6.1
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * Set the marker color.
     * @param color the marker color
     * @since 5.6.1
     */
    public void setColor(Color color)
    {
        this.color = color;
    }

    @Override
    public void setMaterial(Material material)
    {
        color = material.getDiffuse();
        super.setMaterial(material);
    }

    @Override
    public double getOpacity()
    {
        return fadeOpacity != NO_FADE ? fadeOpacity : super.getOpacity();
    }

    /**
     * Determine if the marker should be rendered.  We don't render the marker if the track shape is NONE and the
     * bucket mode is also NONE.  We render a marker if a bucket mode is set even if the track shape is NONE.
     *
     * @return {@code true} if the marker shape should be rendered. {@code false} otherwise
     */
    public boolean shouldRenderMarker()
    {
        return bucketMode != BucketMode.None || trackShape != TrackShape.None;
    }

    /**
     * Apply our state for the rendering operation taking place. If in pick mode, then just prepare things.  If
     * in actual rendering, then finalize all the attribute settings and apply our color/opacity.
     *
     * @param dc       Draw context from WW
     * @param mapPoint map point from which to draw our attribute settings
     */
    public void applyMapPoint(DrawContext dc, MapPoint mapPoint)
    {
        // Update our notion of "now" but only do it once per frame
        if (dc.getFrameTimeStamp() != frameTimeStamp)
        {
            // Determine "now", if we're in playback mode, then use the playback position, otherwise real current time
            now = playbackManager.isLive() ? System.currentTimeMillis() : playbackManager.getCurrentPlaybackPosition();

            // Don't update our frame timestamp yet.
            // A subsequent call to setupGlyph needs to happen, and it needs to know the frame has changed as well.
        }

        setupTooltip(mapPoint); // Prepare any tool tip for this map point
        setupMarkerSize(mapPoint); // Do final marker size determination

        if (!dc.isPickingMode())
        {
            // Handle Bucket Mode

            if (bucketMode != BucketMode.None)
            {
                setAttributesForBucketMode(mapPoint);
            }

            calculateOpacity(mapPoint); // Handle fade
        }

        int color = getColor().getRGB();
        final int opacity = (int) ((color >>> 24) * getOpacity());
        color = (0x00FFFFFF & color) | (opacity << 24);
        mapPoint.setColor(color);
    }

    /**
     * Do the final marker size setup.  The {@code size} member is already set to the "normal" size of the
     * marker.  This method should actually set the marker pixel size on the map point in preparation for rendering.
     *
     * @param mapPoint Map point being processed
     */
    protected void setupMarkerSize(MapPoint mapPoint)
    {
        setMarkerPixels(determineMarkerSize(mapPoint));
    }

    /**
     * Determine the size (in pixels) of the marker to be rendered for the given map point.
     *
     * @param mapPoint Map point being processed
     * @return The size of the marker to render in pixels
     */
    protected double determineMarkerSize(MapPoint mapPoint)
    {
        // Determine our pixel size based on our highlight state.  When secondary highlighted, we increase our size by
        // a factor of 1/2 of our current size.  Twice that if we are the primary highlight.

        final int half = Math.max(1, size / 2);
        int bump = isHighlighted(mapPoint)
                ? 2 * half
                : isSecondaryHighlighted(mapPoint.getMoId()) ? half : 0;

        return size + bump;
    }

    /**
     * Setup any tool tip text that should be displayed for this map point
     *
     * @param mapPoint Map point being processed
     */
    protected void setupTooltip(MapPoint mapPoint)
    {
    }

    /**
     * Setup any glyph that is required to render this track point.  This is also when changes in
     * glyph settings (like vertical alignment) are applied since they may have changed since the last time this
     * track point was rendered.  This method should only be called once per rendering cycle (during the pick).
     *
     * @param dc         Draw context from WW
     * @param mapPoint   map point from which to draw our attribute settings
     * @param markerSize Geographic size of the marker (in meters)
     */
    public void setupGlyph(DrawContext dc, MapPoint mapPoint, double markerSize)
    {
        // Setup any glyph we might use to render, only do this once per frame
        final long stamp = dc.getFrameTimeStamp();
        if (stamp != frameTimeStamp)
        {
            // Rotate glyph to stay parallel to the globe
            glyphAttributes.setPitch(dc.getView().getPitch().degrees);

            frameTimeStamp = stamp;
        }

        if (hasGlyph)
        {
            mapPoint.useGlyph(dc, glyphAttributes, glyphVerticalAlignment, glyphVerticalOffset, markerSize);
        } else
        {
            mapPoint.clearGlyph();
        }
    }

    public void applyMapPointForGlyph(MapPoint mapPoint)
    {
        if (hasGlyph)
        {
            if (linkGlyphToHeading)
            {
                final double heading = mapPoint.hasHeading() ? mapPoint.getHeading().degrees : 0.0;
                glyphAttributes.setHeading(heading);
            }

            // Opacity in PointPlacemark is applied as a color that is blended with the placemark image. Encode the
            // opacity in the Color's alpha channel.  If we are linking the glyph opacity to the track opacity make
            // sure to account for the current opacity of the track point.
            float opacity = linkGlyphToTrackOpacity ? (float) getOpacity() * glyphOpacity : glyphOpacity;
            glyphAttributes.setImageColor(new Color(1f, 1f, 1f, opacity));
        }
    }

    /**
     * Calculate the proper fade opacity (based on current state) for the map point about to be rendered.
     *
     * @param mapPoint Map point being processed
     */
    protected void calculateOpacity(MapPoint mapPoint)
    {
        if (fadeOverTime && !mapPoint.isSecondaryHighlighted())
        {
            calculateFadeOverTimeOpacity(mapPoint);
        } else
        {
            fadeOpacity = NO_FADE; // Default to opacity set in TSC
        }
    }

    /**
     * Calculate the proper fade opacity (based on "fade over time" semantics) for the map point about to be rendered.
     *
     * @param mapPoint Map point being processed
     */
    protected void calculateFadeOverTimeOpacity(MapPoint mapPoint)
    {
        // Determine the time span over which we are fading. If there is a tracks time threshold,
        // then that is the fade time span, otherwise it is the total range of times for the map object.

        final boolean hasTimeThreshold = timeThreshold > 0;
        final long timeStart = hasTimeThreshold ? now : mapObject.getMostRecentCommunicationTime();
        final long fadeTimeSpan = hasTimeThreshold ? timeThreshold : timeStart - mapObject.getOldestCommunicationTime();

        long timeScale = fadeTimeSpan / RAMP_VALUES;
        long timeRatio = timeScale > 0 ? Math.abs(timeStart - mapPoint.getTime()) / timeScale : 1_000;
        int i = Math.min((int) timeRatio, OPACITY_RAMP.length - 1);
        fadeOpacity = OPACITY_RAMP[i];
    }

    /**
     * Cleanup all resources.
     */
    public void cleanup()
    {
        stylingContext.removePropertyChangeListener(this);
    }

    /**
     * Given a bucket mode and a map point, set our attributes (color and shape type) accordingly.
     *
     * @param mapPoint map point being rendered
     */
    protected void setAttributesForBucketMode(MapPoint mapPoint)
    {
        Color c = null;

        switch (bucketMode)
        {
            case None:
                logger.warn("shouldn't have called setAttributesForBucketMode for mode: None");
                break; // Nothing to do here

            case HourOfDay:
                final int hour = DateFormatter.getHourOfTheDayFromTime(mapPoint.getTime());
                c = COLOR_SCALE_HOUR_OF_DAY[hour];

                setShapeType(hour < 12 ? amShape : pmShape); // Use sphere for AM and cube for PM
                break;

            case DayOfWeek:
                // Note that day numbers are 1 - 7, so subtract one to get color index
                c = COLOR_SCALE_DAY_OF_WEEK[DateFormatter.getDayOfTheWeekFromTime(mapPoint.getTime()) - 1];
                break;

            case MonthOfYear:
                c = COLOR_SCALE_MONTH_OF_YEAR[DateFormatter.getMonthOfTheYearFromTime(mapPoint.getTime())];
                break;

            default:
                logger.error("Unknown BucketMode: {}", bucketMode);
                break;
        }

        if (c == null)
        {
            logger.error("Failed to determine a color for bucket mode: {}, color={}", bucketMode, c);
            c = Color.WHITE; // Something that stands out
        }

        setColor(c);
    }

    /**
     * Set the WW shape type based on the TSDs track shape.  This is a simple conversion between specifiers.
     * Also set up the shape types to use for AM/PM indicators in bucket mode.
     */
    protected void setMarkerShapeFromTSD()
    {
        final boolean showHeading = stylingContext.isShowingHeading();

        amShape = showHeading ? BasicMarkerShape.ORIENTED_SPHERE : BasicMarkerShape.SPHERE;
        pmShape = showHeading ? BasicMarkerShape.ORIENTED_CUBE : BasicMarkerShape.CUBE;

        trackShape = stylingContext.getTrackShape();

        switch (trackShape)
        {
            case Sphere:
                setShapeType(showHeading ? BasicMarkerShape.ORIENTED_SPHERE : BasicMarkerShape.SPHERE);
                break;
            case Cone:
                setShapeType(showHeading ? BasicMarkerShape.ORIENTED_CONE : BasicMarkerShape.CONE);
                break;
            case Cylinder:
                setShapeType(showHeading ? BasicMarkerShape.ORIENTED_CYLINDER : BasicMarkerShape.CYLINDER);
                break;
            case Cube:
                setShapeType(showHeading ? BasicMarkerShape.ORIENTED_CUBE : BasicMarkerShape.CUBE);
                break;
            case None:
                // Nothing more to do
                break;
            default:
                logger.error("Unknown track shape: {}", stylingContext.getTrackShape());
                // Default to sphere
                trackShape = TrackShape.Sphere;
                setShapeType(showHeading ? BasicMarkerShape.ORIENTED_SPHERE : BasicMarkerShape.SPHERE);
        }
    }

    /**
     * Set the properties used to render the glyph based on the TSD attributes.
     */
    protected void setGlyphPropertiesFromTSD()
    {
        final String glyphResource = stylingContext.getGlyphResource();
        if (glyphResource != null)
        {
            hasGlyph = true;
            glyphVerticalAlignment = stylingContext.getGlyphVerticalAlignment();
            glyphVerticalOffset = stylingContext.getGlyphVerticalOffset();

            final URL resourceURL = convertResourceToURL(glyphResource);
            glyphAttributes.setImageAddress(resourceURL != null ? resourceURL.toString() : null);

            linkGlyphToHeading = stylingContext.isLinkGlyphToHeading();
            if (!linkGlyphToHeading)
            {
                // CORE-2492: If the glyph is not linked to a device heading (which is globe relative), then force
                // the glyph to be screen relative.  This ensures that glyphs that were not meant to rotate
                // don't rotate as the user rotates the globe.
                //
                // In the future, this should really be another TSD property.

                glyphAttributes.setHeadingReference(AVKey.RELATIVE_TO_SCREEN);
                glyphAttributes.setHeading(0.0); // North heading if not linked
            }

            glyphOpacity = stylingContext.getGlyphConstantOpacity();
            linkGlyphToTrackOpacity = stylingContext.isGlyphLinkedToTrackOpacity();

            int size = stylingContext.isGlyphLinkedToTrackSize()
                    ? stylingContext.getTrackShapeConstantSize()
                    : stylingContext.getGlyphConstantSize();

            glyphAttributes.setScale(size * 0.2); // range 0.2 - 2.0

            // Center image on the marker position
            glyphAttributes.setImageOffset(new Offset(0.5, 0.5, AVKey.FRACTION, AVKey.FRACTION));
        } else
        {
            hasGlyph = false;
        }
    }

    /**
     * Locate a glyph resource in the local file system or in one of our class loaders.
     * If it is a readable local file, then return the URL for that file.
     * Otherwise, start with the core class loader, then the TCCL, and finally scan all the plugin class loaders.
     *
     * @param resource The path to the resource
     * @return URL of located resource, or {@code null} if not found
     */
    @Nullable
    private URL convertResourceToURL(String resource)
    {
        // If it is already a VFS URL, then just return it
        if (resource.startsWith(Schemes.VFS_PREFIX))
        {
            try
            {
                final URL url = new URL(resource);

                // Sadly, even though WW states that it accepts a URL as in image address for a PointPlacemark, the
                // only URL type accepted is 'file:', so we need to convert this vfs URL to a file URL.
                final String path = url.getPath();
                final File file = new File(path);
                final File vfsFile = new File(vfsManager.getVirtualFileSystem(file.getParent()).getRootDir(), file.getName());
                return vfsFile.toURI().toURL();
            } catch (MalformedURLException e)
            {
                logger.warn("Failed to create URL from VFS reference [{}]: {}", resource, e.getMessage());
                return null;
            }
        }

        // First see if it is a local file
        final File file = new File(resource);
        if (file.canRead())
        {
            try
            {
                return file.toURI().toURL();
            } catch (MalformedURLException e)
            {
                logger.warn("Failed to create URL from file path [{}]: {}", resource, e.getMessage());
                return null;
            }
        }

        // Try our class loader first (core/platform resources)
        URL url = getClass().getResource(resource);

        if (url == null)
        {
            // Try the Thread context class loader
            url = Thread.currentThread().getContextClassLoader().getResource(resource);

            if (url == null)
            {
                // Try to fetch it as a plugin resource.  Just try them in sequence until we get one.
                url = pluginModuleManager.getPluginResource(resource);
            }
        }

        return url;
    }
}
