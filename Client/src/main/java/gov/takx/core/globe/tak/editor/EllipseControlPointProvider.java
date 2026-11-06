package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Ellipse;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that represent an {@link Ellipse} object.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class EllipseControlPointProvider extends AControlPointProvider<IGlobeShapeMapObject>
{
    protected EllipseControlPointProvider(@Nonnull IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<IShapeEditorSource.ControlPointDefinition> createControlPointDefinitions()
    {
        Ellipse ellipse = getSource().requireShapeModel();
        ILocation centerLocation = ellipse.getLocation();
        if (centerLocation.equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();

        double orientation = ellipse.getOrientation();
        double semiMajorAxis = ellipse.getSemiMajorAxis();
        double semiMinorAxis = ellipse.getSemiMinorAxis();
        IGeoPoint north = GeoCalculations.pointAtDistance(centerLocation, orientation, semiMinorAxis);
        IGeoPoint east = GeoCalculations.pointAtDistance(centerLocation, orientation + 90, semiMajorAxis);
        IGeoPoint south = GeoCalculations.pointAtDistance(centerLocation, orientation + 180, semiMinorAxis);
        IGeoPoint west = GeoCalculations.pointAtDistance(centerLocation, orientation + 270, semiMajorAxis);
        boolean isEditable = ellipse.isEditable();
        IShape.AltitudeMode altitudeMode = ellipse.getAltitudeMode();
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
        Ellipse ellipse = getSource().requireShapeModel();
        ellipse.setLocation(location);
        ellipse.setSemiMajorAxis(1);
        ellipse.setSemiMinorAxis(1);
        onSourceUpdated.run();
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
    {
        Ellipse ellipse = getSource().requireShapeModel();
        Collection<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
        IControlPoint centerControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_CENTER);
        switch (controlPointType)
        {
            case IControlPoint.CONTROL_POINT_NORTH, IControlPoint.CONTROL_POINT_SOUTH ->
            {
                if (centerControlPoint != null)
                {
                    ellipse.setSemiMinorAxis(GeoCalculations.distance(centerControlPoint.getLocation(), location));
                    onSourceUpdated.run();
                }
            }
            case IControlPoint.CONTROL_POINT_EAST, IControlPoint.CONTROL_POINT_WEST ->
            {
                if (centerControlPoint != null)
                {
                    ILocation centerLocation = centerControlPoint.getLocation();
                    if (getShapeEditor().isArmed())
                    {
                        ILocation ewLocation = new GeoPoint(centerLocation.getLatitude(), location.getLongitude());
                        ILocation nsLocation = new GeoPoint(location.getLatitude(), centerLocation.getLongitude());
                        ellipse.setAxes(GeoCalculations.distance(centerLocation, ewLocation), GeoCalculations.distance(centerLocation, nsLocation));
                    } else
                    {
                        ellipse.setSemiMajorAxis(GeoCalculations.distance(centerLocation, location));
                    }
                    onSourceUpdated.run();
                }
            }
            case IControlPoint.CONTROL_POINT_CENTER ->
            {
                IControlPoint draggedControlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
                double altitude = draggedControlPoint != null ? draggedControlPoint.getLocation().getAltitude() : location.getAltitude();
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
                ellipse.setLocation(newLocation);
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
