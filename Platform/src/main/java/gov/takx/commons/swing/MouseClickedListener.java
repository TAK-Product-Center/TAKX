package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * This is a FunctionalInterface that allows users to easily handle a click event without having to extend base classes
 * or implement unnecessary methods. <br/>
 * To use it with a method reference, the method
 * must be cast to {@link MouseClickedListener} so that the default methods are inherited.
 * <br/>
 * Usage:
 * <br/>
 * <pre>
 * {@code
 * myComponent.addMouseListener((MouseClickedListener)this::handleMouseClicked);
 * ...
 * void handleMouseClicked(MouseEvent e){...}
 * }
 * </pre>
 * Note: Because duplicate method references are not equal to one another, they cannot be used inline if you need to remove the listener
 * at later time. The method reference will need to be assigned to a variable first, which can then be used to remove the listener.
 * @since 5.3
 */
@SuppressWarnings("NewClassNamingConvention")
@FunctionalInterface
public interface MouseClickedListener extends MouseListener
{
    @Override
    void mouseClicked(@Nonnull MouseEvent e);

    @Override
    default void mousePressed(@Nonnull MouseEvent e)
    {
    }

    @Override
    default void mouseReleased(@Nonnull MouseEvent e)
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
