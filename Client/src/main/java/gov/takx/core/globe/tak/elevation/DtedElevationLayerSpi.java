package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.formats.dted.DtedElevationSource;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An {@link ElevationLayerFactorySpi} implementation for DTED data.
 *
 * @since 5.3
 */
@PseudoBean
class DtedElevationLayerSpi
{
    protected static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final ElevationLayerFactorySpi SPI = new AElevationLayerFactorySpi()
    {
        @Inject
        private ElevationDownloaderManager elevationDownloaderManager;

        @Override
        public int getPriority()
        {
            // We want to handle DTED before GDAL tries to
            return GdalElevationLayerSpi.SPI.getPriority() + 1;
        }

        @Override
        public ElevationLayerPair create(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            if (callback != null && callback.isProbeOnly())
            {
                final boolean success = this.probe(raptorLayer, callback);
                callback.setProbeMatch(success);
                return null;
            }
            int limit = callback != null ? callback.getProbeLimit() : 10;
            return createImpl(raptorLayer, limit);
        }

        @Override
        public ElevationLayerPair create(IRaptorLayer raptorLayer)
        {
            return createImpl(raptorLayer, 10);
        }

        private ElevationLayerPair createImpl(IRaptorLayer raptorLayer, int limit)
        {
            boolean isElevationDownloader = ElevationDownloaderLayer.class.getName().equals(raptorLayer.getClassName());
            if (!isElevationDownloader && raptorLayer.getRestorableState() == null) return null;
            File file = isElevationDownloader ? elevationDownloaderManager.getDtedDir() : searchValidDtedPath(new File(raptorLayer.getRestorableState()), limit);
            if (isElevationDownloader || (file != null && file.exists()))
            {
                ElevationSource elevationSource = DtedElevationSource.create(file.getPath());
                ITakElevationModel elevationModel = new DtedElevationModel(elevationSource, file, raptorLayer.getName(), raptorLayer.getDisplayName());
                return new ElevationLayerPair(elevationSource, elevationModel);
            }
            return null;
        }

        @Override
        protected boolean probe(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            boolean isElevationDownloader = ElevationDownloaderLayer.class.getName().equals(raptorLayer.getClassName());
            if (isElevationDownloader) return true;
            if (raptorLayer.getRestorableState() == null) return false;
            int limit = callback != null ? callback.getProbeLimit() : 10;
            File dtedPath = searchValidDtedPath(new File(raptorLayer.getRestorableState()), limit);
            return dtedPath != null && dtedPath.exists();
        }
    };

    /**
     * Search for a valid DTED root path by searching for valid DTED files from the provided starting path. This will check files and descend into
     * up to three child directories until it finds the first valid DTED file and then returns the relative root directory from that file.
     * This method does not check for the validity of the directory structure.
     *
     * @param file the starting point to look for valid DTED files
     * @param limit the limit to the number of files to check in each directory
     * @return a valid DTED root path, or {@code null} if one could not be found
     */
    @Nullable
    private static File searchValidDtedPath(File file, int limit)
    {
        if (!file.exists()) return null;
        // Find the first valid DTED file
        try (var stream = Files.walk(file.toPath(), 3))
        {
            return stream.map(Path::toFile)
                    .limit(limit)
                    .filter(ElevationLayerManager::isSupportedDtedFile)
                    .findFirst()
                    .map(ElevationLayerManager::getRootDtedPath)
                    .orElse(null);
        } catch (IOException e)
        {
            logger.error("Error walking dted directory.", e);
            return null;
        }
    }
}
