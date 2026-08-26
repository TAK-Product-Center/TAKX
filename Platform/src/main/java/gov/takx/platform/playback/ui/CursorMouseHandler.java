package gov.takx.platform.playback.ui;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.playback.IMarker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JComponent;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Mouse listener to handle events on time cursors (and {@link IMarker markers} since they are rendered as cursors).
 * Even though the cursor is a JComponent, it is never actually added to any container, so normal mouse listeners
 * will not work.
 * <p/>
 * This handler will detect hover, and display the tooltip configured on the cursor.  It will detect double-click
 * events and delegate them to {@link IMarker#handleDoubleClick(MouseEvent)}.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class CursorMouseHandler extends MouseAdapter
{
    private final ATimeCursor cursor;
    private final IMarker marker;

    private boolean inCursor;
    private String savedTooltip;

    /**
     * @param cursor The cursor we are monitoring for mouse events
     * @param marker The marker associated with the cursor, {@code null} if none
     */
    public CursorMouseHandler(@Nonnull ATimeCursor cursor, @Nullable IMarker marker)
    {
        this.cursor = cursor;
        this.marker = marker;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1 && e.getButton() == MouseEvent.BUTTON1 && cursor.contains(e.getPoint()))
        {
            if (marker != null) marker.handleDoubleClick(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if (cursor.contains(e.getPoint()))
        {
            if (!inCursor) // Initial entry into cursor, save the current tooltip text
            {
                final var component = (JComponent) e.getSource();
                savedTooltip = component.getToolTipText();
                component.setToolTipText(cursor.getToolTipText());
            }
            inCursor = true;
        } else
        {
            if (inCursor) // Exit, restore saved tooltip text
            {
                final var component = (JComponent) e.getSource();
                component.setToolTipText(savedTooltip);
                savedTooltip = null;
            }
            inCursor = false;
        }
    }
}
