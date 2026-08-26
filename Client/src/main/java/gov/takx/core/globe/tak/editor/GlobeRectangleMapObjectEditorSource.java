package gov.takx.core.globe.tak.editor;

import com.atakmap.map.elevation.ElevationManager;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Rectangle} object.
 *
 * @since 5.4
 */
public class GlobeRectangleMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobeRectangleMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        Rectangle rectangle = getSource().requireShapeModel();
        ILocation centerLocation = rectangle.getLocation();
        if (centerLocation.equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();

        var vertices = rectangle.getVertices();
        if (vertices == null) return Collections.emptyList();

        IGeoPoint orientation = GeoCalculations.pointAtDistance(centerLocation, rectangle.getOrientation(), 3d / 4d * rectangle.getHeight());
        boolean isEditable = rectangle.isEditable();
        boolean isOrientable = rectangle.isOrientable();
        AltitudeMode altitudeMode = rectangle.getAltitudeMode();
        return List.of(
                new ControlPointDefinition(vertices.get(0), IControlPoint.CONTROL_POINT_SOUTHWEST, isEditable, altitudeMode),
                new ControlPointDefinition(vertices.get(1), IControlPoint.CONTROL_POINT_SOUTHEAST, isEditable, altitudeMode),
                new ControlPointDefinition(vertices.get(2), IControlPoint.CONTROL_POINT_NORTHEAST, isEditable, altitudeMode),
                new ControlPointDefinition(vertices.get(3), IControlPoint.CONTROL_POINT_NORTHWEST, isEditable, altitudeMode),
                new ControlPointDefinition(centerLocation, IControlPoint.CONTROL_POINT_CENTER, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(orientation.getLatitude(), wrapIdl(orientation.getLongitude()), centerLocation.getAltitude()), IControlPoint.CONTROL_POINT_ORIENTATION, isEditable && isOrientable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        Rectangle rectangle = getSource().requireShapeModel();
        rectangle.setLocation(location);
        rectangle.setSize(1, 1);
        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        Rectangle rectangle = getSource().requireShapeModel();
        Collection<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
        if (IControlPoint.CONTROL_POINT_NORTHEAST.equals(controlPointType) || IControlPoint.CONTROL_POINT_NORTHWEST.equals(controlPointType)
                || IControlPoint.CONTROL_POINT_SOUTHEAST.equals(controlPointType) || IControlPoint.CONTROL_POINT_SOUTHWEST.equals(controlPointType))
        {
            ILocation oppositeLocation = getOppositeLocation(controlPoints, controlPointType);
            if (oppositeLocation == null) return;
            double bearing = ShapeUtils.rhumbAzimuth(oppositeLocation, location) - rectangle.getOrientation();
            double distance = GeoCalculations.distance(oppositeLocation, location);

            // Compute shape's the width and height in meters from the diagonal between the shape's new center
            // position and its new corner position.
            double widthMeters = distance * Math.abs(Math.sin(Math.toRadians(bearing)));

            ILocation southWest;
            ILocation southEast;
            ILocation northEast;
            ILocation northWest;
            switch (controlPointType)
            {
                case IControlPoint.CONTROL_POINT_SOUTHWEST:
                    southWest = location;
                    southEast = ShapeUtils.rhumbEndPositionMeters(location, 90 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(southEast, controlPoints, IControlPoint.CONTROL_POINT_SOUTHEAST);
                    northEast = oppositeLocation;
                    northWest = ShapeUtils.rhumbEndPositionMeters(oppositeLocation, 270 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(northWest, controlPoints, IControlPoint.CONTROL_POINT_NORTHWEST);
                    break;
                case IControlPoint.CONTROL_POINT_SOUTHEAST:
                    southEast = location;
                    southWest = ShapeUtils.rhumbEndPositionMeters(location, 270 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(southWest, controlPoints, IControlPoint.CONTROL_POINT_SOUTHWEST);
                    northWest = oppositeLocation;
                    northEast = ShapeUtils.rhumbEndPositionMeters(oppositeLocation, 90 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(northEast, controlPoints, IControlPoint.CONTROL_POINT_NORTHEAST);
                    break;
                case IControlPoint.CONTROL_POINT_NORTHEAST:
                    northEast = location;
                    northWest = ShapeUtils.rhumbEndPositionMeters(location, 270 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(northWest, controlPoints, IControlPoint.CONTROL_POINT_NORTHWEST);
                    southWest = oppositeLocation;
                    southEast = ShapeUtils.rhumbEndPositionMeters(oppositeLocation, 90 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(southEast, controlPoints, IControlPoint.CONTROL_POINT_SOUTHEAST);
                    break;
                case IControlPoint.CONTROL_POINT_NORTHWEST:
                default:
                    northWest = location;
                    northEast = ShapeUtils.rhumbEndPositionMeters(location, 90 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(northEast, controlPoints, IControlPoint.CONTROL_POINT_NORTHEAST);
                    southEast = oppositeLocation;
                    southWest = ShapeUtils.rhumbEndPositionMeters(oppositeLocation, 270 + rectangle.getOrientation(), widthMeters);
                    adjustAltitude(southWest, controlPoints, IControlPoint.CONTROL_POINT_SOUTHWEST);
                    break;
            }
            rectangle.setVertices(List.of(southWest, southEast, northEast, northWest));
        } else if (IControlPoint.CONTROL_POINT_CENTER.equals(controlPointType))
        {
            IControlPoint draggedControlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
            double altitude = draggedControlPoint != null ? draggedControlPoint.getLocation().getAltitude() : location.getAltitude();
            GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
            rectangle.setLocation(newLocation);
            onSourceUpdated(getSource());
        } else if (IControlPoint.CONTROL_POINT_ORIENTATION.equals(controlPointType))
        {
            IControlPoint centerControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_CENTER);
            if (centerControlPoint != null)
            {
                rectangle.setOrientation(GeoCalculations.bearing(centerControlPoint.getLocation(), location));
                onSourceUpdated(getSource());
            }
        }
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Rectangle rectangle = getSource().requireShapeModel();
        if (rectangle.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPoint.isSelected() ? MeasureHelper.formatGlobeShapeMeasurements(getSource().requireShapeModel(), controlPoint) : null;
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().addPropertyChangeListener(listener, SHAPE_PROPERTIES);
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().removePropertyChangeListener(listener, SHAPE_PROPERTIES);
    }

    private void adjustAltitude(ILocation location, Collection<IControlPoint> controlPoints, String controlPointType)
    {
        IControlPoint controlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
        double altitude = controlPoint != null ? controlPoint.getLocation().getAltitude() : Double.NaN;
        if (Double.isNaN(altitude)) altitude = ElevationManager.getElevation(location.getLatitude(), location.getLongitude(), null);
        location.setAltitude(altitude);
    }

    /**
     * Get the location opposite the provided control point if available.
     * @param controlPoints the control points to search
     * @param controlPointType the control point to find the opposite location of
     * @return the location opposite the provided control point, or {@code null} if not present
     * @since 5.7
     */
    @Nullable
    protected static ILocation getOppositeLocation(@Nonnull Collection<IControlPoint> controlPoints, @Nonnull String controlPointType)
    {
        String oppositeControlPointType = IControlPoint.getOppositeControlPointType(controlPointType);
        if (oppositeControlPointType == null) return null;
        IControlPoint oppositeControlPoint = IControlPoint.getControlPointByType(controlPoints, oppositeControlPointType);
        if (oppositeControlPoint == null) return null;
        return oppositeControlPoint.getLocation();
    }
}
