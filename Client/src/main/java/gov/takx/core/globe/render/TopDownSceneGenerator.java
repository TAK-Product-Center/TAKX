package gov.takx.core.globe.render;

import com.koloboke.collect.map.LongObjMap;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.core.globe.utils.CellData;
import gov.takx.core.globe.utils.IVisibilityHelper;
import gov.takx.core.globe.utils.RenderingQuadrantSolution;
import gov.takx.core.globe.utils.RenderingSolution;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.globe.utils.SummaryCounter;
import gov.takx.core.globe.utils.SummaryQuadTree;
import gov.takx.core.mapdata.MapPointGroup;
import gov.takx.core.mapdata.TrackPoint;
import gov.takx.platform.tracks.Groupability;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * This class handles the generation of the renderable scene for the Top-Down renderer.  All the elements needed
 * to render the scene are created here and then later installed into the view/map.  This split allows the scene
 * to be generated on a background thread, and then only the work of constructing the view elements from the scene
 * elements needs to be performed on the UI thread.
 *
 * @since 2.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class TopDownSceneGenerator extends ASceneGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Logger renderingDebug = LoggerFactory.getLogger(AStyledRenderingEngine.RENDERING_DEBUG_NAMESPACE + MethodHandles.lookup().lookupClass().getSimpleName());

    // The elevation to use for creating markers, just high enough to keep it from vanishing into the terrain
    private static final double DEFAULT_ELEVATION = 0.2;

    // Tree level above which we should use real track locations instead of the sector centroid
    private static final int USE_REAL_LOCATION_THRESHOLD = 6;

    // Number of track points to show even when grouping
    private static final int SHOW_LAST_N_COUNT = 20;

    @Inject
    private IPlaybackManager playbackManager;

    private final Sector boundingSector;
    private final TopDownRenderingSolver solver;

    private final Map<Integer, List<TrackPoint>> trackPointLists = new HashMap<>();
    private final Map<Integer, List<MapPointGroup>> groupMarkerLists = new HashMap<>();
    private final List<Sector> cellSectors = new ArrayList<>();
    private final List<ClusterData> topQuadrantItemList = new ArrayList<>();

    // These arrays are used to calculate the average location of a map objects markers within a sector
    private int[] moCounts;
    private double[] averageLats;
    private double[] averageLons;
    private int[] moLastCounts;
    private TrackPoint[][] moLastTracks;

    /**
     * Construct the scene generator.
     *
     * @param frame            The frame being calculated
     * @param boundingSector   The sector bounding the scene
     * @param renderingGoal    The target number of renderable objects
     * @param renderingContext The current rendering context
     * @param mapObjectVisible A set of flags indicating which Map Objects are visible
     */
    TopDownSceneGenerator(int frame, Sector boundingSector, int renderingGoal, RenderingContext renderingContext,
                          IVisibilityHelper visibilityHelper, BitSet mapObjectVisible)
    {
        super(frame, renderingContext, renderingGoal, visibilityHelper, mapObjectVisible);

        this.boundingSector = boundingSector;

        solver = new TopDownRenderingSolver(renderingContext.getRenderingModel(), renderingGoal, visibilityHelper);
    }

    /**
     * @return The map of Map Object Id to  {@link TrackPoint}s to render in the scene
     */
    public Map<Integer, List<TrackPoint>> getTrackPointLists()
    {
        return trackPointLists;
    }

    /**
     * @return The map of Map Object Id to  {@link MapPointGroup} markers to render in the scene
     */
    public Map<Integer, List<MapPointGroup>> getGroupMarkerLists()
    {
        return groupMarkerLists;
    }

    /**
     * @return The bounding sector of this scene
     */
    public Sector getBoundingSector()
    {
        return boundingSector;
    }

    /**
     * @return The list of rendered cell sectors, used to annotate the scene with cell boundaries
     */
    public List<Sector> getCellSectors()
    {
        return cellSectors;
    }

    /**
     * @return The list of ClusterData for the top-quadrant items to render
     */
    public List<ClusterData> getTopQuadrantItemList()
    {
        return topQuadrantItemList;
    }

    /**
     * Generate all the renderable elements for the current scene.
     */
    @Override
    public void generateScene() throws InterruptedException
    {
        final long start = System.nanoTime();

        // Now build up the new scene

        final int topLevel = renderingContext.getRenderingModel().getTopTree().findMatchingLevel(boundingSector);

        // Until we get zoomed in far enough, we're not even going to worry about detail levels
        if (topLevel < 4)
        {
            renderTopQuadrants();
        } else
        {
            renderDetailQuadrants(topLevel);
        }

        if (logger.isDebugEnabled())
        {
            final long duration = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            logger.debug("Calculate frame {} took {} micros", frame, duration);
        }
    }

    /**
     * Render the top quadrants of the scene.  This is only used at very large eye altitudes.
     */
    private void renderTopQuadrants()
    {
        final SummaryQuadTree topQuadrants = renderingContext.getRenderingModel().getTopTree();
        final int topLevel = topQuadrants.getNumLevels() - 1;

        final List<CellData> cellLists = topQuadrants.findItemsInRegion(boundingSector, topLevel);

        if (logger.isDebugEnabled())
        {
            logger.debug("TOP: ml={} N={} for {}", topQuadrants.findMatchingLevel(boundingSector), cellLists.size(), boundingSector);
        }

        for (CellData cd : cellLists)
        {
            int visibleCount = visibilityHelper.getVisibleCount(cd.getSummaryCounter());

            if (visibleCount > 0)
            {
                final int cellId = cd.getCellId();

                // Determine the position for this cluster of tracks

                final TrackPoint point = topQuadrants.findSingleton(cellId, topLevel, 0, visibilityHelper);
                final Position markerPos = Position.fromDegrees(point.latitude, point.longitude, DEFAULT_ELEVATION);

                topQuadrantItemList.add(new ClusterData(markerPos, visibleCount, cd)); // Add to the list

                if (renderingDebug.isInfoEnabled()) cellSectors.add(topQuadrants.getSectorForCell(cellId, topLevel));
            }
        }
    }

    /**
     * Render the detail cell scene.  This includes the place marks for grouped elements and markers for individual
     * map points.
     *
     * @param topLevel the top level matching our view
     */
    private void renderDetailQuadrants(int topLevel) throws InterruptedException
    {
        // Determine "now", if we're in playback mode, then use the playback position, otherwise real current time
        final long now = playbackManager.isLive() ? System.currentTimeMillis() : playbackManager.getCurrentPlaybackPosition();

        // Find the rendering solution
        RenderingSolution solution = solver.findRenderingSolution(boundingSector, topLevel);

        if (solution.getTotalDetailCount() == 0)
        {
            logger.debug("No actual points in bounding sector: bail");
            return;
        }

        int sceneObjectCount = 0;
        int summaryCount = 0;
        int summaryObjects = 0;
        final boolean renderingInfoEnabled = renderingDebug.isInfoEnabled();

        // Loop over and render each quadrant (summary quad tree) solution
        for (RenderingQuadrantSolution rqs : solution.getQuadrantSolutions())
        {
            final SummaryQuadTree detailTree = rqs.tree;
            final Sector treeSector = detailTree.getSectorForTree();

            if (logger.isTraceEnabled()) logger.trace("renderDetail tree sector={}", treeSector);
            if (renderingInfoEnabled) detailTreeSectors.add(treeSector);

            // NOTE: rqs.cellList is a reference to the list in the model, not a copy.  So, we need to synchronize on
            // the model in order to avoid having a point added to the list while we're iterating over it.
            //
            // Another option would be to make a copy of the list (while locked) and then process the copy.  I'd
            // like to avoid the overhead of that copy unless we find that this operation blocks too long.

            // TODO: See if we can synchronize on the tree instead of the whole model

            synchronized (renderingContext.getRenderingModel())
            {
                for (CellData cd : rqs.cellList)
                {
                    testInterrupted(); // If we've been interrupted, bail

                    final int level = rqs.level;
                    final int cellId = cd.getCellId();
                    final SummaryCounter summaryCounter = cd.getSummaryCounter();
                    final int visibleCount = visibilityHelper.getVisibleCount(summaryCounter);

                    if (!solution.isDetailLevel() || visibleCount >= solution.getDetailThreshold())
                    {
                        // Rendering at summary level or too many points, render as a group

                        // Render one group marker for each styling group
                        List<GroupRenderData> renderData = getGroupRenderingData(summaryCounter);
                        List<GroupRenderData> remainingRenderData = new ArrayList<>(renderData.size());

                        // Go through and process any groups that are singletons or are marked as "ungroupable" first
                        sceneObjectCount += renderIndividuals(detailTree, cellId, level, renderData, remainingRenderData);

                        // Now process the remaining "grouped" entries

                        final Sector searchSector = detailTree.getSectorForCell(cellId, level); // The "search area" for the group

                        summaryCount += cd.isSummary()
                                ? doGroupLayout(detailTree, cellId, level, remainingRenderData, searchSector)
                                : doAverageGroupLayout(remainingRenderData, cd.getPoints(), searchSector);

                        sceneObjectCount += remainingRenderData.size();
                        summaryObjects += remainingRenderData.size();
                    } else if (visibleCount == 1)
                    {
                        // Render the singleton

                        // This is made more difficult by time thresholds since it means that we can have a count
                        // of 1 when there are more than a single point in the set.  Worse, if the one that we
                        // want to show is has no threshold, and the others do, then a standard "find singleton" without
                        // a key could return us one of the invisible points, and we would then fail to render
                        // anything for this cell.
                        //
                        // In the call to getVisibleCount, we kept track of the "last" visible SC key with data.
                        // That's the one with data (and is visible), so use it to find the singleton.

                        TrackPoint singleton = detailTree.findSingleton(cellId, level, visibilityHelper.getLastVisibleStylingContextKey(), visibilityHelper);
                        addPointRenderable(singleton);
                        sceneObjectCount += 1;
                    } else if (visibleCount > 0)
                    {
                        // Render all the visible points
                        for (TrackPoint point : cd.getPoints())
                        {
                            final int moId = point.moId;

                            if (!mapObjectVisible.get(moId)) continue; // Skip invisible points

                            final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, point.rdmTsdId);

                            // If the TSC is null, then we must assume that the MO has been deleted.  Skip the point.
                            if (tsc != null)
                            {
                                // Don't render points that have a time threshold and are past it
                                long threshold = tsc.getTimeThreshold();
                                boolean withinThreshold = threshold == 0 || point.time >= (now - threshold);

                                if (tsc.isVisible() && withinThreshold)
                                {
                                    addPointRenderable(point);
                                    sceneObjectCount += 1;
                                }
                            }
                        }
                    }

                    if (renderingInfoEnabled) cellSectors.add(detailTree.getSectorForCell(cellId, level));
                }
            }
        }

        testInterrupted();

        if (logger.isDebugEnabled())
        {
            final int dt = solution.getDetailThreshold();
            logger.debug("Scene: {} objects ({} summary, {} total, threshold={}) in {} ({}) scene",
                    sceneObjectCount, summaryObjects, (sceneObjectCount + summaryCount - summaryObjects),
                    dt == Integer.MAX_VALUE ? "None" : String.valueOf(dt),
                    (solution.isDetailLevel() ? "detail level" : "summary level"), solution.getLevel());
        }
    }

    /**
     * Render the "individuals" in the scene.  These are singletons and those elements marked as ungroupable.  Update
     * a list of the remaining elements that need to be rendered.
     *
     * @param detailTree          The tree being processed
     * @param cellId              The ID of the cell being processed
     * @param level               The level in the tree
     * @param renderData          The data to be rendered
     * @param remainingRenderData Updated with the remaining elements to render
     * @return the number of elements added to the scene
     */
    private int renderIndividuals(SummaryQuadTree detailTree, int cellId, int level, List<GroupRenderData> renderData, List<GroupRenderData> remainingRenderData) throws InterruptedException
    {
        int sceneObjectCount = 0;

        // We're going to partition the groups into 3 sets:
        //    1. Those with a count of 1
        //    2. Those that are ungroupable
        //    3. Everything else

        final List<GroupRenderData> singles = new ArrayList<>();
        final List<GroupRenderData> ungroupables = new ArrayList<>();

        for (GroupRenderData rd : renderData)
        {
            if (rd.count == 1)
            {
                singles.add(rd);
            } else if (scManager.getTrackStylingContext(rd.scMapKey).getGroupability() == Groupability.Ungroupable)
            {
                ungroupables.add(rd);
            } else
            {
                remainingRenderData.add(rd); // We don't process these here
            }
        }

        testInterrupted();

        // Process the singletons

        // Get all the singletons for the groups
        final long[] keys = singles.stream().mapToLong(grd -> grd.scMapKey).toArray();
        LongObjMap<List<TrackPoint>> results = detailTree.findSingletons(cellId, level, keys, visibilityHelper);

        for (GroupRenderData grd : singles)
        {
            TrackPoint singleton = results.get(grd.scMapKey).get(0);
            addPointRenderable(singleton);
            sceneObjectCount += 1;
        }

        testInterrupted();

        // Now the ungroupables, show all the detail points for each group
        final long[] ungroupableKeys = ungroupables.stream().mapToLong(grd -> grd.scMapKey).toArray();
        results = detailTree.getTrackPoints(cellId, level, ungroupableKeys, visibilityHelper);

        for (List<TrackPoint> trackPoints : results.values())
        {
            for (TrackPoint point : trackPoints)
            {
                addPointRenderable(point);
            }

            sceneObjectCount += trackPoints.size();
        }

        return sceneObjectCount;
    }

    /**
     * Create and organize the map point group markers for a cell's data.  Depending on the current level,
     * we will either use the cell's sector centroid or the location of the first track point in each group as
     * the location of the group marker.
     *
     * @param detailTree      The quad tree being processed
     * @param cellId          The Id of the cell being processed
     * @param level           The level in the tree of the cell
     * @param groupRenderData The render data for the groups in the cell
     * @param searchSector    The sector covered by the detail cell represented by this group marker
     * @return The total number of map point markers represented by the group markers
     */
    private int doGroupLayout(SummaryQuadTree detailTree, int cellId, int level,
                              List<GroupRenderData> groupRenderData, Sector searchSector)
    {
        final boolean useSingletons = level > USE_REAL_LOCATION_THRESHOLD;

        int totalCount = 0;

        LongObjMap<List<TrackPoint>> singletons = null;
        Position centroidPos = null;

        if (useSingletons)
        {
            // Find all the singletons to render
            final long[] keys = groupRenderData.stream().mapToLong(grd -> grd.scMapKey).toArray();
            singletons = detailTree.findSingletons(cellId, level, keys, visibilityHelper);
        } else
        {
            // Get the centroid of the sector covered by the cell
            final LatLon centroid = detailTree.getSectorForCell(cellId, level).getCentroid();
            centroidPos = new Position(centroid.latitude, centroid.longitude, DEFAULT_ELEVATION);
        }

        // Now that we know where we're putting the groups, prepare the markers

        final MarkerAttributeTracker markerAttrTracker = renderingContext.getMarkerAttrTracker();

        for (GroupRenderData grd : groupRenderData)
        {
            Position markerPos;

            if (useSingletons)
            {
                final TrackPoint point = singletons.get(grd.scMapKey).get(0); // Get first track
                markerPos = Position.fromDegrees(point.latitude, point.longitude, DEFAULT_ELEVATION);
            } else
            {
                markerPos = centroidPos;
            }

            final int moId = StylingContextKey.getMapObjectIdFromMapKey(grd.scMapKey);
            final int rdmTsdId = StylingContextKey.getRdmTsdIdFromMapKey(grd.scMapKey);

            MapPointGroup groupMarker = new MapPointGroup(markerPos, moId, grd.count, grd.latestTime, searchSector);

            setupMarkerAttributes(moId, rdmTsdId, groupMarker, markerAttrTracker);

            totalCount += grd.count;
        }

        return totalCount;
    }

    /**
     * Create and organize the map point group markers for a cell's data using the average geographic location
     * for each map objects track points as the location of the group marker.  This is used for detail cells.
     *
     * @param groupRenderData The render data for the groups in the cell
     * @param points          All the track points in this cell
     * @param searchSector    The sector covered by the detail cell represented by this group marker
     * @return The total number of map point markers represented by the group markers
     */
    private int doAverageGroupLayout(List<GroupRenderData> groupRenderData, List<TrackPoint> points, Sector searchSector)
    {
        // Allocate our calculation arrays if needed (reallocate if the number of MOs has changed)
        final int moCount = mapObjectVisible.length();
        if (moCounts == null || moCounts.length != moCount)
        {
            moCounts = new int[moCount];
            averageLats = new double[moCount];
            averageLons = new double[moCount];
            moLastCounts = new int[moCount];
            moLastTracks = new TrackPoint[moCount][SHOW_LAST_N_COUNT];
        }

        Arrays.fill(moCounts, 0);
        Arrays.fill(averageLats, 0.0);
        Arrays.fill(averageLons, 0.0);
        Arrays.fill(moLastCounts, 0);

        // Calculate the average lat and lon for each map objects track points
        final int size = points.size();
        for (int i = size - 1; i >= 0; i--)
        {
            final TrackPoint trackPoint = points.get(i);
            final int moId = trackPoint.moId;
            final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, trackPoint.rdmTsdId);

            // Ignore invisible points or invisible track styles (and over-indexing moCounts)
            // If the TSC is null, then the MO is most likely deleted
            if (!mapObjectVisible.get(moId) || tsc == null || !tsc.isVisible())
            {
                continue;
            }

            final int count = moCounts[moId];

            if (count == 0)
            {
                averageLats[moId] = trackPoint.latitude;
                averageLons[moId] = trackPoint.longitude;
            } else
            {
                averageLats[moId] = (averageLats[moId] * ((double) count / (count + 1))) + (trackPoint.latitude * ((double) 1 / (count + 1)));
                averageLons[moId] = (averageLons[moId] * ((double) count / (count + 1))) + (trackPoint.longitude * ((double) 1 / (count + 1)));
            }

            moCounts[moId] += 1;

            // Update the "last N" set for this MO
            int lastNCount = moLastCounts[moId];
            if (lastNCount < SHOW_LAST_N_COUNT)
            {
                moLastTracks[moId][lastNCount++] = trackPoint;
                moLastCounts[moId] = lastNCount;
            }
        }

        int totalCount = 0;

        // Now do the actual layout of the markers using the average locations determined above
        final MarkerAttributeTracker markerAttrTracker = renderingContext.getMarkerAttrTracker();

        for (GroupRenderData grd : groupRenderData)
        {
            final int moId = StylingContextKey.getMapObjectIdFromMapKey(grd.scMapKey);
            final int rdmTsdId = StylingContextKey.getRdmTsdIdFromMapKey(grd.scMapKey);

            final Position markerPos = Position.fromDegrees(averageLats[moId], averageLons[moId], DEFAULT_ELEVATION);
            final MapPointGroup groupMarker = new MapPointGroup(markerPos, moId, grd.count, grd.latestTime, searchSector);

            setupMarkerAttributes(moId, rdmTsdId, groupMarker, markerAttrTracker);
            totalCount += grd.count;
        }

        // Now render the "last N" for each MO (these ignore the rendering limits)
        for (int moId = 0; moId < moCount; moId++)
        {
            for (int i = 0; i < moLastCounts[moId]; i++)
            {
                addPointRenderable(moLastTracks[moId][i]);
            }
        }

        // Now scan the list of track points and pull out the last 10 per SC key.  We want to render these as
        // individuals so that the user has a better visual representation of where the MO has been
        return totalCount;
    }

    /**
     * Setup the marker attributes (from the TSC) on the given marker group.
     */
    private void setupMarkerAttributes(int moId, int rdmTsdId, MapPointGroup groupMarker, MarkerAttributeTracker markerAttrTracker)
    {
        final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, rdmTsdId);

        // If the TSC is null, then the MO is most likely deleted
        if (tsc != null)
        {
            final ITrackStyleDefinition rdmTsd = rdmTsdId == 0 ? null : tsc.getTrackStyleDefinition();
            final GlobeMarkerAttributes attrs = markerAttrTracker.getMarkerAttributesForMapPointGroup(groupMarker, rdmTsd);

            // If we somehow end up with a missing map object, we'll get back a null attrs - deal with that.
            if (attrs != null)
            {
                groupMarker.setAttributes(attrs);
            }

            groupMarkerLists.computeIfAbsent(moId, k -> new ArrayList<>()).add(groupMarker);
        }
    }

    /**
     * Add a track point marker as a renderable to our scene.  If the map point was not in the cache, then add it
     * to the list of needed map points.
     *
     * @param point The point to add
     */
    protected void addPointRenderable(TrackPoint point)
    {
        trackPointLists.computeIfAbsent(point.moId, k -> new ArrayList<>()).add(point); // and add it to the marker lists
    }

    /**
     * Determine the rendering data for all the visible style groups on a given summary counter instance.
     *
     * @param summaryCounter SummaryCounter instance to process
     * @return collection of {@link GroupRenderData} for the visible groups
     */
    private List<GroupRenderData> getGroupRenderingData(SummaryCounter summaryCounter)
    {
        List<GroupRenderData> grd = new ArrayList<>();

        summaryCounter.forEachEntry((mapKey, counter) -> {
            // To be visible, the map object must be visible and the styling context must be visible

            if (visibilityHelper.isStylingContextVisible(mapKey))
            {
                final int count = counter.count;
                if (count != 0)
                {
                    grd.add(new GroupRenderData(mapKey, count, counter.latestTime));
                }
            }
            return true;
        });

        return grd;
    }

    /**
     * Holder for the data needed to render a summary group.
     */
    private record GroupRenderData(long scMapKey, int count, long latestTime)
    {
    }

    record ClusterData(Position position, int visibleCount, CellData cellData)
    {
    }
}
