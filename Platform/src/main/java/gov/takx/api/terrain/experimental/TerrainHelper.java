package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileKey;
import gov.nasa.worldwind.util.WWIO;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @since 1.35
 * @deprecated to be removed
 */
@Experimental
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class TerrainHelper extends BasicElevationModel implements MemoryCache.CacheListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static AVList defaultParams = null;
    private final List<ITerrainHelperListener> listeners = new CopyOnWriteArrayList<ITerrainHelperListener>();

    public TerrainHelper()
    {
        super(getDefaultParams());

        //listen to the elevation tile memory cache.
        getMemoryCache().addCacheListener(this);
    }

    public boolean isTileInMemory(TileKey key)
    {
        return areElevationsInMemory(key);
    }

    @Override
    public BasicElevationModel.ElevationTile getTileFromMemory(TileKey key)
    {
        return super.getTileFromMemory(key);
    }

    /**
     * Request WW to load/download an elevation tile associated with a BasicElevationModel.
     *
     * @param key   key info on the tile to load
     * @param model model to which the request should be associated.
     */
    public static void requestMissingTile(TileKey key, BasicElevationModel model, Set<BasicElevationModel> ignoredModels)
    {
        if (WorldWind.getTaskService().isFull())
        {
            return;
        }

        if (model.getLevels().isResourceAbsent(key))
        {
            if (ignoredModels != null)
            {
                ignoredModels.add(model);
                if (logger.isDebugEnabled())
                {
                    Sector sector = model.getLevels().computeSectorForKey(key);
                    logger.debug("adding model to ignore list: {}\n    for sector: {}", model.getName(), sector);
                }
            }
            return;
        }

        WorldWind.getTaskService().addTask(new TerrainRequestTask(key, model));
    }

    public void addListener(ITerrainHelperListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(ITerrainHelperListener listener)
    {
        listeners.remove(listener);
    }

    public void clearListeners()
    {
        listeners.clear();
    }

    /**
     * TerrainHelper listens to the elevation tile cache and notifies each of its own listeners whenever a
     * tile has been removed.
     *
     * @param key          TileKey into the elevation tile cache
     * @param clientObject elevation tile
     */
    @Override
    public synchronized void entryRemoved(Object key, Object clientObject)
    {
        for (ITerrainHelperListener listener : listeners)
        {
            listener.removeEntry(key);
        }
    }

    @Override
    public void removalException(Throwable exception, Object key, Object clientObject)
    {
    }

    /**
     * Replicates some of the functionality in the protected BasicElevationModel.getElevations() method.  The in-memory
     * cache is checked for tiles within the sector in question.  If the tile is not in memory, create a RequestTask
     * so that it may be retrieved from disk or the network.  The difference from the WW method is that no fallback
     * (intermediate/lower resolution) tiles are requested.  Since the goal is to get the highest resolution elevation
     * data possible, any lower res data is unnecessary.  Finally, no elevation data is actually written to a buffer.
     * <p/>
     * This method is useful as a check before the public BasicElevationModel.getElevations() method is called.  If
     * {@code true} is returned, requests were made for missing tiles, otherwise {@code false} is returned.
     *
     * @param model      the BasicElevationModel in question.
     * @param sector     the sector in question.
     * @param resolution the intended resolution.
     * @param listener   a listener for cache removal events
     * @return {@code true} is returned if requests were made for missing tiles, otherwise {@code false} is
     * returned.
     */
    public boolean requestMissingElevations(BasicElevationModel model,
                                            Sector sector, double resolution,
                                            TerrainHelperListener listener,
                                            Set<BasicElevationModel> ignoredModels)
    {
        Level targetLevel = getTargetLevel(model, sector, resolution);
        int levelNumber = targetLevel.getLevelNumber();

        // Top level tiles are not stored in the cache.  In the unlikely case that the 0th level is the highest
        // (i.e. only) resolution elevation available, just let the elevation model take care of elevation lookup.
        if (levelNumber <= 0)
        {
            return false;
        }
        return requestMissingElevations(model, sector, levelNumber, listener, ignoredModels);
    }

    // See comments for above method
    private boolean requestMissingElevations(BasicElevationModel model,
                                             Sector requestedSector,
                                             int targetLevelNumber,
                                             TerrainHelperListener listener,
                                             Set<BasicElevationModel> ignoredModels)
    {
        // Compute the intersection of the requested sector with the LevelSet's sector.
        // The intersection will be used to determine which Tiles in the LevelSet are in the requested sector.
        LevelSet levelSet = model.getLevels();
        Sector sector = requestedSector.intersection(levelSet.getSector());

        Level targetLevel = levelSet.getLevel(targetLevelNumber);
        LatLon delta = targetLevel.getTileDelta();
        LatLon origin = levelSet.getTileOrigin();
        final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());
        final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
        final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
        final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

        boolean missingTargetTiles = false;
        for (int row = seRow; row <= nwRow; row++)
        {
            for (int col = nwCol; col <= seCol; col++)
            {
                TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                if (listener != null)
                {
                    listener.addEntry(key);
                }
                if (!isTileInMemory(key))
                {
                    missingTargetTiles = true;
                    logger.trace("Requesting missing tile: {}", key);
                    requestMissingTile(key, model, ignoredModels);
                }
            }
        }
        return missingTargetTiles;
    }

    /**
     * Get an array of best local resolutions from a list of BasicElevationModels.  Also fill an adjacent array of
     * BasicElevationModels used to associate each resolution value with its corresponding elevation model.
     *
     * @param models  list of elevation models.
     * @param sector  sector in question
     * @param latLons list of LatLons
     * @return array of model keys representing best resolution and their associated models.
     */
    public TerrainUtils.ElevationKey[] getBestLocalResolutions(List<LocalDataBasicElevationModel> models,
                                                               Sector sector,
                                                               List<LatLon> latLons,
                                                               Map<Long, Boolean> tileHashes)
    {
        TerrainUtils.ElevationKey[] elevationKeys = new TerrainUtils.ElevationKey[latLons.size()];
        for (int i = 0; i < elevationKeys.length; i++)
        {
            elevationKeys[i] = new TerrainUtils.ElevationKey(null, null, Double.MAX_VALUE);
        }

        TerrainUtils.sortModelsByResolution(models, sector);

        for (int i = models.size() - 1; i >= 0; i--)
        {
            getBestLocalResolutions(models.get(i), sector, latLons, elevationKeys, tileHashes);
        }
        return elevationKeys;
    }

    /**
     * Get/fill an array of resolutions for a specific elevation model and sector.
     *
     * @param em              elevation model from which to get resolutions.
     * @param requestedSector sector in question.
     * @param latLons         list of lat/lons
     * @param elevationKeys   array of elevation models to associate with the resolution array.
     */
    private void getBestLocalResolutions(LocalDataBasicElevationModel em,
                                         Sector requestedSector,
                                         List<LatLon> latLons,
                                         TerrainUtils.ElevationKey[] elevationKeys,
                                         Map<Long, Boolean> tileHashes)
    {
        int size = latLons.size();

        // Compute intersection of the requested sector and the sector covered by the elevation model.
        LevelSet levelSet = em.getLevels();
        Sector sector = requestedSector.intersection(levelSet.getSector());

        // If there is no intersection there is no data to retrieve
        if (sector == null)
        {
            return;
        }

        for (int i = 0; i < levelSet.getNumLevels(); i++)
        {
            Level targetLevel = levelSet.getLevel(i);
            LatLon delta = targetLevel.getTileDelta();
            LatLon origin = levelSet.getTileOrigin();
            final int nwRow = Tile.computeRow(delta.getLatitude(), sector.getMaxLatitude(), origin.getLatitude());
            final int nwCol = Tile.computeColumn(delta.getLongitude(), sector.getMinLongitude(), origin.getLongitude());
            final int seRow = Tile.computeRow(delta.getLatitude(), sector.getMinLatitude(), origin.getLatitude());
            final int seCol = Tile.computeColumn(delta.getLongitude(), sector.getMaxLongitude(), origin.getLongitude());

            for (int row = nwRow; row >= seRow; row--)
            {
                for (int col = nwCol; col <= seCol; col++)
                {
                    TileKey key = new TileKey(targetLevel.getLevelNumber(), row, col, targetLevel.getCacheName());
                    Sector tileSector = levelSet.computeSectorForKey(key);
                    boolean localTile;
                    Tile tile = getTileFromMemory(key);
                    if (tile == null)
                    {
                        tile = new Tile(tileSector, targetLevel, row, col);

                        // Determine whether this tile is actually available on disk.  If it is marked absent or not
                        // on disk, then it will return false.
                        localTile = isTileLocal(em, tile, tileHashes);
                    } else
                    {
                        tileHashes.put(hashTileElevation(em, tile), true);
                        localTile = true;
                    }

                    // Only consider adding data from a tile if it is available on disk.  I.e. not absent!
                    if (localTile)
                    {
                        double resolution = tile.getLevel().getTexelSize();
                        for (int j = 0; j < size; j++)
                        {
                            //if the level resolution is better than the current model key resolution, then
                            //potentially replace this key (if the lat/lon is in its sector)
                            if (resolution < elevationKeys[j].resolution)
                            {
                                LatLon latlon = latLons.get(j);
                                double lat = latlon.getLatitude().degrees;
                                double lon = latlon.getLongitude().degrees;
                                if (tileSector.containsDegrees(lat, lon))
                                {
                                    elevationKeys[j] = new TerrainUtils.ElevationKey(em, key, resolution);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static long hashTileElevation(BasicElevationModel em, Tile tile)
    {
        return ((long) tile.hashCode() << 32) | em.hashCode();
    }

    private static boolean isTileLocal(BasicElevationModel em, Tile tile, Map<Long, Boolean> tileHashes)
    {
        long hash = hashTileElevation(em, tile);
        if (tileHashes.containsKey(hash))
        {
            return tileHashes.get(hash);
        }

        if (em.getLevels().isResourceAbsent(tile))
        {
            tileHashes.put(hash, false);
            return false;  // tile is absent
        }

        URL url = em.getDataFileStore().findFile(tile.getPath(), false);

        boolean val = url != null && !WWIO.isFileOutOfDate(url, tile.getLevel().getExpiryTime());
        tileHashes.put(hash, val);
        return val;
    }

    /**
     * Static recreation of BasicElevationModel.getTargetLevel (since it has protected access in BasicElevationModel).
     *
     * @param model      model from which to get the target level.
     * @param sector     sector in question.
     * @param targetSize desired level resolution.
     * @return the target level.
     */
    private static Level getTargetLevel(BasicElevationModel model, Sector sector, double targetSize)
    {
        LevelSet levels = model.getLevels();
        Level lastLevel = levels.getLastLevel(sector); // finest resolution available
        if (lastLevel == null)
        {
            return null;
        }

        if (lastLevel.getTexelSize() >= targetSize)
        {
            return lastLevel; // can't do any better than this
        }

        for (Level level : levels.getLevels())
        {
            if (level.getTexelSize() <= targetSize)
            {
                return !level.isEmpty() ? level : null;
            }

            if (level == lastLevel)
            {
                break;
            }
        }

        return lastLevel;
    }

    /**
     * A basic extension of RequestTask to provide the ability to use the RequestTask constructor.
     */
    private static class TerrainRequestTask extends BasicElevationModel.RequestTask
    {
        protected TerrainRequestTask(TileKey tileKey, BasicElevationModel elevationModel)
        {
            super(tileKey, elevationModel);
        }
    }

    /**
     * Get/create a dummy set of default parameters; required for the construction of the super class,
     * BasicElevationModel.
     *
     * @return a set of dummy default params.
     */
    private static AVList getDefaultParams()
    {
        if (defaultParams == null)
        {
            defaultParams = new AVListImpl();
            defaultParams.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, LatLon.fromDegrees(36, 36));
            defaultParams.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
            defaultParams.setValue(AVKey.NUM_LEVELS, 1);
            defaultParams.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
            defaultParams.setValue(AVKey.TILE_WIDTH, 256);
            defaultParams.setValue(AVKey.TILE_HEIGHT, 256);
            // This is a shared LevelSet, so just supply a dummy cache name and dataset name.
            defaultParams.setValue(AVKey.DATA_CACHE_NAME, TerrainHelper.class.getName());
            defaultParams.setValue(AVKey.DATASET_NAME, TerrainHelper.class.getName());
            // We won't use any tile resource paths, so just supply a dummy format suffix.
            defaultParams.setValue(AVKey.FORMAT_SUFFIX, TerrainHelper.class.getName());
        }
        return defaultParams;
    }

    /**
     * A listener interface for the TerrainHelper which is notified whenever an entry is removed from the tile memory
     * cache.
     */
    public interface ITerrainHelperListener
    {
        void removeEntry(Object key);
    }

    /**
     * An implementation of ITerrainHelperListener which allows the user to add entries and get the current number
     * of tiles associated with a sector.
     */
    public static class TerrainHelperListener implements ITerrainHelperListener
    {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
        private Set<Object> addedKeys = new HashSet<>();
        private Set<Object> removedKeys = new HashSet<>();

        public synchronized void addEntry(Object key)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Added key: {} at time {}", key, System.currentTimeMillis());
            }
            addedKeys.add(key);
        }

        @Override
        public synchronized void removeEntry(Object key)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Removed key: {} at time {}", key, System.currentTimeMillis());
            }
            removedKeys.add(key);
        }

        public synchronized boolean isValid()
        {
            for (Object key : removedKeys)
            {
                if (addedKeys.contains(key))
                {
                    return false;
                }
            }
            return true;
        }
    }
}
