package gov.takx.core.globe.tak.features;

import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IArrow;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.ICircle;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRangeFan;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IRoute;
import gov.takx.core.globe.GlobeRouteMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A container class for {@link #SPI} that provides feature providers for {@link IGlobeShapeMapObject}.
 *
 * @since 5.5
 */
public final class GlobeShapeMapObjectSpi
{
    private static final CompositeFeatureProvider<IGlobeShapeMapObject> shapeProvider = new CompositeFeatureProvider<>(
            new GlobeShapeFeatureProvider(),
            new ShapeLabelFeatureProvider(),
            new ExtrudedGlobeShapeFeatureProvider<>(new GlobeShapeFeatureProvider()));

    private static final CompositeFeatureProvider<IGlobeShapeMapObject> arrowShapeProvider = new CompositeFeatureProvider<>(
            new GlobeArrowFeatureProvider(),
            new ShapeLabelFeatureProvider());

    private static final GlobeRangeFanFeatureProvider rangeFanShapeProvider = new GlobeRangeFanFeatureProvider();

    private static final GlobeBullseyeFeatureProvider bullseyeShapeProvider = new GlobeBullseyeFeatureProvider();

    private static final CompositeFeatureProvider<GlobeRouteMapObject> routeShapeProvider = new CompositeFeatureProvider<>(
            new GlobeRouteFeatureProvider(),
            new RouteLabelFeatureProvider());

    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.GLOBE_MAP_OBJECT_PRIORITY, IGlobeShapeMapObject.class)
    {
        @Nullable
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            if (object instanceof IGlobeShapeMapObject globeShapeMapObject)
            {
                IMutableShape shapeModel = globeShapeMapObject.getShapeModel();
                if (shapeModel instanceof IArrow)
                {
                    return arrowShapeProvider;
                } else if (shapeModel instanceof IRangeFan)
                {
                    return rangeFanShapeProvider;
                } else if (shapeModel instanceof IBullseye)
                {
                    return bullseyeShapeProvider;
                } else if (shapeModel instanceof IRoute)
                {
                    return routeShapeProvider;
                } else if (shapeModel instanceof IEllipse ||
                        shapeModel instanceof ICircle ||
                        shapeModel instanceof IRectangle ||
                        shapeModel instanceof IPolygon ||
                        shapeModel instanceof IPolyline)
                {
                    return shapeProvider;
                }
            }
            return null;
        }
    };
}