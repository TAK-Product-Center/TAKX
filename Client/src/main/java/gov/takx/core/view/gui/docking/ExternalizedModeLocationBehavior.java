package gov.takx.core.view.gui.docking;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.CommonDockable;
import bibliothek.gui.dock.facile.mode.ExternalizedModeArea;
import bibliothek.gui.dock.facile.mode.ExternalizedModeBehavior;
import bibliothek.gui.dock.station.screen.ScreenDockProperty;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.ComponentDescriptor;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;

/**
 * Control the initial location of externalized dockables
 *
 * @since 2.15
 */
@PseudoBean
public class ExternalizedModeLocationBehavior implements ExternalizedModeBehavior
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Dimension MIN_SIZE = new Dimension(300, 200);

    @Inject
    private DockingManager dockingManager;

    @Override
    public ScreenDockProperty findLocation(ExternalizedModeArea target, Dockable dockable)
    {
        Component component = dockable.getComponent();

        // Ensure component will be redrawn
        component.invalidate();
        Component parent = component;
        while (parent.getParent() != null)
        {
            parent = parent.getParent();
        }
        parent.validate();

        // If the descriptor has an externalizedRect set, use it
        if (dockable instanceof CommonDockable commonDockable)
        {
            CDockable cDockable = commonDockable.getDockable();
            ComponentDescriptor descriptor = getComponentDescriptor(cDockable);
            if (descriptor != null)
            {
                Rectangle rect = descriptor.getExternalizedRect();
                if (rect != null && !rect.isEmpty())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Setting initial externalized window location: ({}, {}) - {} x {}", rect.x, rect.y, rect.width, rect.height);
                    }
                    return new ScreenDockProperty(rect.x, rect.y, rect.width, rect.height, null, false);
                }
            }
        }

        // Otherwise, center on the screen and use preferredSize
        Dimension currentSize = component.getSize();
        Dimension preferredSize = component.getPreferredSize();

        // From the frame and then the bounds of the device the frame is on
        Frame frame = JFrame.getFrames()[0];
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();

        // Parent size is going to be the size of the screen that the application window is in
        Dimension parentSize = new Dimension(bounds.width, bounds.height);

        // Initial location will basically be the upper left coordinate of the screen
        Point parentLocation = new Point((int) bounds.getX(), (int) bounds.getY());

        // Calculate x and y based on the upper left coordinates of the screen, screen size and size of the component
        int x = parentLocation.x + (parentSize.width - currentSize.width) / 2;
        int y = parentLocation.y + (parentSize.height - currentSize.height) / 2;

        Point corner = new Point(x, y);
        Dimension size = new Dimension();

        size.width = Math.max(Math.max(currentSize.width, preferredSize.width), MIN_SIZE.width);
        size.height = Math.max(Math.max(currentSize.height, preferredSize.height), MIN_SIZE.height);

        if (logger.isDebugEnabled())
        {
            logger.debug("Current size = {}, {}", currentSize.width, currentSize.height);
            logger.debug("Parent location = {}, {}", parentLocation.x, parentLocation.y);
            logger.debug("Parent size = {}, {}", parentSize.width, parentSize.height);
            logger.debug("Preferred Size = {}, {}", preferredSize.width, preferredSize.height);
            logger.debug("Set Location {}, {}", x, y);
            logger.debug("Set size {}, {}", size.width, size.height);
        }

        return new ScreenDockProperty(corner.x, corner.y, size.width, size.height, null, false);
    }

    /**
     * Get the ComponentDescriptor for a given Dockable.
     *
     * @param dockable The dockable to find
     * @return The descriptor, or {@code null} if the dockable is unknown
     * @since 3.4 (moved from UISpaceManager)
     */
    @Nullable
    private ComponentDescriptor getComponentDescriptor(CDockable dockable)
    {
        if (dockable instanceof ComponentDockable componentDockable)
        {
            final DockableAndProps dockableAndProps = dockingManager.findDockableAndProps(componentDockable);
            return dockableAndProps != null ? dockableAndProps.getDescriptor() : null;
        }

        return null;
    }
}
