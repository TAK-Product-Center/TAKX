package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Polyline;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represent a {@link Polyline} object.
 *
 * @since 5.4
 */
public class GlobePolylineMapObjectEditorSource extends AMultipointEditorSource<IGlobeShapeMapObject> implements IControlPointTextProvider
{
    public GlobePolylineMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source, false);
    }

    @Override
    protected List<ILocation> getLocations(IGlobeShapeMapObject source)
    {
        Polyline polyline = source.requireShapeModel();
        return polyline.getVertices();
    }

    @Override
    protected void setLocations(IGlobeShapeMapObject source, List<ILocation> locations)
    {
        Polyline polyline = source.requireShapeModel();
        polyline.setVertices(locations, false);
    }

    @Override
    protected AltitudeMode getAltitudeMode(IGlobeShapeMapObject source)
    {
        Polyline polyline = source.requireShapeModel();
        return polyline.getAltitudeMode();
    }

    @Override
    public void applyControlPointsToSource(List<IControlPoint> controlPoints)
    {
        Polyline polyline = getSource().requireShapeModel();
        // Make sure we validate the edited vertices
        polyline.setVertices(polyline.getVertices());
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
