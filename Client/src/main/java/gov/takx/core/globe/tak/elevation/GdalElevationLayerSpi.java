package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.io.IOProvider;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.gdal.GdalElevationChunk;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.gdal.GdalLayerInfo;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An {@link ElevationLayerFactorySpi} implementation for GDAL supported terrain data.
 *
 * @since 5.3
 */
class GdalElevationLayerSpi
{
    static final ElevationLayerFactorySpi SPI = new AElevationLayerFactorySpi()
    {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        private final File databaseDescCacheDir = new File(cacheDir, "GdalElevationLayer");

        // Clear out any existing dataset descriptor caches
        {
            if (databaseDescCacheDir.exists()) IOProviderFactory.delete(databaseDescCacheDir, IOProvider.RECURSIVE);
            if (!databaseDescCacheDir.mkdirs()) logger.error("Failed to create directory {}", databaseDescCacheDir);
        }

        @Override
        public ElevationLayerPair create(IRaptorLayer raptorLayer)
        {
            if (raptorLayer.getRestorableState() == null) return null;
            File file = new File(raptorLayer.getRestorableState());
            if (!file.exists()) return null;
            // TODO: TAKX-3441 add zip file/mosaic support
            if (file.getName().toLowerCase(LocaleUtil.getCurrent()).endsWith(".zip")) return null;
            File tempDirectory;
            try
            {
                tempDirectory = Files.createTempDirectory(databaseDescCacheDir.toPath(), null).toFile();
            } catch (IOException e)
            {
                logger.error("Could not create temporary path for dataset descriptors.", e);
                return null;
            }
            Set<DatasetDescriptor> datasetDescriptors = GdalLayerInfo.INSTANCE.create(new DatasetDescriptorSpiArgs(file, tempDirectory));
            if (datasetDescriptors == null || datasetDescriptors.isEmpty()) return null;
            DatasetDescriptor datasetDescriptor = datasetDescriptors.iterator().next();
            ElevationSourceImpl elevationSource = new ElevationSourceImpl(raptorLayer.getName(), () -> {
                ElevationChunk chunk = GdalElevationChunk.create(file.getAbsolutePath(), null, datasetDescriptor.getMinResolution(null), ElevationData.MODEL_TERRAIN, true);
                return chunk != null ? List.of(chunk) : Collections.emptyList();
            });
            GdalElevationModel elevationModel = new GdalElevationModel(datasetDescriptor, elevationSource, raptorLayer.getName(), raptorLayer.getDisplayName());
            return new ElevationLayerPair(elevationSource, elevationModel);
        }

        @Override
        protected boolean probe(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            if (raptorLayer.getRestorableState() == null) return false;
            File file = new File(raptorLayer.getRestorableState());
            if (!file.exists()) return false;
            // TODO: TAKX-3441 add zip file/mosaic support
            if (file.getName().toLowerCase(LocaleUtil.getCurrent()).endsWith(".zip")) return false;
            return ((GdalLayerInfo) GdalLayerInfo.INSTANCE).probe(file, callback);
        }
    };
}
