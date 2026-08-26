package gov.takx.commons.swing;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom mouse adapter that performs a vertical scroll on the owner component's ancestor viewport when
 * a mouse dragged event occurs. This is primarily intended to provide a standard touchscreen user
 * experience for scrolling.
 *
 * @since 3.2
 */
public class DragScrollAdapter extends MouseAdapter
{
    private final JComponent owner;
    private Point origin;

    private DragScrollAdapter(JComponent owner)
    {
        this.owner = owner;
    }

    /**
     * Add drag-to-scroll handling to the given component.
     *
     * @param component Component to update
     */
    public static void addDragScrollTo(JComponent component)
    {
        final DragScrollAdapter scroller = new DragScrollAdapter(component);
        component.addMouseListener(scroller);
        component.addMouseMotionListener(scroller);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        origin = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (origin != null)
        {
            JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, owner);

            if (viewport != null)
            {
                int deltaY = origin.y - e.getY();
                int deltaX = origin.x - e.getX();

                Rectangle view = viewport.getViewRect();
                view.y += deltaY;
                view.x += deltaX;

                owner.scrollRectToVisible(view);
            }
        }
    }
}
