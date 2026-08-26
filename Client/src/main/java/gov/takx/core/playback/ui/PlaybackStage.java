package gov.takx.core.playback.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.PlaybackConfigurationAdapter;
import gov.takx.api.playback.ui.IPlaybackStage;
import gov.takx.api.ui.UISpaceResizeEvent;
import gov.takx.core.playback.globe.GlobeDevicePanel;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.invoke.MethodHandles;

/**
 * The primary UI for playback.  The Playback Stage holds the playback controls, global timeline,
 * and a collection of playback lanes.
 *
 * @since 1.47
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PlaybackStage extends JPanel implements IPlaybackStage, IDisposable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final PlaybackControlPanel playbackControlPanel;
    private final IPlaybackSystem playbackSystem;
    private final ITimeSpan timeSpan;
    private int originalControlPanelWidth;

    public PlaybackStage(final IPlaybackSystem playbackSystem)
    {
        this.playbackSystem = playbackSystem;

        timeSpan = playbackSystem.getTimeSpan();
        playbackControlPanel = new PlaybackControlPanel(playbackSystem);

        playbackSystem.getPlaybackConfiguration().addPlaybackConfigurationListener(new ConfigChangeHandler());

        initComponents();

        controlPane.add(playbackControlPanel);
        originalControlPanelWidth = playbackControlPanel.getWidth();

        // Add the ruler in its own panel since it must always be visible (not be hidden when the vertical scroll bar is adjusted).
        final RulerPanel rulerPanel = new RulerPanel(this, timeSpan);
        rulerPane.add(rulerPanel);
        rulerPane.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                super.componentResized(e);
                resizeStage();
            }
        });

        // Ensure that the stage has minimum size so when it is first opened, it is a reasonable size (one lane high)
        Dimension minimumSize = new Dimension(120, 50);
        setMinimumSize(minimumSize);
        setPreferredSize(minimumSize);

        /*
         * I'm making a number of scroll pane modifications in this class due to the way I want the track lanes to be
         * displayed.  For example, I have nested scroll panes (one with a vertical scroll bar, and another with a
         * horizontal scroll bar).  If you are working with this UI code, you should probably know the reasons why
         * I made these changes:
         *
         * 1) I want all the regular track lane views to be controlled by the same vertical scroll bar.  Each track
         * lane includes an info panel on the left side, and the actual track panel on the right.  Because of this, they
         * are all contained within the same vertical scroll pane.
         *
         * 2) The track panels can change in width, so a horizontal scroll bar is necessary.  However, the info panels
         * have a constant width AND must always be visible on the left-side of the track lane.  They should never be
         * hidden because the user adjusts the track panels' horizontal scroll bar.  Because of this, the track panels
         * are placed in a nested horizontal scroll pane.  The two scroll panes (vertical and horizontal) must be nested
         * so that the views that they control are kept independent.  That is, vertical scrolling affects both info
         * panel and track panel views, whereas horizontal scrolling only affects the track panels' view.
         *
         * 3) Since the horizontal scroll pane is nested inside the vertical scroll pane, by default, the horizontal
         * scroll bar can be erroneously hidden when adjusting the vertical scroll bar.  To prevent this from happening,
         * the horizontal scroll bar is removed from the horizontal scroll pane and placed in a panel below the vertical
         * scroll pane.  Now, the view of the vertical scroll pane does not affect the visibility of the horizontal
         * scroll bar.
         *
         * 4) I want the horizontal scroll bar to automatically hide or be made visible depending on the width of the
         * tracks lane.  By removing the horizontal scroll bar from the horizontal scroll pane, and putting it in a
         * different panel, it isn't affected by the "AS_NEEDED" visibility flag.  So I made a horizontal scroll bar
         * adjustment listener refresh the scroll bar's visibility depending on the viewport extent and track panel
         * width.
         *
         * 5) The global track panel (tick marks and time labels) should always be visible.  It should not be affected
         * by the vertical scroll bar.  This panel provides mouse-interactivity for time cursor position, so it should
         * be very easy for the user to find.  So it is in an independent panel above the vertical scroll pane.
         * Weirdly, it is also necessary for this panel to be affected by the horizontal scroll bar: as you change
         * the track panels' view, the globe track panel should match.  The horizontal scroll bar adjustment listener
         * updates the globe panel's scroll pane to the same position as the horizontal scroll pane.
         *
         * 6) The global track scroll panel viewport needs to match the vertical scroll bar's visible width, so that
         * the cursor line on the global track panel matches the other track panels' cursor lines.  The global track
         * scroll panel (tick marks and time labels) is adjusted whenever the vertical scroll bar changes visibility.
         */

        // Add the listener to keep the ruler's end spacer the correct size.
        final var scrollBar = stageVerticalScrollPane.getVerticalScrollBar();
        scrollBar.addComponentListener(new RulerSpacerAdjuster());
        scrollBar.setUnitIncrement(GlobeDevicePanel.FIXED_HEIGHT);

        // Add in the handler to deal with the mouse wheel for zoom/pan/scale operations
        final var zoomScalePanMouseWheelHandler = new ZoomScalePanMouseWheelHandler();
        trackPane.addMouseWheelListener(zoomScalePanMouseWheelHandler);
        rulerPane.addMouseWheelListener(zoomScalePanMouseWheelHandler);

        rulerPanel.addMouseAdapter(new ScrubMouseHandler());

        // Give the track scroll pane a small preferred size, to prevent it from growing larger width-wise than its
        // parent scroll pane (that's right - it's a scroll pane within a scroll pane).
        //
        // If, for some reason, the track scroll pane still extends beyond the time stage boundaries, then this line
        // ain't doing what it's supposed to do.
        trackScrollPane.setPreferredSize(new Dimension(1, 1));

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                ((ZoomPanel) trackPane).refreshZoomPanelSize();
            }
        });

        reallocateTimelines();
    }

    public void panelResized(@Observes UISpaceResizeEvent event)
    {
        if (event.getComponent() instanceof PlaybackView)
        {
            resizeStage();
        }
    }

    private void resizeStage()
    {
        if (originalControlPanelWidth == 0) originalControlPanelWidth = playbackControlPanel.getPreferredSize().width;

        // If the window is too narrow, hide less important visual components.
        playbackControlPanel.setTimeSpanLabelsVisible(getWidth() >= originalControlPanelWidth);

        // If the control panel width is > 80% of the total stage width, compress the buttons into a single
        // pull down menu that contains the buttons within it.
        if (originalControlPanelWidth > getWidth() * 0.8)
        {
            playbackControlPanel.collapseIndividualViewControlButtons();
        } else
        {
            playbackControlPanel.expandIndividualViewControlButtons();
        }
    }

    @InvokeOnEDT
    private void reallocateTimelines()
    {
        logger.debug("reallocateTimelines");

        removeAllLanes();

        int i = 0;
        for (var lane : playbackSystem.getPlaybackLanes())
        {
            lane.setBackgroundColor(i++ % 2 == 0 ? Color.LIGHT_GRAY : Color.GRAY.brighter());

            final var infoPanel = lane.getInfoPanel();
            logger.debug("    add: {}", infoPanel);

            trackInfoPane.add(infoPanel);
            trackPane.add(lane.getTimeDataPanel());
        }
        globalTimePanel.add(playbackSystem.getGlobalTimeControl());

        repaint();
        revalidate();
    }

    @Override
    public void setMinimumHeight(int height)
    {
        Dimension size = getMinimumSize();
        size.height = height;
        setMinimumSize(size);
    }

    @Override
    public void resetZoom()
    {
        ((ZoomPanel) trackPane).resetZoomFactor();
    }

    @Override
    public PlaybackControlPanel getPlaybackControls()
    {
        return playbackControlPanel;
    }

    @Override
    public Rectangle getScrollPaneViewRect()
    {
        return trackScrollPane.getViewport().getViewRect();
    }

    @Override
    public void disposeResources()
    {
        if (logger.isTraceEnabled()) logger.trace("PlaybackStage.dispose");

        removeAllLanes();
    }

    /**
     * Remove all the components in the info pane and the track pane.
     */
    private void removeAllLanes()
    {
        trackInfoPane.removeAll();
        trackPane.removeAll();

        trackInfoPane.revalidate();
        trackPane.revalidate();
        rulerPane.revalidate();

        revalidate();
    }

    /**
     * The timeline ruler should always be in view and is therefore not in the track scroll pane.  Its width still
     * needs to be updated whenever the track scroll pane vertical scroll bar changes visibility: when it is
     * visible, the timeline ruler needs to remove the vertical scroll bar's width from its own width.
     */
    private final class RulerSpacerAdjuster extends ComponentAdapter
    {
        @Override
        public void componentShown(ComponentEvent e)
        {
            setGlobalEndSpacerSize(new Dimension(stageVerticalScrollPane.getVerticalScrollBar().getWidth(),
                    globalEndSpacer.getHeight()));
        }

        @Override
        public void componentHidden(ComponentEvent e)
        {
            setGlobalEndSpacerSize(new Dimension(0, globalEndSpacer.getHeight()));
        }

        private void setGlobalEndSpacerSize(Dimension d)
        {
            globalEndSpacer.setSize(d);
            globalEndSpacer.setPreferredSize(d);
            globalEndSpacer.setMinimumSize(d);
            globalEndSpacer.setMaximumSize(d);
        }
    }

    /**
     * Mouse handler to scrub the playback cursor along the timeline.  If playback is running, then pause playback
     * while the user is moving the thumb.
     * <p>
     * This class handles changing the mouse cursor when it is over the timeline cursor object. We can't just
     * add the mouse listener to that object because of the way panels are nested the
     * mouse events do not register.
     */
    private final class ScrubMouseHandler extends MouseAdapter
    {
        private static final long MINIMUM_UPDATE_TIME = 66;    // ~15 fps position updates
        private static final int MOUSE_CURSOR_CHANGE_AREA_BUFFER = 5;
        private static final Cursor RESIZE_CURSOR = new Cursor(Cursor.W_RESIZE_CURSOR);
        private static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

        private long timestamp;
        private boolean clickPause;

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (e.isConsumed()) return; // Don't work off events that have been consumed

            if (e.getButton() == MouseEvent.BUTTON1)
            {
                if (playbackSystem.isPlaying() || playbackSystem.isLive())
                {
                    clickPause = true; // Pause while the user drags around the thumb
                    playbackSystem.pause();
                }

                timeSpan.setXPosition(timeStampPosition(e.getX()));
                setCursor(RESIZE_CURSOR);

                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (e.isConsumed()) return; // Don't work off events that have been consumed

            if (e.getButton() == MouseEvent.BUTTON1)
            {
                timeSpan.setXPosition(timeStampPosition(e.getX()));
                setCursor(DEFAULT_CURSOR);

                playbackSystem.stopScrubbing();

                if (clickPause)
                {
                    playbackSystem.play(); // Resume playing
                    clickPause = false;
                } else
                {
                    playbackSystem.pause();
                }

                e.consume();
            }
        }

        /**
         * Use the mouse drag x-location to determine where the set the playback cursor line.  Limit changes sent
         * to the actual clips to {@code MINIMUM_UPDATE_TIME} since last change.  This is because events sent by
         * mouse drags may be sent very quickly and continuously, and it is necessary to prevent event FIFO overflows
         * in the clip playback library (i.e. libVLC).
         *
         * @param e the mouse event object.
         */
        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (e.isConsumed()) return; // Don't work off events that have been consumed

            if (SwingUtilities.isLeftMouseButton(e))
            {
                // Use the current time and last timestamp to determine whether this drag event will actually be fired.
                //
                // The most recent mouse drag event may not be sent, but this isn't much of an issue, because the mouse
                // released event always sends a position event.

                long time = System.currentTimeMillis();
                if ((time - timestamp) >= MINIMUM_UPDATE_TIME)
                {
                    timeSpan.setXPosition(timeStampPosition(e.getX()));
                    scrollView(e.getX());
                }

                e.consume();
            }
        }

        private void scrollView(int xPosition)
        {
            Rectangle rect = trackScrollPane.getViewport().getViewRect();
            int xOffset = xPosition - trackScrollPane.getViewport().getViewPosition().x;
            if (xOffset < 0 || xOffset > rect.width)
            {
                JScrollBar scrollBar = trackScrollPane.getHorizontalScrollBar();
                int value = scrollBar.getValue();
                int dx = xOffset;
                if (xOffset > rect.width)
                {
                    dx = xOffset - rect.width;
                }
                scrollBar.setValue(value + dx);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            setCursor(isMouseOverTimelineCursor(e) ? RESIZE_CURSOR : DEFAULT_CURSOR);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (!isMouseOverTimelineCursor(e)) setCursor(DEFAULT_CURSOR);
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            setCursor(isMouseOverTimelineCursor(e) ? RESIZE_CURSOR : DEFAULT_CURSOR);
        }

        /**
         * @return {@code true} if the mouse is over the timeline cursor.
         * @since 5.4
         */
        private boolean isMouseOverTimelineCursor(MouseEvent e)
        {
            return (e.getX() > (timeSpan.getXPosition() - MOUSE_CURSOR_CHANGE_AREA_BUFFER)) &&
                    (e.getX() < (timeSpan.getXPosition() + MOUSE_CURSOR_CHANGE_AREA_BUFFER));
        }

        private int timeStampPosition(int xPosition)
        {
            timestamp = System.currentTimeMillis();
            return xPosition;
        }
    }

    /**
     * Mouse wheel handler to Zoom, Scale, or Pan the time span.
     * <p>
     * If the mouse wheel is used with no modifier keys, then a zoom
     * operation is performed (in or out depending on the direction of the wheel motion).
     * <p>
     * If the Shift key is down while the wheel is used, then a Scale operation (in or out depending on the
     * direction of the wheel motion).
     * <p>
     * If the Control key is down while the wheel is used, then a Pan operation (left or right depending on the
     * direction of the wheel motion).
     */
    private final class ZoomScalePanMouseWheelHandler implements MouseWheelListener
    {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            int rotation = e.getWheelRotation();
            int x = e.getX();

            // Control + wheel pans the time span left or right in time.
            if (e.isControlDown())
            {
                playbackSystem.stopLive();
                timeSpan.panTimeByXOffset(rotation * ITimeSpan.DEFAULT_PAN_OFFSET);
            }

            // Wheel (with no key modifiers) alters the time span duration (scale)
            else
            {
                if (rotation > 0)
                {
                    timeSpan.scaleTimeOut(ITimeSpan.DEFAULT_ZOOM_FACTOR, x);
                } else if (rotation < 0)
                {
                    timeSpan.scaleTimeIn(ITimeSpan.DEFAULT_ZOOM_FACTOR, x);
                }
            }
        }
    }

    /**
     * An extended JPanel intended to facilitate stretching (zooming) itself width-wise inside a JScrollPane.
     */
    private class ZoomPanel extends JPanel
    {
        private int xOffset = -1;
        private long mouseXTime = -1;
        private double zoomFactor = 1;

        public ZoomPanel()
        {
            addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    // Update the scroll bar position so that the time centered on the mouse cursor remains stationary
                    JScrollBar scrollBar = trackScrollPane.getHorizontalScrollBar();
                    int scrollValue = timeSpan.timeToXRounded(mouseXTime) - xOffset;
                    scrollBar.setValue(scrollValue);
                }
            });
        }

        public void resetZoomFactor()
        {
            setZoomFactor(1, 0);
        }

        public void setZoomFactor(double zoomFactor, int x)
        {
            resizeToZoomFactor(zoomFactor < 1 ? 1 : zoomFactor, x);
        }

        public void refreshZoomPanelSize()
        {
            int barHeight = trackScrollPane.getHorizontalScrollBar().getHeight() + 2;
            Dimension d = new Dimension(getWidth(), trackScrollPane.getHeight() - barHeight);
            setPreferredSize(d);
            resizeToZoomFactor(zoomFactor, 0);
            revalidate();
        }

        private void resizeToZoomFactor(double zoomFactor, int x)
        {
            // Limit the zoom factor to a reasonable size.
            // If left unbounded, long span timelines can render incorrectly at high zoom levels.
            if (zoomFactor > 75) return;

            // If the amount of time in the scroll pane view is less than or equal to 4 seconds and the new zoom factor
            // is even greater, then ignore the new zoom factor (want to limit the zoomed panel size).
            if (getZoomViewTime(this.zoomFactor) <= 4000 && zoomFactor > this.zoomFactor) return;

            xOffset = x - trackScrollPane.getViewport().getViewPosition().x;
            mouseXTime = timeSpan.xToTime(x);

            int newWidth = (int) (trackScrollPane.getWidth() * zoomFactor);

            // Limit zoom out to a max of 200k
            if (newWidth >= 200_000) return;

            this.zoomFactor = zoomFactor;
            int barHeight = trackScrollPane.getHorizontalScrollBar().getHeight() + 2;
            Dimension d = new Dimension(newWidth, trackScrollPane.getHeight() - barHeight);
            setPreferredSize(d);

            Dimension e = new Dimension(newWidth, rulerPane.getHeight());
            rulerPane.setPreferredSize(e);
            rulerPane.setSize(e);
            rulerScrollPane.setPreferredSize(new Dimension(trackScrollPane.getWidth(), rulerScrollPane.getHeight()));

            revalidate();
        }

        private long getZoomViewTime(double zoomFactor)
        {
            long duration = timeSpan.getTotalTime();
            return (long) (duration / zoomFactor);
        }
    }

    /**
     * Watch for changes in the playback configuration and update this UI accordingly.
     */
    private final class ConfigChangeHandler extends PlaybackConfigurationAdapter
    {
        /**
         * {@inheritDoc}
         * <p/>
         * Rebuild our UI with the panels from the current lane collection.
         */
        @Override
        public void playbackLanesAdded(IPlaybackLane... playbackLanes)
        {
            reallocateTimelines();
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Rebuild our UI with the panels from the current lane collection.
         *
         * @param playbackLanes
         */
        @Override
        public void playbackLanesRemoved(IPlaybackLane... playbackLanes)
        {
            reallocateTimelines();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        controlPane = new JPanel();
        var rulerContainer = new JPanel();
        globalTimePanel = new JPanel();
        rulerScrollPane = new JScrollPane();
        rulerPane = new JPanel();
        globalEndSpacer = new JPanel(null);
        stageVerticalScrollPane = new JScrollPane();
        var stagePane = new JPanel();
        trackInfoPane = new JPanel();
        trackScrollPane = new JScrollPane();
        trackPane = new ZoomPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(174, 25));
        setName("this");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //======== controlPane ========
        {
            controlPane.setMaximumSize(new Dimension(32767, 30));
            controlPane.setName("controlPane");
            controlPane.setLayout(new BorderLayout());
        }
        add(controlPane);

        //======== rulerContainer ========
        {
            rulerContainer.setBorder(null);
            rulerContainer.setMaximumSize(new Dimension(2147483647, 35));
            rulerContainer.setName("rulerContainer");
            rulerContainer.setLayout(new FormLayout(
                    "85dlu, pref:grow, pref",
                    "top:[35px,pref]"));

            //======== globalTimePanel ========
            {
                globalTimePanel.setName("globalTimePanel");
                globalTimePanel.setLayout(new FlowLayout());
            }
            rulerContainer.add(globalTimePanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

            //======== rulerScrollPane ========
            {
                rulerScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                rulerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                rulerScrollPane.setViewportBorder(null);
                rulerScrollPane.setBorder(null);
                rulerScrollPane.setName("rulerScrollPane");

                //======== rulerPane ========
                {
                    rulerPane.setBorder(null);
                    rulerPane.setName("rulerPane");
                    rulerPane.setLayout(new BoxLayout(rulerPane, BoxLayout.Y_AXIS));
                }
                rulerScrollPane.setViewportView(rulerPane);
            }
            rulerContainer.add(rulerScrollPane, cc.xy(2, 1, CellConstraints.FILL, CellConstraints.FILL));

            //---- globalEndSpacer ----
            globalEndSpacer.setName("globalEndSpacer");
            rulerContainer.add(globalEndSpacer, cc.xy(3, 1));
        }
        add(rulerContainer);

        //======== stageVerticalScrollPane ========
        {
            stageVerticalScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            stageVerticalScrollPane.setViewportBorder(null);
            stageVerticalScrollPane.setBorder(null);
            stageVerticalScrollPane.setName("stageVerticalScrollPane");

            //======== stagePane ========
            {
                stagePane.setName("stagePane");
                stagePane.setLayout(new FormLayout(
                        "85dlu, default:grow",
                        "top:default:grow"));

                //======== trackInfoPane ========
                {
                    trackInfoPane.setName("trackInfoPane");
                    trackInfoPane.setLayout(new BoxLayout(trackInfoPane, BoxLayout.Y_AXIS));
                }
                stagePane.add(trackInfoPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));

                //======== trackScrollPane ========
                {
                    trackScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                    trackScrollPane.setViewportBorder(null);
                    trackScrollPane.setBorder(null);
                    trackScrollPane.setName("trackScrollPane");

                    //======== trackPane ========
                    {
                        trackPane.setBorder(null);
                        trackPane.setName("trackPane");
                        trackPane.setLayout(new BoxLayout(trackPane, BoxLayout.Y_AXIS));
                    }
                    trackScrollPane.setViewportView(trackPane);
                }
                stagePane.add(trackScrollPane, cc.xy(2, 1, CellConstraints.FILL, CellConstraints.FILL));
            }
            stageVerticalScrollPane.setViewportView(stagePane);
        }
        add(stageVerticalScrollPane);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel controlPane;
    private JPanel globalTimePanel;
    private JScrollPane rulerScrollPane;
    private JPanel rulerPane;
    private JPanel globalEndSpacer;
    private JScrollPane stageVerticalScrollPane;
    private JPanel trackInfoPane;
    private JScrollPane trackScrollPane;
    private JPanel trackPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
