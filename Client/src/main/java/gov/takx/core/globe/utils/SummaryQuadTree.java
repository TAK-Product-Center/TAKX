package gov.takx.core.globe.utils;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.map.IntObjCursor;
import com.koloboke.collect.map.IntObjMap;
import com.koloboke.collect.map.LongLongMap;
import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import com.koloboke.collect.map.hash.HashLongLongMaps;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.LongSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.core.globe.render.RenderingStyleContextManager;
import gov.takx.core.mapdata.TrackPoint;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * QuadTree extension that keeps track of summary counts (by Map Object) at each cell.  The summary count on a cell
 * includes all the track points in all the cells beneath that cell.
 *
 * @since 1.13
 */
@SuppressWarnings({"FieldAccessedSynchronizedAndUnsynchronized", "NonPrivateFieldAccessedInSynchronizedContext"})
public class SummaryQuadTree
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int[] CELL_TRANSFORM = {0, 1, 3, 2}; // Convert between subdivide indices and QT indices

    protected final Sector topSector;
    protected int maxLevel;
    protected int numLevels;
    protected final RenderingStyleContextManager scManager;

    // Map used to find a singleton, can be reused since the findSingleton method is synchronized
    protected final LongObjMap<List<TrackPoint>> singletonDataMap;

    protected final SummaryCounter batchUpdateCounter;
    protected final IntObjMap<SummaryCounter> counts; // the count of items at and below each cell
    protected int totalItemsAdded = 0; // How many items have been added to this tree

    // Used to track time thresholds for each styling context
    protected final LongLongMap timeThresholds;

    // Used to generate path keys
    private final static char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    // Used to calculate cell sizes
    private final LatLon[] deltas;

    private double[][] levelZeroCells;
    private final IntObjMap<List<TrackPoint>> items; // the tree's list of items
    private TrackPoint currentItem; // used during add() to pass the added item to doOperation().
    private final int[] powersOf4;
    private final int[] levelSizes; // Cumulative bits at start of each level. Used for position calculations.
    private final int[] path; // valid only during traversal. Maintains the path to a cell from level 0.

    protected SummaryQuadTree source; // Used when constructed on top of an existing tree

    /**
     * Construct a summary QT using the specified number of levels and the sector boundaries for that top level.
     *
     * @param numLevels Number of levels to use in the tree
     * @param sector    The bounding sector for data handled by this tree
     * @param scManager The styling context manager to use to create context keys
     */
    public SummaryQuadTree(int numLevels, Sector sector, RenderingStyleContextManager scManager)
    {
        topSector = sector;
        this.scManager = scManager;

        if (numLevels < 1)
        {
            throw new IllegalArgumentException("numLevels must be >= 1");
        }

        this.numLevels = numLevels;
        maxLevel = numLevels - 1;

        powersOf4 = computePowers(4, this.numLevels);
        levelSizes = computeLevelSizes(this.numLevels);

        batchUpdateCounter = new SummaryCounter();

        counts = HashIntObjMaps.newMutableMap();

        items = HashIntObjMaps.newMutableMap();
        singletonDataMap = HashLongObjMaps.newMutableMap();
        timeThresholds = HashLongLongMaps.newMutableMap();
        path = new int[numLevels];

        makeLevelZeroCells(sector);

        deltas = calculateDeltas();
    }

    /**
     * Special constructor to allow another summery quad tree to be used as the source of the cell data.  Typically
     * used to perform quick intersection tests.
     *
     * @param source {@link SummaryQuadTree} to use as a source index on cells
     */
    protected SummaryQuadTree(SummaryQuadTree source)
    {
        topSector = source.topSector;
        scManager = source.scManager;

        numLevels = source.numLevels;
        maxLevel = numLevels - 1;

        powersOf4 = source.powersOf4;
        levelSizes = source.levelSizes;

        // The next 3 fields aren't used in our current filtering operations, so avoid the allocations
//        batchAddQueue = null;
        batchUpdateCounter = source.batchUpdateCounter;
        counts = null;
        items = null;
        singletonDataMap = source.singletonDataMap;
        timeThresholds = source.timeThresholds;

        path = new int[numLevels];

        levelZeroCells = source.levelZeroCells;
        deltas = source.deltas;

        this.source = source;
    }

    /**
     * @return The number of levels defined for this tree
     */
    public int getNumLevels()
    {
        return numLevels;
    }

    /**
     * Calculate the delta lat/lon for each tree level.
     */
    private LatLon[] calculateDeltas()
    {
        // Calculate the delta lat/lon for each level
        LatLon[] deltas = new LatLon[numLevels];
        Sector d = topSector;
        for (int i = 0; i < numLevels; i++)
        {
            deltas[i] = LatLon.fromDegrees(d.getDeltaLatDegrees(), d.getDeltaLonDegrees());
//            if (logger.isTraceEnabled()) logger.trace(" Delta[" + i + "]=" + deltas[i]);
            d = d.subdivide()[0];
        }
        return deltas;
    }

    /**
     * Find the smallest level in the tree that subsumes the given sector.
     *
     * @param searchSector The sector we want to test
     * @return Lowest level in the tree that subsumes the sector
     */
    public int findMatchingLevel(Sector searchSector)
    {
        double deltaLat = searchSector.getDeltaLatDegrees();
        double deltaLon = searchSector.getDeltaLonDegrees();

        int level = 0;
        for (; level < maxLevel; level++)
        {
            if (deltaLat > deltas[level].getLatitude().degrees || deltaLon > deltas[level].getLongitude().degrees)
            {
                break;
            }
        }

        logger.trace("Matching level: l={} s=({},{}) delta={}", level, deltaLat, deltaLon, deltas[level]);

        return level;
    }

    /**
     * Determines whether an item intersects a cell.  This implementation to ensure that each added
     * object resides in EXACTLY one cell.
     *
     * @param cellRegion an array specifying the coordinates of the cell's region. The first two entries are the minimum
     *                   and maximum values on the Y axis (typically latitude). The last two entries are the minimum and
     *                   maximum values on the X axis, (typically longitude).
     * @param itemCoords an array specifying the region or location of the item. If the array's length is 2 it
     *                   represents a location in [latitude, longitude]. If its length is 4 it represents a region, with
     *                   the same layout as the {@code nodeRegion} argument.
     * @return non-zero if the item intersects the region. 0 if no intersection.
     */
    protected int intersects(double[] cellRegion, double[] itemCoords)
    {
        if (itemCoords.length == 4) // treat test region as a sector
        {
            return !(itemCoords[1] < cellRegion[0] || itemCoords[0] > cellRegion[1]
                    || itemCoords[3] < cellRegion[2] || itemCoords[2] > cellRegion[3]) ? 1 : 0;
        } else // assume test region is a 2-tuple location
        {
            return itemCoords[0] >= cellRegion[0] && itemCoords[0] < cellRegion[1]
                    && itemCoords[1] >= cellRegion[2] && itemCoords[1] < cellRegion[3] ? 1 : 0;
        }
    }

    /**
     * Performs the add operation of the quadtree.  Call out super-class implementation and then, if we actually
     * added an item, update the summary counts on this cell and the ones above it.
     *
     * @param level      the quadtree level currently being traversed.
     * @param position   the position of the cell in its parent cell, either 0, 1, 2, or 3. Cell positions starts with 0
     *                   at the southwest corner of the parent cell and increment counter-clockwise: cell 1 is SE, cell
     *                   2 is NE and cell 3 is NW.
     * @param cellRegion an array specifying the coordinates of the cell's region. The first two entries are the minimum
     *                   and maximum values on the Y axis (typically latitude). The last two entries are the minimum and
     *                   maximum values on the X axis, (typically longitude).
     * @param itemCoords an array specifying the region or location of the intersecting item. If the array's length is 2
     *                   it represents a location in [latitude, longitude]. If its length is 4 it represents a region,
     *                   with the same layout as the {@code nodeRegion} argument.
     * @return true if traversal should continue to the cell's descendants, false if traversal should not continue to
     * the cell's descendants.
     */
    protected boolean doOperation(int level, int position, double[] cellRegion, double[] itemCoords)
    {
        int bitNum = computeBitPosition(level, position);

        if (level < maxLevel)
        {
            return true;
        }

        List<TrackPoint> regionItems = items.get(bitNum);
        if (regionItems == null)
        {
            regionItems = new ArrayList<>();
            items.put(bitNum, regionItems);
        }

        regionItems.add(currentItem);

        updateSummaryCounts(currentItem);

        return false;
    }

    /**
     * Update the summary counts at every cell from the newly added node up to the root (zero level) nodes.
     *
     * @param point The point that was just added to the tree
     */
    protected synchronized void updateSummaryCounts(TrackPoint point)
    {
        int level = maxLevel;

        for (; level >= 0; level--)
        {
            int cellId = computeBitPosition(level, path[level]);
            SummaryCounter counter = counts.get(cellId);
            if (counter == null)
            {
                counter = new SummaryCounter();
                counts.put(cellId, counter);
            }

            bumpCounter(counter, point);
        }

        // Need something finer than TRACE for this
//        if (logger.isTraceEnabled())
//        {
//            String cellIdKey = cellIdToString(getLastCellId(), maxLevel);
//            logger.trace(String.format("   path[%d]=%s key=%s count=%d", path.length, Arrays.toString(path), cellIdKey, counts.size()));
//        }
    }

    /**
     * Increment the counter associated with a given styling context key.  If there is a time threshold set for
     * that key, then only increment the count if the point is within the time threshold.
     *
     * @param counter Summary counter to update
     * @param point   track point of interest
     */
    protected void bumpCounter(SummaryCounter counter, TrackPoint point)
    {
        StylingContextKey key = scManager.getStylingContextKey(point.moId, point.rdmTsdId);

        // If we get a null scKey, then the MO is probably deleted or suppressed.  This condition usually
        // occurs when a pending update gets applied right after the MO is removed.  Since we don't clean up
        // the pending updates at that time, we need to weed them out here.

        if (key == null)
        {
            return;
        }

        // In order to optimize the re-counting when time thresholds are updated, we need to ensure that the
        // key for this point exists, event if its count is 0.  That way, if the time threshold is ever expanded
        // to include the points, we will know to update the count (since it will have a key in the set).

        counter.ensureKeyExists(key);  // Even if 0, we need the key to exist

        final long threshold = timeThresholds.get(key.mapKey);

        if (threshold == 0 || point.time >= threshold)
        {
            counter.bump(key, point);
        }
    }

    /**
     * Determine if the indicated cell is referenced within this quad tree (has data in a cell at or below the
     * given cell).
     *
     * @param cellId The ID of the cell to test
     * @return {@code true} if the cell has data, {@code false} otherwise
     */
    protected boolean isCellReferenced(int cellId)
    {
        final SummaryQuadTree src = source != null ? source : this;

        return src.counts.containsKey(cellId);
    }

    /**
     * Clear a referenced cell.  Clear the index bit (if the bit set is allocated) and remove it from the counts.
     *
     * @param cellId ID of cell to clear
     */
    protected void clearCell(int cellId)
    {
        //noinspection VariableNotUsedInsideIf
        if (source != null)
        {
            throw new IllegalStateException("Should not be modifying a filter SQT");
        }

        counts.remove(cellId);
    }

    /**
     * Removes all items from the tree.
     */
    synchronized public void clear()
    {
        totalItemsAdded = 0;
        counts.clear();
        items.clear();
    }

    /**
     * Dump the contents of the tree.  This should only be used on small trees (obviously).
     */
    public void dump()
    {
        for (IntObjCursor<List<TrackPoint>> it = items.cursor(); it.moveNext();)
        {
            logger.debug("k={} sz={}", cellIdToString(it.key(), maxLevel), it.value().size());
        }
    }

    public void dumpNonZeroCounts()
    {
        SingletonFindIntersectingBitsOp op = new SingletonFindIntersectingBitsOp(this);
        IntSet bitIds = op.getOnBits(levelZeroCells, Sector.FULL_SPHERE, null);

        if (logger.isDebugEnabled())
        {
            logger.debug("DUMP non-zero counts: N={}", bitIds.size());
            bitIds.forEach((int bitId) -> logger.debug("   SQT[{}] = {}", cellIdToString(bitId, maxLevel), counts.get(bitId)));
        }
    }

    /**
     * Get the total number of items that are held in this tree.  This is an EXPENSIVE operation, so don't use it
     * inside any time critical regions.
     *
     * @return Number of items in the tree
     */
    public synchronized int size()
    {
        int sz = 0;
        for (List<TrackPoint> list : items.values())
        {
            sz += list.size();
        }

        return sz;
    }

    /**
     * Given a location, return the Id for the cell that contains that location in this quad tree.
     *
     * @param coords an array specifying the region or location of interest. If the array's length is 2 it
     *               represents a location in [latitude, longitude]. If its length is 4 it represents a region, with
     *               the same layout as the {@code nodeRegion} argument.
     * @return The containing cell's Id or -1 if not contained in the tree
     */
    public int getCellIdForLocation(double[] coords)
    {
        final SingletonFindIntersectingBitsOp op = new SingletonFindIntersectingBitsOp(this);
        final IntSet cellIds = op.getOnBits(levelZeroCells, coords, null);

        int cellId = -1;
        final int size = cellIds.size();

        if (size > 1)
        {
            logger.error("ERROR: Multiple cells found for location: {}", Arrays.toString(coords));
            cellIds.forEach((int c) -> logger.error("   cellId: {}", cellIdToString(c, maxLevel)));
        } else if (size == 0)
        {
            logger.error("ERROR: no cell found for location: {}", Arrays.toString(coords));
        } else
        {
            final IntCursor cursor = cellIds.cursor();
            cursor.moveNext();
            cellId = cursor.elem(); // Got the one we were looking for
        }

        return cellId;
    }

    /**
     * Given a search sector, return all the Ids for bottom level cells that intersect that sector.
     *
     * @param sector Search sector to test against
     * @return List of Ids of all intersecting cells
     */
    public synchronized IntSet getCellIdsForRegion(Sector sector)
    {
        SingletonFindIntersectingBitsOp op = new SingletonFindIntersectingBitsOp(this);
        return op.getOnBits(levelZeroCells, sector, null);
    }

    /**
     * Find and return the CellData for all cells that have data and intersect the given sector.  The maxLevel
     * parameter controls how deeply in the tree to traverse.  If {@code maxLevel} is less than the maximum level
     * defined for this tree, then summary data wil be returned, otherwise detail data (including the list of
     * track points) will be returned.
     *
     * @param sector   The bounding sector for the search
     * @param maxLevel The maximum level to traverse while collecting data
     * @return List of cell data objects for the given level
     */
    public synchronized List<CellData> findItemsInRegion(Sector sector, int maxLevel)
    {
        // Restrict search to the depth we want (maxLevel)
        SingletonFindIntersectingBitsOp op = new SingletonFindIntersectingBitsOp(this, maxLevel);
        final IntSet cellIds = op.getOnBits(levelZeroCells, sector, null);

        return buildItemLists(cellIds, maxLevel, null);
    }

    /**
     * Build CellData elements identified by a list of bit IDs of cells to include and add them to the output list
     * provided.  If the output list is {@code null}, then a new one will be allocated.
     *
     * @param cellIds  the Ids of the cells to return
     * @param maxLevel The max level to process
     * @param outItems a {@link List} in which to place the CellData objects. If {@code null}, a new set is created.
     * @return the list of cell data objects, if {@code outItems} was passed in as non-null, then this will be
     * {@code outItems}
     */
    protected List<CellData> buildItemLists(IntSet cellIds, int maxLevel, List<CellData> outItems)
    {
        if (outItems == null)
        {
            outItems = new ArrayList<>(cellIds.size());
        }

        if (cellIds == null)
        {
            return outItems;
        }

        final IntCursor cursor = cellIds.cursor();
        while (cursor.moveNext())
        {
            final int cellId = cursor.elem();

            // the cell ids may point to cells that are NOT leaf cells.  We need to fetch the summary count
            // from non-leaf cells or fetch the point list from leaf cells and create CellData nodes from that.

            final SummaryCounter counter = counts.get(cellId);

            if (maxLevel < this.maxLevel)
            {
                if (counter == null)
                {
                    // This should never happen since this QT ensures each item lives in exactly one cell
                    logger.error("ERROR: NULL cellId={}", cellIdToString(cellId, maxLevel));
                } else if (counter.getTotalCount() > 0)
                {
                    // Just add the summary count to the output set
                    outItems.add(new CellData(cellId, counter));
                }
            } else
            {
                // Working on leaf nodes
                List<TrackPoint> regionItems = items.get(cellId);

                // Skip nodes with no points or with a count of zero (meaning they are hidden due to time thresholds)
                //noinspection ConstantConditions
                if (regionItems == null || counter.getTotalCount() == 0)
                {
                    continue;
                }

                outItems.add(new CellData(cellId, counter, regionItems));
            }
        }

        return outItems;
    }

    /**
     * Get the sector covered by the cell with the given Id.  The cell Id is assumed to be at the
     * maximum level of this tree.
     *
     * @param cellId Id of cell
     * @return bounding sector for that cell
     */
    public Sector getSectorForCell(int cellId)
    {
        return getSectorForCell(cellId, maxLevel);
    }

    /**
     * @return The top sector of this tree (it's total geographic span)
     */
    public Sector getSectorForTree()
    {
        return topSector;
    }

    /**
     * Get the sector covered by the cell with the given Cell Id and level.
     *
     * @param cellId Id of the cell
     * @param level  The level index (0..n) containing the cell
     * @return The Sector representing the boundaries of the cell
     */
    public Sector getSectorForCell(int cellId, int level)
    {
        Sector s = topSector;

        // Note that we need to alter the index for cells 2 and 3 since Sector.subdivide does not return
        // the cells in the same order as the QuadTree.
        //
        //   Subdivide          QuadTree
        //  +----+----+       +----+----+
        //  |  2 |  3 |       |  3 |  2 |
        //  +----+----+       +----+----+
        //  |  0 |  1 |       |  0 |  1 |
        //  +----+----+       +----+----+
        //
        // Note that cells 2 and 3 are swapped - bogus!

        int bitPath = cellId - levelSizes[level];

        for (int l = 0; l <= level; l++)
        {
            int shift = (level - l) * 2;
            int index = (bitPath & (3 << shift)) >>> shift;

            s = s.subdivide()[CELL_TRANSFORM[index]]; // Get the cell for this index (via the transform)
        }

        return s;
    }

    /**
     * Find the single visible track point below the given cell (cellId and level).  The  {@code visibilityHelper}
     * is used to determine which map objects are visible.
     *
     * @param cellId           Id of the cell
     * @param level            The level index (0..n) containing the cell
     * @param scMapKey         The styling context map key of the singleton we are searching for, pass 0 to match any key
     * @param visibilityHelper visibility helper object
     * @return The single visible track point
     */
    public synchronized TrackPoint findSingleton(int cellId, int level, long scMapKey, IVisibilityHelper visibilityHelper)
    {
        final ArrayList<TrackPoint> trackPoints = new ArrayList<>();

        singletonDataMap.clear();
        singletonDataMap.put(scMapKey, trackPoints);

        getTrackPoints(cellId, level, singletonDataMap, visibilityHelper, 1);

        return !trackPoints.isEmpty() ? trackPoints.get(0) : null;
    }

    /**
     * Find the single visible track point below the given cell (cellId and level).  The {@code visibilityHelper}
     * is used to determine which map objects are visible.
     *
     * @param cellId           Id of the cell at which to start searching
     * @param level            The level index (0..n) containing the cell
     * @param scMapKeys        The set of styling context keys of the singletons we are searching for
     * @param visibilityHelper visibility helper object
     * @return Map from SC map key to the singleton track point for that key, entries may be {@code null} if singleton
     * not found
     */
    public synchronized LongObjMap<List<TrackPoint>> findSingletons(int cellId, int level,
                                                                    long[] scMapKeys,
                                                                    IVisibilityHelper visibilityHelper)
    {
        final LongObjMap<List<TrackPoint>> dataMap = HashLongObjMaps.newUpdatableMap(scMapKeys.length);
        for (long key : scMapKeys)
        {
            dataMap.put(key, new ArrayList<>());
        }

        getTrackPoints(cellId, level, dataMap, visibilityHelper, 1);
        return dataMap;
    }

    /**
     * Get all the visible track points below the given cell (cellId and level) for a given context key.
     * The {@code visibilityHelper} is used to determine which map objects are visible.
     *
     * @param cellId           Id of the cell
     * @param level            The level index (0..n) containing the cell
     * @param scMapKeys        The set of styling context map keys of the singletons we are searching for
     * @param visibilityHelper visibility helper object
     * @return A map from SC map key to list of track points for that key
     */
    public synchronized LongObjMap<List<TrackPoint>> getTrackPoints(int cellId, int level,
                                                                    long[] scMapKeys,
                                                                    IVisibilityHelper visibilityHelper)
    {
        final LongObjMap<List<TrackPoint>> dataMap = HashLongObjMaps.newUpdatableMap(scMapKeys.length);
        for (long key : scMapKeys)
        {
            dataMap.put(key, new ArrayList<>());
        }

        return getTrackPoints(cellId, level, dataMap, visibilityHelper, -1);
    }

    /**
     * Get all the visible track points below the given cell (cellId and level).  The {@code dataMap} must have been
     * pre-loaded with empty lists keyed by the styling context keys of interest.  This method will deposit located
     * track points into the lists according to context key.
     * The {@code visibilityHelper} is used to determine which map objects are visible.
     *
     * @param cellId           Id of the cell at which to start searching
     * @param level            The level index (0..n) containing the cell
     * @param dataMap          Map from styling context key of the track points we are searching for to lists to fill
     * @param visibilityHelper visibility helper object
     * @param maxCount         The max number of track points to return, -1 for all
     * @return A map from SC key to list of track points for that key
     */
    private synchronized LongObjMap<List<TrackPoint>> getTrackPoints(int cellId, int level,
                                                                     LongObjMap<List<TrackPoint>> dataMap,
                                                                     IVisibilityHelper visibilityHelper, int maxCount)
    {
        // If we are at the detail level, then collect the points
        if (level == maxLevel)
        {
            List<TrackPoint> regionItems = items.get(cellId);

            if (regionItems == null)
            {
                return dataMap;
            }

            final List<TrackPoint> nullKeyList = dataMap.get(0);

            for (TrackPoint point : regionItems)
            {
                final StylingContextKey trackKey = scManager.getStylingContextKey(point.moId, point.rdmTsdId);

                if (trackKey == null) continue; // If the key is null, then the MO no longer exists (deleted).

                // If the map has a null key, then any data will do.  If not, then we only want to include data
                // that has a key contained in the map.
                List<TrackPoint> listForTrackKey = dataMap.get(trackKey.mapKey);

                if ((nullKeyList != null || listForTrackKey != null) && visibilityHelper.isStylingContextVisible(trackKey))
                {
                    final List<TrackPoint> listToUse = (nullKeyList != null) ? nullKeyList : listForTrackKey;
                    listToUse.add(point);

                    // See if we've hit our max count on all keys
                    if (maxCount > 0 && maxCountExceeded(dataMap, maxCount)) break;
                }
            }

            return dataMap;
        } else
        {
            // We are at a summary level, so process any child cells that have data of interest
            SummaryCounter counter = counts.get(cellId);

            if (logger.isTraceEnabled())
            {
                logger.trace(String.format("GTP: key=%s bit=%d l=%d counter=%s", cellIdToString(cellId, level),
                        cellId, level, counter));
            }

            int count = visibilityHelper.getVisibleCount(counter);

            if (count > 0)
            {
                // Run through each child cell and descend if it has data of use
                int path = cellId - levelSizes[level];
                level += 1;
                cellId = (path << 2) + levelSizes[level]; // Cell 0 of the next level down

                final LongSet keySet = dataMap.keySet();

                for (int i = 0; i < 4; i++)
                {
                    int childCellId = cellId + i;
                    SummaryCounter childCounters = counts.get(childCellId);

                    // Only include cells that are referenced and have counts for the keys of interest
                    //noinspection ConstantConditions
                    if (isCellReferenced(childCellId) && childCounters.hasCountForAnyKey(keySet))
                    {
                        getTrackPoints(childCellId, level, dataMap, visibilityHelper, maxCount);

                        if (maxCount > 0 && maxCountExceeded(dataMap, maxCount)) break;
                    }
                }
            }
        }

        return dataMap;
    }

    /**
     * Determine if the max count has been exceeded on all the lists being filled.
     *
     * @param dataMap  The map to inspect
     * @param maxCount The max count to test against
     * @return {@code true} if all the lists in the map have a size >= {@code maxCount}
     */
    private boolean maxCountExceeded(LongObjMap<List<TrackPoint>> dataMap, int maxCount)
    {
        boolean maxed = true;
        for (List<TrackPoint> list : dataMap.values())
        {
            if (list.size() < maxCount)
            {
                maxed = false;
                break;
            }
        }

        return maxed;
    }

    /**
     * Update all the TrackPoints and counts in the tree that are affected by the TSD assignments.
     *
     * @param assignments TSD Assignments to process
     */
    public synchronized void updateTrackPointTSDs(TSDAssignmentSet assignments)
    {
        if (logger.isDebugEnabled()) logger.debug("UpdateTrackPointTSDs with {} assignments", assignments.size());

        internalApplyBatchUpdate(new TSDAssignmentUpdater(assignments));
    }

    /**
     * Apply a visitor to the tree.  Each cell that contains data will be handed to the visitor for processing.
     *
     * @param visitor The visitor to apply
     * @since 1.34
     */
    public synchronized void applyVisitor(ITreeVisitor visitor)
    {
        if (logger.isDebugEnabled()) logger.debug("Run visitor: {}", visitor);

        long start = System.currentTimeMillis();

        // Loop over all the item lists and visit cells
        for (IntObjCursor<List<TrackPoint>> it = items.cursor(); it.moveNext();)
        {
            final int cellId = it.key();

            // Only visit referenced leaf cells
            if (isCellReferenced(cellId))
            {
                visitor.visitCell(cellId, counts.get(cellId), it.value());
            }
        }

        long end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) logger.debug("Visitor took {} ms", end - start);
    }

    /**
     * Apply a batch updater to the tree.  Walk all the item lists and let the updater do its thing.
     * This method is synchronized and intended for external access.
     *
     * @param updater Batch updater to apply
     */
    public synchronized void applyBatchUpdate(IBatchTreeUpdater updater)
    {
        internalApplyBatchUpdate(updater);
    }

    /**
     * Apply a batch updater to the tree.  Walk all the item lists and let the updater do its thing.
     *
     * @param updater Batch updater to apply
     */
    private void internalApplyBatchUpdate(IBatchTreeUpdater updater)
    {
        long start = System.currentTimeMillis();

        // If the updater knows the cells to process, then use that.  Otherwise iterate over all cells with data.

        IntSet cellsToProcess = updater.getCellsToProcess();
        if (cellsToProcess == null) cellsToProcess = items.keySet();

        // Loop over all the item lists and process any affected cells
        cellsToProcess.forEach((int cellId) -> {
            final List<TrackPoint> points = items.get(cellId);

            // See if this cell has counts affected by the updater
            SummaryCounter counter = counts.get(cellId);

            if (counter != null && updater.isCounterAffected(counter))
            {
                batchUpdateCell(cellId, counter, points, updater);
            }
        });

        long end = System.currentTimeMillis();
        if (logger.isDebugEnabled()) logger.debug("batchUpdate took {} ms", end - start);
    }

    /**
     * Apply a batch updater to a cell and, if affected, update the counts all the way up the tree.
     *
     * @param cellId  The cell to start at
     * @param points  The points for that cell
     * @param updater The batch updater to apply
     */
    private void batchUpdateCell(int cellId, SummaryCounter oldCounter, List<TrackPoint> points, IBatchTreeUpdater updater)
    {
        // We need to use a copy of the "old" counter since it's counts will be updated as part of this algorithm
        batchUpdateCounter.copyCounts(oldCounter);

        final SummaryCounter newCounter = updater.processCellData(this, batchUpdateCounter, points);

        // If the updater didn't make any changes, then bail on this cell
        if (newCounter == null)
        {
            return;
        }

        batchUpdateCounter.convertToDelta(newCounter); // Convert OLD to DELTA to be applied up the tree

        // Now, with the delta counter constructed, we need to walk up the tree and update the counts along the way.

        // We will track the "clear" status of the cells up the chain to keep the model as tidy as possible
        boolean clear = true;  // Assume so until we know better

        for (int level = maxLevel; level >= 0; level--)
        {
            final SummaryCounter levelCounter = counts.get(cellId);

            if (logger.isTraceEnabled())
            {
                logger.trace(String.format("adjust counts cell=%13s, DELTA=%s LC=%s", cellIdToString(cellId, level), batchUpdateCounter, levelCounter));
            }

            //noinspection ConstantConditions
            levelCounter.adjustCounts(batchUpdateCounter);

            // We need to ensure that the set/clear cell indicator is updated in case data was removed

            if (level == maxLevel)
            {
                if (points.isEmpty())
                {
                    clearCell(cellId); // It no longer has data
                } else
                {
                    clear = false;
                }
            } else if (clear) // No reason to test if we know we're already not clear
            {
                // Check the four child nodes of this node to see is they all are clear, and if they are
                // clear this cell.
                final int baseChildCellId = ((cellId - levelSizes[level]) << 2) + levelSizes[level + 1];

                for (int i = 0; i < 4; i++)
                {
                    if (counts.get(baseChildCellId + i) != null)
                    {
                        clear = false; // This child has data
                        break;
                    }
                }

                if (clear)
                {
                    clearCell(cellId); // All our children are clear, then so are we
                }
            }

            // Construct the cell id for the next level up
            if (level > 0)
            {
                int path = cellId - levelSizes[level];
                path >>>= 2; // Chop off one level
                cellId = path + levelSizes[level - 1];
            }
        }
    }

    /**
     * Set the time thresholds on this tree using the given set of assignments. Update all the counts in the tree
     * accordingly.  A threshold of 0 will remove the threshold setting we have recorded.
     *
     * @param thresholdAssignments List of threshold assignments to install
     */
    public synchronized void setTimeThreshold(List<TimeThresholdAssignment> thresholdAssignments)
    {
        final boolean debugEnabled = logger.isDebugEnabled();
        final long start = debugEnabled ? System.nanoTime() : 0;

        // Unfortunately, we need to do a complete recount on the tree.  Since cells that have 0 counts for
        // a given scKey will not contain that key in the Counter (0 counts are removed), we can't know if a cell
        // has a count affected by a given scKey or not.
        //
        // If we reworked SummaryCounter to maintain keys with 0 counts (due to time thresholds, and not due
        // to having no track points for that scKey), then we could determine which cells are actually affected.
        //
        // TODO: look into reworking SummaryCounter as described above

        // We need to collect all of the keys involved in this change, that includes the ones with newly
        // assigned 0 thresholds.

        for (TimeThresholdAssignment assignment : thresholdAssignments)
        {
            StylingContextKey scKey = assignment.scKey;
            long threshold = assignment.timeThreshold;

            if (debugEnabled) logger.debug("setTimeThreshold scKey={} threshold= {}", scKey, threshold);

            if (threshold != 0)
            {
                timeThresholds.put(scKey.mapKey, threshold); // Keep track of this for later use in various recount ops
            } else
            {
                timeThresholds.remove(scKey.mapKey); // Remove the threshold since it is now 0
            }
        }

        // Construct and apply the batch updater to control the work of updating the counts affected by these thresholds
        internalApplyBatchUpdate(new TimeThresholdUpdater());

        if (debugEnabled)
        {
            final long time = System.nanoTime() - start;
            logger.debug("setTimeThreshold took {} ms", TimeUnit.NANOSECONDS.toMillis(time));
        }
    }

    /**
     * Get the last tested cell's Id.
     *
     * @return The last tested cell's Id
     */
    @SuppressWarnings("UnusedDeclaration")
    protected int getLastCellId()
    {
        return computeBitPosition(maxLevel, path[maxLevel]);
    }

    /**
     * Given a cell Id and level, construct the string value of the bit path.
     * This is a base 4 representation of the number as a 0 padded string.  This is only used for debug.
     *
     * @param cellId Bit position (Id) of cell
     * @param level  level of the cell (0 .. maxLevel)
     * @return number as a base 4 encoded string
     */
    protected String cellIdToString(int cellId, int level)
    {
        int bitPath = cellId - levelSizes[level]; // Just the bit path at that level
        int length = level + 1;

        char[] buf = new char[length];
        Arrays.fill(buf, '0');

        for (int i = 1; i <= length; i++)
        {
            buf[length - i] = digits[bitPath & 3];
            bitPath >>>= 2;  // divide by 4
        }

        return new String(buf);
    }

    /**
     * Creates the quadtree's level-zero cells.
     *
     * @param sector the sector to subdivide to create the cells.
     */
    protected void makeLevelZeroCells(Sector sector)
    {
        Sector[] subSectors = sector.subdivide();

        levelZeroCells = new double[4][];

        levelZeroCells[0] = subSectors[0].asDegreesArray();
        levelZeroCells[1] = subSectors[1].asDegreesArray();
        levelZeroCells[2] = subSectors[3].asDegreesArray();
        levelZeroCells[3] = subSectors[2].asDegreesArray();
    }

    /**
     * Indicates whether an item is contained in the tree.
     *
     * @param item the item to check. If {@code null}, false is returned.
     * @return true if the item is in the tree, otherwise false.
     */
    synchronized public boolean contains(TrackPoint item)
    {
        if (item == null)
        {
            return false;
        }

        for (List<TrackPoint> itemList : items.values())
        {
            if (itemList != null && itemList.contains(item))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Add a track point to the quadtree. Any duplicates are duplicated in the tree.
     *
     * @param point the point to add.
     * @throws IllegalArgumentException if {@code point} is {@code null}.
     */
    public synchronized void add(TrackPoint point)
    {
        final double[] coords = {point.latitude, point.longitude};

        addItem(point, coords);
    }

    /**
     * Walk the tree and add the item at the given location.
     *
     * @param item       the item to add.
     * @param itemCoords an array specifying the region or location of the item. If the array's length is 2 it
     *                   represents a location in [latitude, longitude]. If its length is 4 it represents a region, with
     *                   the same layout as the {@code nodeRegion} argument.
     * @throws IllegalArgumentException if either {@code item} or {@code itemCoords} is {@code null}.
     */
    private void addItem(@Nonnull TrackPoint item, @Nonnull double[] itemCoords)
    {
        currentItem = item;

        for (int i = 0; i < levelZeroCells.length; i++)
        {
            testAndDo(0, i, levelZeroCells[i], itemCoords);
        }

        totalItemsAdded += 1;
    }

    /**
     * Populate an array with the successive powers of a number. Copied directly from WWMath.
     *
     * @param base      the number whose powers to compute.
     * @param numPowers the number of powers to compute.
     * @return an array containing the requested values. Each element contains the value b^i, where b is the base and i
     * is the element number (0, 1, etc.).
     */
    private static int[] computePowers(int base, int numPowers)
    {
        int[] powers = new int[numPowers];

        powers[0] = 1;
        for (int i = 1; i < numPowers; i++)
        {
            powers[i] += base * powers[i - 1];
        }

        return powers;
    }

    /**
     * An internal method that computes the number of ancestor cells at each level. Level 0 has 0 ancestor cells, level
     * 1 has 4, level 2 has 20 (16 + 4), etc.
     *
     * @param numLevels the number of quadtree levels.
     * @return an array of {@code numLevels + 1} elements containing the sums of ancestor-level cell counts for
     * each quadtree level. The last element in the array contains the total number of cells in the tree.
     */
    private static int[] computeLevelSizes(int numLevels)
    {
        int[] sizes = new int[numLevels + 1];

        sizes[0] = 0;

        double accumulatedSize = 0;
        for (int i = 1; i <= numLevels; i++)
        {
            accumulatedSize += Math.pow(4, i);
            sizes[i] = (int) accumulatedSize;
        }

        return sizes;
    }

    /**
     * The main, recursive traversal method. Called once for each cell. Tests for intersection between items and cells,
     * subdivides cells and continues depth-first traversal on those descendants. Terminates traversal when the end of
     * the tree is reached or when the subclass's {@link #doOperation} method returns false.
     *
     * @param level      the quadtree level currently being traversed.
     * @param position   the position o f the cell in its parent cell, either 0, 1, 2, or 3. Cell positions starts with
     *                   0 at the southwest corner of the parent cell and increment counter-clockwise: cell 1 is SE,
     *                   cell 2 is NE and cell 3 is NW.
     * @param cellRegion an array specifying the coordinates of the cell's region. The first two entries are the minimum
     *                   and maximum values on the Y axis (typically latitude). The last two entries are the minimum and
     *                   maximum values on the X axis, (typically longitude).
     * @param itemCoords an array specifying the region or location of the item. If the array's length is 2 it
     *                   represents a location in [latitude, longitude]. If its length is 4 it represents a region, with
     *                   the same layout as the {@code nodeRegion} argument.
     */
    protected void testAndDo(int level, int position, double[] cellRegion, double[] itemCoords)
    {
        if (intersects(cellRegion, itemCoords) == 0)
        {
            return;
        }

        path[level] = position;

        if (!doOperation(level, position, cellRegion, itemCoords))
        {
            return;
        }

        if (level == maxLevel)
        {
            return;
        }

        double latMid = (cellRegion[1] + cellRegion[0]) / 2;
        double lonMid = (cellRegion[3] + cellRegion[2]) / 2;

        double[] subRegion = new double[4];

        subRegion[0] = cellRegion[0];
        subRegion[1] = latMid;
        subRegion[2] = cellRegion[2];
        subRegion[3] = lonMid;
        testAndDo(level + 1, 0, subRegion, itemCoords);

        subRegion[2] = lonMid;
        subRegion[3] = cellRegion[3];
        testAndDo(level + 1, 1, subRegion, itemCoords);

        subRegion[0] = latMid;
        subRegion[1] = cellRegion[1];
        testAndDo(level + 1, 2, subRegion, itemCoords);

        subRegion[2] = cellRegion[2];
        subRegion[3] = lonMid;
        testAndDo(level + 1, 3, subRegion, itemCoords);
    }

    /**
     * Computes the bit position of a quadtree cell.
     *
     * @param level    the quadtree level currently being traversed.
     * @param position the position of the cell in its parent cell, either 0, 1, 2, or 3. Cell positions starts with 0
     *                 at the southwest corner of the parent cell and increment counter-clockwise: cell 1 is SE, cell 2
     *                 is NE and cell 3 is NW.
     * @return the cell's bit position in the class' bit-list.
     */
    protected int computeBitPosition(int level, int position)
    {
        int bitPosition = position;

        // Compute the index of the position within the level
        for (int i = 0; i < level; i++)
        {
            bitPosition += path[i] * powersOf4[level - i];
        }

        // The index within the BitSet is the index within the level plus the number of bits prior to the level's bits
        return bitPosition + levelSizes[level];
    }

    /**
     * Sub-classed version of {@link SummaryQuadTree} that allows for control over the max level used in
     * determining the resulting bit set.
     * <p/>
     * This class requires a previously populated filter and determines which of its cells intersect a specified
     * sector.
     */
    private static class SingletonFindIntersectingBitsOp extends SummaryQuadTree
    {
        protected IntSet intersectingBits;

        /**
         * Constructs a filter instance.
         *
         * @param filter a filter identifying significant cells in a quadtree, typically produced by applying a filter
         *               that identifies quadtree cells associated with items.
         * @throws NullPointerException     if {@code filter} is {@code null}.
         * @throws IllegalArgumentException if {@code filter} is invalid.
         */
        public SingletonFindIntersectingBitsOp(SummaryQuadTree filter)
        {
            super(filter);
        }

        /**
         * Constructs a filter instance with a maximum search level.
         *
         * @param filter   a filter identifying significant cells in a quadtree, typically produced by applying a filter
         *                 that identifies quadtree cells associated with items.
         * @param maxLevel The max level to access during search operations.
         * @throws NullPointerException     if {@code filter} is {@code null}.
         * @throws IllegalArgumentException if {@code filter} is invalid or {@code maxLevel} is invalid.
         */
        public SingletonFindIntersectingBitsOp(SummaryQuadTree filter, int maxLevel)
        {
            super(filter);

            if (maxLevel < 0 || maxLevel >= filter.numLevels)
            {
                String msg = "maxLevel (" + maxLevel + ") must be >= 0 and < numLevels";
                logger.error(msg);
                throw new IllegalArgumentException(msg);
            }

            this.maxLevel = maxLevel;
        }

        /**
         * Returns the bit positions of significant cells that intersect a specified sector. Significant cells are those
         * identified by the filter specified to the constructor.
         *
         * @param topRegions the zeroth-level regions of a quadtree.
         * @param testSector the sector of interest. Significant cells that intersect this sector are returned.
         * @param outIds     a set in which to place the bit positions of intersecting significant cells. May be {@code null},
         *                   in which case a new list is created. In either case the list is the return value of the
         *                   method.
         * @return the bit positions of intersecting significant cells. This is the list specified as the non-null
         * {@code outIds} argument, or a new list if that argument is {@code null}.
         * @throws IllegalArgumentException if either {@code topRegions} or {@code testSector} is {@code null}.
         */
        public IntSet getOnBits(double[][] topRegions, @Nonnull Sector testSector, IntSet outIds)
        {
            return getOnBits(topRegions, testSector.asDegreesArray(), outIds);
        }

        /**
         * Returns the bit positions of significant cells that intersect a specified sector. Significant cells are those
         * identified by the filter specified to the constructor.
         *
         * @param topRegions the zeroth-level regions of a quadtree.
         * @param testRegion the sector of interest, specified as a four-element array containing minimum latitude,
         *                   maximum latitude, minimum longitude and maximum longitude, in that order. Significant cells
         *                   that intersect this sector are returned.
         * @param outIds     a list in which to place the bit positions of intersecting significant cells. May be {@code null},
         *                   in which case a new list is created. In either case the list is the return value of the
         *                   method.
         * @return the bit positions of intersecting significant cells. This is the list specified as the non-null
         * {@code outIds} argument, or a new list if that argument is {@code null}.
         * @throws IllegalArgumentException if either {@code topRegions} or {@code testSector} is {@code null}.
         */
        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        public IntSet getOnBits(@Nonnull double[][] topRegions, @Nonnull double[] testRegion, IntSet outIds)
        {
            intersectingBits = outIds != null ? outIds : HashIntSets.newMutableSet();

            for (int i = 0; i < topRegions.length; i++)
            {
                testAndDo(0, i, topRegions[i], testRegion);
            }

            return intersectingBits;
        }

        /**
         * Assembles the output bit-list during traversal. Overrides the method {@link
         * SummaryQuadTree#doOperation(int, int, double[], double[])}. See the description of that method for
         * further detail.
         *
         * @param level      the quadtree level currently being traversed.
         * @param position   the position of the cell in its parent cell, either 0, 1, 2, or 3. Cell positions starts
         *                   with 0 at the southwest corner of the parent cell and increment counter-clockwise: cell 1
         *                   is SE, cell 2 is NE and cell 3 is NW.
         * @param cellRegion an array specifying the coordinates of the cell's region. The first two entries are the
         *                   minimum and maximum values on the Y axis (typically latitude). The last two entries are the
         *                   minimum and maximum values on the X axis, (typically longitude).
         * @param testSector an array specifying the region or location of the intersecting item. If the array's length
         *                   is 2 then it represents a location in [latitude, longitude]. If its length is 4 it
         *                   represents a region, with the same layout as the {@code nodeRegion} argument.
         * @return true if traversal should continue to the cell's descendants, false if traversal should not continue
         * to the cell's descendants.
         */
        @Override
        protected boolean doOperation(int level, int position, double[] cellRegion, double[] testSector)
        {
            int bitNum = computeBitPosition(level, position);

            if (!isCellReferenced(bitNum))
            {
                return false;
            }

            if (level < maxLevel)
            {
                return true;
            }

            intersectingBits.add(bitNum);

            return false;
        }
    }
}
