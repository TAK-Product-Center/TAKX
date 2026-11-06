package gov.takx.core.globe.tak.editor;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Rectangle;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Rectangle} object.
 *
 * @since 5.4
 */
public class GlobeRectangleMapObjectEditorSource extends ACompositeEditorSource<IGlobeShapeMapObject>
{
    public GlobeRectangleMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, List.of(
                new RectangleControlPointProvider(source),
                new OrientationControlPointProvider(source, () -> ((Rectangle) source.requireShapeModel()).getHeight()),
                new MilStdControlPointProvider<>(source)));
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Rectangle rectangle = getSource().requireShapeModel();
        if (rectangle.getLocation().equals(GeoPoint.UNKNOWN_POINT)) throw new IllegalArgumentException("Center point not set");
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
