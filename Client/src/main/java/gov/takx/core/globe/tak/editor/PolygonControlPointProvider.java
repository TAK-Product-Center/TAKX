package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.dataservices.shapes.Polygon;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that represent a {@link Polygon} object.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class PolygonControlPointProvider extends AMultipointControlPointProvider<IGlobeShapeMapObject>
{
    /**
     * @param source   the source object being edited
     * @param isClosed {@code true} if the locations written to the source need to be closed, {@code false} if not
     */
    protected PolygonControlPointProvider(@Nonnull IGlobeShapeMapObject source, boolean isClosed)
    {
        super(source, isClosed);
    }

    @Override
    @Nonnull
    protected List<ILocation> getLocations(@Nonnull IGlobeShapeMapObject source)
    {
        Polygon polygon = source.requireShapeModel();
        return polygon.getVertices();
    }

    @Override
    protected void setLocations(@Nonnull IGlobeShapeMapObject source, @Nonnull List<ILocation> locations)
    {
        Polygon polygon = source.requireShapeModel();
        polygon.setVertices(locations, false);
    }

    @Override
    @Nonnull
    protected AltitudeMode getAltitudeMode(@Nonnull IGlobeShapeMapObject source)
    {
        Polygon polygon = source.requireShapeModel();
        return polygon.getAltitudeMode();
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
