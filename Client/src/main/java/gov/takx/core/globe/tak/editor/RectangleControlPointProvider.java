package gov.takx.core.globe.tak.editor;

import com.atakmap.map.elevation.ElevationManager;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that represent a {@link Rectangle} or {@link Square} object.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class RectangleControlPointProvider extends AControlPointProvider<IGlobeShapeMapObject>
{
    /**
     * @param source   the source object being edited
     */
    public RectangleControlPointProvider(@Nonnull IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<IShapeEditorSource.ControlPointDefinition> createControlPointDefinitions()
    {
        Rectangle rectangle = getSource().requireShapeModel();
        ILocation centerLocation = rectangle.getLocation();
        if (centerLocation.equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();

        var vertices = rectangle.getVertices();
        if (vertices == null) return Collections.emptyList();

        boolean isEditable = rectangle.isEditable();
        IShape.AltitudeMode altitudeMode = rectangle.getAltitudeMode();
        return List.of(
                createControlPointDefinition(vertices.get(0), IControlPoint.CONTROL_POINT_SOUTHWEST, isEditable, altitudeMode),
                createControlPointDefinition(vertices.get(1), IControlPoint.CONTROL_POINT_SOUTHEAST, isEditable, altitudeMode),
                createControlPointDefinition(vertices.get(2), IControlPoint.CONTROL_POINT_NORTHEAST, isEditable, altitudeMode),
                createControlPointDefinition(vertices.get(3), IControlPoint.CONTROL_POINT_NORTHWEST, isEditable, altitudeMode),
                createControlPointDefinition(centerLocation, IControlPoint.CONTROL_POINT_CENTER, isEditable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated)
    {
        Rectangle rectangle = getSource().requireShapeModel();
        rectangle.setLocation(location);
        rectangle.setSize(1, 1);
        onSourceUpdated.run();
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
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

            if (rectangle instanceof Square square)
            {
                // Compute shape's the width and height in meters from the diagonal between the shape's new center
                // position and its new corner position.
                double widthMeters = distance * Math.abs(Math.sin(Math.toRadians(bearing)));
                double heightMeters = distance * Math.abs(Math.cos(Math.toRadians(bearing)));

                double sideLength = Math.max(widthMeters, heightMeters);
                square.setSideLength(sideLength);
            } else
            {
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
            }
        } else if (IControlPoint.CONTROL_POINT_CENTER.equals(controlPointType))
        {
            IControlPoint draggedControlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
            double altitude = draggedControlPoint != null ? draggedControlPoint.getLocation().getAltitude() : location.getAltitude();
            GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
            rectangle.setLocation(newLocation);
            onSourceUpdated.run();
        }
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPoint.isSelected() ? MeasureHelper.formatGlobeShapeMeasurements(getSource().requireShapeModel(), controlPoint) : null;
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
