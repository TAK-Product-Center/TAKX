package gov.takx.core.globe.render;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ITrackStyle;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.api.ui.event.RenderingModelChangedEvent;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.globe.utils.TimeThresholdAssignment;
import gov.takx.core.tracks.TrackStylingContextFactory;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for managing the track style contexts used as part of the track point rendering process.
 * <p/>
 * It is responsible for tracking changes on the TSCs and firing events back to the renderer and model as needed.
 * <p/>
 * Internally we use nested arrays to hold the TRackStylingContexts for a given StylingContextKey.  The outer array
 * is indexed by the MapObject Id, and the inner array is indexed by the RDM TSD Id.  These are all DB ids, so they
 * should remain small(ish).  This uses a little more memory than a map, but the performance compared to using
 * a map keyed by StylingContextKey is significantly (25% at least) better.
 * <p/>
 * The internal cache holds entries that contain the {@link StylingContextKey} and the {@link ITrackStylingContext}.
 * This way the context keys are singletons and we avoid the time to create them during the rendering cycle.  This
 * was a significant chunk of overhead for complex scenes with lots of rendering groups.
 *
 * @since 1.14
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean(singletonObserver = true)
public class RenderingStyleContextManager implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedSmartTrackManager smartTrackManager;

    @Inject
    private TrackStylingContextFactory trackStylingContextFactory;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private EventBus eventBus;

    @Inject
    private ISmartTrackPersist smartTrackPersist;

    // Inverse map from styling context to context key
    private final Map<ITrackStylingContext, StylingContextKey> contextKeys = new HashMap<>();

    // All the map objects we are working with
    private final Map<Integer, IMapObject> mapObjects = new HashMap<>();

    // Properties we monitor on Track Styling Contexts
    private static final List<String> TSC_PROPERTIES =
            Arrays.asList(ITrackStyle.VISIBILITY, ITrackStyle.GROUPABILITY, ITrackStyle.TIME_THRESHOLD);

    // How long to wait for subsequent time threshold property changes
    private static final int TTA_HANDLER_DELAY = 500; // 0.5 seconds

    private final TimeThresholdQueue timeThresholdQueue = new TimeThresholdQueue();

    // Track styling keys and contexts we are working with
    private CacheEntry[][] cache = new CacheEntry[500][]; // Reasonable starting size

    private final RenderingModel model;

    private final BitSet missingMapObjectsReported = new BitSet();

    /**
     * Constructor.
     */
    public RenderingStyleContextManager(RenderingModel model)
    {
        this.model = model;
    }

    @PostConstruct
    private void postConstruct()
    {
        // Loop through all existing map objects and add them right away.
        //
        // Pass false to have all the time thresholds added to the pending list for processing after we've
        // read in all the RDM TSDs.  See prepareRDMTSCs below.
        mapObjectManager.getMapObjects().forEach(mapObject -> addMapObject(mapObject, false));

        // Do this on a background thread, not the EDT
        executorService.execute(() -> {
            Thread.currentThread().setName("RDMTSC loader");
            prepareRDMTSCs();
        });
    }

    /**
     * Prepare all the RDM-level TSCs, primarily install any time thresholds in place.
     */
    private void prepareRDMTSCs()
    {
        // For each MapObject in use, prepare the TSCs
        final List<Object[]> tuples = smartTrackPersist.queryRDMTSDMapObjectIds();

        for (Object[] tuple : tuples)
        {
            int moId = (Integer) tuple[0];
            int rdmTsdId = tuple[1] == null ? -1 : (Integer) tuple[1]; // Avoid NPE on null TSD id

            logger.trace("setup TSC for rdmTsdId {} moId {}", rdmTsdId, moId);

            if (rdmTsdId == -1) continue; // No TSD actually assigned, skip it

            // Take care not to process entries for MOs that have been suppressed.  They may have entries in
            // the RDM-TSD table, but we won't have any context prepared for them.  So, when we go to get the
            // context key, it will be null.

            StylingContextKey scKey = getStylingContextKey(moId, rdmTsdId);

            if (scKey != null)
            {
                recordPendingTimeThreshold(scKey);
            }
        }

        // Now that we're all done with that, we can install all the pending time thresholds at once
        timeThresholdQueue.processNow(true);
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        addMapObject(event.getMapObject(), true);
    }

    /**
     * Add a new map object to our control set, if it can have map data.
     *
     * @param mapObject The map object to, possibly, add
     * @param immediate Pass {@code true} to process time thresholds immediately, {@code false} if they should just
     *                  be recorded in the pending list for later processing
     */
    private void addMapObject(IMapObject mapObject, boolean immediate)
    {
        if (mapObject.canHaveStyledTrackData())
        {
            final Integer moId = mapObject.getId();

            logger.debug("Add MO id={} : {}", moId, mapObject);
            mapObjects.put(moId, mapObject);

            // If we are in "immediate" mode, then install the time threshold now.  We do this for map objects
            // that come online after the initialization phase.

            final StylingContextKey scKey = getStylingContextKey(moId, 0);

            if (immediate)
            {
                processTimeThreshold(scKey);
            } else
            {
                // In non-immediate mode, we just add any non-zero time thresholds to the pending list
                recordPendingTimeThreshold(scKey);
            }
        }
    }

    /**
     * Inspect the given styling context and if the time threshold is non-zero, add a {@link TimeThresholdAssignment}
     * to the pending list.
     * <p/>
     * On complex/large projects there can literally be hundreds of styling contexts (lots of RDM-level TSDs).
     * This can lead to horrible performance if we try to install the time thresholds one at a time.  So, we
     * batch them up and install them into the model all at once.
     *
     * @param scKey context key to process
     */
    private void recordPendingTimeThreshold(StylingContextKey scKey)
    {
        final ITrackStylingContext tsc = getTrackStylingContext(scKey);
        final long timeThreshold = tsc.getTimeThreshold();

        // Only record the threshold if it is non-zero
        if (timeThreshold != 0)
        {
            final TimeThresholdAssignment tta = new TimeThresholdAssignment(scKey, timeThreshold);
            logger.debug("Add pending: {}", tta);
            timeThresholdQueue.add(tta);
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private synchronized void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final var mapObject = event.getMapObject();
        if (mapObject.isTransient()) return;

        final Integer moId = mapObject.getId();

        if (mapObject.canHaveStyledTrackData())
        {
            mapObjects.remove(moId);
        }

        // Find any TSC entries that refer to this MO and remove them from the context keys

        CacheEntry[] entriesForMO = moId >= cache.length ? null : cache[moId];

        if (entriesForMO != null)
        {
            for (int i = 0; i < entriesForMO.length; i++)
            {
                final CacheEntry entry = entriesForMO[i];

                if (entry != null && entry.tsc != null)
                {
                    final ITrackStylingContext tsc = entry.tsc;

                    tsc.removePropertyChangeListener(this, TSC_PROPERTIES); // remove listeners
                    contextKeys.remove(tsc); // remove it from the context keys
                    entriesForMO[i] = null; // Forget about it
                }
            }
        }
    }

    /**
     * Get the styling context for the given map object id.  This will use a default RDM TSD Id of 0, meaning none.
     *
     * @param moId Map Object Id
     * @return track styling context for map object
     */
    public ITrackStylingContext getTrackStylingContext(int moId)
    {
        return getTrackStylingContext(moId, 0);
    }

    /**
     * Get the styling context for the given map object id and RDM TSD Id.
     *
     * @param moId     Map Object Id
     * @param rdmTsdId RDM TSD Id
     * @return track styling context for map object and TSD Id
     */
    public ITrackStylingContext getTrackStylingContext(int moId, int rdmTsdId)
    {
        CacheEntry entry = getCacheEntry(moId, rdmTsdId);

        // If the entry is null at this point, then we need to add it to the cache
        if (entry == null)
        {
            entry = makeCacheEntry(moId, rdmTsdId);
        }

        return entry != null ? entry.tsc : null;
    }

    /**
     * Get the Track Styling Context for the given styling context key.  Create and add it to the set on first request.
     *
     * @param scKey Styling context key
     * @return Track Styling Context for the given key
     */
    public ITrackStylingContext getTrackStylingContext(@Nonnull StylingContextKey scKey)
    {
        final int moId = scKey.mapObjectId;
        final int rdmTsdId = scKey.rdmTsdId;

        CacheEntry entry = getCacheEntry(moId, rdmTsdId);

        return entry != null ? entry.tsc : null;
    }

    /**
     * Get the Track Styling Context for the given styling context map key.  Create and add it to the set on first request.
     *
     * @param scMapKey Styling context map key
     * @return Track Styling Context for the given key
     */
    public ITrackStylingContext getTrackStylingContext(long scMapKey)
    {
        final int moId = StylingContextKey.getMapObjectIdFromMapKey(scMapKey);
        final int rdmTsdId = StylingContextKey.getRdmTsdIdFromMapKey(scMapKey);

        CacheEntry entry = getCacheEntry(moId, rdmTsdId);

        return entry != null ? entry.tsc : null;
    }

    /**
     * Get the StylingContextKey for the given tuple.
     *
     * @param moId     MapObject Id
     * @param rdmTsdId RDM TSD Id
     * @return StylingContextKey singleton for this tuple, may be {@code null}
     */
    public StylingContextKey getStylingContextKey(int moId, int rdmTsdId)
    {
        CacheEntry entry = getCacheEntry(moId, rdmTsdId);

        // If the entry is null at this point, then we need to add it to the cache
        if (entry == null)
        {
            entry = makeCacheEntry(moId, rdmTsdId);
        }

        return entry == null ? null : entry.key;
    }

    /**
     * Make a new cache entry for the given MapObject ID and RDM TSD Id.  Expand the cache as needed.
     *
     * @param moId     Map Object Id
     * @param rdmTsdId RDM TSD Id
     * @return CacheEntry that was created
     */
    private synchronized CacheEntry makeCacheEntry(int moId, int rdmTsdId)
    {
        final StylingContextKey scKey = new StylingContextKey(moId, rdmTsdId);

        // Since our first test (prior to calling this method) was done without the lock, we need to test again
        // now that we have the lock since some other thread might have requested the same TSC and built it before
        // we got the lock.

        CacheEntry[] entriesForMO = moId >= cache.length ? null : cache[moId];
        CacheEntry entry = entriesForMO == null
                ? null
                : rdmTsdId >= entriesForMO.length ? null : entriesForMO[rdmTsdId];

        if (entry == null)
        {
            // The TSC is not in the cache, create it and add it in

            // If we don't have any TSCs for this MO, then create array, expanding the cache if needed
            if (entriesForMO == null)
            {
                if (moId >= cache.length) // Need to expand the cache
                {
                    CacheEntry[][] newCache = new CacheEntry[moId + 10][]; // With some room
                    System.arraycopy(cache, 0, newCache, 0, cache.length);
                    cache = newCache;
                }

                int newLen = Math.max(20, rdmTsdId + 1); // Avoid allocating length 1 array for rdmTsdId == 0

                cache[moId] = entriesForMO = new CacheEntry[newLen]; // Big enough to hold it, will expand later
            }

            final ITrackStyleDefinition rdmTsd = rdmTsdId > 0
                    ? smartTrackManager.getTrackStyleDefinitionByID(rdmTsdId)
                    : null;

            final IMapObject mapObject = mapObjects.get(moId);

            if (mapObject == null)
            {
                if (!missingMapObjectsReported.get(moId))
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Missing map object, key = {}", scKey, new RuntimeException());
                    }
                    missingMapObjectsReported.set(moId);
                }
                return null;
            }

            // Construct the TSC and register to listen on the key properties
            ITrackStylingContext tsc = trackStylingContextFactory.makeTrackStylingContextForRDM(rdmTsd, mapObject);
            tsc.addPropertyChangeListener(this, TSC_PROPERTIES);

            // If the sub-array is too small, then expand it
            if (rdmTsdId >= entriesForMO.length)
            {
                CacheEntry[] newTscsForMO = new CacheEntry[rdmTsdId + 1];
                System.arraycopy(entriesForMO, 0, newTscsForMO, 0, entriesForMO.length);
                cache[moId] = entriesForMO = newTscsForMO;
            }

            entriesForMO[rdmTsdId] = entry = new CacheEntry(scKey, tsc);
        }

        contextKeys.put(entry.tsc, scKey);

        return entry;
    }

    /**
     * Get an existing cache entry for the given MO id and RDM TSD id.  This is intentionally unsynchronized to
     * avoid the overhead of locking.  The cache entry creation is locked (and the entry is tested again) if needed.
     *
     * @param moId     Map Object id
     * @param rdmTsdId RDM TSD Id
     * @return existing cache entry for tuple, or {@code null} if not present
     */
    @Nullable
    private CacheEntry getCacheEntry(int moId, int rdmTsdId)
    {
        CacheEntry[] entriesForMO = moId >= cache.length ? null : cache[moId];
        return entriesForMO == null
                ? null
                : rdmTsdId >= entriesForMO.length ? null : entriesForMO[rdmTsdId];
    }

    /**
     * Test the TSC for the given key and if it has a time threshold configured, install it into the model.
     *
     * @param scKey context key to process
     */
    private void processTimeThreshold(StylingContextKey scKey)
    {
        final ITrackStylingContext tsc = getTrackStylingContext(scKey);
        final long timeThreshold = tsc.getTimeThreshold();

        // Only install the threshold if it is non-zero
        if (timeThreshold != 0)
        {
            logger.debug("Set time threshold for {} to {}", scKey, timeThreshold);

            model.setTimeThreshold(scKey, timeThreshold); // Install the time threshold
        }
    }

    /**
     * Handle a property change event on a map object or a track styling context.  Update state according to the
     * new property value.
     *
     * @param evt Event to handle
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propName = evt.getPropertyName();

        switch (propName)
        {
            case ITrackStyleDefinition.VISIBILITY:
                // Force a regeneration so we re-render with updated visibility settings
                eventBus.post(new RenderingModelChangedEvent("Visibility change"));

                break;
            case ITrackStyleDefinition.GROUPABILITY:
                // Force a regeneration so we re-render with updated groupability settings
                eventBus.post(new RenderingModelChangedEvent("Groupability change"));

                break;
            case ITrackStyleDefinition.TIME_THRESHOLD:
                ITrackStylingContext tsc = (ITrackStylingContext) evt.getSource();

                StylingContextKey scKey = contextKeys.get(tsc);
                long newThreshold = (Long) evt.getNewValue();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Time threshold change: old={} new={} key={}", evt.getOldValue(), newThreshold, scKey);
                }

                onTimeThresholdChanged(scKey, newThreshold); // Add to pending list

                break;
        }
    }

    /**
     * Most often, when a time threshold changes, it will come in as a bunch of property changes in quick succession.
     * Consider when someone changes the global time threshold - every TSC in the system will report a property
     * change.  Even on a single map object, this can produce a large number of events when RDM-level TSDs are being
     * heavily used.
     * <p/>
     * So, we batch them up by waiting a little bit after the first one comes in to see if any more are coming.  If
     * we get more, then reset the timer and wait some more.  If not, then after the timer expires, we install all
     * the collected time thresholds into the model at once.
     *
     * @param scKey         Styling context key whose threshold changed
     * @param timeThreshold New time threshold for that context
     */
    private void onTimeThresholdChanged(StylingContextKey scKey, long timeThreshold)
    {
        final TimeThresholdAssignment tta = new TimeThresholdAssignment(scKey, timeThreshold);
        logger.debug("Add pending: {}", tta);
        timeThresholdQueue.add(tta);
    }

    /**
     * Shutdown and cleanup.
     */
    @RemoveObserver
    public void shutdown()
    {
        timeThresholdQueue.shutdown(); // Shutdown our timed queue

        // Cleanup our styling context listeners
        for (CacheEntry[] cacheEntries : cache)
        {
            if (cacheEntries != null)
            {
                for (CacheEntry entry : cacheEntries)
                {
                    if (entry != null)
                    {
                        if (entry.tsc != null) entry.tsc.removePropertyChangeListener(this, TSC_PROPERTIES);
                    }
                }
            }
        }

        contextKeys.clear();
    }

    /**
     * This class provides the entries in the managers cache.  We store the styling context key and the track styling
     * context.  We store the key so that they are singletons and we can avoid the huge number of SCK object allocations
     * that would occur otherwise (millions during a complex scene rendering pass).
     */
    private static final class CacheEntry
    {
        public final StylingContextKey key;
        public final ITrackStylingContext tsc;

        public CacheEntry(StylingContextKey key, ITrackStylingContext tsc)
        {
            this.key = key;
            this.tsc = tsc;
        }
    }

    /**
     * Timed queue for processing time threshold assignments.
     */
    private final class TimeThresholdQueue extends TimeBatchedQueue<List<TimeThresholdAssignment>, TimeThresholdAssignment>
    {
        TimeThresholdQueue()
        {
            super("Time Threshold Installer", TTA_HANDLER_DELAY, TTA_HANDLER_DELAY * 10, ArrayList::new);
        }

        @Override
        protected void runBatch(List<TimeThresholdAssignment> batch)
        {
            if (logger.isDebugEnabled()) logger.debug("PendingThresholdHandler running, sz={}", batch.size());

            if (!batch.isEmpty())
            {
                model.setTimeThresholds(batch);
            }
        }
    }
}
