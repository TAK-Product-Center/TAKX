package gov.takx.core.globe.tak.elevation.wcs;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileProxy;
import gov.takx.core.globe.tak.elevation.GdalElevationChunkSpi;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * A factory for creating an {@link TileMatrixElevationSource elevation source} for a WCS server.
 *
 * @since 5.3
 */
public final class WcsElevationSource
{
    private WcsElevationSource()
    {
    }

    /**
     * @param coverageOfferingName Cover offering name, used as the base name of the cache file
     * @param cacheDir Root directory holding elevation data caches
     * @return File referring to the cache (DB) location for retrieved data
     * @since 5.5
     */
    @Nonnull
    public static File getCacheFile(@Nonnull String coverageOfferingName, @Nonnull File cacheDir)
    {
        return new File(cacheDir, FileSystemUtils.sanitizeFilename(coverageOfferingName) + ".sqlite");
    }

    /**
     * Create an {@link TileMatrixElevationSource elevation source} for the provided {@link WcsService} using the provided coverage and coverage offering.
     * @param wcsService a WCS service
     * @param coverageName the name of the coverage
     * @param coverageOfferingName the name of the coverage offering
     * @param cacheDir a cache directory, or null to not cache downloaded data
     * @return the elevation source
     */
    public static TileMatrixElevationSource create(@Nonnull WcsService wcsService, @Nonnull String coverageName,
                                                   @Nonnull String coverageOfferingName, @Nonnull File cacheDir)
    {
        TileClient tileClient = new WcsTileClient(wcsService, coverageName, coverageOfferingName);

        String cachePath = getCacheFile(coverageOfferingName, cacheDir).getPath();
        TileContainer cache = TileContainerFactory.openOrCreateCompatibleContainer(cachePath, tileClient, null);
        tileClient = new TileProxy(tileClient, cache);

        return new TileMatrixElevationSource(tileClient, new GdalElevationChunkSpi(tileClient.getName()), false);
    }
}
