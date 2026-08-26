package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * This is a FunctionalInterface that allows users to easily handle a key pressed event without having to extend base classes
 * or implement unnecessary methods. <br/>
 * To use it with a method reference, the method
 * must be cast to {@link KeyPressedListener} so that the default methods are inherited.
 * <br/>
 * Usage:
 * <br/>
 * <pre>
 * {@code
 * myComponent.addKeyPressedListener((KeyPressedListener)this::handleKeyPressed);
 * ...
 * void handleKeyPressed(KeyEvent e){...}
 * }
 * </pre>
 * Note: Because duplicate method references are not equal to one another, they cannot be used inline if you need to remove the listener
 * at later time. The method reference will need to be assigned to a variable first, which can then be used to remove the listener.
 * @since 5.3
 */
@FunctionalInterface
public interface KeyPressedListener extends KeyListener
{
    @Override
    void keyPressed(@Nonnull KeyEvent e);

    @Override
    default void keyTyped(@Nonnull KeyEvent e)
    {
    }

    @Override
    default void keyReleased(@Nonnull KeyEvent e)
    {
    }
}