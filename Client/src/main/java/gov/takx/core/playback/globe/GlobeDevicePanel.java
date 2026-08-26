package gov.takx.core.playback.globe;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.ITimeSpanListener;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IDeviceTracksModelListener;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.playback.TimeLocation;
import gov.takx.platform.playback.TimeBoundary;
import gov.takx.platform.playback.TimeSpan;
import gov.takx.platform.playback.TimeSpanAdapter;
import gov.takx.platform.playback.TimelineUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This panel displays a summary of the track points in the playback timeline for a given Map Object.  Each track
 * point is represented by a simple round indicator.  The user can double-click on an indicator to navigate the
 * globe associated with our parent playback lane to the location of that track point.
 * <p/>
 * TODO: Need to add scale handling on mouse operation
 *
 * @since 1.47
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeDevicePanel
        extends JPanel
        implements IDeviceTracksModelListener, PropertyChangeListener, IDisposable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private ISmartTrackManager smartTrackManager;

    public static final int FIXED_HEIGHT = 16;
    public static final int MIN_WIDTH = 10;

    private static final int MAX_CACHE_IMAGE_PIXELS = 100000;
    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final Cursor DEFAULT_CURSOR = Cursor.getDefaultCursor();

    private final IPlaybackManager playbackManager;
    private final ITimeSpan globalTimeSpan;
    private final ITimeSpan localTimeSpan = new TimeSpan();
    private final IMapObject device;
    private final int topRowHeight;

    private final ITimeSpanListener timeSpanListener = new TimeSpanAdapter()
    {
        @Override
        public void timeSpanChanged()
        {
            logger.debug("Global timespan changed for: {}", device);
            updateDimension();
        }
    };

    private Color trackColor;
    private IDeviceTracksModel tracksModel;
    private boolean newModelInstalled;
    private final AtomicBoolean updateCachedPaint = new AtomicBoolean(false);
    private BufferedImage cachedPaint;

    private int panelHeight;
    private int pointSize;
    private int halfPointSize;

    /**
     * Construct a device panel on the given device.  Track data will be pulled from the globe model.
     *
     * @param playbackManager Playback manager for access to data models
     * @param device          The device (Map Object) for which we will display track data
     * @param topRowHeight    The height of the first (non-data) row in the track info panel (so our rows will line up)
     */
    public GlobeDevicePanel(IPlaybackManager playbackManager, IMapObject device, int topRowHeight)
    {
        this.playbackManager = playbackManager;
        this.device = device;
        this.topRowHeight = topRowHeight;

        globalTimeSpan = playbackManager.getTimeSpan();
        globalTimeSpan.addListener(timeSpanListener);

        localTimeSpan.setVisualComponent(this);
        localTimeSpan.setTimes(globalTimeSpan.getTimeBoundary()); // Default to global

        // At the point of construction, we can't perform operations that need to calculate our dimensions since
        // we don't have a parent.  We need to postpone the initial installation of the tracks model until
        // we actually get a parent.  See the addNotify() method.

        tracksModel = playbackManager.getGlobeModel().getDeviceTracksModel(device);

        // Track whether we had an empty model upon creation.  If so, we will eventually trigger a "fit" operation
        // to the global timeline when our data model is updated.  See deviceTimesChanged().
        if (tracksModel == null)
        {
            newModelInstalled = false;
        } else
        {
            newModelInstalled = tracksModel.getTrackPoints().isEmpty();
        }
    }

    @PostConstruct
    private void postConstruct()
    {
        smartTrackManager.getTrackStylingContext(device)
                .addPropertyChangeListener(this, ITrackStyleDefinition.TRACK_SHAPE_COLOR);

        init();
    }

    /**
     * We need to wait until we get added to the parent container to actually install our track model data.  Without
     * a parent, we can't determine our proper visual boundaries.
     */
    @Override
    public void addNotify()
    {
        super.addNotify();

        if (tracksModel != null)
        {
            installTracksTimeBoundary(tracksModel.getTimeBoundary());
            tracksModel.addListener(this);
        }
    }

    @Override
    public void disposeResources()
    {
        logger.trace("GlobeDevicePanel.dispose: {}", device);

        globalTimeSpan.removeListener(timeSpanListener);

        if (tracksModel != null)
        {
            tracksModel.removeListener(this);
            tracksModel = null;
        }

        cachedPaint = null;

        smartTrackManager.getTrackStylingContext(device)
                .removePropertyChangeListener(this, ITrackStyleDefinition.TRACK_SHAPE_COLOR);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since we just added an interior track point, we don't need to update our dimensions, but we do need to
     * force a full repaint.
     */
    @Override
    public void trackPointAdded(IMapObject device, ITimeLocation trackPoint)
    {
        updateCachedPaint.set(true);
        repaint();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Since our time boundaries have changed, we need to completely update our dimensions, tool tip, and force
     * a full repaint.
     */
    @InvokeOnEDT
    @Override
    public void deviceTimesChanged(IMapObject device, TimeBoundary boundary)
    {
        logger.debug("deviceTimesChanged: {} on {} (me={})", boundary, device, this.device);

        installTracksTimeBoundary(boundary);

        // If we originally had a new (empty) model, then we should now request that the global time span
        // be updated to fit our new data.  We need to do this since the initial filling of the model can
        // take some time and the initial "auto-fit" when our playback lane was added would not have had
        // a correct size since the model was initially empty.

        if (newModelInstalled)
        {
            if (playbackManager.getPlaybackConfiguration().isAutofitTimeline())
            {
                newModelInstalled = false;
            }
        }
    }

    /**
     * Install the given time boundary as our local time span.
     *
     * @param boundary Time boundary to install
     */
    private void installTracksTimeBoundary(TimeBoundary boundary)
    {
        if (boundary == null) return;

        localTimeSpan.setTimes(boundary);

        updateToolTip(null);

        updateDimension();
        updateCachedPaint.set(true);
        repaint();
    }

    /**
     * Custom paint component method which uses two forms of optimization to render timestamps as points on this clip.
     * The optimizations are as follows:
     * <p/>
     * 1) The area of the clip is computed, and if the area is less than a maximum allowed value (currently 5 million
     * pixels), then the entire clip will be rendered onto a cached image.  This cached image will then be painted
     * over the clip for as long as the clip data and size does not change.  Whenever the clip is zoomed, or more
     * timestamps are added to it, the cached image will be updated.  If the clip has many points (i.e. greater than
     * a few thousand), using a cached render image greatly enhances UI performance.  However, if the clip spans an
     * area greater than the max area, then this optimization can not be used, because it can lead to out-of-heap-space
     * errors.
     * <p/>
     * 2) Usually when the clip area is larger than the max area, it is due to the user zooming into the clip to
     * magnify its width and see greater detail within.  The clips exist on track panels which, in turn, are laid
     * out in a panel inside a scroll pane.  This means that the clip can extend way beyond the actual visible area.
     * The second optimization uses the parent scroll pane's view rectangle to determine the currently visible time
     * range.  In a zoomed-in situation, this probably means that most of the timestamps are not visible, so the visible
     * time range is treated as a boundary and passed into the render method.  The render method will only iterate
     * through and render the visible timestamps, skipping timestamps outside the current view.
     * <p/>
     * These optimizations compliment one another, in that they reduce render times for different situations.  The
     * first is only useful when the clip area is not very large (i.e. when most of the timestamps are visible).  But it
     * can't be used when the clip area is huge, as it will use all the available heap memory.  The second optimization
     * is only useful when only a small portion of the clip is in view (i.e. when most of the timestamps are not
     * visible).  Otherwise, too much time will be spent attempting to render a huge amount of timestamp points on every
     * paint cycle.
     *
     * @param g graphics context for this clip.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        int width = getWidth();
        int height = getHeight();
        int widthXHeight = width * height; // check for overflow

        if (width > 0 && height > 0 && widthXHeight < MAX_CACHE_IMAGE_PIXELS && widthXHeight > 0)
        {
            if (updateCachedPaint.get())
            {
                cachedPaint = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics cachedG = cachedPaint.createGraphics();
                super.paintComponent(cachedG);
                renderToGraphics(cachedG, null);
                updateCachedPaint.set(false);
            }
            g.drawImage(cachedPaint, 0, 0, null);
        } else
        {
            super.paintComponent(g);
            Rectangle view = playbackManager.getPlaybackStage().getScrollPaneViewRect();
            TimeBoundary boundary = new TimeBoundary(globalTimeSpan.xToTime(view.x), globalTimeSpan.xToTime(view.x + view.width));
            renderToGraphics(g, boundary);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event)
    {
        // Update the cursor and tool tip text according to the X coordinate of the mouse event.  If there is a track
        // point under that coordinate, then show the hand cursor and update the tool tip with the location data of
        // the track point. If not, then set the default cursor and show the general tool tip.

        final ITimeLocation location = getTimeLocationAtX(translateLocalXToGlobalX(event.getX()));

        //noinspection VariableNotUsedInsideIf
        setCursor(location != null ? HAND_CURSOR : DEFAULT_CURSOR);

        return updateToolTip(location);
    }

    @InvokeOnEDT
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        setTrackColorFromDevice();

        updateCachedPaint.set(true);
        repaint();
    }

    /**
     * Renderer to graphics context, which can be the paint graphics or cached buffered image graphics.  Using a cached
     * image often greatly speeds up paint times, but also greatly increases memory usage -> when zoom in on a large
     * track, out of heap space errors can occur.  So care should be taken to limit the size of the cached image.
     * <p/>
     * There is a secondary optimization to this renderer, which uses a {@code TimeBoundary} to limit the number
     * of rendered points/timestamps.  Only the timestamps that fall within the boundary will be rendered.  This
     * boundary is optional, so it may be {@code null}. If the boundary is {@code null}, all the timestamps will be
     * iterated and rendered, even if they are outside the current view.
     *
     * @param g        graphics context for this clip.
     * @param boundary optional time boundary for this render - i.e. only render the timestamps within this boundary.
     *                 If this value is {@code null}, the full extent of timestamps will be rendered.
     */
    private void renderToGraphics(Graphics g, TimeBoundary boundary)
    {
        // If we have a boundary, then just render the subset of tracks within it, otherwise, render all of them

        final NavigableSet<ITimeLocation> renderSet = boundary != null
                ? tracksModel.getTrackPoints(boundary.getStartTime(), boundary.getEndTime())
                : tracksModel.getTrackPoints();

        if (renderSet.isEmpty())
        {
            return;
        }

        if (logger.isTraceEnabled()) logger.trace("RenderToGraphics: n={} tb={}", renderSet.size(), boundary);

        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int halfHeight = (int) (getHeight() / 2.0);

        drawPoints(g, halfHeight, renderSet);
    }

    /**
     * Updates the dimensions based up on its internal duration and its owner's effective width and height.
     */
    @InvokeOnEDT
    private void updateDimension()
    {
        final double totalTimeFraction = ((double) localTimeSpan.getTotalTime()) / globalTimeSpan.getTotalTime();

        // Set a minimum width so that the panel never becomes invisible due to a 0 width
        final int width = Math.max((int) (totalTimeFraction * globalTimeSpan.getEffectiveWidth()), MIN_WIDTH);

        setMinimumSize(new Dimension(width, panelHeight));
        setPreferredSize(new Dimension(width, panelHeight));
        setMaximumSize(new Dimension(width, panelHeight));

        final int xOffset = getXOffset();
        final int yOffset = getYOffset();

        if (logger.isTraceEnabled())
        {
            logger.trace("{}", String.format("setBounds(%6d, %3d, %6d, %2d) on %s", xOffset, yOffset, width, panelHeight, device));
        }

        setBounds(xOffset, yOffset, width, panelHeight);

        revalidate();
    }

    /**
     * Get the x-offset of the start time of our local time span on the global time span
     * (the start time x-position within the owner panel).
     *
     * @return the x-offset in the parent container's coordinate system
     */
    private int getXOffset()
    {
        return (int) globalTimeSpan.timeToX(localTimeSpan.getStartTime());
    }

    /**
     * Get the Y-offset of this panel within its container (the playback lanes time data panel).  Each instance
     * of this panel is a fixed height, and they are stacked vertically with no gaps.
     *
     * @return Y offset within parent container's coordinate system
     */
    private int getYOffset()
    {
        // Locate our position within the components of our container.  Our index determines our offset since we are
        // stacked vertically.

        final var container = getParent();
        if (container != null)
        {
            final int nComps = container.getComponentCount();
            for (int i = 0; i < nComps; i++)
            {
                if (container.getComponent(i) == this) return topRowHeight + i * FIXED_HEIGHT;
            }
        }

        return 0;
    }

    /**
     * Draw a collection of track points (from timestamps)
     *
     * @param g          graphics instance
     * @param halfHeight half height of this component
     * @param timestamps collection of timestamps to render
     */
    private void drawPoints(Graphics g, int halfHeight, Collection<ITimeLocation> timestamps)
    {
        // Note that the order of these paint operations are important.  All the point borders are drawn first, then
        // the points are drawn over these borders.  This ensures that none of the borders obscure neighboring track
        // points when track points are densely clustered.  Basically, the white point borders become part of the
        // background before drawing any of the actual points.

        // NOTE: CORE-3808: Without the (x != lastX) optimizations below, there are cases where this method
        // somehow interferes with WW rendering.  I don't understand it.  Maybe the openGL pipeline is somehow
        // used in both areas and this blast of "fillOval" calls interferes with the WW rendering loop.
        // Strangely, this has no effect on the normal EDT event processing.  The UI remains responsive, but
        // WW gets completely locked up.

        // I also pulled some logic out of TimeSpan.timeToXRounded() to avoid numerous method calls per timestamp
        // that are just determining values that are static during our rendering loop.
        //
        //  localTimeSpan.timeToXRounded(time)
        //      is the same as
        //  (int) Math.round(((double) (timestamp.getTime() - startTime) / totalTime) * effectiveWidth)

        final long totalTime = localTimeSpan.getTotalTime();
        final long startTime = localTimeSpan.getStartTime();
        final int effectiveWidth = localTimeSpan.getEffectiveWidth();

        if (totalTime <= 0) return; // don't render anything unless we have a non-zero time span

        // Paint all the track point borders (in white, to contrast with the black background)
        g.setColor(Color.WHITE);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (timestamps)
        {
            int lastX = Integer.MIN_VALUE;

            for (ITimeLocation timestamp : timestamps)
            {
                int x = (int) Math.round(((double) (timestamp.getTime() - startTime) / totalTime) * effectiveWidth);

                // Optimize for when scale is large enough that ovals overlap completely (no pixel difference)
                if (x != lastX)
                {
                    g.fillOval(x - halfPointSize - 1, halfHeight - halfPointSize - 1, pointSize + 2, pointSize + 2);
                    lastX = x;
                }
            }
        }

        // Paint all the track points
        g.setColor(trackColor);

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (timestamps)
        {
            int lastX = Integer.MIN_VALUE;

            for (ITimeLocation timestamp : timestamps)
            {
                int x = (int) Math.round(((double) (timestamp.getTime() - startTime) / totalTime) * effectiveWidth);

                // Optimize for when scale is large enough that ovals overlap completely (no pixel difference)
                if (x != lastX)
                {
                    g.fillOval(x - halfPointSize, halfHeight - halfPointSize, pointSize, pointSize);
                    lastX = x;
                }
            }
        }
    }

    /**
     * Update our tool tip text according to the given location.  If the location is {@code null}, then show the general
     * tool tip including our bounds and range.  If it is not {@code null}, then display the location data.
     *
     * @param location Location of interest
     * @return Tool tip text
     */
    private String updateToolTip(ITimeLocation location)
    {
        String s;

        if (location != null)
        {
            s = "<html>"
                    + CoordinateSystem.getLocationString(location, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class))
                    + "<br>Double-Click to go to this location on the Globe.</html>";
        } else
        {
            s = "<html>" + device.getName() + "<br>"
                    + TimelineUtils.getDateRangeString(localTimeSpan.getTimeBoundary()) + "</html>";
        }

        setToolTipText(s);

        return s;
    }

    /**
     * Get the TimeLocation of an entry in the track data for this Map Object that contains the given
     * X coordinate.  Only return a TimeLocation if the X positions are within the boundaries of the marker.
     * If the X location is contained within more than one TimeLocation's marker, then return the one that is
     * closest to the given X coordinate.
     *
     * @param x The X coordinate to translate
     * @return The TimeLocation of the
     */
    private ITimeLocation getTimeLocationAtX(int x)
    {
        // Shift the mouse
        final NavigableSet<ITimeLocation> trackPoints = tracksModel.getTrackPoints();
        final TimeLocation mouseLocation = TimeLocation.makeTimeKey(globalTimeSpan.xToTime(x));

        // Choose either floor or ceiling value based upon which one is closer to the mouse location (unless one
        // or both of these values are null).
        final ITimeLocation floor = trackPoints.floor(mouseLocation);
        final ITimeLocation ceiling = trackPoints.ceiling(mouseLocation);

        if (floor == null && ceiling == null)
        {
            return null;
        }

        ITimeLocation location;
        if (floor != null && ceiling != null)
        {
            long floorDiff = Math.abs(mouseLocation.getTime() - floor.getTime());
            long ceilingDiff = Math.abs(mouseLocation.getTime() - ceiling.getTime());

            location = floorDiff < ceilingDiff ? floor : ceiling;
        } else
        {
            location = Objects.requireNonNullElse(ceiling, floor);
        }

        // Determine the x-position of the found location.  This x-position must be within halfPointSize pixels
        // away from the parameter x.  This is to ensure that the parameter x is actually within the bounds of the
        // onscreen rendered location point.
        final int locationX = globalTimeSpan.timeToXRounded(location.getTime());

        if (Math.abs(locationX - x) <= halfPointSize)
        {
            return location;
        }

        return null;
    }

    /**
     * Set the color of our track markers to match the current track shape color of our device.
     */
    private void setTrackColorFromDevice()
    {
        final ITrackStylingContext tsc = smartTrackManager.getTrackStylingContext(device);
        trackColor = tsc.getTrackShapeConstantColor();
    }

    /**
     * Translate the given X coordinate (in local coordinates) into an X coordinate relative to the global time span.
     *
     * @param x The X coordinate to translate
     * @return X coordinate in global time span coordinate system
     */
    private int translateLocalXToGlobalX(int x)
    {
        return getX() + x;
    }

    /**
     * Handle a single click on our display.  If we are not playing, set the playback position to the clicked position.
     *
     * @param e Mouse event
     */
    private void handleSingleClick(MouseEvent e)
    {
        if (!playbackManager.isPlaying())
        {
            globalTimeSpan.setXPosition(translateLocalXToGlobalX(e.getX()));
        }
    }

    /**
     * Handle a double click event on our display.  If there is a track point under the mouse, then navigate the
     * globe to that location.
     *
     * @param e Mouse event
     */
    @SuppressWarnings("DataFlowIssue")
    private void handleDoubleClick(MouseEvent e)
    {
        final var location = getTimeLocationAtX(translateLocalXToGlobalX(e.getX()));
        if (location != null) globeManager.getLastFocusedGlobe().goTo(location);
    }

    /**
     * Initialize various UI-related values and handlers
     */
    private void init()
    {
        setBackground(Color.BLACK); // TODO KMB: Fix for FlatLAF (TAKX-739)

        setTrackColorFromDevice(); // Determine our track point color

        // Handle double-click to navigate the globe
        addMouseListener(new ClickHandler());

        // Regenerate cached image if we are ever resized
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateCachedPaint.set(true);
                repaint();
            }
        });

        // Setup our marker rendering sizes
        panelHeight = FIXED_HEIGHT;
        pointSize = Math.round(panelHeight * 0.625f);
        halfPointSize = pointSize / 2;
    }

    /**
     * Handler to avoid firing both single and double click events when the user double-clicks in the track.
     */
    private final class ClickHandler extends MouseAdapter implements ActionListener
    {
        private final static int TIME_BETWEEN_CLICKS = 300;
        private MouseEvent event;
        private final Timer timer;

        public ClickHandler()
        {
            timer = new Timer(TIME_BETWEEN_CLICKS, this);
            timer.setRepeats(false);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            timer.stop();
            handleSingleClick(event);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() > 2) return;

            event = e;

            if (timer.isRunning())
            {
                timer.stop();
                handleDoubleClick(e);
            } else
            {
                timer.restart();
            }
        }
    }
}
