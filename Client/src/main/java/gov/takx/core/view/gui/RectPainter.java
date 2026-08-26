package gov.takx.core.view.gui;

import bibliothek.extension.gui.dock.theme.eclipse.stack.EclipseTabPane;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.BorderedComponent;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.DefaultInvisibleTab;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.InvisibleTab;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.InvisibleTabPane;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.RectGradientPainter;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.TabComponent;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.TabPainter;
import bibliothek.extension.gui.dock.theme.eclipse.stack.tab.TabPanePainter;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

/**
 * Painter for tab components which uses a rectangle without gradients.
 *
 * @since 2.14
 */
public class RectPainter extends RectGradientPainter
{
    public static final TabPainter FACTORY = new TabPainter()
    {
        public TabComponent createTabComponent(EclipseTabPane pane, Dockable dockable)
        {
            return new RectPainter(pane, dockable);
        }

        public TabPanePainter createDecorationPainter(EclipseTabPane pane)
        {
            return new TabPanePainter()
            {
                @Override
                public void setController(DockController controller)
                {
                    // Do nothing.  We do not want any extra decorations.
                }

                @Override
                public void paintBackground(Graphics g)
                {
                    // Do nothing.  We do not want any extra decorations.
                }

                @Override
                public void paintForeground(Graphics g)
                {
                    // Do nothing.  We do not want any extra decorations.
                }
            };
        }

        public InvisibleTab createInvisibleTab(InvisibleTabPane pane, Dockable dockable)
        {
            return new DefaultInvisibleTab(pane, dockable);
        }

        public Border getFullBorder(BorderedComponent owner, DockController controller, Dockable dockable)
        {
            return new EmptyBorder(0, 0, 0, 0);
        }
    };

    public RectPainter(EclipseTabPane pane, Dockable dockable)
    {
        super(pane, dockable);
    }

    @Override
    public void paintBackground(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        Color lineColor = colorStackBorder.value();
        Color backgroundColor;
        boolean focusTemporarilyLost = isFocusTemporarilyLost();
        boolean isSelected = isSelected();
        if (!isEnabled())
        {
            backgroundColor = colorStackTabTopDisabled.value();
        } else if (isFocused() && !focusTemporarilyLost)
        {
            backgroundColor = colorStackTabTopSelectedFocused.value();
        } else if (isFocused() && focusTemporarilyLost)
        {
            backgroundColor = colorStackTabTopSelectedFocusLost.value();
        } else if (isSelected)
        {
            backgroundColor = colorStackTabTopSelected.value();
        } else
        {
            backgroundColor = colorStackTabTop.value();
        }
        g2d.setColor(lineColor);
        Paint old = g2d.getPaint();
        g2d.setPaint(backgroundColor);
        if (getTransparency() != bibliothek.gui.dock.util.Transparency.TRANSPARENT)
        {
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g2d.setPaint(old);
    }
}
