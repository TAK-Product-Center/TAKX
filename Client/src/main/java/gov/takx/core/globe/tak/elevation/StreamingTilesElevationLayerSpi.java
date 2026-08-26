package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.tak.OfflineCacheRegistry;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;

/**
 * An {@link ElevationLayerFactorySpi} implementation for streaming terrain tile data.
 *
 * @since 5.3
 */
class StreamingTilesElevationLayerSpi
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final ElevationLayerFactorySpi SPI = new ElevationLayerFactorySpi();

    @PseudoBean
    private static final class ElevationLayerFactorySpi extends AElevationLayerFactorySpi
    {
        @Inject
        private OfflineCacheRegistry offlineCacheRegistry;

        @Override
        public ElevationLayerPair create(IRaptorLayer raptorLayer)
        {
            final String configuration = readConfigurationFromRestorableState(raptorLayer);
            if (configuration == null) return null;

            final var tiles = parseTiles(configuration);
            if (tiles == null) return null;

            final File file;
            try
            {
                file = createTemporaryConfigFile(configuration);
            } catch (IOException e)
            {
                logger.error("Could not create temporary streaming tiles elevation configuration file.", e);
                return null;
            }

            // Check for cache conflicts
            final File cacheFile = new File(cacheDir, tiles.name + ".sqlite");
            offlineCacheRegistry.registerCacheFile(raptorLayer.getName(), cacheFile); // Will throw an exception if already in use

            final var client = TileClientFactory.create(file.getAbsolutePath(), cacheFile.getAbsolutePath(), null);
            if (client == null) return null;

            var elevationSource = new TileMatrixElevationSource(client, tiles.mimeType, false);
            var elevationModel = TileMatrixElevationModel.create(elevationSource, raptorLayer.getName(), raptorLayer.getDisplayName());
            return new ElevationLayerPair(elevationSource, elevationModel);
        }

        @Override
        protected boolean probe(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            final String configuration = readConfigurationFromRestorableState(raptorLayer);
            return configuration != null && parseTiles(configuration) != null;
        }

        /**
         * When invoked from a file being dropped on the globe, the restorable state will be a file path.
         * But, this class wants the restorable state to be the actual JSON text.
         * 
         * @param raptorLayer Layer to process
         * @return JSON configuration or {@code null}
         * @since 5.5
         */
        @Nullable
        private String readConfigurationFromRestorableState(@Nonnull IRaptorLayer raptorLayer)
        {
            final String restorableState = raptorLayer.getRestorableState();
            logger.debug("create {}, state {}", raptorLayer, restorableState);

            if (restorableState == null) return null;

            // If it contains braces, assume it is the JSON content we want
            if (restorableState.contains("{") && restorableState.contains("}"))
            {
                return restorableState;
            } else
            {
                // Else read the content from the file, if it exists.  Also, the JSON configuration is not very
                // large, typically < 1K, so if the file is large (arbitrarily 4K) don't bother trying to process it.
                final File file = new File(restorableState);
                if (!file.exists() || file.length() > 4096) return null;

                try
                {
                    return Files.readString(file.toPath());
                } catch (MalformedInputException e)
                {
                    return null; // This happens when a file containing binary data can't be read as a string
                } catch (IOException e)
                {
                    logger.error("Could not read config file: {}", restorableState, e);
                    return null;
                }
            }
        }

        private File createTemporaryConfigFile(String data) throws IOException
        {
            File config = IOProviderFactory.createTempFile("config", ".xml", null);
            config.deleteOnExit();
            Files.writeString(config.toPath(), data);
            return config;
        }

        /**
         * @return The parsed tiles or {@code null} if parsing fails or the content type is not {@code terrain}
         * @since 5.5
         */
        @Nullable
        private StreamingTiles parseTiles(@Nonnull String configuration)
        {
            final var tiles = StreamingTiles.parse(configuration);
            return tiles != null && "terrain".equals(tiles.content) ? tiles : null;
        }
    }
}
