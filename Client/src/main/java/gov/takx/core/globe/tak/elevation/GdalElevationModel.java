package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.LevelSet;

/**
 * An implementation of {@link gov.nasa.worldwind.globes.ElevationModel} that queries a GDAL {@link ElevationSource} for elevation data.
 *
 * @since 5.3
 */
public class GdalElevationModel extends ATakElevationModel
{
    // Represent a reasonably small grid of 1000x1000 tiles.
    private static final int TILE_GRID_SIZE = 1000;

    private final Sector sector;
    private final double bestResolution;
    private final LevelSet levelSet;

    public GdalElevationModel(DatasetDescriptor datasetDescriptor, ElevationSource elevationSource, String elevationSetName, String displayName)
    {
        super(elevationSource, displayName);
        Envelope boundingBox = datasetDescriptor.getMinimumBoundingBox();
        sector = Sector.fromDegrees(boundingBox.minY, boundingBox.maxY, boundingBox.minX, boundingBox.maxX);
        bestResolution = datasetDescriptor.getMaxResolution(null) / Ellipsoid.WGS_84.getSemiMajorAxis();

        AVList avList = new AVListImpl();
        avList.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(sector.getDeltaLat(), sector.getDeltaLon()));
        avList.setValue(AVKey.SECTOR, sector);
        avList.setValue(AVKey.NUM_LEVELS, 1);
        avList.setValue(AVKey.TILE_WIDTH, TILE_GRID_SIZE);
        avList.setValue(AVKey.TILE_HEIGHT, TILE_GRID_SIZE);
        avList.setValue(AVKey.FORMAT_SUFFIX, ".bil");
        avList.setValue(AVKey.DATA_CACHE_NAME, elevationSetName);
        avList.setValue(AVKey.DATASET_NAME, elevationSetName);
        avList.setValue(AVKey.DISPLAY_NAME, getName());
        levelSet = new LevelSet(avList);
    }

    @Override
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public LevelSet getLevels()
    {
        return levelSet;
    }

    @Override
    public double getBestResolution(Sector sector)
    {
        return bestResolution;
    }
}
