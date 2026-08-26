package gov.takx.core.globe;

import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.nativeimagery.NativeImageryRasterLayer2;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BasicRasterServer;
import gov.nasa.worldwind.data.RasterServer;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LocalRasterServerLayer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.retrieve.LocalRasterServerRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.RetrieverFactory;
import gov.nasa.worldwind.util.DataConfigurationFilter;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.globe.ReservedLayers;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRestorable;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.layer.builder.LocalImageryLayerBuilder;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.imagerywizard.FileStoreInfo;
import gov.takx.core.globe.imagerywizard.FileStoreUtil;
import gov.takx.core.globe.imagerywizard.ImportedDataSet;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import gov.takx.core.globe.layers.ConfigFileSupport;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.LatLonGridGlobeLayer;
import gov.takx.core.globe.layers.SurfaceImageLayer;
import gov.takx.core.globe.layers.SurfaceStructureLayer;
import gov.takx.core.globe.layers.TracksLayer;
import gov.takx.core.globe.layers.WWGlobeLayer;
import gov.takx.core.globe.layers.XMLConfigFileLayerFactory;
import gov.takx.core.globe.layers.mapsforge.MapsForgeLayerFactory;
import gov.takx.core.globe.layers.mapsforge.MapsforgeMapFileLayer;
import gov.takx.core.globe.layers.mobac.CustomMobacTiledImageLayer;
import gov.takx.core.globe.layers.worldwind.ApplicationWmsTiledImageLayer;
import gov.takx.core.globe.tak.ImageryGlobeLayer;
import gov.takx.core.globe.tak.ImageryLayerManager;
import gov.takx.core.globe.tak.ModelLayerManager;
import gov.takx.core.globe.tak.TakSceneLayer;
import gov.takx.core.globe.tak.elevation.ElevationDownloaderLayer;
import gov.takx.core.globe.tak.elevation.ElevationLayer;
import gov.takx.core.globe.tak.layer.AtmosphereLayer;
import gov.takx.core.globe.tak.layer.FrameRateOverlay;
import gov.takx.core.globe.tak.layer.StarsLayer;
import gov.takx.core.globe.tak.ww.NoOpGlobeLayer;
import gov.takx.core.globe.tak.ww.layer.TakGARSGraticuleLayer;
import gov.takx.core.globe.tak.ww.layer.TakLatLonGraticuleLayer;
import gov.takx.core.globe.tak.ww.layer.TakMGRSGraticuleLayer;
import gov.takx.core.structures.LargeShapefileLayer;
import gov.takx.core.view.gui.mapobjectmanager.AlarmsLayer;
import gov.takx.core.view.gui.maps.exporters.dataset.MapDataWrapper;
import gov.takx.platform.layer.WorldWindRenderableLayer;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Factory class for creating {@link GlobeLayer}, WW layer, and elevation layer instances.
 *
 * @since 1.26
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeLayerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // List of layers that have been removed and should not be processed.  This can eventually go away, but
    // as the default layers are changing, and users might not delete their ProjectData as directed, this allows
    // us to avoid exceptions, and give the user an actionable error message (assuming they're using the console launcher).
    private static final Set<String> DEAD_LAYERS = Set.of(
            ReservedLayers.ANNOTATIONS,
            ReservedLayers.FLAT_ANNOTATIONS,
            ReservedLayers.NAVIGATION,
            ReservedLayers.TERRAIN_PROFILER,
            ReservedLayers.PROJECT_LABEL,
            ReservedLayers.WIDGETS,
            ReservedLayers.COMPASS,
            ReservedLayers.ICONS,
            ReservedLayers.SHAPES,
            ReservedLayers.LAYER_BOUNDS,
            ReservedLayers.PLAYBACK_ICONS);

    // List of layer class names that have been removed and should not be processed.
    private static final Set<String> DEAD_LAYER_CLASSES = Set.of(
            gov.nasa.worldwind.layers.StarsLayer.class.getName(),
            WorldMapLayer.class.getName(),
            BMNGOneImage.class.getName(),
            SkyGradientLayer.class.getName(),
            ScalebarLayer.class.getName());

    private final IApplicationGlobe globe;
    private final IWorldWindowGL wwd;
    private final ConfigFileSupport configFileSupport = new ConfigFileSupport();

    private boolean deadLayerReported = false;

    @Inject
    @PlatformUnderTest
    private boolean platformUnderTest;

    @Inject
    private XMLConfigFileLayerFactory xmlConfigFileLayerFactory;

    @Inject
    private TakLayerFactory takLayerFactory;

    @Inject
    private MapsForgeLayerFactory mapsForgeLayerFactory;

    @Inject
    private ImageryLayerManager imageryLayerManager;

    @Inject
    private ModelLayerManager modelLayerManager;

    @Inject
    private IProgramEnvironment programEnvironment;

    /**
     * Construct a factory for the given {@link IApplicationGlobe}.
     */
    protected GlobeLayerFactory(IApplicationGlobe globe)
    {
        this.globe = globe;
        wwd = globe.getWorldWindow();
    }

    /**
     * Construct the GlobeLayer and WW Layer to render this {@link IRaptorLayer} on this globe.
     *
     * @param raptorLayer RaptorLayer for which to construct GlobeLayer and WW Layer
     * @return globe layer
     */
    public IGlobeLayer constructGlobeLayer(IRaptorLayer raptorLayer)
    {
        final String name = raptorLayer.getName();
        final String className = raptorLayer.getClassName();
        String configFileName = raptorLayer.getConfigFileName();

        IGlobeLayer globeLayer = null;

        if (configFileName != null)
        {
            // Special handling for paths that are within ProjectData or VFS
            final String configFileToLoad;

            if (configFileName.startsWith(ProgramEnvironment.PROJECT_DATA))
            {
                configFileToLoad = configFileName.replace(ProgramEnvironment.PROJECT_DATA, programEnvironment.getProjectDataDirectory());
            } else
            {
                configFileToLoad = configFileSupport.getRealFileToLoad(configFileName);
            }

            if (!new File(configFileToLoad).exists())
            {
                logger.error("Configuration file for layer '{}' is missing: {}", name, configFileToLoad);
                return null;
            }

            globeLayer = xmlConfigFileLayerFactory.makeLayerFromConfigFile(raptorLayer, configFileToLoad);
            if (globeLayer == null)
            {
                globeLayer = xmlConfigFileLayerFactory.makeLayerFromWWConfigFile(raptorLayer, configFileToLoad);
            }
        } else if (className != null)
        {
            globeLayer = createGlobeLayerFromClassName(raptorLayer, className);
        } else
        {
            logger.error("RaptorLayer '{}' defines neither className nor configFileName.", name);
        }

        return globeLayer;
    }

    /**
     * Create a {@link IGlobeLayer} from the given {@link IRaptorLayer} and the layer class name.
     *
     * @param raptorLayer The layer definition from which to create the globe layer
     * @param className   The name of the class to instantiate as the WW layer class
     * @return newly created globe layer, or {@code null} on failure
     */
    @Nullable
    private IGlobeLayer createGlobeLayerFromClassName(IRaptorLayer raptorLayer, String className)
    {
        final String name = raptorLayer.getName();
        final LayerGroup layerGroup = raptorLayer.getLayerGroup();
        final String restorableState = raptorLayer.getRestorableState();
        final com.atakmap.map.layer.Layer constructionLayer = raptorLayer.getTakConstructionLayer();
        final boolean hasRestorableState = !StringUtils.isEmpty(restorableState);
        final String layerType = raptorLayer.getType();

        if (DEAD_LAYERS.contains(name) || DEAD_LAYER_CLASSES.contains(className))
        {
            if (!deadLayerReported)
            {
                logger.warn("===================================================================================================");
                logger.warn("Found layer '{}'", name);
                logger.warn("Your layer definitions in ProjectData/GlobeLayers.xml contains layers that are no longer supported.");
                logger.warn("Please delete your ProjectData directory and relaunch TAKX to get the correct layer definitions.");
                logger.warn("===================================================================================================");
                deadLayerReported = true;
            }

            return new NoOpGlobeLayer(raptorLayer);
        }

        IGlobeLayer globeLayer = null;

        if (IRaptorLayer.INSTALLED_TYPE.equals(layerType))
        {
            globeLayer = getInstalledLayer(raptorLayer);
        } else if (className.equals(NativeImageryRasterLayer2.class.getName()))
        {
            final var dataStore = imageryLayerManager.getDataStore();
            final var params = new RasterDataStore.DatasetQueryParameters();
            params.names = Arrays.stream(raptorLayer.getRestorableState().split(LocalImageryLayerBuilder.ESCAPED_PATH_SEPARATOR))
                    .map(s -> new File(s).getName()).toList();
            final var nativeImageryRasterLayer = new NativeImageryRasterLayer2(name, dataStore, params);
            globeLayer = new GlobeLayer(raptorLayer, nativeImageryRasterLayer);
        } else if (className.equals(DatasetRasterLayer2.class.getName()) && hasRestorableState)
        {
            com.atakmap.map.layer.Layer rasterLayer = takLayerFactory.createDatasetDescriptorLayer(raptorLayer.getRestorableState(), raptorLayer.getName());
            if (rasterLayer != null) globeLayer = new GlobeLayer(raptorLayer, rasterLayer);
        } else if (className.equals(ModelLayerManager.SCENE_LAYER_CLASS) && hasRestorableState)
        {
            TakSceneLayer sceneLayer = new TakSceneLayer(raptorLayer.getName(), restorableState, modelLayerManager.getSceneLayer());
            globeLayer = new GlobeLayer(raptorLayer, sceneLayer, sceneLayer.getEnvelope());
        } else if (className.equals(ElevationLayer.class.getName()) && hasRestorableState)
        {
            ElevationLayer elevationLayer = new ElevationLayer(raptorLayer.getDisplayName());
            globeLayer = new GlobeLayer(raptorLayer, elevationLayer);
        } else if (className.equals(ElevationDownloaderLayer.class.getName()))
        {
            ElevationDownloaderLayer elevationDownloaderLayer = new ElevationDownloaderLayer();
            globeLayer = new GlobeLayer(raptorLayer, elevationDownloaderLayer);
        } else if (className.equals(WorldWindRenderableLayer.class.getName()))
        {
            globeLayer = new GlobeLayer(raptorLayer, constructionLayer != null ? constructionLayer : new WorldWindRenderableLayer(raptorLayer.getName()));
        } else if (name.equals(ReservedLayers.ALARMS) && !platformUnderTest)
        {
            globeLayer = new AlarmsLayer(raptorLayer, (Layer) WorldWind.createComponent(className), wwd);
        } else if (name.equals(ReservedLayers.TRACKS))
        {
            globeLayer = new TracksLayer(globe, raptorLayer);
        } else if (name.equals(ReservedLayers.MGRS_GRID))
        {
            globeLayer = new WWGlobeLayer(raptorLayer, new TakMGRSGraticuleLayer());
        } else if (name.equals(ReservedLayers.GARS_GRID))
        {
            globeLayer = new WWGlobeLayer(raptorLayer, new TakGARSGraticuleLayer());
        } else if (name.equals(ReservedLayers.LAT_LON_GRID))
        {
            globeLayer = new LatLonGridGlobeLayer(raptorLayer, new TakLatLonGraticuleLayer());
        } else if (LayerGroup.SURFACE_IMAGERY == layerGroup)
        {
            globeLayer = new SurfaceImageLayer(raptorLayer, new RenderableLayer());
        } else if (LayerGroup.SHAPE_FILES == layerGroup && hasRestorableState)
        {
            globeLayer = takLayerFactory.createShapeFileLayer(raptorLayer);
        } else if (className.equals(MapsforgeMapFileLayer.class.getName()))
        {
            if (hasRestorableState)
            {
                globeLayer = mapsForgeLayerFactory.makeGlobeLayer(raptorLayer, restorableState);
            } else
            {
                logger.error("Mapsforge map layers can not be constructed without state (map file name)");
            }
        } else if (className.equals(CustomMobacTiledImageLayer.class.getName()))
        {
            com.atakmap.map.layer.Layer mobacImageryLayer = takLayerFactory.createMobacImageryLayer(raptorLayer);
            if (mobacImageryLayer != null)
            {
                globeLayer = new ImageryGlobeLayer(raptorLayer, mobacImageryLayer);
                DatasetDescriptor[] descriptors = TakLayerFactory.queryForDatasetDescriptors(mobacImageryLayer);
                if (descriptors.length > 0) globeLayer.setValue(MapDataWrapper.DATASET_DESCRIPTORS, descriptors);
            }
        } else if ((className.equals(WMSTiledImageLayer.class.getName())
                || className.equals(ApplicationWmsTiledImageLayer.class.getName()))
                && hasRestorableState)
        {
            com.atakmap.map.layer.Layer wmsImageryLayer = takLayerFactory.createWmsImageryLayer(raptorLayer);
            if (wmsImageryLayer != null)
            {
                globeLayer = new ImageryGlobeLayer(raptorLayer, wmsImageryLayer);
                DatasetDescriptor[] descriptors = TakLayerFactory.queryForDatasetDescriptors(wmsImageryLayer);
                if (descriptors.length > 0) globeLayer.setValue(MapDataWrapper.DATASET_DESCRIPTORS, descriptors);
            }
        } else if (className.equals(StarsLayer.class.getName()))
        {
            globeLayer = new GlobeLayer(raptorLayer, new StarsLayer());
        } else if (className.equals(AtmosphereLayer.class.getName()))
        {
            globeLayer = new GlobeLayer(raptorLayer, new AtmosphereLayer());
        } else if (className.equals(FrameRateOverlay.class.getName()))
        {
            globeLayer = new GlobeLayer(raptorLayer, constructionLayer != null ? constructionLayer : new FrameRateOverlay());
        } else if (className.equals(BasicTiledImageLayer.class.getName()) && hasRestorableState)
        {
            globeLayer = new WWGlobeLayer(raptorLayer, new BasicTiledImageLayer(restorableState));
            globeLayer.setSharable(true); // These types of layers are sharable
        } else if (className.equals(LocalRasterServerLayer.class.getName()) && hasRestorableState)
        {
            final LocalRasterServerLayer layer = new LocalRasterServerLayer(restorableState);
            setLocalRasterServerLayerParameters(layer);
            globeLayer = new WWGlobeLayer(raptorLayer, layer);
        } else if (className.endsWith(LargeShapefileLayer.class.getName()))
        {
            Layer wwLayer = raptorLayer.getConstructionlayer();
            if (wwLayer == null)
            {
                final LargeShapefileLayer layer = new LargeShapefileLayer();
                layer.setRaptorLayer(raptorLayer);
                layer.restoreState(restorableState);
                wwLayer = layer;
            }
            globeLayer = new WWGlobeLayer(raptorLayer, wwLayer);
        } else if (LayerGroup.SURFACE_STRUCTURES == layerGroup)
        {
            try
            {
                Layer layer = raptorLayer.getConstructionlayer();

                if (layer == null)
                {
                    layer = SurfaceStructureLayer.makeWWLayer(restorableState);
                }
                globeLayer = new SurfaceStructureLayer(raptorLayer, layer);
                if (layer instanceof IRestorable)
                {
                    globeLayer.restoreState(restorableState);
                }
            } catch (Exception e)
            {
                logger.warn("Exception adding shape file layer", e);
            }
        } else if (constructionLayer != null)
        {
            globeLayer = new GlobeLayer(raptorLayer, constructionLayer);
            globeLayer.setSharable(raptorLayer.isSharable());

            if (constructionLayer instanceof IRestorable)
            {
                globeLayer.restoreState(restorableState);
            }
        } else
        {
            Layer layer = raptorLayer.getConstructionlayer();

            if (layer == null)
            {
                try
                {
                    layer = (Layer) WorldWind.createComponent(className);
                } catch (WWRuntimeException e)
                {
                    logger.error("WorldWind threw an exception while trying to create a {} layer", className, e);
                }
            }

            if (layer != null)
            {
                globeLayer = new WWGlobeLayer(raptorLayer, layer);
                globeLayer.setSharable(raptorLayer.isSharable());

                if (layer instanceof IRestorable)
                {
                    globeLayer.restoreState(restorableState);
                }
            }
        }

        return globeLayer;
    }

    private IGlobeLayer getInstalledLayer(IRaptorLayer raptorLayer)
    {
        for (ImportedDataSet importedDataSet : getImportedInstalledDataSets())
        {
            if (importedDataSet.getValue(AVKey.DISPLAY_NAME).equals(raptorLayer.getName()))
            {
                Layer layer = FileStoreUtil.createWWLayer(importedDataSet.getDataConfigElement(), importedDataSet);
                layer.setValue(InstallConstants.DESCRIPTION, importedDataSet);
                layer.setName((String) importedDataSet.getValue(AVKey.DISPLAY_NAME));
                final WWGlobeLayer globeLayer = new WWGlobeLayer(raptorLayer, layer);
                globeLayer.getLayer().setValue(AVKey.DATA_CACHE_NAME, "\\" + raptorLayer.getName());
                globeLayer.setNetworkRetrievalEnabled(false);
                return globeLayer;
            }
        }
        return null;
    }

    private List<? extends ImportedDataSet> getImportedInstalledDataSets()
    {
        List<ImportedDataSet> importedInstalledDataSets = new ArrayList<>();
        Iterable<FileStoreInfo> installedFileStores = FileStoreUtil.getDefaultFileStores();
        final DataConfigurationFilter emptyFilter = new DataConfigurationFilter();
        for (FileStoreInfo fileStore : installedFileStores)
        {
            ArrayList<ImportedDataSet> dataSets = FileStoreUtil.findImportedDataSets(
                    new File(fileStore.getLocation()), emptyFilter);
            fileStore.setDataSets(dataSets);
            FileStoreUtil.setFileStoreParams(fileStore);
            if (dataSets != null)
            {
                importedInstalledDataSets.addAll(dataSets);
            } else
            {
                logger.warn("No datasets found for: ");
            }
        }
        return importedInstalledDataSets;
    }

    private void setLocalRasterServerLayerParameters(LocalRasterServerLayer layer)
    {
        // restorable state isn't working properly with LocalRasterServerLayer, so it is necessary to override
        // its internal RetrieverFactory
        AVList params = (AVList) layer.getValue(AVKey.CONSTRUCTION_PARAMETERS);

        String datasetName = params.getStringValue(AVKey.DATASET_NAME);
        String dataCacheName = params.getStringValue(AVKey.DATA_CACHE_NAME);
        String rasterServerConfigFilename = dataCacheName + File.separator + datasetName + ".RasterServer.xml";
        final URL rasterServerFileURL = layer.getDataFileStore().findFile(rasterServerConfigFilename, false);
        if (WWUtil.isEmpty(rasterServerFileURL))
        {
            String reason = "Configuration " + rasterServerConfigFilename + " not found";
            String msg = "Unable to create raster server instance; " + reason;
            logger.error(msg);
            throw new IllegalStateException(msg);
        }
        final AVList rasterServerParams = params.copy();

        //explicitly set missing values in construction parameters
        rasterServerParams.setValue(AVKey.DISPLAY_NAME, layer.getValue(AVKey.DISPLAY_NAME));
        String suffix = (String) layer.getValue(AVKey.FORMAT_SUFFIX);
        if (suffix == null)
        {
            suffix = ".png";
        }

        rasterServerParams.setValue(AVKey.FORMAT_SUFFIX, suffix);
        if (".jpg".equals(suffix))
        {
            rasterServerParams.setValue(AVKey.IMAGE_FORMAT, "image/jpg");
        } else if (".dds".equals(suffix))
        {
            rasterServerParams.setValue(AVKey.IMAGE_FORMAT, "image/dds");
        } else
        {
            rasterServerParams.setValue(AVKey.IMAGE_FORMAT, "image/png");
        }

        RetrieverFactory retrieverFactory = new RasterRetrieverFactory(rasterServerFileURL, rasterServerParams);

        params.setValue(AVKey.RETRIEVER_FACTORY_LOCAL, retrieverFactory);
        layer.setValue(AVKey.RETRIEVER_FACTORY_LOCAL, retrieverFactory);
    }

    /**
     * Factory to create local raster retriever instances.
     *
     * @since 3.0
     */
    private static class RasterRetrieverFactory implements RetrieverFactory
    {
        private final RasterServer rasterServer;
        private final AVList rasterServerParams;

        RasterRetrieverFactory(URL rasterServerFileURL, AVList rasterServerParams)
        {
            this.rasterServerParams = rasterServerParams;
            rasterServer = new BasicRasterServer(rasterServerFileURL, rasterServerParams);
        }

        @Override
        public Retriever createRetriever(AVList tileParams, RetrievalPostProcessor postProcessor)
        {
            LocalRasterServerRetriever retriever =
                    new LocalRasterServerRetriever(tileParams, rasterServer, postProcessor);

            String[] keysToCopy = {
                    AVKey.DATASET_NAME, AVKey.DISPLAY_NAME, AVKey.FILE_STORE, AVKey.IMAGE_FORMAT, AVKey.FORMAT_SUFFIX
            };

            WWUtil.copyValues(rasterServerParams, retriever, keysToCopy, false);

            return retriever;
        }
    }
}
