package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Polygon;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Polygon} object.
 *
 * @since 5.4
 */
public class GlobePolygonMapObjectEditorSource extends AMultipointEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobePolygonMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, true);
    }

    @Override
    protected List<ILocation> getLocations(IGlobeShapeMapObject source)
    {
        Polygon polygon = source.requireShapeModel();
        return polygon.getVertices();
    }

    @Override
    protected void setLocations(IGlobeShapeMapObject source, List<ILocation> locations)
    {
        Polygon polygon = source.requireShapeModel();
        polygon.setVertices(locations, false);
    }

    @Override
    protected AltitudeMode getAltitudeMode(IGlobeShapeMapObject source)
    {
        Polygon polygon = source.requireShapeModel();
        return polygon.getAltitudeMode();
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Polygon polygon = getSource().requireShapeModel();
        // Make sure we validate the edited vertices
        polygon.setVertices(polygon.getVertices());
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPoint.isSelected() ? MeasureHelper.formatGlobeShapeMeasurements(getSource().requireShapeModel(), controlPoint) : null;
    }

    @Override
    protected boolean isEditable()
    {
        return getSource().requireShapeModel().isEditable();
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
