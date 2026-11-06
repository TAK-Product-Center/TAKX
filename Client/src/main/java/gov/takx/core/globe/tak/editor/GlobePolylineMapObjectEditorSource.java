package gov.takx.core.globe.tak.editor;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Polyline;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Polyline} object.
 *
 * @since 5.4
 */
public class GlobePolylineMapObjectEditorSource extends ACompositeEditorSource<IGlobeShapeMapObject>
{
    public GlobePolylineMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, List.of(
                new PolylineControlPointProvider(source, false),
                new MilStdControlPointProvider<>(source)));
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Polyline polyline = getSource().requireShapeModel();
        // Make sure we validate the edited vertices
        polyline.setVertices(polyline.getVertices());
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
