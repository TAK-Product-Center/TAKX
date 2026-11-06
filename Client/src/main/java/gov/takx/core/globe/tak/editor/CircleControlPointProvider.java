package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Circle;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that represent a {@link Circle} object.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class CircleControlPointProvider extends AControlPointProvider<IGlobeShapeMapObject>
{
    protected CircleControlPointProvider(@Nonnull IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<IShapeEditorSource.ControlPointDefinition> createControlPointDefinitions()
    {
        Circle circle = getSource().requireShapeModel();
        ILocation centerLocation = circle.getLocation();
        if (centerLocation.equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();
        double orientation = circle.getOrientation();
        double radius = circle.getRadius();
        IGeoPoint north = GeoCalculations.pointAtDistance(centerLocation, orientation, radius);
        IGeoPoint east = GeoCalculations.pointAtDistance(centerLocation, orientation + 90, radius);
        IGeoPoint south = GeoCalculations.pointAtDistance(centerLocation, orientation + 180, radius);
        IGeoPoint west = GeoCalculations.pointAtDistance(centerLocation, orientation + 270, radius);
        boolean isEditable = circle.isEditable();
        IShape.AltitudeMode altitudeMode = circle.getAltitudeMode();
        double centerAltitude = centerLocation.getAltitude();
        return List.of(
                createControlPointDefinition(north, centerAltitude, IControlPoint.CONTROL_POINT_NORTH, isEditable, altitudeMode),
                createControlPointDefinition(east, centerAltitude, IControlPoint.CONTROL_POINT_EAST, isEditable, altitudeMode),
                createControlPointDefinition(south, centerAltitude, IControlPoint.CONTROL_POINT_SOUTH, isEditable, altitudeMode),
                createControlPointDefinition(west, centerAltitude, IControlPoint.CONTROL_POINT_WEST, isEditable, altitudeMode),
                createControlPointDefinition(centerLocation, IControlPoint.CONTROL_POINT_CENTER, isEditable, altitudeMode));

    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated)
    {
        Circle circle = getSource().requireShapeModel();
        circle.setLocation(location);
        // Set a small initial radius so the control points are not on top of each other
        circle.setRadius(1);
        onSourceUpdated.run();
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
    {
        Circle circle = getSource().requireShapeModel();
        Collection<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
        IControlPoint centerControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_CENTER);
        switch (controlPointType)
        {
            case IControlPoint.CONTROL_POINT_NORTH, IControlPoint.CONTROL_POINT_SOUTH, IControlPoint.CONTROL_POINT_EAST, IControlPoint.CONTROL_POINT_WEST ->
            {
                if (centerControlPoint != null)
                {
                    circle.setRadius(GeoCalculations.distance(centerControlPoint.getLocation(), location));
                    onSourceUpdated.run();
                }
            }
            case IControlPoint.CONTROL_POINT_CENTER ->
            {
                double altitude = centerControlPoint != null ? centerControlPoint.getLocation().getAltitude() : location.getAltitude();
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
                circle.setLocation(newLocation);
                onSourceUpdated.run();
            }
        }
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPoint.isSelected() ? MeasureHelper.formatGlobeShapeMeasurements(getSource().requireShapeModel(), controlPoint) : null;
    }
}
