package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * A {@link ComponentListener} that defaults {@link #componentResized(ComponentEvent)}
 * and {@link #componentMoved(ComponentEvent)} to no-op so that implementations only have to handle {@link #componentShown(ComponentEvent)}
 * and {@link #componentHidden(ComponentEvent)}.
 * <p>
 * Note: Because duplicate method references are not equal to one another, they cannot be used when it is necessary to
 * remove the listener unless assigned to a variable first, which can then be used to remove the listener.
 *
 * @since 5.6
 */
public interface ComponentVisibilityListener extends ComponentListener
{
    @Override
    void componentShown(@Nonnull ComponentEvent e);

    @Override
    void componentHidden(@Nonnull ComponentEvent e);

    @Override
    default void componentResized(@Nonnull ComponentEvent ignored)
    {
    }

    @Override
    default void componentMoved(@Nonnull ComponentEvent ignored)
    {
    }
}
