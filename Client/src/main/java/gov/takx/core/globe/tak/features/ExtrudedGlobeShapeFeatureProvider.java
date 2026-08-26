package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.math.MathUtils;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * A {@link IFeatureProvider} for extruded {@link IGlobeShapeMapObject} that are {@link gov.takx.api.shapes.IShape.AltitudeMode#ClampToGround}. {@code ClampToGround} will
 * render with the ceiling being the vertical height and will extrude <I>down</I> to the terrain.
 *
 * @since 5.0
 */
public class ExtrudedGlobeShapeFeatureProvider<StateObject> implements IFeatureProvider<IGlobeShapeMapObject, ExtrudedGlobeShapeFeatureProvider.ExtrudedShapeState<StateObject>>
{
    private final IFeatureProvider<IGlobeShapeMapObject, StateObject> featureProvider;

    public ExtrudedGlobeShapeFeatureProvider(IFeatureProvider<IGlobeShapeMapObject, StateObject> featureProvider)
    {
        this.featureProvider = featureProvider;
    }

    protected IFeatureProvider<IGlobeShapeMapObject, StateObject> getFeatureProvider()
    {
        return featureProvider;
    }

    @Nonnull
    @Override
    public ExtrudedShapeState<StateObject> createState(IGlobeShapeMapObject globeShapeMapObject)
    {
        StateObject innerState = getFeatureProvider().createState(globeShapeMapObject);
        IMutableShape shapeModel = globeShapeMapObject.requireShapeModel();
        double verticalHeight = shapeModel.getVerticalHeight();
        // This feature provider is only interested in extrusions for ClampToGround which extrude down to the surface
        boolean isExtruded = shapeModel.isExtruded() && (shapeModel.getAltitudeMode() == IShape.AltitudeMode.ClampToGround);
        return new ExtrudedShapeState<>(innerState, isExtruded, verticalHeight);
    }

    @Override
    public int getFeatureCount(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        return state.isExtrudedClampToGround ? getFeatureProvider().getFeatureCount(state.innerState) : 0;
    }

    @Override
    public boolean isVisible(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        return state.isExtrudedClampToGround && getFeatureProvider().isVisible(state.innerState);
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        if (!state.isExtrudedClampToGround) return new Geometry[0];

        Geometry[] geometries = getFeatureProvider().createGeometry(state.innerState);
        extrudeGeometries(geometries, state.verticalHeight);
        return geometries;
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        if (!state.isExtrudedClampToGround) return new Style[0];

        Style[] styles = getFeatureProvider().createStyle(state.innerState);
        extrudeStyles(styles);
        return styles;
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(AltitudeMode.Relative, featureCount);
    }

    @Nonnull
    @Override
    public double[] createExtrusions(@Nonnull ExtrudedShapeState<StateObject> state)
    {
        double[] extrusions = IFeatureProvider.super.createExtrusions(state);
        if (isMultiRingedShape(state) && extrusions.length > 0)
        {
            // We're only interested in extruding the outer ring
            extrusions[0] = -1;
        } else
        {
            Arrays.fill(extrusions, -1);
        }
        return extrusions;
    }

    /**
     * Adjust the z-value of geometries by the specified vertical height.
     *
     * @param geometries     the geometries
     * @param verticalHeight the vertical height
     */
    private static void extrudeGeometries(Geometry[] geometries, double verticalHeight)
    {
        for (Geometry geometry : geometries)
        {
            if (geometry instanceof Polygon polygon)
            {
                adjustLineString(polygon.getExteriorRing(), verticalHeight);
                polygon.getInteriorRings().forEach(ls -> adjustLineString(ls, verticalHeight));
            } else if (geometry instanceof LineString lineString)
            {
                adjustLineString(lineString, verticalHeight);
            } else if (geometry instanceof Point point)
            {
                point.set(point.getX(), point.getY(), point.getZ() + verticalHeight);
            } else if (geometry instanceof GeometryCollection collection)
            {
                extrudeGeometries(collection.getGeometries().toArray(Geometry[]::new), verticalHeight);
            }
        }
    }

    private static void adjustLineString(LineString lineString, double elevation)
    {
        for (int i = 0; i < lineString.getNumPoints(); i++) lineString.setZ(i, elevation);
    }

    /**
     * Adjust the alpha color of fill styles to be semi-transparent. This will attempt to go one-level deep on any {@link CompositeStyle} passed to it.
     *
     * @param styles the styles
     */
    private static void extrudeStyles(Style[] styles)
    {
        for (int i = 0; i < styles.length; i++)
        {
            Style style = styles[i];
            if (style instanceof CompositeStyle compositeStyle)
            {
                Style[] adjustedStyles = IntStream.range(0, compositeStyle.getNumStyles())
                        .mapToObj(compositeStyle::getStyle)
                        .map(ExtrudedGlobeShapeFeatureProvider::adjustStyle)
                        .toArray(Style[]::new);
                styles[i] = new CompositeStyle(adjustedStyles);
            } else
            {
                styles[i] = adjustStyle(style);
            }
        }
    }

    private static Style adjustStyle(Style style)
    {
        if (style instanceof BasicFillStyle fillStyle)
        {
            int fillColor = fillStyle.getColor();
            // Skip adjustments if we are at full opacity
            if (!MathUtils.hasBits(fillColor, 0xFF000000))
            {
                // Halve the fill alpha so the surface and extrusion contribute to the transparency. This gives full alpha from the nadir view.
                fillColor = ((fillColor >>> 1) & 0xFF000000) | (fillColor & 0x00FFFFFF);
                return new BasicFillStyle(fillColor);
            }
        }
        return style;
    }

    private boolean isMultiRingedShape(ExtrudedShapeState<StateObject> state)
    {
        return state.innerState instanceof AShapeFeatureProvider.ShapeState shapeState &&
                shapeState.geometryState() instanceof AShapeFeatureProvider.EllipseGeometryState ellipseGeometryState &&
                ellipseGeometryState.ringCount() > 1;
    }

    /**
     * State object for {@link ExtrudedGlobeShapeFeatureProvider}.
     * @since 5.1
     */
    public record ExtrudedShapeState<StateObject>(StateObject innerState, boolean isExtrudedClampToGround, double verticalHeight)
    {
    }
}
