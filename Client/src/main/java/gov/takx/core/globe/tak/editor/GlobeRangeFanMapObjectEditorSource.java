package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.dataservices.shapes.RangeFan;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static gov.takx.core.globe.tak.editor.AControlPointProvider.wrapIdl;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a range fan via an underlying {@link RangeFan} object.
 *
 * @since 5.4
 */
public class GlobeRangeFanMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject>
{
    /**
     * A collection of common properties that need to be monitored for range fan objects.
     */
    private static final List<String> RANGE_FAN_PROPERTIES = List.of(
            ShapeProperties.EDITABLE, ShapeProperties.INTERVAL, ShapeProperties.LOCATION, ShapeProperties.LOCATIONS,
            ShapeProperties.MAX_DISTANCE, ShapeProperties.MIN_DISTANCE, ShapeProperties.ORIENTATION,
            ShapeProperties.RANGE_COLORS, ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS,
            ShapeProperties.RANGE_TYPE, ShapeProperties.SAMPLE_DISTANCE, ShapeProperties.SHOW_EDGES,
            ShapeProperties.SHOW_ORIENTATION, ShapeProperties.SHOW_RANGE_VALUES, ShapeProperties.SWEEP,
            ShapeProperties.VISIBILITY);

    public GlobeRangeFanMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        getSource().setEditing(enabled);
        super.setEnabled(enabled);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        RangeFan rangeFan = getSource().requireShapeModel();
        if (rangeFan.getLocation().equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();
        IGeoPoint rangeGeoPoint = GeoCalculations.pointAtDistance(rangeFan.getLocation(), 90, rangeFan.getMaxDistance());
        ILocation rangeLocation = new GeoPoint(rangeGeoPoint.getLatitude(), wrapIdl(rangeGeoPoint.getLongitude()), rangeFan.getLocation().getAltitude());
        List<ControlPointDefinition> cps = new ArrayList<>();
        cps.add(new ControlPointDefinition(rangeFan.getLocation(), IControlPoint.CONTROL_POINT_CENTER, rangeFan.isEditable(), rangeFan.getAltitudeMode()));
        if (rangeFan.getRangeDistances() == null) cps.add(new ControlPointDefinition(rangeLocation, IControlPoint.CONTROL_POINT_EAST, rangeFan.isEditable(), rangeFan.getAltitudeMode()));
        return cps;
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        RangeFan rangeFan = getSource().requireShapeModel();
        rangeFan.setLocation(location);
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        RangeFan rangeFan = getSource().requireShapeModel();
        if (IControlPoint.CONTROL_POINT_CENTER.equals(controlPointType))
        {
            rangeFan.setLocation(location);
        } else
        {
            final double minDistance = rangeFan.getMinDistance();
            final double radius = rangeFan.getMaxDistance();
            final double interval = rangeFan.getInterval();
            final int numRings = (int) ((radius - minDistance) / interval) + 1;

            // Update the min/max/interval of the range fan to reflect the new radius
            double newRadius = Math.max(GeoCalculations.distance(rangeFan.getLocation(), location), 1);
            double newInterval = newRadius / numRings;
            // Round the interval and radius if we can do so without changing the number of rings
            if ((int) (Math.ceil(newRadius) / Math.floor(newInterval)) == numRings)
            {
                newInterval = Math.floor(newInterval);
                newRadius = Math.ceil(newRadius);
            }

            rangeFan.setMinDistance(newInterval);
            rangeFan.setMaxDistance(newRadius);
            rangeFan.setInterval(newInterval);
        }
        onSourceUpdated(getSource());
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        RangeFan rangeFan = getSource().requireShapeModel();
        if (rangeFan.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().addPropertyChangeListener(listener, RANGE_FAN_PROPERTIES);
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().removePropertyChangeListener(listener, RANGE_FAN_PROPERTIES);
    }
}
