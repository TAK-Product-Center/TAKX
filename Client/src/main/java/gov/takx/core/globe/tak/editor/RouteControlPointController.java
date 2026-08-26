package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

/**
 * An implementation of {@link IControlPointController} for editing route shapes.
 *
 * @since 5.4
 */
public class RouteControlPointController extends PointAndClickControlPointController
{
    static final String CONTROL_POINT_ROUTE_POINT_INDEX = "CONTROL_POINT_ROUTE_POINT_";
    static final String CONTROL_POINT_WAYPOINT_INDEX = "CONTROL_POINT_WAYPOINT_";
    static final String PROMOTE_TO_WAYPOINT = "PROMOTE_TO_WAYPOINT";

    @Override
    protected void mouseClicked(MouseEvent e, IApplicationGlobe globe)
    {
        if (e.getButton() == MouseEvent.BUTTON1)
        {
            if (e.getClickCount() == 1)
            {
                if (e.isShiftDown())
                {
                    IControlPoint selectedControlPoint = getSelectedControlPoint();
                    if (selectedControlPoint != null)
                    {
                        HashMap<String, Object> metadata = new HashMap<>();
                        metadata.put(PROMOTE_TO_WAYPOINT, selectedControlPoint);
                        onControlPointAdded(selectedControlPoint.getLocation(), metadata);
                    }
                } else if (!e.isControlDown()) // CTRL-click removing of points will be handled by MultipointControlPointController
                {
                    ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());
                    if (inverse != null)
                    {
                        onControlPointAdded(inverse);
                    }
                }
            } else if (e.getClickCount() == 2)
            {
                List<IControlPoint> controlPoints = getControlPoints();
                IControlPoint selectedControlPoint = controlPoints.get(controlPoints.size() - 1);
                HashMap<String, Object> metadata = new HashMap<>();
                metadata.put(PROMOTE_TO_WAYPOINT, selectedControlPoint);
                onControlPointAdded(selectedControlPoint.getLocation(), metadata);
            }
        }
        if (e.getButton() == MouseEvent.BUTTON3)
        {
            if (e.getClickCount() == 1)
            {
                ILocation inverse = inverse(e.getPoint(), globe.getPortableMapRenderer());
                if (inverse != null)
                {
                    onControlPointAdded(inverse);
                    List<IControlPoint> controlPoints = getControlPoints();
                    IControlPoint selectedControlPoint = controlPoints.get(controlPoints.size() - 1);
                    HashMap<String, Object> metadata = new HashMap<>();
                    metadata.put(PROMOTE_TO_WAYPOINT, selectedControlPoint);
                    onControlPointAdded(selectedControlPoint.getLocation(), metadata);
                }
            }
        }
    }

    private IControlPoint getSelectedControlPoint()
    {
        return getControlPoints().stream().filter(IControlPoint::isSelected).findFirst().orElse(null);
    }
}
