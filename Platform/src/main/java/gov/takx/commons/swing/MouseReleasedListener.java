package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This is a FunctionalInterface that allows users to easily handle a mouse event without having to extend base classes
 * or implement unnecessary methods. <br/>
 * For usage see {@link MouseClickedListener}
 * @since 5.3
 */
@FunctionalInterface
public interface MouseReleasedListener extends MouseListener
{
    @Override
    void mouseReleased(@Nonnull MouseEvent e);

    @Override
    default void mouseClicked(@Nonnull MouseEvent e)
    {
    }

    @Override
    default void mousePressed(@Nonnull MouseEvent e)
    {
    }

    @Override
    default void mouseEntered(@Nonnull MouseEvent e)
    {
    }

    @Override
    default void mouseExited(@Nonnull MouseEvent e)
    {
    }
}
