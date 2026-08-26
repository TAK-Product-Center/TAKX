package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Per the swing documentation <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/menu.html#popup">here</a>
 * this is a FunctionalInterface that should capture the popup trigger on any platform. Designed to be used on a method reference
 * by casting to {@link PopupTriggerListener} so that the default methods are inherited.
 * <br/>
 * Usage:
 * <br/>
 * <pre>
 * {@code
 * myComponent.addMouseListener((PopupTriggerListener)this::handlePopup);
 * ...
 * void handlePopup(MouseEvent e){...}
 * }
 * </pre>
 * @since 5.3
 */
@FunctionalInterface
public interface PopupTriggerListener extends MouseListener
{
    @Override
    default void mousePressed(@Nonnull MouseEvent e)
    {

        if (e.isPopupTrigger()) popupTriggered(e);
    }

    @Override
    default void mouseReleased(@Nonnull MouseEvent e)
    {
        if (e.isPopupTrigger()) popupTriggered(e);
    }

    /**
     * Callback when the popup trigger is fired. This may be a different combination of mouse buttons and keys on different
     * platforms.
     *
     * @param event the mouse event that constitutes a popup trigger for the current platform
     */
    void popupTriggered(@Nonnull MouseEvent event);

    @Override
    default void mouseClicked(@Nonnull MouseEvent e)
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
