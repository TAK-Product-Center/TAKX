package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Ellipse;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent an {@link Ellipse} object.
 *
 * @since 5.4
 */
public class GlobeEllipseMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobeEllipseMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
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
        IGeoPoint orientationLocation = GeoCalculations.pointAtDistance(centerLocation, orientation, 3d / 4d * (semiMinorAxis * 2d));
        boolean isEditable = ellipse.isEditable();
        boolean isOrientable = ellipse.isOrientable();
        AltitudeMode altitudeMode = ellipse.getAltitudeMode();
        double centerAltitude = centerLocation.getAltitude();
        return List.of(
                new ControlPointDefinition(new GeoPoint(north.getLatitude(), wrapIdl(north.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_NORTH, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(east.getLatitude(), wrapIdl(east.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_EAST, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(south.getLatitude(), wrapIdl(south.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_SOUTH, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(west.getLatitude(), wrapIdl(west.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_WEST, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(centerLocation), IControlPoint.CONTROL_POINT_CENTER, isEditable, altitudeMode),
                new ControlPointDefinition(new GeoPoint(orientationLocation.getLatitude(), wrapIdl(orientationLocation.getLongitude()), centerAltitude), IControlPoint.CONTROL_POINT_ORIENTATION, isEditable && isOrientable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        Ellipse ellipse = getSource().requireShapeModel();
        ellipse.setLocation(location);
        ellipse.setSemiMajorAxis(1);
        ellipse.setSemiMinorAxis(1);
        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
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
                    onSourceUpdated(getSource());
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
                    onSourceUpdated(getSource());
                }
            }
            case IControlPoint.CONTROL_POINT_CENTER ->
            {
                IControlPoint draggedControlPoint = IControlPoint.getControlPointByType(controlPoints, controlPointType);
                double altitude = draggedControlPoint != null ? draggedControlPoint.getLocation().getAltitude() : location.getAltitude();
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude(), altitude);
                ellipse.setLocation(newLocation);
                onSourceUpdated(getSource());
            }
            case IControlPoint.CONTROL_POINT_ORIENTATION ->
            {
                if (centerControlPoint != null)
                {
                    ellipse.setOrientation(GeoCalculations.bearing(centerControlPoint.getLocation(), location));
                    onSourceUpdated(getSource());
                }
            }
        }
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Ellipse ellipse = getSource().requireShapeModel();
        if (ellipse.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
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
