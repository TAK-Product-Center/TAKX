package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A MouseListener that defaults all methods except {@link MouseListener#mouseEntered(MouseEvent)} and
 * {@link MouseListener#mouseExited(MouseEvent)} to no-ops, and sends entered and exited events to the functional
 * interface method {@link #mouseEnteredExited(MouseEvent)}. Mainly for use with {@link MouseRolloverListener}.
 *
 * @since 5.8
 */
@FunctionalInterface
public interface MouseEnteredExitedListener extends MouseListener
{
    @Override
    default void mouseClicked(MouseEvent e)
    {
    }

    @Override
    default void mousePressed(MouseEvent e)
    {
    }

    @Override
    default void mouseReleased(MouseEvent e)
    {
    }

    @Override
    default void mouseEntered(MouseEvent e)
    {
        mouseEnteredExited(e);
    }

    @Override
    default void mouseExited(MouseEvent e)
    {
        mouseEnteredExited(e);
    }

    /**
     * @param e a {@link MouseEvent#MOUSE_ENTERED} or {@link MouseEvent#MOUSE_EXITED} event
     */
    void mouseEnteredExited(@Nonnull MouseEvent e);
}
