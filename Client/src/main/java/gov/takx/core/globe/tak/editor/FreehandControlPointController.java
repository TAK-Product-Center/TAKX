package gov.takx.core.globe.tak.editor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import jakarta.inject.Inject;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * An implementation of {@link IControlPointController} for drawing freehand multipoint source objects. Will allow drawing a successive series of points by clicking
 * and dragging when armed.
 *
 * @since 5.4
 */
@PseudoBean
public class FreehandControlPointController extends MultipointControlPointController
{
    @Inject
    private ApplicationGlobeManager globeManager;

    private List<GlobeMouseAdapter> globeMouseAdapters;
    private int pixelSpacing = 25;
    private Point lastPoint;

    @Override
    public void setArmed(boolean armed)
    {
        super.setArmed(armed);
        if (armed)
        {
            globeMouseAdapters = globeManager.getAllGlobeDelegates().stream().map(GlobeMouseAdapter::new).toList();
        } else
        {
            if (globeMouseAdapters != null)
            {
                globeMouseAdapters.forEach(GlobeMouseAdapter::dispose);
                globeMouseAdapters = null;
            }
        }
    }

    /**
     * Get the minimum distance in pixels between newly added points.
     * @return the minimum distance in pixels
     */
    public double getFreeHandMinSpacing()
    {
        return pixelSpacing;
    }

    /**
     * Set the minimum distance in pixels between newly added points.
     * @param pixelSpacing the minimum distance in pixels
     */
    public void setFreeHandMinSpacing(int pixelSpacing)
    {
        this.pixelSpacing = pixelSpacing;
    }

    /**
     * A {@link MouseAdapter} to track pointer input for control points.
     */
    private class GlobeMouseAdapter extends MouseAdapter
    {
        private final IApplicationGlobe globe;

        private GlobeMouseAdapter(IApplicationGlobe globe)
        {
            this.globe = globe;
            globe.addMouseListener(this);
            globe.addMouseMotionListener(this);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());
                if (inverse != null)
                {
                    onControlPointAdded(inverse);
                    e.consume();
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            final Point point = e.getPoint();

            if (lastPoint != null)
            {
                final double distance = lastPoint.distance(point);
                if (distance < pixelSpacing) return;
            }

            final ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());

            if (inverse != null)
            {
                lastPoint = point;
                onControlPointAdded(inverse);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                getShapeEditor().setArmed(false);
            }
        }

        private void dispose()
        {
            globe.removeMouseListener(this);
            globe.removeMouseMotionListener(this);
        }
    }
}
