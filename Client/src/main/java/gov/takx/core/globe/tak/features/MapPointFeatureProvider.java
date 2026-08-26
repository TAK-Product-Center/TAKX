package gov.takx.core.globe.tak.features;

import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapSceneModel;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.MeshPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.math.MathUtils;
import gov.nasa.worldwind.geom.Position;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.platform.commons.opengl.Matrix;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.platform.tracks.TrackShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link IFeatureProvider} for {@link MapPoint}.
 *
 * @since 5.1
 */
@PseudoBean
public final class MapPointFeatureProvider implements IFeatureProvider<MapPoint, MapPointFeatureProvider.MapPointState>
{
    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, MapPoint.class, MapPointFeatureProvider::new)
    {
    };

    @Inject
    private IGlobeManager globeManager;

    private MapPointState lastState;
    private List<GlobeIdAndFeature> lastFeatureList;

    @Nonnull
    @Override
    public MapPointState createState(MapPoint mapPoint)
    {
        final var attributes = mapPoint.getAttributes();
        final var tsc = attributes.getTrackStylingContext();

        final boolean clampToGround = !mapPoint.hasElevation() || tsc.isSnappedToSurface();
        final int color = mapPoint.getColor();
        final double heading = mapPoint.hasHeading() ? mapPoint.getHeading().degrees : 0;

        // Calculate the level of detail for each globe; used when comparing MapPointStates for equality
        final Map<Integer, Integer> lodMap = new HashMap<>();
        globeManager.getAllGlobeDelegates().forEach(appGlobe -> {
            MapRenderer<?> mapRenderer = appGlobe.getPortableMapRenderer();
            GLMapView glMapView = LegacyAdapters.adapt(mapRenderer);
            GLMapView.State state = glMapView.currentScene;
            int currentLod = OSMUtils.mapnikTileLevel(state.scene.gsd, Math.toRadians(state.drawLat));
            lodMap.put(appGlobe.getGlobeId(), currentLod);
        });

        return new MapPointState(mapPoint.getPosition(), clampToGround, color, attributes.getMarkerPixels(),
                mapPoint.hasHeading(), heading, tsc.getTrackShape(), lodMap);
    }

    @Override
    public int getFeatureCount(@Nonnull MapPointState state)
    {
        return getFeatures(state).size();
    }

    @Override
    public boolean isVisible(@Nonnull MapPointState state)
    {
        return true;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull MapPointState state)
    {
        return getFeatures(state).stream().map(Feature::getGeometry).toArray(Geometry[]::new);
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull MapPointState state)
    {
        return getFeatures(state).stream().map(Feature::getStyle).toArray(Style[]::new);
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull MapPointState state)
    {
        final var featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(
                state.clampToGround ? AltitudeMode.ClampToGround : AltitudeMode.Absolute,
                featureCount);
    }

    @Nonnull
    @Override
    public String[] createGroupingNames(MapPointState state)
    {
        // Associates each element in the arrays returned by create methods with its globe
        return getFeaturesByGlobe(state).stream()
                .map(globeIdAndFeature -> String.valueOf(globeIdAndFeature.id))
                .toArray(String[]::new);
    }

    /**
     * @param shape Track shape type
     * @return Mesh definition for the given track shape or {@code null} if the type is {@link TrackShape#None}
     * @since 5.3
     */
    @Nullable
    private static MeshDefinition getMesh(@Nonnull TrackShape shape)
    {
        return switch (shape)
        {
            case Cylinder -> cylinder;
            case Sphere -> sphere;
            case Cone -> pyramid;
            case Cube -> cube;
            case None -> null;
        };
    }

    @Nonnull
    private Collection<Feature> getFeatures(MapPointState state)
    {
        return getFeaturesByGlobe(state).stream().map(globeIdAndFeature -> globeIdAndFeature.feature).toList();
    }

    /**
     * @param state The map point's state
     * @return A list containing each globe's id and the {@link Feature} that should be used to render the map point
     */
    @Nonnull
    private List<GlobeIdAndFeature> getFeaturesByGlobe(MapPointState state)
    {
        if (state.equals(lastState)) return lastFeatureList;

        // The same geometry can be used for all globes
        final var position = state.position;
        final var geom = new Point(position.longitude.degrees, position.latitude.degrees, state.clampToGround ? 0 : position.elevation);

        // Create a unique style for each globe and create the feature list, one feature for each globe
        final var globesAndFeatures = globeManager.getAllGlobeDelegates().stream()
                .sorted(Comparator.comparingInt((IGlobeDelegate g) -> g.getGlobeId()))
                .map(g -> {
                    final var style = createStyleForGlobe(g, state);
                    final var feature = new Feature("Map Point Feature", geom, style, null);

                    return new GlobeIdAndFeature(g.getGlobeId(), feature);
                }).toList();

        lastState = state;
        lastFeatureList = globesAndFeatures;

        return globesAndFeatures;
    }

    /**
     * @param globeDelegate The globe on which the map point will be rendered
     * @param state The map point's state
     * @return The style to use when rendering the map point on the specified globe
     * @since 5.3
     */
    @Nonnull
    private Style createStyleForGlobe(@Nonnull IGlobeDelegate globeDelegate, @Nonnull MapPointState state)
    {
        final var meshDefinition = getMesh(state.trackShape());

        if (meshDefinition != null)
        {
            float[] matrix = new float[16];
            float[] matrixTranspose = new float[16];
            Matrix.setIdentityM(matrix, 0);

            // Get the globe-specific size
            final float sizeInMeters = getSizeInMeters(globeDelegate, state.sizePx);
            Matrix.scaleM(matrix, 0, sizeInMeters, sizeInMeters, sizeInMeters);

            // Set heading
            if (state.hasHeading) Matrix.rotateM(matrix, 0, (float) -state.heading, 0, 0, 1);

            // Apply model transform
            Matrix.multiplyMM(matrix, 0, matrix, 0, meshDefinition.transform, 0);
            Matrix.transposeM(matrixTranspose, 0, matrix, 0);

            final var meshPointStyle = new MeshPointStyle(meshDefinition.uri, state.color, matrixTranspose);

            return FeatureUtility.makeNoLabelStyle(meshPointStyle);
        } else
        {
            return new LabelPointStyle("", 0, 0, ScrollMode.DEFAULT);
        }
    }

    /**
     * @param globeDelegate The globe on which the map point will be rendered
     * @param sizePx The map point's pixel size
     * @return The map point size in meters, adjusted for the provided globe's camera view
     * @since 5.3
     */
    private float getSizeInMeters(@Nonnull IGlobeDelegate globeDelegate, double sizePx)
    {
        var renderer = globeDelegate.getPortableMapRenderer();
        var mapSceneModel = renderer.getMapSceneModel(false, IMapRendererEnums.DisplayOrigin.UpperLeft);
        var location = mapSceneModel.camera.location;
        var target = mapSceneModel.camera.target;

        double range = MathUtils.distance(location.x, location.y, location.z, target.x, target.y, target.z);
        double gsd = MapSceneModel.gsd(range, mapSceneModel.camera.fov, mapSceneModel.height);

        return (float) (sizePx * 3 * gsd); // 3 is arbitrary, based on testing
    }

    static MeshDefinition cylinder = new MeshDefinition("Cylinder", "asset://meshes/cylinder.obj", createCylinderMatrix());
    static MeshDefinition pyramid = new MeshDefinition("Pyramid", "asset://meshes/pyramid.obj", createPyramidMatrix());
    static MeshDefinition sphere = new MeshDefinition("Sphere", "asset://meshes/sphere.obj", createIdentityMatrix());
    static MeshDefinition cube = new MeshDefinition("Cube", "asset://meshes/cube.obj", createIdentityMatrix());

    private record GlobeIdAndFeature(int id, Feature feature)
    {
    }

    public record MeshDefinition(String name, String uri, float[] transform)
    {
    }

    private static float[] createCylinderMatrix()
    {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        // Rotate so top of cylinder is pointing parallel to the surface towards North
        Matrix.rotateM(matrix, 0, 90, 1, 0, 0);
        return matrix;
    }

    private static float[] createPyramidMatrix()
    {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        // Rotate so pyramid tip is pointing parallel to the surface
        Matrix.rotateM(matrix, 0, -90, 1, 0, 0);
        // Move tip of pyramid to origin
        Matrix.translateM(matrix, 0, 0, 0, -0.5f);
        return matrix;
    }

    private static float[] createIdentityMatrix()
    {
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        return matrix;
    }

    /**
     * State object for {@link MapPointFeatureProvider}.
     * @since 5.1
     */
    public record MapPointState(Position position, boolean clampToGround, int color, double sizePx, boolean hasHeading, double heading, TrackShape trackShape, Map<Integer, Integer> lodMap)
    {
    }
}
