package gov.takx.core.globe;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSource.Content.ContentPointer;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.datastore.FeatureSetDatabase2;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterDataStore.DatasetQueryParameters;
import com.atakmap.map.layer.raster.RuntimeRasterDataStore;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.tak.OfflineCacheRegistry;
import gov.takx.core.globe.utils.ImageryLayerConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class to create TAK layers from IRaptorLayers.
 *
 * @since 3.4
 */
@ApplicationScoped
public class TakLayerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static Map<String, String> WMS_FORMAT_MIME_TO_TILE_FORMAT = Map.of(
            "image/jpeg", "JPG",
            "image/png", "PNG",
            "image/dds", "PNG");

    private final IVirtualFileSystemManager vfsManager;
    private final ICoreExecutorService executorService;
    private final OfflineCacheRegistry offlineCacheRegistry;

    private final Object shpFileParseLock = new Object();

    @Inject
    public TakLayerFactory(@VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager vfsManager, ICoreExecutorService executorService, OfflineCacheRegistry offlineCacheRegistry)
    {
        this.vfsManager = vfsManager;
        this.executorService = executorService;
        this.offlineCacheRegistry = offlineCacheRegistry;
    }

    /**
     * Create a {@link Layer} to render the MOBAC imagery layer represented by raptorLayer.
     * @param raptorLayer the raptor layer
     * @return a layer to render the content, or null
     * @throws IllegalArgumentException if the layer does not have restorable state
     * @since 3.4
     */
    @Nullable
    public Layer createMobacImageryLayer(IRaptorLayer raptorLayer)
    {
        String restorableState = raptorLayer.getRestorableState();
        if (StringUtils.isEmpty(restorableState))
        {
            String message = "Restorable state is null for layer " + raptorLayer.getName();
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        File config;
        try
        {
            config = createTemporaryConfigFile(restorableState);
        } catch (IOException e)
        {
            logger.error("Error creating MOBAC imagery layer.", e);
            return null;
        }

        return createRasterLayer(config, raptorLayer.getName());
    }

    /**
     * Create a {@link Layer} to render the WMS imagery layer represented by raptorLayer.
     * @param raptorLayer the raptor layer
     * @return a layer to render the content, or null
     * @throws IllegalArgumentException if the layer does not have restorable state
     * @since 3.4
     */
    @Nullable
    public Layer createWmsImageryLayer(IRaptorLayer raptorLayer)
    {
        String restorableState = raptorLayer.getRestorableState();
        if (StringUtils.isEmpty(restorableState))
        {
            String message = "Restorable state is null for layer " + raptorLayer.getName();
            logger.error(message);
            throw new IllegalArgumentException(message);
        }
        RestorableSupport rs = RestorableSupport.parse(restorableState);
        AVList avList = new AVListImpl();
        for (RestorableSupport.StateObject stateObject : rs.getAllStateObjects((RestorableSupport.StateObject) null))
        {
            avList.setValue(stateObject.getName(), stateObject.getValue());
        }
        ImageryLayerConfiguration imageryLayerConfiguration = new ImageryLayerConfiguration(avList);
        try
        {
            return createWmsImageryLayer(imageryLayerConfiguration);
        } catch (IOException e)
        {
            logger.error("Error creating WMS imagery layer.", e);
            return null;
        }
    }

    /**
     * Create a {@link GlobeLayer} to render the Shapefile layer represented by raptorLayer.
     * @param raptorLayer the raptor layer
     * @return a globe layer to render the content, or null
     * @since 3.4
     */
    @Nullable
    public GlobeLayer createShapeFileLayer(IRaptorLayer raptorLayer)
    {
        String shapeFilePath = raptorLayer.getRestorableState();
        IVirtualFileSystem fileSystem = vfsManager.getVirtualFileSystem("ShapeFiles");
        File shapeFileSource = new File(fileSystem.getRootDir(), shapeFilePath);

        if (!shapeFileSource.exists())
        { // shape file not found in the vfs
            shapeFileSource = new File(shapeFilePath);
            if (!shapeFileSource.exists())
            { // shape file not found in the resources/ShapesFiles directory from default globe layers xml
                logger.error("Shape file not found in virtual file system or resources directory: {}", shapeFilePath);
                return null;
            }
        }

        var dataStore = new FeatureSetDatabase2(null);
        var featureLayer = new FeatureLayer3("FeatureLayer3", dataStore);
        var globeLayer = new GlobeLayer(raptorLayer, featureLayer);

        final File finalShapeFileSource = shapeFileSource;

        executorService.execute(() -> {
            Thread.currentThread().setName("Shapefile Loader: " + finalShapeFileSource.getName());
            final Envelope.Builder envBuilder = new Envelope.Builder();
            FeatureDataSource.Content content;

            try
            {
                // There is a bug in the underlying SHP file parsing library causing it to fail when multiple threads
                // try to parse the same file.  So, we lock this section to avoid that issue.
                synchronized (shpFileParseLock)
                {
                    content = FeatureDataSourceContentFactory.parse(finalShapeFileSource, "OGR");
                    if (content != null)
                    {
                        try
                        {
                            dataStore.acquireModifyLock(true);
                            while (content.moveToNext(ContentPointer.FEATURE_SET))
                            {
                                long fsid = dataStore.insertFeatureSet(new FeatureSet(content.getProvider(), content.getType(), content.getFeatureSetName(), content.getMinResolution(), content.getMaxResolution()));
                                try (ShapefileFeatureCursor cursor = new ShapefileFeatureCursor(content, fsid, envBuilder))
                                {
                                    dataStore.insertFeatures(cursor);
                                }
                            }
                        } finally
                        {
                            dataStore.releaseModifyLock();
                            content.close();
                        }
                    } else
                    {
                        logger.error("Failed to parse SHP file: {}", shapeFilePath);
                        return;
                    }
                }
            } catch (InterruptedException | DataStoreException e)
            {
                logger.error("Could not create shapefile from {}", finalShapeFileSource, e);
                return;
            }
            Envelope envelope = envBuilder.build();
            if (envelope != null) globeLayer.setSector(envelope);
        });
        return globeLayer;
    }

    /**
     * Query a layer's associated datastore for its dataset descriptors.
     *
     * @param layer the layer
     * @return an array of dataset descriptors
     * @since 5.0
     */
    @Nonnull
    public static DatasetDescriptor[] queryForDatasetDescriptors(Layer layer)
    {
        ArrayList<DatasetDescriptor> descriptors = new ArrayList<>();
        if (layer instanceof AbstractDataStoreRasterLayer2 dataStoreRasterLayer)
        {
            DatasetQueryParameters params = new DatasetQueryParameters();
            dataStoreRasterLayer.filterQueryParams(params);
            try (RasterDataStore.DatasetDescriptorCursor cursor = dataStoreRasterLayer.getDataStore().queryDatasets(params))
            {
                while (cursor.moveToNext()) descriptors.add(cursor.get());
            }
        }
        return descriptors.toArray(DatasetDescriptor[]::new);
    }

    public Layer createWmsImageryLayer(ImageryLayerConfiguration config) throws IOException
    {
        String displayName = Objects.requireNonNull(config.getDisplayName());
        int numLevelsCount = config.getNumLevels();
        int numLevelsEmpty = config.getNumEmptyLevels();
        String imageFormat = config.getImageFormat();
        String tileType = WMS_FORMAT_MIME_TO_TILE_FORMAT.get(imageFormat);
        String version = config.getWMSVersion();
        String layerNames = String.join(",", config.getLayerNames());
        String mapUrl = config.getMapURL() != null ? config.getMapURL().toString() : "";
        if ("".equals(mapUrl)) mapUrl = config.getService();
        return createWmsImageryLayer(displayName, numLevelsEmpty, numLevelsCount, tileType, version, layerNames, mapUrl);
    }

    private Layer createWmsImageryLayer(String name, int minZoom, int maxZoom, String tileType, String version, String layers, String url) throws IOException
    {
        if (url == null) return null;
        url = url.replace("&", "&amp;");
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<customWmsMapSource>" +
                "    <name>" + name + "</name>" +
                "    <minZoom>" + minZoom + "</minZoom>" +
                "    <maxZoom>" + maxZoom + "</maxZoom>" +
                "    <tileType>" + tileType + "</tileType>" +
                "    <version>" + version + "</version>" +
                "    <layers>" + layers + "</layers>" +
                "    <url>" + url + "</url>" +
                "    <backgroundColor>#000000</backgroundColor>" +
                "</customWmsMapSource>";

        return createRasterLayer(createTemporaryConfigFile(configXml), name);
    }

    /**
     * Create a {@link Layer} to render the imagery layer described by the dataset descriptor in {@code source}.
     * @param source the dataset descriptor source
     * @param name the name of the imagery layer
     * @return a layer to render the content, or null
     * @since 5.1
     */
    @Nullable
    public Layer createDatasetDescriptorLayer(String source, String name)
    {
        try
        {
            return createRasterLayer(createTemporaryConfigFile(source), name);
        } catch (IOException e)
        {
            logger.error("Error creating MOBAC imagery layer.", e);
            return null;
        }
    }

    private static File createTemporaryConfigFile(String data) throws IOException
    {
        File config = IOProviderFactory.createTempFile("config", ".xml", null);
        config.deleteOnExit();
        Files.writeString(config.toPath(), data);
        return config;
    }

    private DatasetRasterLayer2 createRasterLayer(File config, String name)
    {
        var rasterDataStore = new RuntimeRasterDataStore();

        // GDAL gets a little noisy here.
        org.gdal.gdal.gdal.PushErrorHandler();
        var descriptors = DatasetDescriptorFactory2.create(config, null, null, null);
        org.gdal.gdal.gdal.PopErrorHandler();

        if (descriptors != null)
        {
            for (var desc : descriptors)
            {
                // Check for cache conflicts
                final String offlineCache = desc.getExtraData("offlineCache");
                logger.debug("Register '{}', cache {}", name, offlineCache);
                if (offlineCache != null) offlineCacheRegistry.registerCacheFile(name, new File(offlineCache)); // Will throw an exception if already in use

                rasterDataStore.add(desc);
            }
        }

        // It is necessary to have an order set to call getSelectionOptions()
        var filter = new DatasetQueryParameters();
        filter.order = Collections.singletonList(DatasetQueryParameters.GSD.INSTANCE);

        return new DatasetRasterLayer2(name, rasterDataStore, filter, 1);
    }
}
