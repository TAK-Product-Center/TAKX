package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Circle;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Circle} object.
 *
 * @since 5.4
 */
public class GlobeCircleMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobeCircleMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
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
        IGeoPoint orientationLocation = GeoCalculations.pointAtDistance(centerLocation, orientation, 3d / 4d * (radius * 2));
        boolean isEditable = circle.isEditable();
        boolean isOrientable = circle.isOrientable();
        AltitudeMode altitudeMode = circle.getAltitudeMode();
        double centerAltitude = centerLocation.getAltitude();
        return List.of(
                new ControlPointDefinition(new GeoPoint(north.getLatitude(), wrapIdl(north.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_NORTH, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(east.getLatitude(), wrapIdl(east.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_EAST, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(south.getLatitude(), wrapIdl(south.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_SOUTH, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(west.getLatitude(), wrapIdl(west.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_WEST, isEditable, altitudeMode),
                new ControlPointDefinition(centerLocation, IControlPoint.CONTROL_POINT_CENTER, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(orientationLocation.getLatitude(), wrapIdl(orientationLocation.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_ORIENTATION, isEditable && isOrientable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        Circle circle = getSource().requireShapeModel();
        circle.setLocation(location);
        // Set a small initial radius so the control points are not on top of each other
        circle.setRadius(1);
        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
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
                    onSourceUpdated(getSource());
                }
            }
            case IControlPoint.CONTROL_POINT_CENTER ->
            {
                IControlPoint draggedControlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
                double altitude = draggedControlPoint != null ? draggedControlPoint.getLocation().getAltitude() : location.getAltitude();
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
                circle.setLocation(newLocation);
                onSourceUpdated(getSource());
            }
            case IControlPoint.CONTROL_POINT_ORIENTATION ->
            {
                if (centerControlPoint != null)
                {
                    circle.setOrientation(GeoCalculations.bearing(centerControlPoint.getLocation(), location));
                    onSourceUpdated(getSource());
                }
            }
        }
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Circle circle = getSource().requireShapeModel();
        if (circle.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
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
}
