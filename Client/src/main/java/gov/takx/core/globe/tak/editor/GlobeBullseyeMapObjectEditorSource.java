package gov.takx.core.globe.tak.editor;

import com.atakmap.coremap.conversions.Span;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Bullseye;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent an {@link Bullseye} object.
 *
 * @since 5.5
 */
public class GlobeBullseyeMapObjectEditorSource extends AShapeEditorSource<IGlobeShapeMapObject>
{
    /**
     * @param source the source object being edited
     */
    public GlobeBullseyeMapObjectEditorSource(@Nonnull IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        if (!getShapeEditor().isArmed()) return Collections.emptyList();
        Bullseye bullseye = getSource().requireShapeModel();
        if (bullseye.getLocation().equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();
        return List.of(new ControlPointDefinition(bullseye.getLocation(), IControlPoint.CONTROL_POINT_CENTER, false, AltitudeMode.ClampToGround));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        if (!getShapeEditor().isArmed()) return;
        Bullseye bullseye = getSource().requireShapeModel();
        if (bullseye.getLocation().equals(GeoPoint.UNKNOWN_POINT))
        {
            bullseye.setLocation(location);
            onSourceUpdated(getSource());
        } else
        {
            double distance = GeoCalculations.distance(bullseye.getLocation(), location);
            bullseye.setRadius(distance, Span.METER);
            onSourceUpdated(getSource());
            getShapeEditor().setArmed(false);
        }
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        // no-op, editing happens via the panel
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        // no-op, editing happens via the panel
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().addPropertyChangeListener(listener, IBullseye.PROPERTIES_TO_WATCH);
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        getSource().addPropertyChangeListener(listener, IBullseye.PROPERTIES_TO_WATCH);
    }
}
