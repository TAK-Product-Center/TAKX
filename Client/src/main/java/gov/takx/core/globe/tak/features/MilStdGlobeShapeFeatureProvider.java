package gov.takx.core.globe.tak.features;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.MutableUTMPoint;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.symbology.milstd2525.MilStd2525;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.milsym.IMilStd2525ServiceProvider;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.ShapeType;
import gov.takx.core.globe.tak.features.AShapeFeatureProvider.ShapeGeometryState;
import gov.takx.dataservices.persist.AShapeMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A {@link IFeatureProvider} for {@link IGlobeShapeMapObject} that are MIL-STD-2525 shapes.
 *
 * @since 5.1
 */
@PseudoBean
public final class MilStdGlobeShapeFeatureProvider implements IFeatureProvider<IGlobeShapeMapObject, MilStdGlobeShapeFeatureProvider.MilStdShapeState>
{
    private static final Map<gov.takx.api.shapes.ShapeType, gov.tak.api.symbology.ShapeType> shapeLookup = Map.ofEntries(
            Map.entry(ShapeType.RECTANGLE, gov.tak.api.symbology.ShapeType.Rectangle),
            Map.entry(ShapeType.SQUARE, gov.tak.api.symbology.ShapeType.Rectangle),
            Map.entry(ShapeType.FREEHAND_POLYGON, gov.tak.api.symbology.ShapeType.Polygon),
            Map.entry(ShapeType.POLYGON, gov.tak.api.symbology.ShapeType.Polygon),
            Map.entry(ShapeType.FREEHAND_POLYLINE, gov.tak.api.symbology.ShapeType.LineString),
            Map.entry(ShapeType.POLYLINE, gov.tak.api.symbology.ShapeType.LineString),
            Map.entry(ShapeType.ARROW, gov.tak.api.symbology.ShapeType.LineString),
            Map.entry(ShapeType.CIRCLE, gov.tak.api.symbology.ShapeType.LineString),
            Map.entry(ShapeType.ELLIPSE, gov.tak.api.symbology.ShapeType.Ellipse),
            Map.entry(ShapeType.BULLSEYE, gov.tak.api.symbology.ShapeType.Circle),
            Map.entry(ShapeType.RANGE_FAN, gov.tak.api.symbology.ShapeType.Circle),
            Map.entry(ShapeType.ROUTE, gov.tak.api.symbology.ShapeType.LineString));

    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.TOP_PRIORITY, IGlobeShapeMapObject.class)
    {
        @Override
        protected IFeatureProvider<?, ?> createFeatureProvider(@Nonnull Object object)
        {
            final IGlobeShapeMapObject globeShapeMapObject = (IGlobeShapeMapObject) object;
            if (globeShapeMapObject.getMapObject() != null)
            {
                boolean hasSymbolCode = globeShapeMapObject.getMapObject().hasMetaValue(StandardAttributeNames.MIL_SYM);
                return hasSymbolCode ? new MilStdGlobeShapeFeatureProvider() : null;
            }
            return null;
        }
    };

    private static final GlobeShapeFeatureProvider GLOBE_SHAPE_FEATURE_PROVIDER = new GlobeShapeFeatureProvider();

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMilStd2525ServiceProvider symbologyServiceProvider;

    private IGlobeMapObjectManager globeMapObjectManager;
    private MilStdShapeState lastState;
    private Map<Integer, Collection<Feature>> lastFeatureCollection;

    @PostConstruct
    private void postConstruct()
    {
        globeMapObjectManager = globeManager.getPrimaryGlobeDelegate().getGlobeMapObjectManager();
    }

    @Nonnull
    @Override
    public MilStdShapeState createState(IGlobeShapeMapObject globeShapeMapObject)
    {
        IMutableShape shapeModel = globeShapeMapObject.requireShapeModel();
        IMapObject mapObject = globeShapeMapObject.getMapObject();
        final AttributeSet attributeSet = mapObject.getAttributeSet();
        String symbolCode = attributeSet.getStringAttribute(StandardAttributeNames.MIL_SYM);
        Map<String, String> unitModifiers = new HashMap<>();
        for (String attributeName : attributeSet.getAttributeNames())
        {
            if (attributeName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
            {
                unitModifiers.put(attributeName, attributeSet.getStringAttribute(attributeName));
            }
            if (attributeName.equals("milsym.drawversion"))
            {
                unitModifiers.put(attributeName, attributeSet.getStringAttribute(attributeName));
            }
        }
        GeoPoint controlPoint = null;
        if (attributeSet.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LAT) && attributeSet.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LON))
        {
            double lat = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LAT);
            double lon = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LON);
            double alt = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, 0);
            controlPoint = new GeoPoint(lat, lon, alt);
        }

        boolean wasClipped = attributeSet.getBooleanAttribute(StandardAttributeNames.MIL_SYM_HINT_WAS_CLIPPED, false);
        boolean lodAware = attributeSet.getBooleanAttribute(StandardAttributeNames.MIL_SYM_HINT_LOD_AWARE, false);

        Map<Integer, Double> milStdGsdHintMap = mapObject instanceof AShapeMapObject<?> mo ? mo.getMilStdGsdHintMap() : null;
        Map<Integer, GeoBounds> milStdSceneBoundsHintMap = mapObject instanceof AShapeMapObject<?> mo ? mo.getMilStdSceneBoundsHintMap() : null;

        AShapeFeatureProvider.ShapeState gmoState = GLOBE_SHAPE_FEATURE_PROVIDER.createState(globeShapeMapObject);
        AltitudeMode altitudeMode = AltitudeMode.ClampToGround;
        List<ILocation> geoPoints = getGeoPoints(gmoState.geometryState());
        gov.tak.api.symbology.ShapeType shapeType = getShapeType(globeShapeMapObject);
        String moKey = mapObject.getKey();
        return new MilStdShapeState(symbolCode, geoPoints, unitModifiers, gmoState.outlineColor(), gmoState.outlineWidth(), shapeModel.isVisible(), altitudeMode, milStdSceneBoundsHintMap, milStdGsdHintMap, wasClipped, lodAware, shapeType, controlPoint, moKey);
    }

    @Override
    public int getFeatureCount(@Nonnull MilStdShapeState state)
    {
        return getFeatures(state).size();
    }

    @Override
    public boolean isVisible(@Nonnull MilStdShapeState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull MilStdShapeState state)
    {
        return getFeatures(state).stream().map(Feature::getGeometry).toArray(Geometry[]::new);
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull MilStdShapeState state)
    {
        return getFeatures(state).stream().map(Feature::getStyle).toArray(Style[]::new);
    }

    @Nonnull
    @Override
    public String[] createGroupingNames(MilStdShapeState state)
    {
        Map<Integer, Collection<Feature>> featuresByGlobe = getFeaturesByGlobe(state);
        return featuresByGlobe.entrySet().stream()
                .flatMap(entry -> Stream.generate(() -> Integer.toString(entry.getKey())).limit(entry.getValue().size()))
                .toArray(String[]::new);
    }

    @Nonnull
    @Override
    public String[] createNames(@Nonnull MilStdShapeState state)
    {
        final String[] names = new String[getFeatureCount(state)];
        Arrays.fill(names, "");
        return names;
    }

    @Nonnull
    private Collection<Feature> getFeatures(MilStdShapeState state)
    {
        return getFeaturesByGlobe(state).values().stream().flatMap(Collection::stream).toList();
    }

    @Nonnull
    private Map<Integer, Collection<Feature>> getFeaturesByGlobe(MilStdShapeState state)
    {
        if (state.equals(lastState)) return lastFeatureCollection;
        Map<Integer, Collection<Feature>> featureResultsByGlobe = new LinkedHashMap<>();
        IGeoPoint[] geoPoints = state.geoPoints.toArray(IGeoPoint[]::new);
        AttributeSet unitModifiers = new AttributeSet();
        state.unitModifiers.forEach(unitModifiers::setAttribute);
        globeManager.getAllGlobeDelegates().forEach(g -> {
            int appGlobeId = g.getGlobeId();
            ISymbologyProvider.RendererHints hints = new ISymbologyProvider.RendererHints();
            hints.strokeColor = state.strokeColor;
            hints.strokeWidth = (float) state.strokeWidth;
            hints.fontSize = 10;
            hints.boundingBox = null;
            hints.shapeType = state.shapeType();
            hints.controlPoint = state.controlPoint();
            // compute the minimum resolution for the symbol based on source points. If the current globe resolution is
            // lower than the minimum, use the minimum resolution and discard any bounding box; else if the current
            // globe resolution is higher than the minimum, use actual resolution and apply the bounding box.
            final Component worldWindow = g.getWorldWindow().getComponent();
            final double minimumResolution = MilStd2525.getDefaultResolutionFromPoints(worldWindow.getWidth(), worldWindow.getHeight(), state.geoPoints);
            Double hintGsd = state.hintGsd != null ? state.hintGsd.get(appGlobeId) : null;
            final double hintResolution = hintGsd != null ? hintGsd : minimumResolution;
            hints.resolution = Math.min(hintResolution, minimumResolution);
            GeoBounds hintSceneBounds = state.hintSceneBounds != null ? state.hintSceneBounds.get(appGlobeId) : null;
            hints.boundingBox = hintSceneBounds != null && !hintSceneBounds.crossesIDL() && hintResolution < minimumResolution ? new Envelope(hintSceneBounds.getWest(), hintSceneBounds.getSouth(), 0,
                    hintSceneBounds.getEast(), hintSceneBounds.getNorth(), 0) : null;
            // GDAL gets a little noisy here.
            org.gdal.gdal.gdal.PushErrorHandler();
            Collection<Feature> result = symbologyServiceProvider.get2525ServiceForSymbol(state.symbolCode)
                    .renderMultipointSymbol(state.symbolCode, geoPoints, unitModifiers, hints);
            org.gdal.gdal.gdal.PopErrorHandler();
            if (result == null || result.isEmpty()) result = getFallbackFeatures(state);
            featureResultsByGlobe.put(appGlobeId, result);
            updateMapObjectHints(state, hints);
        });
        lastState = state;
        lastFeatureCollection = featureResultsByGlobe;
        return featureResultsByGlobe;
    }

    /**
     * Apply the provided rendering hints to the {@link IGlobeMapObject} associated with the provided {@link MilStdShapeState state object}
     * if the map object can be retrieved from the {@link IGlobeMapObjectManager}. The relevant hints are stored in the map object's {@link AttributeSet} for later use.
     * @param state the state object
     * @param hints the rendering hints
     */
    private void updateMapObjectHints(@Nonnull MilStdShapeState state, @Nonnull ISymbologyProvider.RendererHints hints)
    {
        IGlobeMapObject gmo = globeMapObjectManager.getGlobeMapObject(state.moKey);
        if (gmo != null)
        {
            AttributeSet attributeSet = gmo.getMapObject().getAttributeSet();
            attributeSet.setAttribute(StandardAttributeNames.MIL_SYM_HINT_WAS_CLIPPED, hints.wasClipped);
            attributeSet.setAttribute(StandardAttributeNames.MIL_SYM_HINT_LOD_AWARE, hints.lodAware);
            if (hints.controlPoint != null)
            {
                attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_LAT, hints.controlPoint.getLatitude());
                attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_LON, hints.controlPoint.getLongitude());
                attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, hints.controlPoint.getAltitude());
            }
        }
    }

    @Nonnull
    private Collection<Feature> getFallbackFeatures(MilStdShapeState state)
    {
        LineString lineString = new LineString(3);
        state.geoPoints.forEach(g -> lineString.addPoint(g.getLongitude(), g.getLatitude()));
        BasicStrokeStyle style = new BasicStrokeStyle(state.strokeColor, (float) state.strokeWidth);
        Feature feature = new Feature("", lineString, style, null);
        return Collections.singleton(feature);
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull MilStdShapeState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(state.altitudeMode, featureCount);
    }

    @Nonnull
    private List<ILocation> getGeoPoints(ShapeGeometryState geometryState)
    {
        if (geometryState instanceof AShapeFeatureProvider.PolygonGeometryState polygonGeometryState)
        {
            return polygonGeometryState.locations();
        } else if (geometryState instanceof AShapeFeatureProvider.PolylineGeometryState polylineGeometryState)
        {
            return polylineGeometryState.locations();
        } else if (geometryState instanceof AShapeFeatureProvider.EllipseGeometryState ellipseGeometryState)
        {
            return createEllipse(ellipseGeometryState.centerPoint(), ellipseGeometryState.heading(), ellipseGeometryState.majorRadius() * 2, ellipseGeometryState.minorRadius() * 2);
        } else
        {
            return Collections.emptyList();
        }
    }

    private static gov.tak.api.symbology.ShapeType getShapeType(IGlobeShapeMapObject globeShapeMapObject)
    {
        ShapeType st = globeShapeMapObject.getMapToolType().getShapeType();
        return shapeLookup.getOrDefault(st, null);
    }

    private static List<ILocation> createEllipse(ILocation centerPoint, double heading, double width, double length)
    {
        int segments = samplingFactor(width, length);
        List<ILocation> coords = new ArrayList<>(segments);
        MutableUTMPoint centerUTM = new MutableUTMPoint(
                centerPoint.getLatitude(),
                centerPoint.getLongitude());

        double angRad;
        double angRad_cos = 1;
        double angRad_sin = 0;

        if (heading != 0d)
        {
            angRad = Math.toRadians(heading);
            angRad_cos = Math.cos(angRad);
            angRad_sin = Math.sin(angRad);
        }

        double alt = centerPoint.getAltitude();

        double radiusW = width / 2;
        double radiusL = length / 2;
        for (int i = 0; i < segments; i++)
        {
            double angdev = Math.toRadians((i * 6));
            double x = Math.cos(angdev) * radiusL;
            double y = Math.sin(angdev) * radiusW;

            double x2 = angRad_sin * x - angRad_cos * y;
            double y2 = angRad_cos * x + angRad_sin * y;

            centerUTM.offset(x2, y2);
            double[] cor1ll = centerUTM.toLatLng(null);

            coords.add(new GeoPoint(cor1ll[0], cor1ll[1], alt));
            centerUTM.offset(-x2, -y2);
        }
        return coords;
    }

    private static int samplingFactor(double semiMajorAxis, double semiMinorAxis)
    {
        double maxe = Math.max(semiMajorAxis, semiMinorAxis);
        double mine = Math.min(semiMajorAxis, semiMinorAxis);
        int sampling = Math.max(360, (int) (40.0 * maxe / mine));
        return sampling + (sampling % 4);
    }

    /**
     * State object for {@link MilStdGlobeShapeFeatureProvider}.
     * @since 5.1
     */
    public record MilStdShapeState(@Nonnull String symbolCode, @Nonnull List<ILocation> geoPoints, @Nonnull Map<String, String> unitModifiers, int strokeColor, double strokeWidth, boolean isVisible, @Nonnull AltitudeMode altitudeMode, @Nullable Map<Integer, GeoBounds> hintSceneBounds, @Nullable Map<Integer, Double> hintGsd, boolean wasClipped, boolean isLodAware, @Nullable gov.tak.api.symbology.ShapeType shapeType, @Nullable GeoPoint controlPoint, @Nonnull String moKey)
    {
    }
}
