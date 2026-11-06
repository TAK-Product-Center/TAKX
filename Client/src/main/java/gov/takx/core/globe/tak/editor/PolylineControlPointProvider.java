package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Polyline;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that represent a {@link Polyline} object.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class PolylineControlPointProvider extends AMultipointControlPointProvider<IGlobeShapeMapObject>
{
    /**
     * @param source   the source object being edited
     * @param isClosed {@code true} if the locations written to the source need to be closed, {@code false} if not
     */
    protected PolylineControlPointProvider(@Nonnull IGlobeShapeMapObject source, boolean isClosed)
    {
        super(source, isClosed);
    }

    @Override
    @Nonnull
    protected List<ILocation> getLocations(@Nonnull IGlobeShapeMapObject source)
    {
        Polyline polyline = source.requireShapeModel();
        return polyline.getVertices();
    }

    @Override
    protected void setLocations(@Nonnull IGlobeShapeMapObject source, @Nonnull List<ILocation> locations)
    {
        Polyline polyline = source.requireShapeModel();
        polyline.setVertices(locations, false);
    }

    @Override
    @Nonnull
    protected AltitudeMode getAltitudeMode(@Nonnull IGlobeShapeMapObject source)
    {
        Polyline polyline = source.requireShapeModel();
        return polyline.getAltitudeMode();
    }

    @Override
    protected boolean isEditable(@Nonnull IGlobeShapeMapObject source)
    {
        return source.requireShapeModel().isEditable();
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return controlPoint.isSelected() ? MeasureHelper.formatGlobeShapeMeasurements(getSource().requireShapeModel(), controlPoint) : null;
    }
}
