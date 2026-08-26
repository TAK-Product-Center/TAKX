package gov.takx.platform.playback.ui;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.takx.api.playback.IDisposable;
import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.ITimeSpanListener;
import jakarta.annotation.Nonnull;

import javax.swing.JPanel;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A time span aware panel that can be used to render elements that need coordination with said time span.
 * A time panel includes a position-aware cursor that renders on the panel.  The default cursor is a
 * {@link TimeCursorLine}, but it may be replaced.
 *
 * @since 1.47
 */
public class TimePanel extends JPanel implements ITimeSpanListener, IDisposable
{
    protected ITimeSpan timeSpan;
    protected ATimeCursor timeCursor;
    protected final TimeCursorMouseHandler mouseHandler = new TimeCursorMouseHandler();
    private final Map<IMarker, CursorMouseHandler> cursorHandlerMap = Collections.synchronizedMap(new IdentityHashMap<>());

    public TimePanel(ITimeSpan timeSpan)
    {
        this(new TimeCursorLine(), timeSpan);
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public TimePanel(@Nonnull ATimeCursor timeCursor, @Nonnull ITimeSpan timeSpan)
    {
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        setTimeSpan(timeSpan);
        this.timeCursor = timeCursor;
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

    public ITimeSpan getTimeSpan()
    {
        return timeSpan;
    }

    public ATimeCursor getTimeCursor()
    {
        return timeCursor;
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        final Graphics2D g2 = (Graphics2D) g;

        preLinePaint(g2);
        renderCursor(timeCursor, g2, timeSpan.getXPosition());
        postLinePaint(g2);
    }

    /**
     * Render a cursor at its current X position using the provided graphics context.
     *
     * @param cursor Cursor to render
     * @param g      Graphics context to use
     */
    protected void renderCursor(ATimeCursor cursor, Graphics2D g)
    {
        cursor.setHeight(getHeight());
        cursor.paintComponents(g);
    }

    /**
     * Render a cursor at the given X position using the provided graphics context.
     *
     * @param cursor Cursor to render
     * @param g      Graphics context to use
     * @param x      X position at which to render cursor
     */
    protected void renderCursor(ATimeCursor cursor, Graphics2D g, int x)
    {
        cursor.setXLocation(x);
        cursor.setHeight(getHeight());
        cursor.paintComponents(g);
    }

    /**
     * Override this method if you want to paint custom graphics on this panel before the playback line is painted.
     *
     * @param g this panel's Graphics2D instance.
     */
    @SuppressWarnings("UnusedParameters")
    protected void preLinePaint(Graphics2D g)
    {
    }

    /**
     * Override this method if you want to paint custom graphics on this panel after the playback line is painted.
     *
     * @param g this panel's Graphics2D instance.
     */
    @SuppressWarnings("UnusedParameters")
    protected void postLinePaint(Graphics2D g)
    {
    }

    @Override
    public void timeSpanChanged()
    {
        repaint();
    }

    @Override
    public void timePositionChanged()
    {
        repaint();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void markerAdded(IMarker marker)
    {
        final var handler = new CursorMouseHandler(getCursorForMarker(marker), marker);
        cursorHandlerMap.put(marker, handler);
        addMouseListener(handler);
        addMouseMotionListener(handler);
        repaint();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void markerRemoved(IMarker marker)
    {
        final var handler = cursorHandlerMap.remove(marker);
        if (handler != null)
        {
            removeMouseListener(handler);
            removeMouseMotionListener(handler);
        }
        repaint();
    }

    /**
     * Return the cursor to use for the given marker within this panel's boundaries.
     *
     * @param marker Marker of interest
     * @return Cursor to use
     * @implNote This base implementation always returns the {@link IMarker#getRulerCursor() ruler cursor}.
     * @since 5.7
     */
    protected ATimeCursor getCursorForMarker(IMarker marker)
    {
        return marker.getRulerCursor();
    }

    @Override
    public void disposeResources()
    {
        if (timeSpan != null) timeSpan.removeListener(this);
        cursorHandlerMap.clear();
    }

    /**
     * Manage the cursor according to whether the mouse is over the time cursor or not.
     * We can't just add the mouse listener to the cursor because the cursor is never added to the UI directly.
     */
    protected class TimeCursorMouseHandler extends MouseAdapter
    {
        private static final int MOUSE_CURSOR_BUFFER = 5;

        private static final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
        private static final Cursor W_RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);

        @Override
        public void mouseEntered(MouseEvent e)
        {
            setCursor(isWithinCursorArea(e) ? W_RESIZE_CURSOR : DEFAULT_CURSOR);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (!isWithinCursorArea(e)) setCursor(DEFAULT_CURSOR);
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            setCursor(isWithinCursorArea(e) ? W_RESIZE_CURSOR : DEFAULT_CURSOR);
        }

        /**
         * @return {@code true} if the X position of the mouse event is within the current position's area + buffer
         * @since 5.7
         */
        private boolean isWithinCursorArea(MouseEvent e)
        {
            final int x = e.getX();
            final int timePositionX = timeSpan.getXPosition();
            return (x > timePositionX - MOUSE_CURSOR_BUFFER) && (x < timePositionX + MOUSE_CURSOR_BUFFER);
        }
    }
}
