package gov.takx.core.globe.tak.features;

import android.graphics.Color;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.feature.style.TextureFillStyle;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.dataservices.shapes.AShape.TextureFillParams;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;

/**
 * An abstract {@link IFeatureProvider} for shapes.
 *
 * @since 5.5
 */
@PseudoBean
abstract class AShapeFeatureProvider<T> implements IFeatureProvider<T, AShapeFeatureProvider.ShapeState>
{
    private static final int WGS84_ALGORITHM = GeometryFactory.Algorithm.wgs84.ordinal();

    // Style to use if there is no visible style provided by the object
    private static final BasicFillStyle EMPTY_BASIC_FILL_STYLE = new BasicFillStyle(0);

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Override
    public int getFeatureCount(@Nonnull ShapeState state)
    {
        return isMultiRingedShape(state) ? 2 : 1;
    }

    @Override
    public boolean isVisible(@Nonnull ShapeState state)
    {
        return state.isVisible();
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull ShapeState state)
    {
        // Note we use three-dimensional geometry even though we don't set the z value.
        // This allows consumers of this provider to manipulate the z value for things like extrusion.
        if (state.geometryState instanceof PolylineGeometryState polylineGeometryState)
        {
            var lineString = new LineString(3);
            polylineGeometryState.locations.forEach(l -> lineString.addPoint(l.getLongitude(), l.getLatitude(), l.getAltitude()));
            if (lineString.getNumPoints() == 0) return EMPTY_GEOMETRY;
            if (polylineGeometryState.isClosed() && !lineString.isClosed()) lineString.addPoint(lineString.getX(0), lineString.getY(0), lineString.getZ(0));
            return new Geometry[]{lineString};
        } else if (state.geometryState instanceof PolygonGeometryState polygonGeometryState)
        {
            var polygon = new Polygon(3);
            final LineString exteriorRing = new LineString(3);
            polygonGeometryState.locations.forEach(l -> exteriorRing.addPoint(l.getLongitude(), l.getLatitude(), l.getAltitude()));
            if (exteriorRing.getNumPoints() == 0) return EMPTY_GEOMETRY;
            polygon.addRing(exteriorRing);
            addPolygonHoles(polygonGeometryState.holes, polygon);
            return new Geometry[]{polygon};
        } else if (state.geometryState instanceof EllipseGeometryState ellipseGeometryState)
        {
            ILocation centerPoint = ellipseGeometryState.centerPoint();
            if (isMultiRingedShape(state))
            {
                var point = new Point(centerPoint.getLongitude(), centerPoint.getLatitude(), centerPoint.getAltitude());
                final var outerRing = makeRingGeometry(ellipseGeometryState, point, ellipseGeometryState.ringCount);

                final var innerRings = new GeometryCollection(3);
                for (int i = 1; i < ellipseGeometryState.ringCount; i++) innerRings.addGeometry(makeRingGeometry(ellipseGeometryState, point, i));
                return new Geometry[]{outerRing, innerRings};
            }

            var point = new Point(ellipseGeometryState.centerPoint.getLongitude(), ellipseGeometryState.centerPoint.getLatitude(), centerPoint.getAltitude());
            final var ring = makeRingGeometry(ellipseGeometryState, point, 1);
            return new Geometry[]{ring};
        }
        return EMPTY_GEOMETRY;
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull ShapeState state)
    {
        int interiorAlpha = Color.alpha(state.interiorColor);
        boolean drawInterior = interiorAlpha != 0;
        boolean drawOutline = Color.alpha(state.outlineColor) != 0;
        boolean isMultiRingedShape = isMultiRingedShape(state);

        Style interiorStyle = null;
        Style outlineStyle = null;

        if (drawInterior)
        {
            interiorStyle = new BasicFillStyle(state.interiorColor);
            if (state.textureFillParams != null)
            {
                int textureColor = (interiorAlpha << 24) | 0xFFFFFF;
                interiorStyle = new CompositeStyle(new Style[]{interiorStyle, new TextureFillStyle(FeatureProviderUtility.sanitizeUri(state.textureFillParams.uri()),
                        textureColor, state.textureFillParams.repeatTypeX(), state.textureFillParams.repeatTypeY(),
                        state.textureFillParams.width(), state.textureFillParams.height(), state.textureFillParams.rotation(), state.textureFillParams.isAbsolute())});
            }
        }

        if (drawOutline || isMultiRingedShape) outlineStyle = FeatureProviderUtility.createStrokeStyle(state.outlineWidth, state.outlineColor, state.stippleFactor, state.stipplePattern);

        if (isMultiRingedShape)
        {
            final var outerRingStyle = drawInterior ? new CompositeStyle(new Style[]{interiorStyle, outlineStyle}) : outlineStyle;
            return new Style[]{outerRingStyle, outlineStyle};
        }

        Style finalStyle;
        if (drawInterior && drawOutline)
        {
            finalStyle = new CompositeStyle(new Style[]{interiorStyle, outlineStyle});
        } else if (drawInterior)
        {
            finalStyle = interiorStyle;
        } else if (drawOutline)
        {
            finalStyle = outlineStyle;
        } else
        {
            finalStyle = EMPTY_BASIC_FILL_STYLE;
        }
        return new Style[]{finalStyle};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull ShapeState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(state.altitudeMode, featureCount);
    }

    @Override
    @Nonnull
    public double[] createExtrusions(@Nonnull AShapeFeatureProvider.ShapeState state)
    {
        double[] extrusions = IFeatureProvider.super.createExtrusions(state);
        // Extrude only if not ClampToGround
        if (state.altitudeMode != AltitudeMode.ClampToGround) Arrays.fill(extrusions, state.verticalHeight);
        return extrusions;
    }

    /**
     * Add polygon holes to {@link Polygon}.
     * @param polygonHoles the polygon holes
     * @param polygon the polygon geometry
     */
    private void addPolygonHoles(@Nonnull List<List<ILocation>> polygonHoles, @Nonnull Polygon polygon)
    {
        for (List<ILocation> hole : polygonHoles)
        {
            LineString interiorRing = new LineString(3);
            hole.forEach(l -> interiorRing.addPoint(l.getLongitude(), l.getLatitude()));
            polygon.addRing(interiorRing);
        }
    }

    /**
     * @param state the ellipse geometry state
     * @param center The center location of the shape
     * @param ring The ring to create
     * @return geometry for a specified ring
     */
    Geometry makeRingGeometry(EllipseGeometryState state, Point center, int ring)
    {
        // major/minor radius needs to be reversed here (?)
        Geometry ellipse = GeometryFactory.createEllipse(center, state.heading, state.minorRadius * ring, state.majorRadius * ring, WGS84_ALGORITHM);
        Envelope envelope = ellipse.getEnvelope();
        boolean invalidGeometry = (Double.isNaN(envelope.minX) || Double.isNaN(envelope.minY) || Double.isNaN(envelope.maxX) || Double.isNaN(envelope.maxY));
        return invalidGeometry ? null : ellipse;
    }

    private boolean isMultiRingedShape(ShapeState shapeState)
    {
        return shapeState.geometryState() instanceof EllipseGeometryState ellipseGeometryState &&
                ellipseGeometryState.ringCount() > 1;
    }

    /**
     * Generic interface for {@link ShapeState} geometries.
     */
    interface ShapeGeometryState
    {
    }

    /**
     * State object for polygon geometries.
     */
    record PolygonGeometryState(List<ILocation> locations, List<List<ILocation>> holes) implements ShapeGeometryState
    {
    }

    /**
     * State object for polyline geometries.
     */
    record PolylineGeometryState(List<ILocation> locations, boolean isClosed) implements ShapeGeometryState
    {
    }

    /**
     * State object for ellipse geometries.
     */
    record EllipseGeometryState(ILocation centerPoint, double heading, double majorRadius, double minorRadius, int ringCount) implements ShapeGeometryState
    {
    }

    /**
     * State object for {@link AShapeFeatureProvider}.
     */
    public record ShapeState(ShapeGeometryState geometryState, boolean isVisible, int interiorColor, int outlineColor, double outlineWidth, int stippleFactor, short stipplePattern, TextureFillParams textureFillParams, AltitudeMode altitudeMode, double verticalHeight)
    {
    }
}
