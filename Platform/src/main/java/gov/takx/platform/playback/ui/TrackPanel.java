package gov.takx.platform.playback.ui;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.ITimeSpan;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A TrackPanel is used to render the time-based data associated with a playback lane.  It is primarily a container,
 * but it also provides the functionality to render the marker cursors according to the playback lane's registered
 * interest in a marker.  If the lane owning this track is interested in a marker, then the marker will be rendered
 * with a unique shape (default is a diamond).
 *
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration")
public class TrackPanel extends TimePanel
{
    protected final IPlaybackLane playbackLane;
    protected boolean drawBorder;
    protected Color borderColor;
    protected final PanningMouseHandler mouseHandler = new PanningMouseHandler();

    public TrackPanel(IPlaybackLane playbackLane, ITimeSpan timeSpan, Color color)
    {
        super(timeSpan);

        this.playbackLane = playbackLane;

        setLayout(null); // Set layout to null; components are positioned with absolute coordinates
        setBackground(color);

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    @Override
    public void setBackground(Color c)
    {
        borderColor = c.darker();
        super.setBackground(c);
    }

    /**
     * Enable or disable a line border drawn around this track.
     *
     * @param enable if {@code true}, enable border to be drawn.
     */
    protected void enableDrawBorder(boolean enable)
    {
        drawBorder = enable;
        repaint();
    }

    @Override
    public void preLinePaint(Graphics2D g)
    {
        if (drawBorder)
        {
            g.setPaint(borderColor);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

        super.preLinePaint(g);
    }

    @Override
    protected void postLinePaint(Graphics2D g)
    {
        // Render the markers in this lane

        for (IMarker marker : timeSpan.getMarkers())
        {
            if (marker.hasMoment() && marker.isVisible())
            {
                final var laneCursor = marker.getLaneCursor(playbackLane);
                laneCursor.setXLocation((int) timeSpan.timeToX(marker.getMoment())); // Update the render X position
                renderCursor(laneCursor, g);
            }
        }
    }

    @Override
    protected ATimeCursor getCursorForMarker(IMarker marker)
    {
        return marker.getLaneCursor(playbackLane);
    }

    @Override
    public void disposeResources()
    {
        super.disposeResources();

        removeMouseListener(mouseHandler);
        removeMouseMotionListener(mouseHandler);
    }

    /**
     * Mouse event handler to let the user Pan the time span by using Click-drag operation using any button.
     */
    private final class PanningMouseHandler extends MouseAdapter
    {
        private static final Cursor PAN_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        private Integer lastXPos;
        private final Cursor defaultCursor = getCursor();

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (!e.isConsumed() && e.isControlDown())
            {
                // Allow mouse panning if Control is down.
                lastXPos = e.getX();
                setCursor(PAN_CURSOR);
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (!e.isConsumed() && lastXPos != null)
            {
                lastXPos = null;
                setCursor(defaultCursor);
                e.consume();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (!e.isConsumed() && lastXPos != null)
            {
                final int x = e.getX();
                timeSpan.panTimeByXOffset(-(x - lastXPos));
                lastXPos = x;
            }
        }
    }
}
