package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * An event listener that reliably fires {@link MouseEvent#MOUSE_ENTERED} and {@link MouseEvent#MOUSE_EXITED} events for a
 * given component. Standard swing components do not reliably fire these events due to the way component hierarchies
 * interact. This class avoids that by using a global event listener. The global listener is not shared amongst components
 * and is removed when the given component is detached from the swing hierarchy.
 * <p>
 * Because accomplishing this requires two listeners, this listener must be attached via
 * {@link #addRolloverListener(JComponent, MouseListener)} and removed via
 * {@link #removeRolloverListener(JComponent, MouseListener)}.
 *
 * @since 5.8
 */
public class MouseRolloverListener implements AWTEventListener, HierarchyListener
{
    private static final String LISTENER_LIST_CLIENT_KEY = "MOUSE_ROLLOVER_LISTENERS";
    private final JComponent component;
    private final MouseListener mouseListener;
    private boolean entered;

    private MouseRolloverListener(@Nonnull JComponent component, @Nonnull MouseListener mouseListener)
    {
        this.component = component;
        this.mouseListener = mouseListener;
    }

    @Override
    public void eventDispatched(AWTEvent e)
    {
        var mouseEvent = (MouseEvent) e;
        var screenPoint = mouseEvent.getLocationOnScreen();
        SwingUtilities.convertPointFromScreen(screenPoint, component);
        if (component.contains(screenPoint))
        {
            if (!entered)
            {
                if (mouseEvent.getID() != MouseEvent.MOUSE_ENTERED) mouseEvent = new MouseEvent(mouseEvent.getComponent(), MouseEvent.MOUSE_ENTERED, mouseEvent.getWhen(), mouseEvent.getModifiersEx(), mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount(), mouseEvent.isPopupTrigger());
                entered = true;
                mouseListener.mouseEntered(mouseEvent);
            }
        } else if (entered)
        {
            entered = false;
            if (mouseEvent.getID() != MouseEvent.MOUSE_EXITED) mouseEvent = new MouseEvent(mouseEvent.getComponent(), MouseEvent.MOUSE_EXITED, mouseEvent.getWhen(), mouseEvent.getModifiersEx(), mouseEvent.getX(), mouseEvent.getY(), mouseEvent.getClickCount(), mouseEvent.isPopupTrigger());
            mouseListener.mouseExited(mouseEvent);
        }
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e)
    {
        if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0 && component.getParent() == null)
            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }

    /**
     * Adds a {@link MouseRolloverListener} for a given component.
     *
     * @param component the component to detect and produce entered and exited events for
     * @param listener the mouse listener to call {@code mouseEntered} and {@code mouseExited} on
     * @apiNote {@link MouseListener} is used here for compatibility and familiarity, but note that the only methods that
     * will be called on the listener are {@link MouseListener#mouseEntered(MouseEvent)} and
     * {@link MouseListener#mouseExited(MouseEvent)}. Consumers should prefer to pass a {@link MouseEnteredExitedListener} for clarity
     * when possible.
     * @return the listener that was created and added
     */
    @Nonnull
    public static MouseRolloverListener addRolloverListener(@Nonnull JComponent component, @Nonnull MouseListener listener)
    {
        var map = getRolloverListeners(component);
        var rolloverListener = map.get(listener);
        if (rolloverListener == null)
        {
            rolloverListener = new MouseRolloverListener(component, listener);
            Toolkit.getDefaultToolkit().addAWTEventListener(rolloverListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
            component.addHierarchyListener(rolloverListener);
            map.put(listener, rolloverListener);
        }
        return rolloverListener;
    }

    /**
     * @param component the component to remove the listener from
     * @param listener the listener to remove
     */
    public static void removeRolloverListener(@Nonnull JComponent component, @Nonnull MouseListener listener)
    {
        var map = getRolloverListeners(component);
        var rolloverListener = map.get(listener);
        if (rolloverListener != null)
        {
            Toolkit.getDefaultToolkit().removeAWTEventListener(rolloverListener);
            component.removeHierarchyListener(rolloverListener);
        }
    }

    @Nonnull
    private static Map<MouseListener, MouseRolloverListener> getRolloverListeners(@Nonnull JComponent component)
    {
        //noinspection unchecked
        var map = (Map<MouseListener, MouseRolloverListener>) component.getClientProperty(LISTENER_LIST_CLIENT_KEY);
        if (map == null)
        {
            map = new IdentityHashMap<>();
            component.putClientProperty(LISTENER_LIST_CLIENT_KEY, map);
        }
        return map;
    }
}
