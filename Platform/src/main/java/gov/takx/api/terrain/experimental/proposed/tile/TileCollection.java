package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileKey;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.platform.IProgressCallback;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
@Experimental
@SuppressWarnings("unchecked")
public class TileCollection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ElevationAttributes attributes;
    private final List<Set<ElevationTile>> tileSets;
    private final Map<Long, ElevationTile> tileMap;
    private final AtomicBoolean enabled;

    public TileCollection(ElevationAttributes attributes)
    {
        this.attributes = attributes;
        int levels = attributes.getLevels();
        tileSets = new ArrayList<>();
        for (int i = 0; i < levels; i++)
        {
            tileSets.add(Collections.newSetFromMap(new ConcurrentHashMap<ElevationTile, Boolean>()));
        }
        tileMap = new ConcurrentHashMap<>();
        enabled = new AtomicBoolean(true);
    }

    protected TileCollection(Collection<ElevationTile> tiles, ElevationAttributes attributes)
    {
        this(attributes);
        for (ElevationTile tile : tiles)
        {
            addTile(tile);
        }
    }

    public void update(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return;
        }

        int highest = getAttributes().getHighestLevel(latitude, longitude);
        for (int level = highest; level >= 0; level--)
        {
            loadAndGetTile(latitude, longitude, level, null);
        }
    }

    /**
     * Update this tile collection with any valid local tiles that have not already been added.
     */
    public void update(Sector sector)
    {
        TerrainDataUtils.updateTileCollection(sector, this);
    }

    public ElevationAttributes getAttributes()
    {
        return attributes;
    }

    public double getHighestResolution(double latitude, double longitude)
    {
        return attributes.getHighestResolution(latitude, longitude);
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled.set(enabled);
    }

    public boolean isEnabled()
    {
        return enabled.get();
    }

    public Sector intersection(Sector sector)
    {
        return attributes.getElevationModel().getSector().intersection(sector);
    }

    public void flushRasters()
    {
        for (ElevationTile tile : getAllTiles())
        {
            tile.flushValues();
        }
    }

    public void flushAll()
    {
        for (Set<ElevationTile> set : tileSets)
        {
            set.clear();
        }
        tileMap.clear();
    }

    public List<ElevationTile> getTiles(Sector sector, int level)
    {
        List<ElevationTile> tiles = new ArrayList<>();
        if (sector == null)
        {
            return tiles;
        }

        LatLon origin = attributes.getTileOrigin();
        LatLon delta = attributes.getLevelDelta(level);
        int minCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        int maxCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        int minRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        int maxRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());

        for (int row = minRow; row <= maxRow; row++)
        {
            for (int col = minCol; col <= maxCol; col++)
            {
                long key = TerrainDataUtils.getKey(row, col, level);
                if (containsKey(key))
                {
                    tiles.add(getTile(key));
                }
            }
        }
        return tiles;
    }

    public List<ElevationTile> getHighestResolutionTiles(Sector sector)
    {
        sector = attributes.getSector().intersection(sector);

        int levelCount = getLevelCount();
        List<ElevationTile> tiles = new ArrayList<>();
        for (int i = 0; i < levelCount; i++)
        {
            for (ElevationTile tile : getTiles(sector, i))
            {
                if (attributes.isHighestResolution(tile))
                {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public Set<ElevationTile> getBestAvailableTiles(Sector sector)
    {
        sector = attributes.getSector().intersection(sector);

        // Using Set rather than List to avoid duplicate tiles.
        Set<ElevationTile> tiles = new HashSet<>();

        int highestLevel = attributes.getHighestLevel();

        // Iterate through the row/cols, convert them to LatLons and find the best available tiles at each location.
        for (int[] rowCol : TerrainDataUtils.getRowColsForSector(sector, attributes, highestLevel))
        {
            LatLon centroid = TerrainDataUtils.getSector(
                    rowCol[0],
                    rowCol[1],
                    highestLevel,
                    attributes,
                    true).getCentroid();
            ElevationTile tile = getElevationTile(centroid.latitude.degrees, centroid.longitude.degrees);
            if (tile != null)
            {
                tiles.add(tile);
            }
        }
        return tiles;
    }

    public boolean hasHighestResolutionLocation(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return false;
        }
        int highestLevel = attributes.getHighestLevel(latitude, longitude);
        long key = TerrainDataUtils.getTileKey(latitude, longitude, highestLevel, attributes);
        return containsKey(key);
    }

    public ElevationTile getElevationTile(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return null;
        }
        int highest = getAttributes().getHighestLevel(latitude, longitude);
        for (int level = highest; level >= 0; level--)
        {
            long tileKey = TerrainDataUtils.getTileKey(latitude, longitude, level, attributes);
            ElevationTile tile = getTile(tileKey);
            if (tile != null)
            {
                return tile;
            }
        }
        return null;
    }

    /**
     * Get best available (on disk) elevation value at a location.
     */
    public ElevationValue getElevationValue(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return null;
        }
        int highest = getAttributes().getHighestLevel(latitude, longitude);
        for (int level = highest; level >= 0; level--)
        {
            ElevationValue value = getElevationValue(latitude, longitude, level, null);
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }

    public ElevationValue getBestElevationValue(double latitude, double longitude)
    {
        int highest = getAttributes().getHighestLevel(latitude, longitude);
        return getElevationValue(latitude, longitude, highest, null);
    }

    /**
     * Get elevation value at a location and a specific level.
     */
    public ElevationValue getElevationValue(double latitude, double longitude, int level, Set<String> ignoreTilenames)
    {
        if (!contains(latitude, longitude))
        {
            return null;
        }
        ElevationTile tile = loadAndGetTile(latitude, longitude, level, ignoreTilenames);
        return tile != null ? tile.getElevationValue(latitude, longitude) : null;
    }

    /**
     * Optimized version of looking up local elevation values.  In order to use this method, the tile collection
     * should already be updated for the region of interest (use update(Sector sector) first).
     */
    public ElevationValue getLocalElevationValue(double latitude, double longitude, int level)
    {
        if (!hasLocation(latitude, longitude, level))
        {
            return null;
        }
        ElevationTile tile = getTile(TerrainDataUtils.getTileKey(latitude, longitude, level, attributes));
        return tile != null ? tile.getElevationValue(latitude, longitude) : null;
    }

    public boolean contains(double latitude, double longitude)
    {
        return attributes.getSector().containsDegrees(latitude, longitude);
    }

    public int getLevelForResolution(double rawResolution)
    {
        List<Level> levels = attributes.getElevationModel().getLevels().getLevels();
        for (int i = levels.size() - 1; i >= 0; i--)
        {
            if (levels.get(i).getTexelSize() == rawResolution)
            {
                return i;
            }
        }
        return -1;
    }

    public IElevationRaster createElevationRaster(Sector sector,
                                                  int width,
                                                  int height,
                                                  IProgressCallback callback)
    {
        if (callback != null)
        {
            callback.onProgress(0d);
        }

        final double maxX = width - 1;
        final double maxY = height - 1;

        final double minLongitude = sector.getMinLongitude().degrees;
        final double maxLatitude = sector.getMaxLatitude().degrees;

        final double deltaLat = sector.getDeltaLatDegrees();
        final double deltaLon = sector.getDeltaLonDegrees();

        double latitude;
        double longitude;
        final float[] values = new float[width * height];

        Set<String> ignoreTilenames = new HashSet<>();
        int yOffset;
        for (int y = 0; y < height; y++)
        {
            yOffset = y * width;
            latitude = maxLatitude - (y / maxY) * deltaLat;

            if (Thread.currentThread().isInterrupted())
            {
                return null;
            }

            for (int x = 0; x < width; x++)
            {
                longitude = minLongitude + (x / maxX) * deltaLon;
                double resolution = getHighestResolution(latitude, longitude);
                final int level = getLevelForResolution(resolution);
                ElevationValue value = getElevationValue(latitude, longitude, level, ignoreTilenames);
                values[yOffset + x] = value != null ? value.getValue() : TerrainDataUtils.NOT_LOCAL_SIGNAL;
            }

            if (callback != null && y % 20 == 0)
            {
                callback.onProgress(((double) y) / height * 100d);
            }
        }
        return new ElevationRaster(sector, values, width, height, attributes.getHighestResolution());
    }

    public float getElevation(double latitude, double longitude)
    {
        int highest = getAttributes().getHighestLevel(latitude, longitude);
        for (int level = highest; level >= 0; level--)
        {
            ElevationTile tile = loadAndGetTile(latitude, longitude, level, null);
            if (tile != null)
            {
                float elevation = tile.getElevation(latitude, longitude);
                if (!TerrainDataUtils.isMissingOrNotLocalSignal(elevation))
                {
                    return elevation;
                }
            }
        }
        return TerrainDataUtils.NOT_LOCAL_SIGNAL;
    }

    public void addTile(ElevationTile tile)
    {
        addTile(tile, tile.getLevel());
    }

    public Set<ElevationTile> getTiles(int level)
    {
        return tileSets.get(level);
    }

    public Collection<ElevationTile> getAllTiles()
    {
        return tileMap.values();
    }

    public int size()
    {
        return tileMap.size();
    }

    public int getLevelCount()
    {
        return tileSets.size();
    }

    public ElevationTile getTile(long key)
    {
        return tileMap.get(key);
    }

    public boolean containsKey(long key)
    {
        return tileMap.containsKey(key);
    }

    public boolean hasLocation(double latitude, double longitude, int level)
    {
        return getTile(TerrainDataUtils.getTileKey(latitude, longitude, level, attributes)) != null;
    }

    protected ElevationTile loadAndGetTile(double latitude,
                                           double longitude,
                                           int level,
                                           Set<String> ignoreTilenames)
    {
        return loadAndGetTile(latitude, longitude, level, ignoreTilenames, false);
    }

    protected ElevationTile loadAndGetTile(double latitude,
                                           double longitude,
                                           int level,
                                           Set<String> ignoreTilenames,
                                           boolean requestMissing)
    {
        if (level < 0)
        {
            return null;
        }

        long tileKey = TerrainDataUtils.getTileKey(latitude, longitude, level, attributes);
        ElevationTile tile = getTile(tileKey);
        if (tile != null)
        {
            return tile;
        }
        tile = TerrainDataUtils.getElevationTile(latitude, longitude, level, attributes, ignoreTilenames);
        if (tile != null)
        {
            addTile(tile);
        } else if (requestMissing)
        {
            requestTileDownload(latitude, longitude, level);
        }
        return tile;
    }

    protected void requestTileDownload(double latitude, double longitude, int level)
    {
        int[] rowCol = TerrainDataUtils.getRowCol(latitude, longitude, attributes, level);
        ITakElevationModel elevationModel = attributes.getElevationModel();
        TileKey tileKey = new TileKey(
                level,
                rowCol[0],
                rowCol[1],
                elevationModel.getLevels().getFirstLevel().getCacheName());
        // TODO TAKX-2694: do we need to implement this with TileMatrix?
        //TerrainHelper.requestMissingTile(tileKey, elevationModel, null);
    }

    protected float getNearestNeighborElevation(double latitude, double longitude, ElevationTile ignoreTile)
    {
        int highest = attributes.getLevels() - 1;
        for (int level = highest; level >= 0; level--)
        {
            Collection<ElevationTile> levelTiles = getTiles(level);
            if (levelTiles != null)
            {
                for (ElevationTile tile : levelTiles)
                {
                    if (tile != ignoreTile)
                    {
                        float elevation = tile.getRawElevation(latitude, longitude);
                        if (!Float.isNaN(elevation))
                        {
                            return elevation;
                        }
                    }
                }
            }
        }
        return TerrainDataUtils.MISSING_DATA_SIGNAL;
    }

    private synchronized void addTile(ElevationTile tile, int level)
    {
        tileSets.get(level).add(tile);
        tileMap.put(tile.getKey(), tile);
    }
}
