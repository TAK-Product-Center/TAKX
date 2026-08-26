package gov.takx.core.globe.render;

import com.koloboke.collect.map.LongObjMap;
import com.koloboke.collect.map.hash.HashLongObjMaps;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.globe.utils.CellData;
import gov.takx.core.globe.utils.IVisibilityHelper;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.globe.utils.SummaryCounter;
import gov.takx.core.globe.utils.SummaryQuadTree;
import gov.takx.core.mapdata.MapDataUtils;
import gov.takx.core.mapdata.TrackPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class handles the generation of the renderable scene for the Spatial renderer.  All of the elements needed
 * to render the scene are created here and then later installed into the view/map.  This split allows the scene
 * to be generated on a background thread, and then only the work of constructing the view elements from the scene
 * elements needs to be performed on the UI thread.
 *
 * @since 2.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SpatialSceneGenerator extends ASceneGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Logger renderingDebug = LoggerFactory.getLogger(AStyledRenderingEngine.RENDERING_DEBUG_NAMESPACE + MethodHandles.lookup().lookupClass().getSimpleName());

    private static final double PROXIMITY_THRESHOLD = .00001; // TODO: this should scale with regionSize

    private static final LongObjMap<List<TrackPoint>> EMPTY_TRACKS_MAP = HashLongObjMaps.newMutableMap();

    @Inject
    protected MapDataUtils mapDataUtils;

    private final SectorGeometryList geometryList;

    private LongObjMap<List<TrackPoint>> trackPointsToRender;

    /**
     * Construct the scene generator.
     *
     * @param frame            The frame being calculated
     * @param geometryList     The geometry bounding the scene
     * @param renderingContext The current rendering context
     * @param renderingGoal    The target number of renderable objects
     * @param mapObjectVisible A set of flags indicating which Map Objects are visible
     */
    public SpatialSceneGenerator(int frame, SectorGeometryList geometryList, RenderingContext renderingContext,
                                 int renderingGoal, IVisibilityHelper visibilityHelper, BitSet mapObjectVisible)
    {
        super(frame, renderingContext, renderingGoal, visibilityHelper, mapObjectVisible);

        this.geometryList = geometryList;
    }

    public LongObjMap<List<TrackPoint>> getTrackPointsToRender()
    {
        return trackPointsToRender != null ? trackPointsToRender : EMPTY_TRACKS_MAP;
    }

    /**
     * Calculate the scene content for the configured sector geometry list
     */
    @Override
    public void generateScene() throws InterruptedException
    {
        renderSceneFromGeometryList();

        if (logger.isDebugEnabled())
        {
            int pointCount = 0;
            for (List<TrackPoint> trackPoints : trackPointsToRender.values())
            {
                pointCount += trackPoints.size();
            }

            logger.debug("Frame {} has {} lists, {} total points", frame, trackPointsToRender.size(), pointCount);
        }
    }

    /**
     * Determine the scene rendering using the sector geometry list in place.
     */
    private void renderSceneFromGeometryList() throws InterruptedException
    {
        // Determine the set of cells that need to be rendered based on this geometry list
        List<Set<CellData>> cellsToRender = getCellsToRender();

        if (cellsToRender.isEmpty())
        {
            logger.debug("Nothing to render - BAIL!");
            return;
        }

        int cellCount = 0;
        for (Set<CellData> cells : cellsToRender)
        {
            cellCount += cells.size();
        }

        if (cellCount == 0)
        {
            logger.debug("Nothing to render 2 - BAIL!");
            return;
        }

        if (logger.isTraceEnabled())
        {
            logger.trace("{} lists to render, total size = {}", cellsToRender.size(), cellCount);
        }

        testInterrupted();

        // NOTE: CellData.getPoints is a reference to the list in the model, not a copy.  So, we need to synchronize on
        // the model in order to avoid having a point added to the list while we're iterating over it.

        // Loop over all our cells and accumulate the visible counts for each styling key

        // TODO: This may be too slow (using a SummaryCounter), we might need to use a different approach to get total counts
        SummaryCounter counter = new SummaryCounter();

        synchronized (renderingContext.getRenderingModel())
        {
            for (Set<CellData> cells : cellsToRender)
            {
                for (CellData cd : cells)
                {
                    cd.getSummaryCounter().forEachEntry((mapKey, cellCounter) -> {
                        if (visibilityHelper.isStylingContextVisible(mapKey) && cellCounter.count > 0)
                        {
                            counter.adjustCount(mapKey, cellCounter.count);
                        }
                        return true;
                    });
                }
            }

            // Now we have a counter containing ALL the visible entities
            final int totalCount = counter.getTotalCount();
            logger.debug("Total count: {} Counter: {}", totalCount, counter);

            if (totalCount == 0)
            {
                logger.debug("Nothing to render, totalCount == 0 - BAIL!");
                return;
            }

            testInterrupted();

            // Now that we have the counts, if we are oversubscribed, we need to generate the allocations
            if (totalCount > renderingGoal)
            {
                final long[] scKeys = counter.getScKeys(); // Grab a copy of the keys
                final int scKeyCount = scKeys.length;

                final int tscCount = counter.getSize();
                final int[] tscCounts = new int[scKeyCount];

                for (int i = 0; i < scKeyCount; i++)
                {
                    tscCounts[i] = counter.getCount(scKeys[i]);
                }

                // This array will hold our allocation for each TSC
                final int[] allocations = new int[tscCount];
                Arrays.fill(allocations, 0); // Start off with all zeros

                int allocation = renderingGoal / tscCount; // Initial per-TSC allocation
                mapDataUtils.calculateAllocations(renderingGoal, tscCounts, allocation, totalCount, allocations);

                if (logger.isTraceEnabled())
                {
                    logger.trace("sc keys: {}", Arrays.toString(scKeys));
                    logger.trace("allocations: {}", Arrays.toString(allocations));

                    int t = 0;
                    for (int i : allocations)
                    {
                        t += i;
                    }
                    logger.trace("TOTAL ALLOCATION: {}", t);
                }

//                renderPointsByUniformAllocation(cellsToRender, scKeys, tscCounts, allocations);
                renderPointsByTimeAndSpace(cellsToRender, scKeys, allocations);
            } else
            {
                renderAllPoints(cellsToRender, counter);
            }
        }
    }

    /**
     * Determine the detail cells to render given the current visible surface geometries.
     * <p/>
     * Note that need to keep the cells from each detail tree separate since their "equals" test doesn't know about
     * the tree containing it.  Two cells in different trees with the same cell Id would compare as equal.
     *
     * @return List of cells to render
     */
    private List<Set<CellData>> getCellsToRender()
    {
        // We need to find all the detail trees that hold data intersecting the surface geometries
        // Use a set to ensure no tree is included more than once
        final Set<SummaryQuadTree> detailTrees = new HashSet<>();

        // Since not all sectors will have data, track the ones that do
        final SectorGeometryList sectorsWithData = new SectorGeometryList();

        final RenderingModel model = renderingContext.getRenderingModel();

        for (SectorGeometry sg : geometryList)
        {
            final List<SummaryQuadTree> trees = model.findDetailTrees(sg.getSector());

            if (!trees.isEmpty())
            {
                sectorsWithData.add(sg); // This one has data
                detailTrees.addAll(trees);
            }
        }

        // Now that we have the set of trees, we need to find all the detail cells from those trees
        // For each detail tree, loop over the sectors that have data and get the cells from the tree in that sector

        final int detailTreeCount = detailTrees.size();
        List<Set<CellData>> cellsToRender = new ArrayList<>(detailTreeCount);
        final boolean renderingInfoEnabled = renderingDebug.isInfoEnabled();

        for (SummaryQuadTree detailTree : detailTrees)
        {
            final Sector treeSector = detailTree.getSectorForTree();

            if (renderingInfoEnabled) detailTreeSectors.add(treeSector);

            Set<CellData> cellsInTree = new HashSet<>(100);

            for (SectorGeometry sg : sectorsWithData)
            {
                final int maxLevel = detailTree.getNumLevels() - 1;
                final List<CellData> cellList = detailTree.findItemsInRegion(sg.getSector(), maxLevel);

                if (!cellList.isEmpty())
                {
                    cellsInTree.addAll(cellList);  // Adding it to the set ensures unique cells
                }
            }

            cellsToRender.add(cellsInTree); // Add all the cells from this tree to the list
        }

        return cellsToRender;
    }

    /**
     * Given a collection of styling context keys, initialize the trackPointsToRender collection with empty lists
     * for each key.
     *
     * @param scKeys Collection of context keys to process
     */
    private void initializeTrackPointsToRender(long[] scKeys)
    {
        // Make an immutable map since we never add/remove elements, we just update the individual list content

        //noinspection unchecked
        final List<TrackPoint>[] lists = new List[scKeys.length];

        for (int i = 0; i < scKeys.length; i++)
        {
            lists[i] = new ArrayList<>();
        }

        trackPointsToRender = HashLongObjMaps.newImmutableMap(scKeys, lists);
    }

    /**
     * Render the points in the scene applying the given allocation limits to each context group.  The track points for
     * each context group are pruned by proximity (according to the relevant time of the point) to reduce the count
     * to the given allocation.
     *
     * @param cellsToRender The list of sets of cells to render
     * @param scKeys        The context keys associated with the allocations (1:1 by ordinal position)
     * @param allocations   The allocation limit for each context group
     */
    @SuppressWarnings("LogStatementGuardedByLogCondition")
    private void renderPointsByTimeAndSpace(List<Set<CellData>> cellsToRender, long[] scKeys, int[] allocations) throws InterruptedException
    {
        initializeTrackPointsToRender(scKeys);

        // Loop through all our cells and add the points to the proper set
        fillTrackPointsToRender(cellsToRender, trackPointsToRender);

        // We need to perform time/space proximity operations, so sort the track point lists
        for (List<TrackPoint> trackPoints : trackPointsToRender.values())
        {
            trackPoints.sort(new TrackPointTimeComparator());
        }

        final boolean traceEnabled = logger.isTraceEnabled();

        for (int i = 0; i < allocations.length; i++)
        {
            testInterrupted();

            final long scMapKey = scKeys[i];
            List<TrackPoint> trackPoints = trackPointsToRender.get(scMapKey);

            //noinspection ConstantConditions
            if (trackPoints.size() > allocations[i])
            {
                pruneDataBasedOnProximity(trackPoints, scMapKey, PROXIMITY_THRESHOLD, allocations[i]);

                if (traceEnabled)
                {
                    logger.trace("Prune  {} to {} points --> got {}", StylingContextKey.fromMapKey(scMapKey), allocations[i], trackPoints.size());
                }
            } else
            {
                if (traceEnabled)
                {
                    logger.trace("Render {} ALL tracks: {}", StylingContextKey.fromMapKey(scMapKey), trackPoints.size());
                }
            }
        }
    }

//    /**
//     * Render the points in the scene applying the given allocation limits to each context group.  A uniform sampling
//     * of points are retained from each context group to determine the points to include in the scene.
//     *
//     * @param cellsToRender The list of sets of cells to render
//     * @param scKeys        The context keys associated with the allocations (1:1 by ordinal position)
//     * @param tscCounts     The total count of visible points for each context group
//     * @param allocations   The allocation limit for each context group
//     */
//    @SuppressWarnings("unused")
//    private void renderPointsByUniformAllocation(List<Set<CellData>> cellsToRender, TLongList scKeys,
//                                                 int[] tscCounts, int[] allocations)
//    {
//        final long now = System.currentTimeMillis(); // For time threshold calculations
//
//        // Use a map of sorted sets so pruning is respectful of points from all cells in time order
//        trackPointsToRender = new TLongObjectHashMap<>(scKeys.size());
//
//        final TLongIterator iterator = scKeys.iterator();
//        while (iterator.hasNext())
//        {
//            trackPointsToRender.put(iterator.next(), new TreeSet<>(new MapDataUtils.TrackPointTimeComparator()));
//        }
//
//        // Loop through all our cells and add the points to the proper set
//        trackPointCount = fillTrackPointsToRender(cellsToRender, trackPointsToRender);
//
//        // We will render each context group by showing as many points as allocated.  We elide points from the group
//        // evenly by determining the percentage that need to be removed and converting that to a cost.  As we add
//        // points, if the cost is < 1, then we include the point and add the cost and when the cost is over 1,
//        // we reset to the amount over 1.
//        //
//        // The "invert" flag indicates that we need to invert the sense of the operation, if invert is true, then
//        // when the current cost is < 1, we exclude the point (so it is really how often we include instead of skip).
//        //
//        // A cost of 0.0 means we include all the points.
//
//        final int nGroups = tscCounts.length;
//        float[] costs = new float[nGroups];
//        boolean[] inverted = new boolean[nGroups];
//
//        for (int i = 0; i < nGroups; i++)
//        {
//            final int allocation = allocations[i];
//            final int tscCount = tscCounts[i];
//
//            if (allocation >= tscCount)
//            {
//                costs[i] = 0.0f;
//                if (logger.isTraceEnabled()) logger.trace("SHOW FULL COUNT : " + scKeys.get(i));
//            } else
//            {
//                int excess = tscCount - allocation;
//
//                if (excess > allocation)
//                {
//                    inverted[i] = true;
//                    costs[i] = (float) allocation / (float) tscCount;
//                } else
//                {
//                    inverted[i] = false;
//                    costs[i] = (float) excess / (float) tscCount;
//                }
//
//                if (logger.isTraceEnabled())
//                {
//                    logger.trace("alloc=" + allocation + " e=" + excess + " cost=" + costs[i] + " inverted=" + inverted[i]);
//                }
//            }
//        }
//
//        // Now that we have our allocation costs computed, go through the data and prune out tracks above the allocation
//
//        for (int i = 0; i < nGroups; i++)
//        {
//            long scMapKey = scKeys.get(i);
//            Set<TrackPoint> trackPoints = trackPointsToRender.get(scMapKey);
//
//            final float cost = costs[i];
//            final boolean invert = inverted[i];
//            float currentCost = 0.0f;
//
//            // If this context has a cost of 0, then we keep them all
//            if (cost == 0.0)
//            {
//                continue;
//            }
//
//            // Otherwise, loop over the points and determine the ones to keep/discard
//            int added = 0;
//            final Iterator<TrackPoint> iter = trackPoints.iterator();
//
//            while (iter.hasNext())
//            {
//                TrackPoint point = iter.next();
//
//                currentCost += cost;
//
//                if (invert)
//                {
//                    if (currentCost > 1.0f)
//                    {
//                        added += 1;
//                        currentCost = currentCost - 1.0f;
//                    } else
//                    {
//                        iter.remove(); // Skip this point
//                    }
//                } else
//                {
//                    if (currentCost < 1.0f)
//                    {
//                        added += 1;
//                    } else
//                    {
//                        iter.remove(); // Skip this point
//                        currentCost = currentCost - 1.0f;
//                    }
//                }
//            }
//
//            if (logger.isTraceEnabled())
//            {
//                logger.trace("scKey = " + StylingContextKey.fromMapKey(scMapKey) + " count=" + trackPoints.size() + " alloc=" + allocations[i] + " cost=" + costs[i] + " kept=" + added);
//            }
//        }
//    }

    /**
     * Prune a set of track points based on proximity.  Points that are "too close" as determined by a sliding
     * proximity value are removed from the list.
     *
     * @param trackPoints The set of track points to prune
     * @param scMapKey    context key of points being processed
     * @param proximity   Initial proximity
     * @param allocation  The target track point count
     */
    private void pruneDataBasedOnProximity(List<TrackPoint> trackPoints, long scMapKey, double proximity, int allocation)
    {
        final int originalSize = trackPoints.size();
        final int reductionNeeded = originalSize - allocation;
        double overshotProximity = -1.0;
        int totalPointsRemoved = 0;
        double oldProximity = proximity;

        if (allocation == 0 || originalSize <= 1)
        {
            return;     // Nothing we can do if our limit is 0 or we have 1 or no points
        }

        // The set of track points is kept in time order so we can scan them and remove points that are "too close" as
        // determined by a sliding proximity.

        Set<TrackPoint> pointsToRemove = new HashSet<>(originalSize - allocation);

        while (totalPointsRemoved < reductionNeeded)
        {
            pointsToRemove.clear();

            if (originalSize - totalPointsRemoved <= 1)
            {
                break;  // Done if we get down to 1 message, even if our limit is 0
            }

            TrackPoint lastPoint = null;

            // Find messages that are too close to each other
            for (TrackPoint point : trackPoints)
            {
                if (lastPoint == null)
                {
                    lastPoint = point;
                } else
                {
                    if (mapDataUtils.pointsAreProximal(proximity, lastPoint, point))
                    {
                        pointsToRemove.add(point);
                    } else
                    {
                        lastPoint = point;
                    }
                }
            }

            final int passRemoveSize = pointsToRemove.size();
            if ((passRemoveSize + totalPointsRemoved) >= originalSize)
            {
                logger.trace("Bailed, proximity {} would remove all points", proximity);
                break;
            }

            // Now try to deal with a large overshot.  If the last pass took us more than 20% past the reduction
            // amount we were looking for, then we need to nudge our proximity back down in between the last
            // pass and this pass.

            if ((totalPointsRemoved + passRemoveSize) > reductionNeeded * 1.2)
            {
                logger.trace("  OVERSHOT: would have removed {} needed {} at proximity {} oldProximity {}",
                        (totalPointsRemoved + passRemoveSize), reductionNeeded, proximity, oldProximity);

                // Keep track of this proximity because we know that at this point or smaller we will overshoot
                overshotProximity = proximity;

                final double backoff = (proximity - oldProximity) / 2; // Back off by 50%

                // Ensure this algorithm will terminate by exiting if our backoff amount falls below the threshold.
                if (backoff < .000001)
                {
                    if (logger.isTraceEnabled() && proximity != oldProximity)
                    {
                        logger.trace("Bailed on overshoot threshold");
                    }
                } else
                {
                    // Don't set oldProximity here because we want to keep oldProximity as it was the last known
                    // value that didn't cause us to overshoot.
                    proximity -= backoff;

                    logger.trace("  new proximity {}", proximity);

                    continue; // back to top and try again
                }
            }

            trackPoints.removeAll(pointsToRemove);
            totalPointsRemoved += passRemoveSize;

            if (logger.isTraceEnabled())
            {
                logger.trace(String.format(" Partial remove %s tracks: %d proximity: %.9f now at %d/%d needed: %d mapDataLimit: %d",
                        StylingContextKey.fromMapKey(scMapKey), passRemoveSize, proximity, (originalSize - totalPointsRemoved),
                        totalPointsRemoved, reductionNeeded, allocation));
            }

            oldProximity = proximity;

            // Determine the scaling factor for the next pass.  If we have an overshotProximity, then we know that we
            // can't go past that value without overshooting.  So, we scale between the current value and that value.
            // If we don't have an overshotProximity and we are within 50% of our goal, then just scale by 1.5
            // instead of 2.  This helps avoid "overshoot" on subsequent passes.
            if (overshotProximity >= 0.0)
            {
                final double window = overshotProximity - proximity;
                proximity += window / 2.0; // Half way there
            } else
            {
                proximity *= (((double) totalPointsRemoved / reductionNeeded) > .5) ? 1.5 : 2;
            }
        }

        if (totalPointsRemoved > 0)
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Removed {} tracks: {} (from {} -> {}), mapDataLimit: {}",
                        StylingContextKey.fromMapKey(scMapKey), totalPointsRemoved, originalSize, trackPoints.size(), allocation);
            }
        }
    }

    /**
     * Render all the points in the cells given.  The only culling done is to deal with visibility and time thresholds.
     *
     * @param cellsToRender The list of sets of cells to render
     * @param counter       SummaryCounter for all data in the scene
     */
    private void renderAllPoints(List<Set<CellData>> cellsToRender, SummaryCounter counter) throws InterruptedException
    {
        initializeTrackPointsToRender(counter.getScKeys());

        // Loop through all our cells and add the points to the proper set
        fillTrackPointsToRender(cellsToRender, trackPointsToRender);
    }

    /**
     * Given all the data to be rendered, group all the track points in the scene into sets based on their
     * styling context group.  Any cells that have a very high density compared to the average are trimmed here
     * to avoid the cost of processing the excess data later on.
     *
     * @param cellsToRender     The cells to be rendered
     * @param trackPointsToFill The map to be filled out
     * @return The number of track points placed into {@code trackPointsToFill}
     */
    private int fillTrackPointsToRender(List<Set<CellData>> cellsToRender, LongObjMap<List<TrackPoint>> trackPointsToFill) throws InterruptedException
    {
        final long now = System.currentTimeMillis(); // For time threshold calculations

        // Take a quick loop through and determine the average cell density (point count in the cell)
        int trackPointCount = 0;
        int cellCount = 0;

        for (Set<CellData> cells : cellsToRender)
        {
            for (CellData cd : cells)
            {
                cellCount += 1;
                trackPointCount += cd.getPoints().size();
            }
        }

        // We want to pre-prune ultra high density cells to avoid processing them later.  The assumption here is
        // that the data is so dense that we really don't want to process/render it all (that much locally dense
        // information is not useful to the user).
        //
        // NOTE: We only do this pruning if the scene is oversubscribed

        boolean overSubscribed = trackPointCount > renderingGoal;

        int averageDensity = trackPointCount / cellCount;
        int limit = overSubscribed ? averageDensity * 4 : 0; // 0 means no limit

        logger.debug("Cell count={} tpc={} avg={} limit={}", cellCount, trackPointCount, averageDensity, limit);

        // Loop through all our cells and add the points to the proper set
        trackPointCount = 0;

        for (Set<CellData> cells : cellsToRender)
        {
            for (CellData cd : cells)
            {
                testInterrupted();

                int countInThisCell = 0;

                for (TrackPoint point : cd.getPoints())
                {
                    final int moId = point.moId;
                    final int rdmTsdId = point.rdmTsdId;
                    final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, rdmTsdId);

                    // If the point should be rendered, then add it to the lists
                    if (pointShouldBeRendered(tsc, now, moId, point.time))
                    {
                        // We have the scKey, but this call is just marginally faster
                        final StylingContextKey scKey = scManager.getStylingContextKey(moId, rdmTsdId);

                        final List<TrackPoint> pointSet = trackPointsToFill.get(scKey.mapKey);
                        if (pointSet != null)
                        {
                            pointSet.add(point);
                            trackPointCount += 1;
                            countInThisCell += 1;

                            if (limit > 0 && countInThisCell > limit)
                            {
//                            System.out.println("Trimmed " + cd.getPoints().size() + " to " + limit);
                                break;
                            }
                        } else
                        {
                            logger.warn("Unexpected styling context while collecting points: {}", scKey);
                        }
                    }
                }
            }
        }

        return trackPointCount;
    }

    /**
     * Determine if a track/map point should be rendered. In order to support both TrackPoint and MapPoint usage,
     * the needed data is passed in instead of a reference to the map/track point.
     *
     * @param tsc       The track styling context for the point
     * @param now       the current time
     * @param moId      The map object id of from the point
     * @param pointTime The relevant time from the point
     * @return {@code true} if the point should be rendered
     */
    private boolean pointShouldBeRendered(ITrackStylingContext tsc, long now, int moId, long pointTime)
    {
        // If the TSC is null, then we must assume that the MO has been deleted.  Don't render the point.
        if (tsc == null) return false;

        long threshold = tsc.getTimeThreshold();
        boolean withinThreshold = threshold == 0 || pointTime >= (now - threshold);

        // To be rendered, it the MO must be visible, the TSC must be visible, and it must not be outside the time threshold
        return mapObjectVisible.get(moId) && tsc.isVisible() && withinThreshold;
    }

    /**
     * Comparator for ordering TrackPoint objects by the time field.
     *
     * @since 2.29 (moved here from MapDataUtils)
     */
    private static final class TrackPointTimeComparator implements Comparator<TrackPoint>
    {
        @Override
        public int compare(TrackPoint tp1, TrackPoint tp2)
        {
            final int cmp = Long.compare(tp1.time, tp2.time);

            return cmp == 0
                    ? Integer.compare(tp1.rdmId, tp2.rdmId)  // Compare RDM ID if times are equal
                    : cmp;
        }
    }
}
