package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Listener for when the ESC key is pressed.
 * <p/>
 * Consider using {@link KeyActionHandler} as a more robust alternative to a simple key listener.
 *
 * @since 3.5
 */
public class EscapeKeyListener extends KeyAdapter
{
    private final Runnable callback;

    /**
     * @param callback The callback to be notified when ESC is pressed
     */
    public EscapeKeyListener(@Nonnull Runnable callback)
    {
        this.callback = callback;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) callback.run();
    }
}
