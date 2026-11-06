package gov.takx.core.globe.tak.editor;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Circle;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Circle} object.
 *
 * @since 5.4
 */
public class GlobeCircleMapObjectEditorSource extends ACompositeEditorSource<IGlobeShapeMapObject>
{
    public GlobeCircleMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, List.of(
                new CircleControlPointProvider(source),
                new OrientationControlPointProvider(source, () -> ((Circle) source.requireShapeModel()).getRadius() * 2),
                new MilStdControlPointProvider<>(source)));
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Circle circle = getSource().requireShapeModel();
        if (circle.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
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
