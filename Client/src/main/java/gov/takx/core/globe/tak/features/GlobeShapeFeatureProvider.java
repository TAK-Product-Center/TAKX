package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link IGlobeShapeMapObject}.
 *
 * @since 5.5
 */
public final class GlobeShapeFeatureProvider extends AShapeFeatureProvider<IGlobeShapeMapObject>
{
    @Nonnull
    @Override
    public AShapeFeatureProvider.ShapeState createState(IGlobeShapeMapObject object)
    {
        IMutableShape shapeModel = object.getShapeModel();
        ShapeGeometryState shapeGeometryState;
        if (shapeModel instanceof IEllipse ellipse)
        {
            shapeGeometryState = new EllipseGeometryState(ellipse.getLocation(), ellipse.getOrientation(), ellipse.getSemiMajorAxis(), ellipse.getSemiMinorAxis(), ellipse.getRingCount());
        } else if (shapeModel instanceof ICircle circle)
        {
            shapeGeometryState = new EllipseGeometryState(circle.getLocation(), circle.getOrientation(), circle.getRadius(), circle.getRadius(), circle.getRingCount());
        } else if (shapeModel instanceof IRectangle rectangle)
        {
            List<ILocation> vertices = closeGeoPoints(rectangle.getVertices(true));
            shapeGeometryState = new PolygonGeometryState(vertices != null ? vertices : Collections.emptyList(), Collections.emptyList());
        } else if (shapeModel instanceof IPolygon polygon)
        {
            List<List<ILocation>> polygonHoles = polygon.getHoles().stream()
                    .map(IIrregularShape::getVertices)
                    .toList();
            shapeGeometryState = new PolygonGeometryState(polygon.getVertices(), polygonHoles);
        } else if (shapeModel instanceof IPolyline polyline)
        {
            shapeGeometryState = new PolylineGeometryState(polyline.getVertices(), false /*polyline.isClosed()*/);
        } else
        {
            throw new IllegalStateException();
        }

        AltitudeMode altitudeMode = MarshalManager.marshal(shapeModel.getAltitudeMode(), IShape.AltitudeMode.class, AltitudeMode.class);

        return new ShapeState(shapeGeometryState, shapeModel.isVisible(), shapeModel.getFillColor().getRGB(),
                shapeModel.getLineColor().getRGB(), shapeModel.getLineWidth(), shapeModel.getLineStippleFactor(), shapeModel.getLineStipplePattern(),
                shapeModel.getTextureFillParams(), altitudeMode, shapeModel.getVerticalHeight());
    }

    private List<ILocation> closeGeoPoints(@Nullable List<ILocation> geoPoints)
    {
        if (geoPoints == null || geoPoints.isEmpty()) return geoPoints;
        ILocation first = geoPoints.get(0);
        ILocation last = geoPoints.get(geoPoints.size() - 1);
        if (!first.equals(last))
        {
            geoPoints = new ArrayList<>(geoPoints);
            geoPoints.add(geoPoints.get(0));
        }
        return geoPoints;
    }
}
