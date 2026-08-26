package gov.takx.core.view.gui.maps.exporters.dataset;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.core.globe.tak.mapsetbuilder.DownloadAndCacheService;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Runnable used to update the estimated size of an area within a layer. The estimated size is calculated by multiplying
 * the estimated number of missing tiles in each layer by the estimated tile size for that layer.
 *
 * @since 1.43
 */
class MapDataQueries
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static File TEMP_DIR;

    @Nullable
    private static synchronized File getEmptyDir()
    {
        if (TEMP_DIR == null)
        {
            try
            {
                TEMP_DIR = IOProviderFactory.createTempFile("tmpdted", null, null);
                TEMP_DIR.deleteOnExit();
            } catch (IOException e)
            {
                logger.error("Could not create temp directory.", e);
            }
        }
        return TEMP_DIR;
    }

    static class UpdateEstimatedSizeByBoundary extends NameFixingRunnable
    {
        private static final int ESTIMATED_DTED_FILE_SIZE = 36_000;
        private final MapDataWrapper wrapper;
        private final Boundary boundary;

        /**
         * Calculate the estimated missing data size using a specified boundary.
         *
         * @param wrapper  The wrapper to update.
         * @param boundary The user defined area.
         */
        UpdateEstimatedSizeByBoundary(MapDataWrapper wrapper, Boundary boundary)
        {
            super("Map Set Builder - Update Estimated Size");
            this.wrapper = wrapper;
            this.boundary = boundary;
        }

        @Override
        protected void doRun()
        {
            ILocation[] geometry = boundary.getVertices().stream().map(GeoPoint::new).toArray(ILocation[]::new);
            if (geometry.length == 0) return;
            if (boundary.isClosed()) geometry = Boundary.closeVertices(geometry);
            long totalTileCount;
            int minLevel;
            int maxLevel;
            try (MapDataWrapper.TileClientWrapper tileClientWrapper = wrapper.getTileClient())
            {
                if (tileClientWrapper != null)
                {
                    TileMatrix.ZoomLevel[] zoomLevels = tileClientWrapper.tileClient().getZoomLevel();
                    double minRes = zoomLevels[0].resolution;
                    double maxRes = zoomLevels[zoomLevels.length - 1].resolution;
                    totalTileCount = DownloadAndCacheService.estimateTileCount(tileClientWrapper.tileClient(), minRes, maxRes, geometry);
                    minLevel = OSMUtils.mapnikTileLevel(minRes);
                    maxLevel = OSMUtils.mapnikTileLevel(maxRes);
                } else if (wrapper.getElevationSource() != null && "DTED".equals(wrapper.getElevationSource().getName()))
                {
                    GeoBounds.Builder builder = new GeoBounds.Builder();
                    for (ILocation location : geometry) builder.add(location.getLatitude(), location.getLongitude());
                    // This method requires an existing DTED path - use an empty path so we get a full tile count
                    File emptyDir = getEmptyDir();
                    totalTileCount = emptyDir != null ? ElevationSourceUtils.getMissingDtedFileCount(emptyDir, new GeoBounds[]{builder.build()}) : 0;
                    // DTED 0 resolution falls between zoom levels 8 and 9
                    minLevel = 8;
                    maxLevel = 9;
                    wrapper.setTileDataSize(ESTIMATED_DTED_FILE_SIZE);
                } else
                {
                    return;
                }
            }
            if (totalTileCount == 0) return;

            wrapper.setBoundary(boundary);
            wrapper.setMinAvailableResolutionLevel(minLevel);
            wrapper.setMaxAvailableResolutionLevel(maxLevel);
            wrapper.setMinAcceptableLevel(minLevel);
            wrapper.setMaxAcceptableLevel(maxLevel);
            wrapper.setTotalTileCount(totalTileCount);
            wrapper.setTotalAcceptableTileCount(totalTileCount);
        }
    }

    static class UpdateRangeEstimate extends NameFixingRunnable
    {
        private final MapDataWrapper wrapper;

        UpdateRangeEstimate(MapDataWrapper wrapper)
        {
            super("Map Set Builder - Update Range Estimate");
            this.wrapper = wrapper;
        }

        @Override
        protected void doRun()
        {
            Boundary boundary = wrapper.getBoundary();
            if (boundary == null) return;

            double minRes = OSMUtils.mapnikTileResolution(wrapper.getMinAcceptableLevel());
            double maxRes = OSMUtils.mapnikTileResolution(wrapper.getMaxAcceptableLevel());
            ILocation[] geometry = boundary.getVertices().stream().map(GeoPoint::new).toArray(ILocation[]::new);
            if (boundary.isClosed()) geometry = Boundary.closeVertices(geometry);
            long totalTileCount = 0;
            try (MapDataWrapper.TileClientWrapper tileClientWrapper = wrapper.getTileClient())
            {
                if (tileClientWrapper != null)
                {
                    totalTileCount = DownloadAndCacheService.estimateTileCount(tileClientWrapper.tileClient(), minRes, maxRes, geometry);
                }
            }
            wrapper.setTotalAcceptableTileCount(totalTileCount);
            wrapper.setMinAcceptableResolutionLevel(minRes);
            wrapper.setMaxAcceptableResolutionLevel(maxRes);
        }
    }
}
