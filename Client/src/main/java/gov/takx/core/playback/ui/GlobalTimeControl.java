package gov.takx.core.playback.ui;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectPartialDelete;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.ITrackInfo;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.ITimeSpanListener;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.commons.lang.LongArrayList;
import gov.takx.core.playback.globe.PlaybackQueryUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.StampedLock;

import static java.lang.Math.abs;

/**
 * A user control for interacting with the playback lane in a global sense. The goal of this control is to give the
 * user a visualization of the entire time span represented in TAKX. The entire time span is defined as the earliest
 * temporal reference to the latest. This should include both tracks and non track data such as media in media analysis.
 * The Global control will display a representation of this to the user and will also allow the user to navigate
 * very quickly in time using a thumb control or by clicking in the control bar.
 *
 * @since 2.18
 */
@PseudoBean
public class GlobalTimeControl extends JPanel
        implements ITimeSpanListener, IMapObjectControllerListener, IDisposable, ComponentListener //, SkinChangeListener // TODO KMB: Fix for FlatLAF (TAKX-739)
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MIN_SLIDE_WIDTH = 4;

    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

    private ITimeSpan timeSpan;
    protected final Map<String, LongArrayList> trackTimes = new ConcurrentHashMap<>(); // All of our track points
    protected boolean hasBeenShown;

    private boolean controlInitialized;
    private final Color borderColor = Color.DARK_GRAY; // TODO KMB: Fix for FlatLAF (TAKX-739)
    private final Color fillColor = Color.LIGHT_GRAY;
    private final Color tickColor = Color.BLUE;
    private final Color thumbColor = Color.GREEN;
    private final Color thumbHighlightColor = Color.WHITE;
    private static final int CONTROL_HEIGHT = 14;
    private Rectangle controlRect;
    private Rectangle thumbRect;
    private final GlobalTimeMouseHandler mouseHandler = new GlobalTimeMouseHandler();
    private boolean inSlider;
    private int xPressed = -1;

    @Inject
    private IPlaybackManager playbackManager;

    @Inject
    private ExecutorService executorService;

    private final TrackTimeBounds trackTimeBounds = new TrackTimeBounds();

    @PostConstruct
    public void postConstruct()
    {
        timeSpan = playbackManager.getTimeSpan();
        setLayout(null);
        setOpaque(false);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        if (timeSpan == null) throw new IllegalArgumentException("Time span must not be null");

        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setTimeSpan(playbackManager.getTimeSpan());
        addComponentListener(this);

        for (var device : playbackManager.getGlobeModel().getAllDevices())
        {
            device.getController().addMapObjectControllerListener(this);
        }
        initializeTimeline();
        calculateSlider();
//        SubstanceLookAndFeel.registerSkinChangeListener(this); // TODO KMB: Fix for FlatLAF (TAKX-739)
    }

    /**
     * Initialize the timeline for all MapObjects.
     */
    private void initializeTimeline()
    {
        initializeTimeline(null);
    }

    /**
     * Initialize the timeline for all MapObjects.
     *
     * @param removedMapObject When a MO gets removed we get called on clientMapObjectRemoved, but the MO may still
     *                         be represented in the device list. In this case, we pass in the MO that has been
     *                         deleted so we can ignore it.
     */
    private void initializeTimeline(IMapObject removedMapObject)
    {
        synchronized (trackTimeBounds)
        {
            long oldestTrack = Long.MAX_VALUE;
            long newestTrack = Long.MIN_VALUE;

            for (var device : playbackManager.getGlobeModel().getAllDevices())
            {
                if (device.equals(removedMapObject)) continue;

                final var trackInfo = device.getTrackInfo();

                final long start = trackInfo.getTrackStartDate();
                if (start < oldestTrack) oldestTrack = start;

                final long end = trackInfo.getTrackEndDate();
                if (end > newestTrack) newestTrack = end;
            }

            trackTimeBounds.newest = newestTrack;
            trackTimeBounds.oldest = oldestTrack;
        }
    }

    /**
     * Determine how wide the slider needs to be and where it should start according to the current timespan
     * within the global timespan.
     */
    private void calculateSlider()
    {
        if (thumbRect == null) return;

        double sliderRatio = 1;
        double startRatio = 0;

        synchronized (trackTimeBounds)
        {
            if (trackTimeBounds.newest > trackTimeBounds.oldest)
            {
                sliderRatio = timeSpan.getTotalTime() / (double) getTotalGlobalTime();
                startRatio = (timeSpan.getStartTime() - trackTimeBounds.oldest) / (double) getTotalGlobalTime();
                startRatio = Double.max(startRatio, 0.0);
            }
        }
        thumbRect.width = (int) (sliderRatio * controlRect.width);
        thumbRect.width = Integer.max(MIN_SLIDE_WIDTH, thumbRect.width);
        thumbRect.x = (int) (startRatio * controlRect.width) + controlRect.x;
        thumbRect.x = Integer.min(controlRect.x + controlRect.width - thumbRect.width, thumbRect.x);
        repaint();
    }

    /**
     * Get the earliest time represented in this control
     *
     * @return the earliest time
     */
    private long getGlobalStartTime()
    {
        return Long.min(timeSpan.getStartTime(), trackTimeBounds.oldest);
    }

    /**
     * Get the latest time represented in this control
     *
     * @return the latest time
     */
    private long getGlobalEndTime()
    {
        return Long.max(timeSpan.getEndTime(), trackTimeBounds.newest);
    }

    /**
     * Get the latest time minus the earliest time.
     *
     * @return the total time
     */
    private long getTotalGlobalTime()
    {
        return getGlobalEndTime() - getGlobalStartTime();
    }

    @Override
    public void addNotify()
    {
        // Notice that this only gets called when the component is actually displayed. So if the user never opens
        // playback this will never get called. so this is an excellent place to allow notification that playback
        // has not been initialized yet.
        super.addNotify();

        controlInitialized = true;
        // TODO: must be a better way. Really want this controlRect to auto size to its parent panel
        setPreferredSize(new Dimension(149, 25));
        if (logger.isDebugEnabled()) logger.debug("addNotify: {}", getParent().getPreferredSize());
        setControlSize();
//        skinChanged(); // TODO KMB: Fix for FlatLaf (TAKX-739)
    }

    /**
     * Find the tick marks in the control
     */
    private void buildTickMarks()
    {
        if (!hasBeenShown) return;

        for (var device : playbackManager.getGlobeModel().getAllDevices())
        {
            // Populate the track points using a BG thread
            executorService.execute(() -> {
                Thread.currentThread().setName("Playback Global Tick Loader: " + device.getName());
                initTrackPoints(device);
            });
        }
    }

    /**
     * Load the tracks for this Map Object into our set of points.
     */
    private void initTrackPoints(IMapObject device)
    {
        try
        {
            // Add the list to the map after it is populated to avoid any concurrency issues
            trackTimes.put(device.getKey(), getRelevantTimes(device));

            setControlSize();
        } catch (InvalidFieldSelectorException | ConflictingConstraintException e)
        {
            logger.error("Failed to query for tracks", e);
        }
    }

    /**
     * @return The track point relevant times for the given device
     * @since 5.6
     */
    @Nonnull
    private static LongArrayList getRelevantTimes(@Nonnull IMapObject device) throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        // Retrieve just the relevant time field
        final var results = PlaybackQueryUtils.query(device, CoreField.relevantTime);
        final var times = new LongArrayList(results.size());

        for (var result : results) times.add((long) result[0]);

        if (logger.isDebugEnabled()) logger.debug("Relevant times built for: {} ({})", device.getName(), times.size());

        return times;
    }

    public void setTimeSpan(ITimeSpan timeSpan)
    {
        if (this.timeSpan != null)
        {
            this.timeSpan.removeListener(this);
        }

        this.timeSpan = timeSpan;
        timeSpan.addListener(this);
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        final Graphics2D g2 = (Graphics2D) g;

        preLinePaint(g2);
        paintLines(g2);
        postLinePaint(g2);
    }

    public void preLinePaint(Graphics2D g)
    {
        g.setPaint(fillColor);
        g.fillRect(controlRect.x, controlRect.y, controlRect.width, controlRect.height);
        g.setPaint(borderColor);
        g.drawRect(controlRect.x, controlRect.y, controlRect.width, controlRect.height);
    }

    private final StampedLock sl = new StampedLock();

    /**
     * Render a cursor at its current X position using the provided graphics context.
     *
     * @param g Graphics context to use
     */
    protected void paintLines(Graphics2D g)
    {
        long stamp = sl.tryOptimisticRead();
        try
        {
            if (!sl.validate(stamp))
            {
                stamp = sl.readLock();
            }
            trackTimes.forEach((key, times) -> {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (times)
                {
                    times.forEach(time -> {
                        int tick = (int) (controlRect.x + controlRect.width * (time - getGlobalStartTime()) / getTotalGlobalTime());

                        g.setPaint(tickColor);
                        g.drawLine(tick, controlRect.y + 1, tick, controlRect.y + controlRect.height - 1);
                    });
                }
            });
        } finally
        {
            if (sl.isReadLocked()) sl.unlockRead(stamp);
        }
    }

    protected void postLinePaint(Graphics2D g)
    {
        g.setPaint(thumbColor);
        g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
        if (inSlider || xPressed > -1)
        {
            g.setPaint(thumbHighlightColor);
            Rectangle r = new Rectangle(thumbRect.x + 1, thumbRect.y + 1, thumbRect.width - 2, thumbRect.height - 2);
            g.drawRect(r.x, r.y, r.width, r.height);
        }
    }

    @RemoveObserver
    @Override
    public void disposeResources()
    {
        if (timeSpan != null)
        {
            timeSpan.removeListener(this);
        }
    }

    @Override
    public void timeSpanChanged()
    {
        calculateSlider();
        if (logger.isTraceEnabled())
        {
            logger.trace("start={}, end = {}", timeSpan.getStartTime(), timeSpan.getEndTime());
        }
    }

    @Override
    public void timePositionChanged()
    {
        calculateSlider();
    }

    @Override
    public void markerAdded(IMarker marker)
    {
        repaint();
    }

    @Override
    public void markerRemoved(IMarker marker)
    {
        repaint();
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        setControlSize();
        if (logger.isDebugEnabled()) logger.debug("Resized: {}", getParent().getPreferredSize());
    }

    @InvokeOnEDT
    private void setControlSize()
    {
        if (!controlInitialized) return;

        // TODO: we should be responding to size changes of the parent control
        controlRect = new Rectangle(2, getHeight() / 2 - CONTROL_HEIGHT / 2, getWidth() - 4, CONTROL_HEIGHT);
        thumbRect = new Rectangle(4, getHeight() / 2 - CONTROL_HEIGHT / 2 - 1, getWidth() / 2, CONTROL_HEIGHT + 2);
        calculateSlider();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
        if (logger.isDebugEnabled()) logger.debug("Moved");
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
        if (logger.isDebugEnabled()) logger.debug("Shown");
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
        if (logger.isDebugEnabled()) logger.debug("Hidden");
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        IMapObject mapObject = event.getMapObject();

        if (!hasBeenShown) return;

        if (playbackManager.getGlobeModel().eligibleForPlayback(mapObject))
        {
            if (logger.isDebugEnabled()) logger.debug("Adding MO: {}", mapObject.getKey());
            ITrackInfo trackInfo = mapObject.getTrackInfo();
            synchronized (trackTimeBounds)
            {
                final long trackStartDate = trackInfo.getTrackStartDate();
                if (trackStartDate < trackTimeBounds.oldest) trackTimeBounds.oldest = trackStartDate;

                final long trackEndDate = trackInfo.getTrackEndDate();
                if (trackEndDate > trackTimeBounds.newest) trackTimeBounds.newest = trackEndDate;
            }

            mapObject.getController().addMapObjectControllerListener(this);
            executorService.execute(() -> {
                Thread.currentThread().setName("Playback Global Tick Loader: " + mapObject.getName());
                initTrackPoints(mapObject);
            });
            repaint();
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        IMapObject mapObject = event.getMapObject();

        if (!hasBeenShown) return;

        if (logger.isDebugEnabled()) logger.debug("Removing MO: {}", mapObject.getKey());
        initializeTimeline(mapObject);
        trackTimes.remove(mapObject.getKey());
        repaint();
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is partially deleted.
     *
     * @param event The event providing the partially deleted map object
     * @since 2.38 - formerly mapObjectPartialDelete
     */
    private void onMapObjectPartialDelete(@Observes MapObjectPartialDelete event)
    {
        IMapObject mapObject = event.getMapObject();

        if (!hasBeenShown) return;

        if (logger.isDebugEnabled()) logger.debug("Partial Delete MO: {}", mapObject.getKey());
        initializeTimeline();

        LongArrayList tracks = trackTimes.get(mapObject.getKey());
        if (tracks == null) return;

        synchronized (tracks)
        {
            tracks.sort();
            int floor = tracks.binarySearch(event.getTimeOfDeletion());

            if (floor < 0) floor = abs(floor + 1);
            tracks.remove(0, floor);
        }

        repaint();
    }

    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
        if (rdm.hasLocation() && rdm.isRenderable() && !rdm.isManuallyGeneratedLocation())
        {
            final long t = rdm.getRelevantTime();
            synchronized (trackTimeBounds)
            {
                if (t > trackTimeBounds.newest) trackTimeBounds.newest = t;
                if (t < trackTimeBounds.oldest) trackTimeBounds.oldest = t;
            }

            final LongArrayList times = trackTimes.get(rdm.getUniqueKey());
            if (times != null)
            {
                synchronized (times)
                {
                    times.add(t);
                }
            }
            setControlSize();
        }
    }

    /**
     * Adjust the TimeSpan of the main playback control according to the location of the thumb in the GlobalTimeControl
     */
    private void adjustTimeSpan()
    {
        playbackManager.stopLive();
        final long globalTime = getTotalGlobalTime();
        long startTime;
        if (thumbRect.x + thumbRect.width == controlRect.x + controlRect.width)
        {
            // Make special accommodation for the end of the control
            startTime = getGlobalEndTime() - timeSpan.getTotalTime();
        } else
        {
            final double startRatio = (thumbRect.x - controlRect.x) / (double) controlRect.width;

            synchronized (trackTimeBounds)
            {
                startTime = (long) (globalTime * startRatio) + Long.min(trackTimeBounds.oldest, timeSpan.getStartTime());
                startTime = Long.min(startTime, Long.max(trackTimeBounds.newest, timeSpan.getEndTime()) - timeSpan.getTotalTime());
            }
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("thumbRect={}, controlRect={}, globalTime={}, oldestTrack={}", thumbRect, controlRect, globalTime, trackTimeBounds.oldest);
        }
        timeSpan.setTimes(startTime, startTime + timeSpan.getTotalTime());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void visibilityChanged(@Observes UISpaceVisibilityEvent event)
    {
        if (event.getTitle().equals(PlaybackView.TITLE))
        {
            if (!hasBeenShown && event.isVisible())
            {
                hasBeenShown = true;
                buildTickMarks();
            }
            if (logger.isDebugEnabled()) logger.debug("Playback Visibility = {}", event.isVisible());
        }
    }

    // TODO KMB: Fix for FlatLaf (TAKX-739)
//    @Override
//    public void skinChanged()
//    {
//        SubstanceColorScheme compScheme = SubstanceColorSchemeUtilities
//                .getColorScheme(this, ColorSchemeAssociationKind.FILL, ComponentState.DEFAULT);
//        SubstanceColorScheme compSchemeEnabled = SubstanceColorSchemeUtilities
//                .getColorScheme(this, ColorSchemeAssociationKind.FILL, ComponentState.ENABLED);
//        Color bgColor = compScheme.getBackgroundFillColor();
//        Color lineColor = compScheme.getLineColor();
//        Color selectedColor = compSchemeEnabled.getForegroundColor();
//        fillColor = bgColor;
//        borderColor = lineColor;
//        thumbColor = lineColor.darker();
//        tickColor = selectedColor.darker();
//        thumbHighlightColor = selectedColor;
//        repaint();
//    }

    /**
     * Simple holder to track the oldest and newest times on our timeline.
     */
    private static final class TrackTimeBounds
    {
        long newest = Long.MIN_VALUE;
        long oldest = Long.MAX_VALUE;
    }

    /**
     * A mouse handler for the GlobalTimeControl
     */
    private class GlobalTimeMouseHandler extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (thumbRect.contains(e.getPoint()))
            {
                xPressed = -1;
                e.consume();
            } else if (controlRect.contains(e.getPoint()))
            {
                int x = (e.getX() - controlRect.x) - thumbRect.width / 2;
                x = x < controlRect.x ? controlRect.x : x + thumbRect.width > controlRect.width + controlRect.x
                        ? controlRect.x + controlRect.width - thumbRect.width : x;
                thumbRect.setLocation(x, thumbRect.y);
                adjustTimeSpan();
                repaint();
                e.consume();
            } else
            {
                xPressed = -1;
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (thumbRect.contains(e.getPoint()))
            {
                xPressed = e.getX() - thumbRect.x;
                e.consume();
            } else
            {
                xPressed = -1;
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (xPressed > 0)
            {
                xPressed = -1;
                repaint();
                e.consume();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (xPressed > -1)
            {
                int x = e.getX() - xPressed;
                x = x < controlRect.x ? controlRect.x
                        : x + thumbRect.width > controlRect.width + controlRect.x
                                ? controlRect.width - thumbRect.width + controlRect.x
                        : x;
                thumbRect.setLocation(x, thumbRect.y);
                repaint();
                e.consume();
                if (logger.isTraceEnabled())
                {
                    logger.trace("xPressed = {}, xDrag = {}, x = {}", xPressed, e.getX(), x);
                }
                adjustTimeSpan();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            updateCursor(e);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (xPressed == -1)
            {
                setCursor(DEFAULT_CURSOR);
                inSlider = false;
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            updateCursor(e);
        }

        private void updateCursor(MouseEvent e)
        {
            inSlider = thumbRect.contains(e.getPoint());

            setCursor(inSlider ? HAND_CURSOR : DEFAULT_CURSOR);
            repaint();
        }
    }
}
