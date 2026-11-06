package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IMutableRegularShape;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that provide orientation control points.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class OrientationControlPointProvider extends AControlPointProvider<IGlobeShapeMapObject>
{
    private final Supplier<Double> getShapeHeight;

    /**
     * @param source the source object being edited
     * @param getShapeHeight a {@link Supplier} that provides the shape height of the source object
     */
    protected OrientationControlPointProvider(@Nonnull IGlobeShapeMapObject source, @Nonnull Supplier<Double> getShapeHeight)
    {
        super(source);
        this.getShapeHeight = getShapeHeight;
    }

    @Override
    @Nonnull
    public List<IShapeEditorSource.ControlPointDefinition> createControlPointDefinitions()
    {
        IMutableRegularShape shape = getSource().requireShapeModel();
        ILocation centerLocation = shape.getLocation();
        if (centerLocation.equals(GeoPoint.UNKNOWN_POINT)) return Collections.emptyList();

        IGeoPoint orientation = GeoCalculations.pointAtDistance(centerLocation, shape.getOrientation(), 3d / 4d * getShapeHeight.get());
        boolean isEditable = shape.isEditable();
        boolean isOrientable = shape.isOrientable();
        IShape.AltitudeMode altitudeMode = shape.getAltitudeMode();
        return List.of(createControlPointDefinition(orientation, centerLocation.getAltitude(), IControlPoint.CONTROL_POINT_ORIENTATION, isEditable && isOrientable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
    {
        IMutableRegularShape shape = getSource().requireShapeModel();
        Collection<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
        if (IControlPoint.CONTROL_POINT_ORIENTATION.equals(controlPointType))
        {
            IControlPoint centerControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_CENTER);
            if (centerControlPoint != null)
            {
                shape.setOrientation(GeoCalculations.bearing(centerControlPoint.getLocation(), location));
                onSourceUpdated.run();
            }
        }
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return null;
    }
}
