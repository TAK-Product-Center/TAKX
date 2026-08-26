package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.core.map.layer.IPickedObjectManager;
import gov.tak.core.map.layer.PickedObjectList;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import jakarta.inject.Inject;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Base class for building an {@link IControlPointController} implementation for a rubber band style editor. A rubber band style editor
 * will allow drawing a new shape with an initial click followed by a drag operation to define the size of the shape when armed.
 *
 * @since 5.4
 */
@PseudoBean
public abstract class ARubberBandControlPointController extends AControlPointController
{
    @Inject
    private ApplicationGlobeManager globeManager;

    @Inject
    private IPickedObjectManager pickedObjectManager;

    private List<GlobeMouseAdapter> globeMouseAdapters;

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (enabled)
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

    @Override
    public void setArmed(boolean armed)
    {
    }

    /**
     * Get the initial control point type selected for dragging when drawing a new shape.
     * @return the initial control point type
     */
    abstract String getInitialControlPointType();

    /**
     * Calculate the bearing of the shape using the position of its control points.
     * @return the shape bearing
     */
    private double getShapeBearing()
    {
        List<IControlPoint> controlPoints = getControlPoints();
        IControlPoint centerControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_CENTER);
        IControlPoint orientationControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_ORIENTATION);
        IControlPoint northControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_NORTH);
        IControlPoint southControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_SOUTH);
        IControlPoint northEastControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_NORTHEAST);
        IControlPoint southEastControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_SOUTHEAST);
        if (centerControlPoint != null && orientationControlPoint != null)
        {
            return GeoCalculations.bearing(centerControlPoint.getLocation(), orientationControlPoint.getLocation());
        } else if (northControlPoint != null && southControlPoint != null)
        {
            return GeoCalculations.bearing(southControlPoint.getLocation(), northControlPoint.getLocation());
        } else if (northEastControlPoint != null && southEastControlPoint != null)
        {
            return GeoCalculations.bearing(southEastControlPoint.getLocation(), northEastControlPoint.getLocation());
        } else
        {
            return 0;
        }
    }

    /**
     * Get the control point type after adjusting its new location. This will "swap" opposite control points if they cross each other.
     * @param controlPointType the starting control point type
     * @param location the new location of the control point
     * @return the adjusted control point type
     */
    private String getAdjustedControlPointForPositionSwap(String controlPointType, ILocation location)
    {
        IControlPoint centerControlPoint = IControlPoint.getControlPointByType(getControlPoints(), IControlPoint.CONTROL_POINT_CENTER);
        if (centerControlPoint == null) return controlPointType;
        double bearing = GeoCalculations.bearing(centerControlPoint.getLocation(), location) - getShapeBearing();
        if (IControlPoint.CONTROL_POINT_WEST.equals(controlPointType) || IControlPoint.CONTROL_POINT_EAST.equals(controlPointType))
        {
            return getWEControlPointTypeFromRelativePositionFromCenter(bearing);
        } else if (IControlPoint.CONTROL_POINT_NORTH.equals(controlPointType) || IControlPoint.CONTROL_POINT_SOUTH.equals(controlPointType))
        {
            return getNSControlPointTypeFromRelativePositionFromCenter(bearing);
        } else if (IControlPoint.CONTROL_POINT_NORTHWEST.equals(controlPointType) || IControlPoint.CONTROL_POINT_NORTHEAST.equals(controlPointType)
                || IControlPoint.CONTROL_POINT_SOUTHWEST.equals(controlPointType) || IControlPoint.CONTROL_POINT_SOUTHEAST.equals(controlPointType))
        {
            return getCornerControlPointTypeFromRelativePositionFromCenter(bearing);
        }
        return controlPointType;
    }

    /**
     * Get the appropriate corner control point type based on the bearing from the center of the shape.
     * @param bearing the bearing from the center of the shape
     * @return the appropriate control point type
     */
    private static String getCornerControlPointTypeFromRelativePositionFromCenter(double bearing)
    {
        bearing = normalizeAngle(bearing);
        if (bearing >= 0 && bearing < 90)
            return IControlPoint.CONTROL_POINT_NORTHEAST;
        else if (bearing >= 90 && bearing < 180)
            return IControlPoint.CONTROL_POINT_SOUTHEAST;
        else if (bearing >= 180 && bearing < 270)
            return IControlPoint.CONTROL_POINT_SOUTHWEST;
        else if (bearing >= 270 && bearing < 360)
            return IControlPoint.CONTROL_POINT_NORTHWEST;
        else
            return IControlPoint.CONTROL_POINT_NORTHEAST;
    }

    /**
     * Get the appropriate East/West control point type based on the bearing from the center of the shape.
     * @param bearing the bearing from the center of the shape
     * @return the appropriate control point type
     */
    private static String getWEControlPointTypeFromRelativePositionFromCenter(double bearing)
    {
        bearing = normalizeAngle(bearing);
        if (bearing >= 0 && bearing < 180)
            return IControlPoint.CONTROL_POINT_EAST;
        else if (bearing >= 180 && bearing < 360)
            return IControlPoint.CONTROL_POINT_WEST;
        else
            return IControlPoint.CONTROL_POINT_EAST;
    }

    /**
     * Get the appropriate North/South control point type based on the bearing from the center of the shape.
     * @param bearing the bearing from the center of the shape
     * @return the appropriate control point type
     */
    private static String getNSControlPointTypeFromRelativePositionFromCenter(double bearing)
    {
        bearing = normalizeAngle(bearing);
        if (bearing >= 0 && bearing < 90 || bearing >= 270 && bearing < 360)
            return IControlPoint.CONTROL_POINT_NORTH;
        else if (bearing >= 90 && bearing < 270)
            return IControlPoint.CONTROL_POINT_SOUTH;
        else
            return IControlPoint.CONTROL_POINT_NORTH;
    }

    /**
     * Normalize an angle to be 0 >= angle > 360.
     * @param angle the angle
     * @return the normalized angle
     */
    private static double normalizeAngle(double angle)
    {
        if (angle >= 360d)
        {
            return angle - 360d;
        } else if (angle < 0d)
        {
            return angle + 360d;
        } else
        {
            return angle;
        }
    }

    /**
     * A {@link MouseAdapter} to track pointer input for control points.
     */
    private class GlobeMouseAdapter extends MouseAdapter
    {
        private final IApplicationGlobe globe;
        private IControlPoint hoveredControlPoint;
        private String dragControlPointType;

        private GlobeMouseAdapter(IApplicationGlobe globe)
        {
            this.globe = globe;
            globe.addMouseListener(this);
            globe.addMouseMotionListener(this);
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            PickedObjectList pickedObjectList = pickedObjectManager.getPickedObjectList();
            IControlPoint controlPoint = !pickedObjectList.isEmpty() && pickedObjectList.get(0).userObject() instanceof IControlPoint cp ? cp : null;
            if (controlPoint != hoveredControlPoint && hoveredControlPoint != null)
            {
                hoveredControlPoint.setSelected(false);
                hoveredControlPoint = null;
            }
            if (controlPoint != null)
            {
                controlPoint.setSelected(true);
                hoveredControlPoint = controlPoint;
            }
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                if (getShapeEditor().isArmed())
                {
                    ILocation location = inverse(e.getPoint(), globe.getPortableMapRenderer());
                    if (location != null)
                    {
                        onControlPointAdded(location);
                        dragControlPointType = getInitialControlPointType();
                        IControlPoint dragControlPoint = IControlPoint.getControlPointByType(getControlPoints(), dragControlPointType);
                        if (dragControlPoint != null) dragControlPoint.setDragging(true);
                        e.consume();
                    }
                } else
                {
                    if (hoveredControlPoint != null)
                    {
                        dragControlPointType = hoveredControlPoint.getControlPointType();
                        hoveredControlPoint.setDragging(true);
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            if (dragControlPointType != null)
            {
                IControlPoint controlPointByType = IControlPoint.getControlPointByType(getControlPoints(), dragControlPointType);
                ILocation location;
                if (controlPointByType != null)
                {
                    location = inverse(e.getPoint(), globe.getPortableMapRenderer(), controlPointByType);
                } else
                {
                    location = inverse(e.getPoint(), globe.getPortableMapRenderer());
                }
                if (location != null && (controlPointByType == null || controlPointByType.isDraggable()))
                {
                    onControlPointDragged(dragControlPointType, location);
                    dragControlPointType = getAdjustedControlPointForPositionSwap(dragControlPointType, location);
                    if (controlPointByType != null) controlPointByType.setSelected(true);
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (e.getButton() == MouseEvent.BUTTON1)
            {
                if (dragControlPointType != null)
                {
                    IControlPoint dragControlPoint = IControlPoint.getControlPointByType(getControlPoints(), dragControlPointType);
                    if (dragControlPoint != null) dragControlPoint.setDragging(false);
                }
                dragControlPointType = null;
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
