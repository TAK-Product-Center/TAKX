package gov.takx.core.globe.tak.editor;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import jakarta.inject.Inject;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * An implementation of {@link IControlPointController} for multipoint source objects using a point and click input style.
 *
 * @since 5.4
 */
@PseudoBean
public class PointAndClickControlPointController extends MultipointControlPointController
{
    @Inject
    private ApplicationGlobeManager globeManager;

    private List<GlobeMouseAdapter> globeMouseAdapters;

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
     * Handle a {@link MouseEvent}.
     * @param e the mouse event
     * @param globe the globe the event occurred on
     */
    protected void mouseClicked(MouseEvent e, IApplicationGlobe globe)
    {
        if (e.getButton() == MouseEvent.BUTTON1)
        {
            if (!e.isControlDown()) // CTRL-click removing of points will be handled by MultipointControlPointController
            {
                if (e.getClickCount() == 1)
                {
                    ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());
                    if (inverse != null)
                    {
                        onControlPointAdded(inverse);
                    }
                } else if (e.getClickCount() == 2)
                {
                    e.consume(); // Consume this so the event doesn't trigger the enter location dialog
                    getShapeEditor().setArmed(false);
                }
            }
        }
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
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            PointAndClickControlPointController.this.mouseClicked(e, globe);
        }

        private void dispose()
        {
            globe.removeMouseListener(this);
        }
    }
}
