package gov.takx.core.globe.tak.elevation;

import com.atakmap.coremap.maps.coords.Ellipsoid;
import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.LevelSet;

/**
 * An implementation of {@link ATakElevationModel} that queries an {@link TileMatrixElevationSource} for elevation data.
 *
 * @since 5.3
 */
public class TileMatrixElevationModel extends ATakElevationModel
{
    private final Sector sector = Sector.FULL_SPHERE;
    private final LevelSet levelSet;
    private final TileMatrix.ZoomLevel[] zoomLevels;

    /**
     * Create a {@link TileMatrixElevationModel}.
     * @param elevationSource the elevation source
     * @param elevationSetName a unique elevation source name
     * @param displayName a display name for the model
     * @return the TileMatrixElevationModel, or {@code null} if the model can not be created
     */
    public static TileMatrixElevationModel create(TileMatrixElevationSource elevationSource, String elevationSetName, String displayName)
    {
        TileMatrix tileMatrix = elevationSource.getControl(TileMatrix.class);
        if (tileMatrix == null) return null;
        TileMatrix.ZoomLevel[] zoomLevels = tileMatrix.getZoomLevel();
        if (zoomLevels.length == 0) return null;
        return new TileMatrixElevationModel(elevationSource, elevationSetName, displayName, zoomLevels);
    }

    private TileMatrixElevationModel(TileMatrixElevationSource elevationSource, String elevationSetName, String displayName, TileMatrix.ZoomLevel[] zoomLevels)
    {
        super(elevationSource, displayName);
        this.zoomLevels = zoomLevels;

        AVList avList = new AVListImpl();
        avList.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(sector.getDeltaLat(), sector.getDeltaLon()));
        avList.setValue(AVKey.SECTOR, sector);
        avList.setValue(AVKey.NUM_LEVELS, zoomLevels.length);
        avList.setValue(AVKey.TILE_WIDTH, zoomLevels[0].tileWidth);
        avList.setValue(AVKey.TILE_HEIGHT, zoomLevels[0].tileHeight);
        avList.setValue(AVKey.FORMAT_SUFFIX, ".bil");
        avList.setValue(AVKey.DATA_CACHE_NAME, elevationSetName);
        avList.setValue(AVKey.DATASET_NAME, elevationSetName);
        avList.setValue(AVKey.DISPLAY_NAME, getName());

        levelSet = new LevelSet(avList);
    }

    @Override
    public double getBestResolution(Sector sector)
    {
        // get top resolution
        return zoomLevels[zoomLevels.length - 1].resolution / Ellipsoid.WGS_84.getSemiMajorAxis();
    }

    @Override
    public LevelSet getLevels()
    {
        return levelSet;
    }

    @Override
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public TileMatrixElevationSource getElevationSource()
    {
        return (TileMatrixElevationSource) super.getElevationSource();
    }
}