package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Arrow;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gov.takx.core.globe.tak.editor.AControlPointProvider.wrapIdl;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent an {@link Arrow} object.
 *
 * @since 5.4
 */
public class GlobeArrowMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobeArrowMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        ArrayList<ControlPointDefinition> controlPointDefinitions = new ArrayList<>();
        Arrow arrow = getSource().requireShapeModel();
        List<ILocation> locations = arrow.getVertices();
        ILocation firstLocation = !locations.isEmpty() ? locations.get(0) : null;
        ILocation secondLocation = locations.size() > 1 ? locations.get(1) : null;
        boolean isEditable = arrow.isEditable();
        AltitudeMode altitudeMode = arrow.getAltitudeMode();
        if (firstLocation != null)
        {
            controlPointDefinitions.add(new ControlPointDefinition(firstLocation, IControlPoint.toVertexControlPointType(0), isEditable, altitudeMode));
            if (secondLocation != null)
            {
                controlPointDefinitions.add(new ControlPointDefinition(secondLocation, IControlPoint.toVertexControlPointType(1), isEditable, altitudeMode));
            }
        }
        return controlPointDefinitions;
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        Arrow arrow = getSource().requireShapeModel();
        arrow.setStartingPoint(location);
        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        Arrow arrow = getSource().requireShapeModel();
        Integer index = IControlPoint.getVertexIndex(controlPointType);
        if (index == null) return;
        List<ILocation> locations = arrow.getVertices();
        ILocation firstPathLocation = !locations.isEmpty() ? locations.get(0) : null;
        ILocation secondPathLocation = locations.size() > 1 ? locations.get(1) : null;
        ILocation firstLocation = index == 0 ? location : firstPathLocation;
        ILocation secondLocation = index == 1 ? location : secondPathLocation;
        if (firstLocation == null || secondLocation == null) return;
        if (arrow.isRangeLocked())
        {
            // Index is 0 for the origin and 1 is the head of the arrow
            final ILocation referencePoint = index == 0 ? secondLocation : firstLocation;

            // Calculate new bearing from the reference point to the control point location
            final double bearing = GeoCalculations.bearing(referencePoint, location);

            // Constrain the new position using the current arrow range and new bearing
            IGeoPoint contrainedLocation = GeoCalculations.pointAtDistance(referencePoint, bearing, arrow.getRange());
            location = new GeoPoint(contrainedLocation.getLatitude(), wrapIdl(contrainedLocation.getLongitude()), referencePoint.getAltitude());
            if (index == 0)
            {
                firstLocation = location;
            } else
            {
                secondLocation = location;
            }
        }
        arrow.setVertices(List.of(firstLocation, secondLocation));
        onSourceUpdated(getSource());
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Arrow arrow = getSource().requireShapeModel();
        if (arrow.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
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
