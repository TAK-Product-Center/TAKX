package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.station.screen.ScreenDockWindow;
import bibliothek.gui.dock.station.screen.window.DefaultScreenDockWindowBorder;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

import java.awt.Component;
import java.awt.Graphics;

/**
 * A border for {@link ScreenDockWindow}s that does not paint the border buttons.
 *
 * @since 2.1
 */
public class EmptyScreenDockWindowBorder extends DefaultScreenDockWindowBorder
{
    /**
     * Creates a new border
     *
     * @param window the window for which this border is used
     * @param target the component which is painted by this
     */
    public EmptyScreenDockWindowBorder(@Nonnull ScreenDockWindow window, @Nonnull JComponent target)
    {
        super(window, target);
        setCornerSize(0);
        setMoveSize(0);
        setDrawDividers(false);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
    {
        // Ignore; we don't want to paint the border at all
    }
}
