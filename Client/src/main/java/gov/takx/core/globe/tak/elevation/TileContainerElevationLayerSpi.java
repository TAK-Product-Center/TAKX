package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.formats.mapbox.TerrainRGBChunkSpi;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;

/**
 * An {@link ElevationLayerFactorySpi} implementation for elevation data in a tile container.
 *
 * @since 5.6
 */
class TileContainerElevationLayerSpi
{
    static final ElevationLayerFactorySpi SPI = new AElevationLayerFactorySpi()
    {
        @Override
        public ElevationLayerPair create(IRaptorLayer raptorLayer)
        {
            if (raptorLayer.getRestorableState() == null) return null;
            File file = new File(raptorLayer.getRestorableState());
            if (!file.exists()) return null;
            TileContainer container = TileContainerFactory.open(file.getAbsolutePath(), true, null);
            if (container == null) return null;
            TileMatrixElevationSource elevationSource = new TileMatrixElevationSource(container, new TerrainRGBChunkSpi(), false);
            return new ElevationLayerPair(elevationSource, TileMatrixElevationModel.create(elevationSource, raptorLayer.getName(), raptorLayer.getDisplayName()));
        }

        @Override
        protected boolean probe(@Nonnull IRaptorLayer raptorLayer, @Nullable Callback callback)
        {
            if (raptorLayer.getRestorableState() == null) return false;
            File file = new File(raptorLayer.getRestorableState());
            if (!file.exists()) return false;
            TileContainer container = TileContainerFactory.open(file.getAbsolutePath(), true, null);
            if (container == null) return false;
            container.dispose();
            return true;
        }
    };
}
