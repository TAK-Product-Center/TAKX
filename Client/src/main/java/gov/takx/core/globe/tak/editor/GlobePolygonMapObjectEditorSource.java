package gov.takx.core.globe.tak.editor;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Polygon;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Polygon} object.
 *
 * @since 5.4
 */
public class GlobePolygonMapObjectEditorSource extends ACompositeEditorSource<IGlobeShapeMapObject>
{
    public GlobePolygonMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, List.of(
                new PolygonControlPointProvider(source, true),
                new MilStdControlPointProvider<>(source)));
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Polygon polygon = getSource().requireShapeModel();
        // Make sure we validate the edited vertices
        polygon.setVertices(polygon.getVertices());
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
