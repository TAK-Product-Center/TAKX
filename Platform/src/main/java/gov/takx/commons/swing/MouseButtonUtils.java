package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.event.MouseEvent;

/**
 * Static helper methods related to {@link java.awt.event.MouseEvent} buttons.
 * @since 5.4
 */
public class MouseButtonUtils
{
    /**
     * The number of buttons on the mouse, or {@code -1} if headless or no mouse is present.
     * @see MouseInfo#getNumberOfButtons()
     */
    public static final int BUTTON_COUNT = GraphicsEnvironment.isHeadless() ? -1 : MouseInfo.getNumberOfButtons();

    /**
     * On mice with > 3 buttons, button 4 is typically the "back" button.
     *
     * @param event Mouse event
     * @return {@code true} if the button clicked is the "back" button
     */
    public static boolean isBackButton(MouseEvent event)
    {
        return event.getButton() == 4;
    }

    /**
     * On mice with > 3 buttons, button 5 is typically the "forward" button.
     *
     * @param event Mouse event
     * @return {@code true} if the button clicked is the "forward" button
     */
    public static boolean isForwardButton(MouseEvent event)
    {
        return event.getButton() == 5;
    }

    /**
     * Create a mouse click listener that reacts only to the specified button.
     *
     * @param button Button to accept
     * @param action Action to invoke if the given {@code button} is clicked
     * @return new listener
     */
    public static MouseClickedListener specificButtonListener(int button, @Nonnull Runnable action)
    {
        return e -> {
            if (button == e.getButton()) action.run();
        };
    }

    private MouseButtonUtils()
    {
    }
}
