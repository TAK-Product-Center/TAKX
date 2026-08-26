package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;

import java.awt.event.MouseEvent;

/**
 * Implementation of {@link PointAndClickControlPointController} for single point shapes.
 *
 * @since 5.5
 */
public class SinglePointControlPointController extends PointAndClickControlPointController
{
    @Override
    protected void mouseClicked(MouseEvent e, IApplicationGlobe globe)
    {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1)
        {
            ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());
            if (inverse != null) onControlPointAdded(inverse);
            getShapeEditor().setArmed(false);
        }
    }
}
