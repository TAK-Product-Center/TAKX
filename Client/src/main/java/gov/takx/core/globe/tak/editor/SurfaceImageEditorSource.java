package gov.takx.core.globe.tak.editor;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link SurfaceImage} objects.
 *
 * @since 5.4
 */
public class SurfaceImageEditorSource extends AShapeEditorSource<SurfaceImage>
{
    public SurfaceImageEditorSource(SurfaceImage source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        // We want a list of Position, not LatLon, so that there is at least some altitude for the feature renderer
        List<Position> corners = getSource().getCorners().stream().map(ll -> new Position(ll, 0)).toList();
        return List.of(
                new ControlPointDefinition(WWUtils.positionToRaptorLocation(corners.get(0)), IControlPoint.CONTROL_POINT_SOUTHWEST, true, AltitudeMode.ClampToGround),
                new ControlPointDefinition(WWUtils.positionToRaptorLocation(corners.get(1)), IControlPoint.CONTROL_POINT_SOUTHEAST, true, AltitudeMode.ClampToGround),
                new ControlPointDefinition(WWUtils.positionToRaptorLocation(corners.get(2)), IControlPoint.CONTROL_POINT_NORTHEAST, true, AltitudeMode.ClampToGround),
                new ControlPointDefinition(WWUtils.positionToRaptorLocation(corners.get(3)), IControlPoint.CONTROL_POINT_NORTHWEST, true, AltitudeMode.ClampToGround),
                new ControlPointDefinition(WWUtils.positionToRaptorLocation(getSource().getReferencePosition()), IControlPoint.CONTROL_POINT_CENTER, true, AltitudeMode.ClampToGround));
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        List<LatLon> corners = getSource().getCorners();
        Position position = Position.fromDegrees(location.getLatitude(), location.getLongitude());
        switch (controlPointType)
        {
            case IControlPoint.CONTROL_POINT_SOUTHWEST ->
            {
                corners.set(0, position);
                getSource().setCorners(corners);
            }
            case IControlPoint.CONTROL_POINT_SOUTHEAST ->
            {
                corners.set(1, position);
                getSource().setCorners(corners);
            }
            case IControlPoint.CONTROL_POINT_NORTHEAST ->
            {
                corners.set(2, position);
                getSource().setCorners(corners);
            }
            case IControlPoint.CONTROL_POINT_NORTHWEST ->
            {
                corners.set(3, position);
                getSource().setCorners(corners);
            }
            case IControlPoint.CONTROL_POINT_CENTER -> getSource().moveTo(position);
        }
        onSourceUpdated(getSource());
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        //
    }

    @Override
    protected void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        //
    }

    @Override
    protected void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        //
    }
}
