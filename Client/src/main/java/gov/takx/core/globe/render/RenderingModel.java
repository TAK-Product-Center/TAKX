package gov.takx.core.globe.render;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.IntSet;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IPluginRendererManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.playback.IPlaybackEventListener;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentListener;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.api.ui.event.RenderingModelChangedEvent;
import gov.takx.core.globe.utils.QuadTreeCleaner;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.globe.utils.SummaryQuadTree;
import gov.takx.core.globe.utils.TSDAssignment;
import gov.takx.core.globe.utils.TSDAssignmentSet;
import gov.takx.core.globe.utils.TimeThresholdAssignment;
import gov.takx.core.mapdata.TrackPoint;
import gov.takx.platform.globe.renderer.ARenderingModel;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class holds the model for rendering track points on the globe.  The globe is initially divided using a
 * shallow quad tree.  This gives us a reasonable division of the globe into areas where we can create subtrees
 * to handle the data all the way down to just a few meters in detail.  For each leaf level cell in the top
 * tree, a detail tree may be created.  The detail trees are created as needed so we only grow the model as data
 * dictates.
 *
 * @since 1.13
 */
@PseudoBean
public class RenderingModel
        extends ARenderingModel
        implements ISmartTrackAssignmentListener, IMapObjectControllerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TOP_QUAD_LEVELS = 7;
    private static final int DETAIL_LEVELS = 13; // Number of levels in the detail trees

    // RDM-level TSDs need to be handled in batches.  So, we limit how often we will kick off a recount of the
    // model.  However, TSD assignments can come in continuously.  Some sponsors use RDM-level TSDs on *every*
    // single RDM.  So, we need an upper limit on how long we will wait to do a recount as well.

    private static final int TSD_ASSIGNMENT_HANDLER_DELAY = 500; // 0.5 seconds
    private static final int TSD_ASSIGNMENT_HANDLER_MAX_WAIT = 5000; // 5 seconds
    private final TSDAssignmentQueue tsdAssignmentQueue = new TSDAssignmentQueue();

    private RenderingStyleContextManager scManager;

    private SummaryQuadTree topTree;
    private final Map<Integer, SummaryQuadTree> detailTrees = new ConcurrentHashMap<>();

    // time thresholds in effect
    private final Map<StylingContextKey, TimeThresholdAssignment> timeThresholds = new ConcurrentHashMap<>();

    // Task to keep time threshold based counts up to date in the model
    private TimerTask thresholdTimerTask;

    private static final String THRESHOLD_TIMER = "RenderingModel threshold timer";
    private static final long ONE_MINUTE_MS = TimeUnit.MINUTES.toMillis(1);

    private static final ThreadLocal<double[]> coords = ThreadLocal.withInitial(() -> new double[2]);

    // Track all the MOs that can have styled tracks
    private final BitSet moHasStyledTracks = new BitSet();

    @Inject
    protected EventBus eventBus;

    @Inject
    protected ICoreExecutorService executorService;

    @Inject
    protected IPersistServiceProvider persistServiceProvider;

    @Inject
    private IPluginRendererManager pluginRendererManager;

    @Inject
    private ITimerService timerService;

    @Inject
    private ISmartTrackAssignmentManager smartTrackAssignmentManager;

    @Inject
    private IPlaybackManager playbackManager;

    private final PlaybackThresholdUpdater thresholdUpdater = new PlaybackThresholdUpdater();

    /**
     * Set up our world.  Add listeners, load in all the existing map object data.
     *
     * @param scManager The styling context manager to use
     */
    public void initialize(RenderingStyleContextManager scManager)
    {
        this.scManager = scManager;

        topTree = new SummaryQuadTree(TOP_QUAD_LEVELS, Sector.FULL_SPHERE, scManager);

        smartTrackAssignmentManager.addAssignmentListener(this);
        playbackManager.addPlaybackEventListener(thresholdUpdater);

        internalInitialize();
        loadMapObjects();
    }

    /**
     * Load in all the existing Map Objects and their associated data.
     *
     * @since 2.29
     */
    private void loadMapObjects()
    {
        // Loop through all existing map objects and add them right away
        for (IMapObjectController mapObjectController : mapObjectManager.getMapObjectControllers())
        {
            final IMapObject mapObject = mapObjectController.getMapObject();
            final boolean renderingExcluded = pluginRendererManager.isRenderingExcluded(AStyledRenderingEngine.class, mapObject);

            // Preload data from the project DB, but only if we're supposed to include it in our model
            if (mapObject.isDevice() && !renderingExcluded)
            {
                clientMapObjectAdded(mapObject); // Prepare to handle this MO
            }
        }
    }

    /**
     * @return The top quadrants tree
     */
    public SummaryQuadTree getTopTree()
    {
        return topTree;
    }

    /**
     * We only want to process map objects that can produce track points, and are not configured to be excluded
     * for this renderer.
     */
    @Override
    protected void handleMapObjectAdded(IMapObject mapObject)
    {
        final boolean renderingExcluded = pluginRendererManager.isRenderingExcluded(AStyledRenderingEngine.class, mapObject);

        logger.debug("MO: {}, excluded = {}", mapObject, renderingExcluded);

        if (mapObject.isDevice() && !renderingExcluded)
        {
            // If the MO can have styled track data, then record that for fast access as RDMs are received
            if (mapObject.canHaveStyledTrackData())
            {
                moHasStyledTracks.set(mapObject.getId());
                mapObject.getController().addMapObjectControllerListener(this); // Monitor for RDMs generated for this MO
                populateFromMapObject(mapObject); // And load its current data
            }
        }
    }

    @Override
    protected void handleMapObjectRemoved(IMapObject mapObject)
    {
        // Unregister as a message listener on this MO
        mapObject.getController().removeMapObjectControllerListener(this);

        final Integer moId = mapObject.getId();

        // Delete any time thresholds that refer to the map object
        timeThresholds.entrySet().removeIf(entry -> entry.getKey().mapObjectId == moId);

        // Delete all the data associated with the map object from the trees and adjust the counts accordingly.
        // Do this remaining work on a worker thread to avoid locking up our invoker.
        //
        // Note: if the executor service is null, then we are probably being shutdown, and we can skip this work

        if (executorService != null)
        {
            executorService.execute(() -> cleanupMapObjectData(moId, Long.MAX_VALUE));
        }
    }

    /**
     * Update our model to account for the RDMs (TrackPoints) deletion by removing them from the model.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected void handleMapObjectPartialDelete(IMapObject mapObject, long timeOfDeletion)
    {
        executorService.execute(() -> cleanupMapObjectData(mapObject.getId(), timeOfDeletion));
    }

    /**
     * Cleanup our data relating to a map object that has been removed (suppressed/deleted).  Don't make this method
     * synchronized since we try to keep our synchronization tighter than that for this operation.
     *
     * @param moId           Id of MapObject that was removed
     * @param timeOfDeletion The time of deletion, all RDMs earlier than this cutoff are being deleted
     */
    private void cleanupMapObjectData(int moId, long timeOfDeletion)
    {
        // Always give executor threads meaningful names
        Thread.currentThread().setName("cleanupMapObjectData[" + moId + "]");

        new QuadTreeCleaner(topTree, moId, timeOfDeletion).execute(); // Do the delete

        // Make our own copy since we don't want the set to change out from under us, and we don't want to
        // lock detailTrees for the duration of the operation.

        final List<SummaryQuadTree> treesToClean = new ArrayList<>(detailTrees.values());

        for (SummaryQuadTree sqt : treesToClean)
        {
            new QuadTreeCleaner(sqt, moId, timeOfDeletion).execute(); // Do the delete
        }
    }

    /**
     * Determine if the given message should be excluded from rendering.  This eliminates a message
     * if it:
     * <ul>
     *     <li>has a message type that is exclusively handled by a renderer other than {@link AStyledRenderingEngine}</li>
     *     <li>belongs to a MO whose options declare it doesn't have styled tracks</li>
     *     <li>is not persistable</li>
     *     <li>is not renderable</li>
     *     <li>has no location</li>
     *     <li>is manually generated</li>
     * </ul>
     *
     * @param rdm Message to test
     * @return {@code true} if the message should be excluded, {@code false} otherwise
     * @since 2.36
     */
    public boolean isMessageExcluded(IRaptorDataMessage rdm)
    {
        boolean excluded = !rdm.isPersistable() || !rdm.isRenderable() || !rdm.hasLocation() || rdm.isManuallyGeneratedLocation();

        excluded = excluded || !moHasStyledTracks.get(rdm.getMapObject().getId());

        excluded = excluded || pluginRendererManager.isRenderingExcluded(AStyledRenderingEngine.class, rdm.getMessageType());

        return excluded;
    }

    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
        if (isMessageExcluded(rdm)) return;

        logger.trace("receiveMessage(): {}", rdm);

        addPoint(new TrackPoint(rdm));
    }

    /**
     * The project is closing, quickly nuke the model. No reason to do it MO by MO.
     */
    @Override
    protected void projectClosing()
    {
        tsdAssignmentQueue.shutdown();

        topTree.clear();

        detailTrees.values().forEach(SummaryQuadTree::clear);
        detailTrees.clear();
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        super.onShutdown(progressMonitor);

        smartTrackAssignmentManager.removeAssignmentListener(this);
        playbackManager.removePlaybackEventListener(thresholdUpdater);

        topTree.clear();
        detailTrees.clear();
    }

    /**
     * Add a point to the model.
     * <p/>
     * For now, this method specifically doesn't fire a model change event.  Points can come in very quickly, and
     * we don't want to swamp that process with unnecessary scene regenerations.  So, the renderer is responsible
     * for "doing the right thing" as individual map points come in.
     *
     * @param point TrackPoint to add
     */
    private void addPoint(TrackPoint point)
    {
        // If the MO for this track point has been deleted, then just skip it
        if (isMapObjectDeleted(point.getMapObjectId()))
        {
            logger.debug("MO deleted, skip TP: {}", point);
            return;
        }

        internalAddPoint(point);
    }

    /**
     * Add a point to the model.  It will be added to the top quadrants tree and then to the proper detail tree.
     * <p/>
     * For now, this method specifically doesn't fire a model change event.  Points can come in very quickly, and
     * we don't want to swamp that process with unnecessary scene regenerations.  So, the renderer is responsible
     * for "doing the right thing" as individual map points come in.
     *
     * @param point TrackPoint to add
     */
    private void internalAddPoint(TrackPoint point)
    {
        topTree.add(point); // Add to top quadrants QT

        final double[] pointCoords = coords.get();

        pointCoords[0] = point.latitude;
        pointCoords[1] = point.longitude;

        final int cellId = topTree.getCellIdForLocation(pointCoords);

        final SummaryQuadTree detailTree = detailTrees.computeIfAbsent(cellId, this::createDetailTreeForCell);

        detailTree.add(point); // Add to detail QT
    }

    /**
     * Create and initialize a new detail tree for the given top level cell.
     *
     * @param cellId ID of the top level cell for which to create a detail tree
     * @return new detail-level quad tree
     */
    private SummaryQuadTree createDetailTreeForCell(int cellId)
    {
        Sector sector = topTree.getSectorForCell(cellId);
        SummaryQuadTree detailTree = new SummaryQuadTree(DETAIL_LEVELS, sector, scManager);

        // Install configured time thresholds

        detailTree.setTimeThreshold(makeCurrentThresholdList());

        return detailTree;
    }

    /**
     * Set the time threshold for a given styling context. Update all the trees with this new value.
     *
     * @param scKey     The styling context key whose threshold has changed
     * @param threshold The time threshold, anything older than this will not be included in the updated counts
     */
    public void setTimeThreshold(StylingContextKey scKey, long threshold)
    {
        final List<TimeThresholdAssignment> ttaList = Collections.singletonList(new TimeThresholdAssignment(scKey, threshold));
        setTimeThresholds(ttaList);
    }

    /**
     * Install a collection of time threshold assignments.  Update all the trees with new counts based on these
     * thresholds.
     *
     * @param thresholdAssignments List of time thresholds to install
     */
    public void setTimeThresholds(List<TimeThresholdAssignment> thresholdAssignments)
    {
        // Make a copy of the list so our caller can do what they want to with it (like clear it).
        final var workingAssignments = new ArrayList<>(thresholdAssignments);

        // Always do this work on a background thread - never on the EDT
        executorService.execute(() -> {
            Thread.currentThread().setName("timeThreshold Installer");
            doSetTimeThreshold(workingAssignments);
        });
    }

    /**
     * Set the time threshold for a given styling context. Update all the trees with this new value.
     * This method simply gives us the entry point from the runnable above.
     *
     * @param thresholdAssignments The list of time threshold assignments to install
     */
    private synchronized void doSetTimeThreshold(List<TimeThresholdAssignment> thresholdAssignments)
    {
        if (logger.isDebugEnabled()) logger.debug("Install thresholds: count={}", thresholdAssignments.size());

        // Update the time thresholds map with the new values
        //
        // The threshold we've been given is a "raw" threshold, the actual window, like 18000 ms.  We need to
        // convert that to the actual time threshold (absolute time) and install that into the model.
        // Ensure that we don't add entries for MOs that no longer exist.  This can happen since calls to this
        // method are queued up and executed in batches.

        // Record "raw" threshold
        thresholdAssignments.stream()
                .filter(assignment -> mapObjectManager.getMapObjectController(assignment.scKey.mapObjectId) != null)
                .forEach(assignment -> timeThresholds.put(assignment.scKey, assignment));

        installTimeThresholds(); // Apply all the thresholds to the model

        // A threshold of 0 means no threshold, so we can remove them from the set now that the model is updated

        // Expunge 0 entries
        thresholdAssignments.stream()
                .filter(assignment -> assignment.timeThreshold == 0)
                .forEach(assignment -> timeThresholds.remove(assignment.scKey));

        // If we have any thresholds left, then we need to start a timer that will keep the model updated
        // on a regular basis.

        if (!timeThresholds.isEmpty())
        {
            if (thresholdTimerTask == null)
            {
                thresholdTimerTask = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        installTimeThresholds();
                    }
                };

                logger.debug("Starting threshold timer task");

                timerService.scheduleAtFixedRate(THRESHOLD_TIMER, thresholdTimerTask, ONE_MINUTE_MS, ONE_MINUTE_MS);
            } else
            {
                logger.debug("Threshold timer task already running");
            }
        } else
        {
            if (thresholdTimerTask != null)
            {
                logger.debug("Stopping threshold timer");

                // Just cancel the whole timer, this will prevent any subsequent executions of the task
                timerService.destroyTimer(THRESHOLD_TIMER);
                thresholdTimerTask = null;
            } else
            {
                logger.warn("Odd, there wasn't a threshold timer task running.");
            }
        }
    }

    /**
     * Install the recorded time thresholds into the model trees.
     */
    private void installTimeThresholds()
    {
        // Update the model with the actual threshold for every TSC that has a threshold defined.

        // We need to create a new list of threshold assignments where we have calculated the absolute threshold from
        // the "raw" threshold.

        final List<TimeThresholdAssignment> currentAssignments = makeCurrentThresholdList();
        if (currentAssignments.isEmpty()) return;

        topTree.setTimeThreshold(currentAssignments);

        for (SummaryQuadTree sqt : detailTrees.values())
        {
            sqt.setTimeThreshold(currentAssignments);
        }

        // We have changed the counts, so fire the event
        eventBus.post(new RenderingModelChangedEvent("Time threshold updated"));
    }

    /**
     * Create a list of threshold assignments from our current recorded set where the absolute threshold has been
     * calculated from the recorded "raw" threshold and the current time.
     *
     * @return New list of threshold assignments with the thresholds set to absolute times
     */
    private synchronized List<TimeThresholdAssignment> makeCurrentThresholdList()
    {
        // We need to create a new list of threshold assignments where we have calculated the absolute threshold from
        // the "raw" threshold.

        // Determine "now", if we're in playback mode, then use the playback position, otherwise real current time
        final long now = playbackManager.isLive() ? System.currentTimeMillis() : playbackManager.getCurrentPlaybackPosition();

        final List<TimeThresholdAssignment> currentAssignments = new ArrayList<>(timeThresholds.size());

        for (Map.Entry<StylingContextKey, TimeThresholdAssignment> entry : timeThresholds.entrySet())
        {
            final var scKey = entry.getKey();

            // A threshold of 0 means no threshold, a non-zero threshold needs to be converted to an absolute threshold.
            long threshold = entry.getValue().timeThreshold;
            if (threshold != 0) threshold = now - threshold;

            logger.debug("installTimeThresholds key={} threshold={}", scKey, threshold);
            currentAssignments.add(new TimeThresholdAssignment(scKey, threshold));
        }

        return currentAssignments;
    }

    /**
     * Add all the track points owned by the given map object to the model.
     *
     * @param mapObject Map object to load
     */
    private void populateFromMapObject(final IMapObject mapObject)
    {
        final String threadName = getClass().getSimpleName() + " populator for: " + mapObject;

        Runnable populator = () -> {
            Thread.currentThread().setName(threadName);

            try
            {
                final List<TrackPoint> points = executeMapObjectPointQuery(mapObject);

                if (logger.isDebugEnabled()) logger.debug("MO: {} has {} map points", mapObject.getId(), points.size());

                addPoints(points); // Add the points to the model
            } catch (Exception e)
            {
                logger.error("Failed to load MapPoints", e);
            } finally
            {
                persistServiceProvider.cleanupPersistService();
            }
        };

        // Submit it for execution
        executorService.execute(populator);
    }

    /**
     * Add the set of points to the model.
     *
     * @param points Points to add
     */
    protected void addPoints(List<TrackPoint> points)
    {
        final long start = logger.isDebugEnabled() ? System.currentTimeMillis() : 0;

        points.forEach(this::addPoint);

        // We have new data, so fire the event
        eventBus.post(new RenderingModelChangedEvent("New points added"));

        if (logger.isDebugEnabled())
        {
            logger.debug("Rendering model update took {} ms\n", (System.currentTimeMillis() - start));

            debugDumpModelAfterPointsAdded();
        }
    }

    @SuppressWarnings("LogStatementGuardedByLogCondition")
    private void debugDumpModelAfterPointsAdded()
    {
        logger.debug("QT summary counts:");

        for (Map.Entry<Integer, SummaryQuadTree> entry : detailTrees.entrySet())
        {
            logger.debug("   p={} size={}", entry.getKey(), entry.getValue().size());
        }

        topTree.dumpNonZeroCounts();
        topTree.dump();
    }

    private List<TrackPoint> executeMapObjectPointQuery(IMapObject mapObject)
    {
        final Set<String> exclusions = pluginRendererManager.getExcludedMessageTypes(AStyledRenderingEngine.class);
        boolean hasExclusions = !exclusions.isEmpty();

        String hql = "select new " + TrackPoint.class.getName()
                + "(rdm.id, rdm.trackStyleId, " + mapObject.getId() + ", rdm.latitude, rdm.longitude, rdm.relevantTime)"
                + " from RaptorDataMessage rdm" + (hasExclusions ? " join rdm.structure s" : "")
                + " where rdm.hasLocation=true"
                + "   and rdm.isManualLocation=false"
                + "   and rdm.isRenderable=true"
                + "   and rdm.mapObject_id = :moId" + (hasExclusions ? " and s.messageType not in (:excludedList)" : "");

        logger.debug("populator running");

        return persistServiceProvider.getPersistService().queryFromRdm(hql,
                new HQLParameter("moId", mapObject.getId()),
                new HQLParameter("excludedList", hasExclusions ? exclusions : null));
    }

    /**
     * Locate all the detail trees intersecting the given sector.
     *
     * @param searchSector The sector to search on, any detail tree whose bounding sector intersects this sector
     *                     will be returned
     * @return List of detail trees that intersect the search sector
     */
    public List<SummaryQuadTree> findDetailTrees(Sector searchSector)
    {
        // Find all the paths in the top quadrants that intersect the search sector and return the detail
        // trees for those top-level cells.

        final IntSet cellIdsForRegion = topTree.getCellIdsForRegion(searchSector);

        final List<SummaryQuadTree> detailTreeList = new ArrayList<>(cellIdsForRegion.size());
        final IntCursor cursor = cellIdsForRegion.cursor();

        while (cursor.moveNext())
        {
            detailTreeList.add(detailTrees.get(cursor.elem()));
        }
        return detailTreeList;
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        final Integer newTsdId = tsd == null ? null : tsd.getId();
        onTSDAssignedToRDM(oldTsdId, newTsdId, rdm.getMapObject().getId(), rdm.getIdAsInt());
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, Integer tsdId, int moId, int rdmId)
    {
        // Changes to TSD assignment usually come in batches, so we don't want to kick off a re-count on every
        // assignment.  Instead, we batch them up in a queue.

        final TSDAssignment assignment = new TSDAssignment(oldTsdId, tsdId, moId, rdmId);

        logger.trace("Add assignment: {}", assignment);

        tsdAssignmentQueue.add(assignment);
    }

    /**
     * Dump the trees (only used for debug).
     */
    @SuppressWarnings({"unused", "UseOfSystemOutOrSystemErr"})
    public void dumpTrees()
    {
        System.out.println("QT model:");

        topTree.dumpNonZeroCounts();
        topTree.dump();

        for (Map.Entry<Integer, SummaryQuadTree> entry : detailTrees.entrySet())
        {
            System.out.println("   p=" + entry.getKey() + " size=" + entry.getValue().size());
        }
    }

    /**
     * Listener to force a re-application of the time thresholds whenever the user scrubs the timeline.  Without this,
     * if the user scrubs backward in time, track points that were previously before the threshold would not reappear
     * if they are now within the threshold.
     *
     * @since 5.7
     */
    private class PlaybackThresholdUpdater implements IPlaybackEventListener
    {
        private long lastScrubTime;

        @Override
        public void onPlaybackEvent(PlaybackEvent event)
        {
            if (event == PlaybackEvent.ScrubPositionChanged)
            {
                final long playbackPosition = playbackManager.getCurrentPlaybackPosition();

                if (!playbackManager.isLive() && lastScrubTime > playbackPosition) installTimeThresholds();

                lastScrubTime = playbackPosition;
            }
        }
    }

    /**
     * Timed queue to manage TSD assignments.
     */
    private final class TSDAssignmentQueue extends TimeBatchedQueue<List<TSDAssignment>, TSDAssignment>
    {
        TSDAssignmentQueue()
        {
            super("Pending TSD Assignment Installer", TSD_ASSIGNMENT_HANDLER_DELAY, TSD_ASSIGNMENT_HANDLER_MAX_WAIT, ArrayList::new);
        }

        /**
         * Take all the outstanding TSD assignments and apply them to our trees.
         *
         * @param batch Collection of batched entries to process
         */
        @Override
        protected void runBatch(List<TSDAssignment> batch)
        {
            final TSDAssignmentSet assignments = new TSDAssignmentSet();

            batch.forEach(assignments::add);

            topTree.updateTrackPointTSDs(assignments);

            detailTrees.values().forEach(sqt -> sqt.updateTrackPointTSDs(assignments));

            // We have changed the counts, so fire the event
            eventBus.post(new RenderingModelChangedEvent("Summary counts adjusted"));
        }
    }
}
