package gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationManager.HeightmapStrategy;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationAttributes;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationRaster;
import gov.takx.api.terrain.experimental.proposed.tile.ElevationTile;
import gov.takx.api.terrain.experimental.proposed.tile.IElevationRaster;
import gov.takx.api.terrain.experimental.proposed.tile.TileCollection;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.TerrainSurfaceImage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class TerrainLookUpTasks
{
    private static final int SIZE = 1024;
    private final ITerrainDiscoveryController controller;
    private final ITakElevationModel elevationModel;
    private final IRaptorLayer elevationLayer;
    private final TileCollection tileCollection;
    private final SurfaceMultiSector negativeSpace;
    private final Sector originalSector;
    private final Sector sector;
    private final SurfaceMultiSector surfaceShape;
    private final TerrainSurfaceImage surfaceImage;
    private final int imageWidth;
    private final int imageHeight;
    private final boolean imageCalculationsEnabled;

    public TerrainLookUpTasks(ITerrainDiscoveryController controller,
                              IRaptorLayer elevationLayer,
                              TileCollection tileCollection,
                              SurfaceMultiSector negativeSpace,
                              Sector sector,
                              boolean imageCalculationsEnabled)
    {
        this.controller = controller;
        this.tileCollection = tileCollection;
        this.negativeSpace = negativeSpace;
        this.originalSector = sector;
        this.imageCalculationsEnabled = imageCalculationsEnabled;
        this.elevationLayer = elevationLayer;
        ElevationAttributes attributes = tileCollection.getAttributes();
        elevationModel = attributes.getElevationModel();
        this.sector = sector.intersection(elevationModel.getSector());
        surfaceShape = new SurfaceMultiSector(this.sector);
        surfaceShape.setBoundingSector(this.sector);
        surfaceImage = new TerrainSurfaceImage();
        surfaceImage.setSector(this.sector);
        imageWidth = imageHeight = SIZE;

        ShapeAttributes shapeAttributes = surfaceShape.getAttributes();
        shapeAttributes.setInteriorOpacity(0.5d);
        surfaceShape.getAttributes().setOutlineOpacity(0.6d);
        surfaceShape.setOutlineVisible(false);
        setSurfaceColor(controller.getModel().getElevationLayerColor(elevationLayer));
    }

    public List<TerrainDiscoveryTask> createTasks()
    {
        List<TerrainDiscoveryTask> tasks = new ArrayList<>();

        tasks.add(new SurfaceShapeTask());
        tasks.add(new AvailabilityTask());

        if (Thread.currentThread().isInterrupted())
        {
            return null;
        }

        if (imageCalculationsEnabled)
        {
            tasks.add(new CreateHeightmapTask());
        }
        return tasks;
    }

    protected void setSurfaceColor(Color surfaceColor)
    {
        surfaceShape.setSurfaceColor(surfaceColor);
        surfaceImage.setColor(surfaceColor);
    }

    private class CreateHeightmapTask extends TerrainDiscoveryTask
    {
        @Override
        protected void run() throws Exception
        {
            if (isCanceled()) return;

            float[] values = new float[imageWidth * imageHeight];
            double[] valuesD = new double[imageWidth * imageHeight];
            ElevationManager.HeightmapParams params = new ElevationManager.HeightmapParams();
            params.bounds = new Envelope(sector.getMinLongitude().degrees, sector.getMinLatitude().degrees, 0,
                    sector.getMaxLongitude().degrees, sector.getMaxLatitude().degrees, 0);
            params.numPostsLat = imageHeight;
            params.numPostsLng = imageWidth;
            ElevationManager.createHeightmap(valuesD, elevationModel.getElevationSource(), Double.MIN_VALUE, params, HeightmapStrategy.HighestResolution);
            for (int a = 0; a < values.length; a++) values[a] = (float) valuesD[a];

            IElevationRaster raster = new ElevationRaster(
                    sector,
                    values,
                    imageWidth,
                    imageHeight,
                    0);
            surfaceImage.setRaster(raster);
            controller.setSurfaceImage(elevationLayer, surfaceImage);
        }

        @Override
        public String getDisplayName()
        {
            return "Creating Heightmap...";
        }
    }

    // TODO TAKX-2693: replace with TAK solution
    private class SurfaceShapeTask extends TerrainDiscoveryTask
    {
        @Override
        protected void run() throws Exception
        {
            if (isCanceled())
            {
                return;
            }
            List<ElevationTile> highestResolutionTiles = tileCollection.getHighestResolutionTiles(sector);
            for (ElevationTile tile : highestResolutionTiles)
            {
                Sector s = tile.getSector().intersection(sector);
                if (s != null)
                {
                    surfaceShape.addSector(s);
                    negativeSpace.addSector(s);
                }
            }

            controller.setSurfaceShape(elevationLayer, surfaceShape);
        }

        @Override
        public String getDisplayName()
        {
            return "Calculating Terrain Coverage...";
        }
    }

    private class AvailabilityTask extends TerrainDiscoveryTask
    {
        @Override
        protected void run() throws Exception
        {
            if (isCanceled())
            {
                return;
            }

            // Create gridSize x gridSize grid of locations inside sector
            final int gridSize = 1000;
            final double minLat = originalSector.getMinLatitude().degrees;
            final double minLon = originalSector.getMinLongitude().degrees;
            final double diffLat = originalSector.getDeltaLatDegrees();
            final double diffLon = originalSector.getDeltaLonDegrees();
            final double max = gridSize - 1d;
            final double total = gridSize * gridSize;
            int count = 0;
            for (int y = 0; y < gridSize; y++)
            {
                if (isCanceled()) return;
                double lat = minLat + diffLat * (y / max);

                for (int x = 0; x < gridSize; x++)
                {
                    double lon = minLon + diffLon * (x / max);
                    if (tileCollection.hasHighestResolutionLocation(lat, lon))
                    {
                        count++;
                    }
                }
            }

            controller.setElevationLayerAvailability(elevationLayer, count / total);
            controller.updateElevationLayerCoverage(elevationLayer);
        }

        @Override
        public String getDisplayName()
        {
            return "Calculating Availability...";
        }
    }
}
