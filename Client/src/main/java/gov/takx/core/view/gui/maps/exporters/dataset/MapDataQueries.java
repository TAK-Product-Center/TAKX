package gov.takx.core.view.gui.maps.exporters.dataset;

import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.core.globe.tak.mapsetbuilder.DownloadAndCacheService;

/**
 * Runnable used to update the estimated size of an area within a layer. The estimated size is calculated by multiplying
 * the estimated number of missing tiles in each layer by the estimated tile size for that layer.
 *
 * @since 1.43
 */
class MapDataQueries
{
    static class UpdateEstimatedSizeByBoundary extends NameFixingRunnable
    {
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
            if (boundary.isClosed()) geometry = Boundary.closeVertices(geometry);
            long totalTileCount = 0;
            double minRes = Double.MAX_VALUE;
            double maxRes = 0;
            try (MapDataWrapper.TileClientWrapper tileClientWrapper = wrapper.getTileClient())
            {
                if (tileClientWrapper != null)
                {
                    TileMatrix.ZoomLevel[] zoomLevels = tileClientWrapper.tileClient().getZoomLevel();
                    double localMinRes = zoomLevels[0].resolution;
                    double localMaxRes = zoomLevels[zoomLevels.length - 1].resolution;
                    totalTileCount = DownloadAndCacheService.estimateTileCount(tileClientWrapper.tileClient(), localMinRes, localMaxRes, geometry);
                    minRes = Math.min(minRes, localMinRes);
                    maxRes = Math.max(maxRes, localMaxRes);
                }
            }
            if (totalTileCount == 0) return;

            wrapper.setBoundary(boundary);
            int minLevel = OSMUtils.mapnikTileLevel(minRes);
            int maxLevel = OSMUtils.mapnikTileLevel(maxRes);
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
