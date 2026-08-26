
package gov.takx.core.globe.tak;

import com.atakmap.map.EngineLibrary;
import com.atakmap.map.formats.c3dt.Cesium3DTilesModelInfoSpi;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.map.layer.model.ModelInfoFactory;
import com.atakmap.map.layer.model.SceneLayer;
import com.atakmap.map.layer.model.pix4d.Pix4dGeoreferencer;
import com.atakmap.util.ConfigOptions;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.system.SystemUtils;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.builder.ModelLayerBuilder;
import gov.takx.api.project.IProjectManager;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jassimp.Jassimp;
import jassimp.JassimpLibraryLoader;

import java.io.File;

/**
 * A service that manages the list of configured 3D model data set renderers in TAK.
 *
 * @since 5.0
 */
@ApplicationScoped
public class ModelLayerManager implements IRaptorLayerManagerListener
{
    public static final String SCENE_LAYER_CLASS = SceneLayer.class.getName();

    static
    {
        // Install no-op Jassimp library loader to support loading vehicle models via AssimpModelSpi
        Jassimp.setLibraryLoader(new MyJassimpLibraryLoader());

        // We use the Java based renderer (ModelInfoFactory) for Cesium 3D tiles
        // until the c++ implementation meets the feature set of the java implementation.
        ModelInfoFactory.registerGeoreferencer(Pix4dGeoreferencer.INSTANCE);
        ModelInfoFactory.registerSpi(Cesium3DTilesModelInfoSpi.INSTANCE);
    }

    private final IRaptorLayerManager raptorLayerManager;
    private final IProjectManager projectManager;
    private final ProgramEnvironment programEnvironment;
    private final boolean headless;

    private SceneLayer sceneLayer;
    private boolean started = false;

    @Inject
    ModelLayerManager(IRaptorLayerManager raptorLayerManager, IProjectManager projectManager,
                      ProgramEnvironment programEnvironment, @Headless boolean headless)
    {
        this.raptorLayerManager = raptorLayerManager;
        this.projectManager = projectManager;
        this.programEnvironment = programEnvironment;
        this.headless = headless;
    }

    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        if (headless) return;
        event.getProgressMonitor().setText("Starting 3D model services");

        File offlineCacheModelsDir = new File(programEnvironment.getProjectDataDirectory(), "OfflineCache/Models");
        offlineCacheModelsDir.mkdirs();
        ConfigOptions.setOption(KernelConfigOptionConstants.MODEL_OFFLINE_CACHE_DIR, offlineCacheModelsDir.getAbsolutePath());

        File projectDir = new File(projectManager.getProjectDirectory());
        String modelIconUri = UIResources.getUrl(ResourceDirectory.TAK_ASSETS_PNG, "3D-Model.png").toString();
        // TAK uris require an extra slash
        modelIconUri = modelIconUri.startsWith("file:/") && modelIconUri.charAt(6) != '/' ? "file://" + modelIconUri.substring(6) : modelIconUri;
        if (SystemUtils.isOsLinux()) modelIconUri = modelIconUri.startsWith("file://") && modelIconUri.charAt(7) != '/' ? "file:///" + modelIconUri.substring(7) : modelIconUri;
        ConfigOptions.setOption(KernelConfigOptionConstants.MODEL_ICON_URI, modelIconUri);
        File sceneLayerCacheDir = new File(projectDir, "/Databases/scenes.db");
        sceneLayerCacheDir.mkdirs();
        sceneLayer = new SceneLayer("Scene Layer", sceneLayerCacheDir.getAbsolutePath());

        raptorLayerManager.addRaptorLayerManagerListener(this);

        started = true;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        if (headless) return;
        if (started)
        {
            raptorLayerManager.removeRaptorLayerManagerListener(this);
            sceneLayer.shutdown();
            started = false;
        }
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        if (SCENE_LAYER_CLASS.equals(raptorLayer.getClassName()))
        {
            ModelLayerBuilder.parseRestorableString(raptorLayer.getRestorableState()).stream()
                    .filter(path -> !sceneLayer.contains(path))
                    .forEach(sceneLayer::add);
        }
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (SCENE_LAYER_CLASS.equals(raptorLayer.getClassName()))
        {
            ModelLayerBuilder.parseRestorableString(raptorLayer.getRestorableState()).forEach(path -> sceneLayer.remove(path));
        }
    }

    /**
     * The {@link SceneLayer} for rendering the 3D model data.
     *
     * @return the layer
     * @since 5.1
     */
    public SceneLayer getSceneLayer()
    {
        return sceneLayer;
    }

    /**
     * Extract a suitable {@link Envelope} from the provided {@link ModelInfo}. Returns {@code null} if envelope data is not available.
     * @param modelInfo the model info
     * @return an {@link Envelope} for the model info, or {@code null} if none can be determined
     * @since 5.7
     */
    @Nullable
    public static Envelope getModelEnvelope(@Nonnull ModelInfo modelInfo)
    {
        Envelope.Builder envelope = new Envelope.Builder();
        if (modelInfo.metadata != null && modelInfo.metadata.getAttributeNames().contains(StandardAttributeNames.AXIS_ALIGNED_BOUNDING_BOX))
        {
            double[] aabb = modelInfo.metadata.getDoubleArrayAttribute(StandardAttributeNames.AXIS_ALIGNED_BOUNDING_BOX);
            envelope.add(aabb[0], aabb[1], aabb[2]);
            envelope.add(aabb[3], aabb[4], aabb[5]);
            return envelope.build();
        }
        if (modelInfo.location != null)
        {
            envelope.add(modelInfo.location.getLongitude(), modelInfo.location.getLatitude(), modelInfo.location.getAltitude());
            return envelope.build();
        }
        return null;
    }

    /**
     * No-op library loader for Jassimp. The default Jassimp library loader will fail to find the shared library and refuse to initialize.
     * We use {@link EngineLibrary#initialize()} to load the jassimp library for us.
     * @since 5.8
     */
    private static class MyJassimpLibraryLoader extends JassimpLibraryLoader
    {
        @Override
        public void loadLibrary()
        {
            // No-op - EngineLibrary loads the Jassimp library for us.
        }
    }
}
