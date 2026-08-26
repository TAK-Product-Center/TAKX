package gov.takx.core.globe;

import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.commons.swing.RaptorTransferable;
import gov.takx.core.view.gui.DragGlassPane;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Point;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;

/**
 */
public class DragImageController extends DragSourceAdapter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final DragGlassPane glassPane;

    public DragImageController(@Nonnull IWorldWindowGL wwd)
    {
        Component c = SwingUtilities.getRootPane(wwd.getComponent()).getGlassPane();
        if (c instanceof DragGlassPane)
        {
            glassPane = (DragGlassPane) c;
        } else
        {
            glassPane = null;
            logger.debug("No DraggableGlassPane on wwd: {}", wwd);
        }
    }

    @Override
    public void dragDropEnd(DragSourceDropEvent event)
    {
        if (glassPane != null) glassPane.setVisible(false);
    }

    @Override
    public void dragMouseMoved(DragSourceDragEvent event)
    {
        if (glassPane != null && event.getDragSourceContext().getTransferable() instanceof RaptorTransferable transferable)
        {
            BufferedImage image = transferable.getBufferedImage();
            if (image == null) return;

            glassPane.setImage(image);

            Point point = event.getLocation().getLocation();

            SwingUtilities.convertPointFromScreen(point, glassPane);

            glassPane.setVisible(true);
            glassPane.setPoint(point);
            glassPane.repaint(glassPane.getRepaintRect());
        }
    }
}
