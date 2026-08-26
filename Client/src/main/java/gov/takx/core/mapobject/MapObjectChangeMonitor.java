package gov.takx.core.mapobject;

import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Monitor all Map Objects for property changes and queue them up for persistence.
 *
 * @since 3.2
 */
@Alternative
@ApplicationScoped
public class MapObjectChangeMonitor implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Set of properties that should NOT trigger a MO persistence operation (since they are already handled in
    // MapObjectController.persistLocationData).
    static final Set<String> LOCATION_RELATED_PROPERTIES = Set.of(
            IMapObject.LOCATION,
            IMapObject.HEADING,
            IMapObject.SPEED,
            IMapObject.MOST_RECENT_COMMUNICATION_TIME,
            IMapObject.OLDEST_COMMUNICATION_TIME);

    private final Set<IMapObject> monitoredMapObjects = ConcurrentHashMap.newKeySet();

    private final BatchQueue persistQueue = new BatchQueue();

    private final IMapObjectManager mapObjectManager;
    private final IPersistServiceProvider persistServiceProvider;

    @Inject
    MapObjectChangeMonitor(IMapObjectManager mapObjectManager, IPersistServiceProvider persistServiceProvider)
    {
        this.mapObjectManager = mapObjectManager;
        this.persistServiceProvider = persistServiceProvider;
    }

    /**
     * Note public access for use by {@code TestMapObjectChangeMonitor}.
     *
     * @return {@code true} if the given property is one that is handled by the MO controller on location updates
     */
    @VisibleForTesting
    public static boolean isLocationPropertyChange(PropertyChangeEvent pce)
    {
        return LOCATION_RELATED_PROPERTIES.contains(pce.getPropertyName());
    }

    @Override
    public void propertyChange(PropertyChangeEvent pce)
    {
        if (isLocationPropertyChange(pce)) return; // Skip changes associated with location data

        if (logger.isDebugEnabled()) logger.debug("PCE: source={} prop={}", pce.getSource(), pce.getPropertyName());

        persistQueue.add((IMapObject) pce.getSource());
    }

    /**
     * Persist the given Map Object, ensuring that it has not been deleted first.
     */
    private void persistMapObject(IMapObject mapObject)
    {
        if (mapObjectManager.keyExists(mapObject.getKey())) // Ensure it hasn't been deleted
        {
            logger.debug("Persist MO: {}", mapObject);

            try
            {
                persistServiceProvider.getPersistService().forceUpdate(mapObject);
            } catch (Exception e)
            {
                logger.error("Failed to persist MO: {}", mapObject.getKey(), e);
            }
        }
    }

    /**
     * Add listener to each new Map Object.
     */
    private void onMapObjectAdded(@Observes @Priority(PLATFORM_BEFORE) MapObjectAdded event)
    {
        logger.debug("{}", event);

        final var mapObject = event.getMapObject();

        // Since transient MOs are not persisted, there's no need to track them
        if (!mapObject.isTransient())
        {
            mapObject.addPropertyChangeListener(this);
            monitoredMapObjects.add(mapObject);
        }
    }

    /**
     * Remove our listener from the Map Object.
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        logger.debug("{}", event);

        final IMapObject mapObject = event.getMapObject();
        mapObject.removePropertyChangeListener(this);
        monitoredMapObjects.remove(mapObject);
    }

    /**
     * Immediately process all pending persist operations when the project is saved.
     */
    private void onProjectSaving(@Observes ProjectSaving event)
    {
        persistQueue.processNow(true);
    }

    /**
     * Immediately process all pending persist operations when core services are stopped.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        persistQueue.processNow(true);

        // Cleanup all MOs we're monitoring
        monitoredMapObjects.forEach(mo -> mo.removePropertyChangeListener(this));
        monitoredMapObjects.clear();
    }

    /**
     * Timed queue to persist Map Objects, using a single executor thread.
     */
    private class BatchQueue extends TimeBatchedQueue<Set<IMapObject>, IMapObject>
    {
        private static final long INITIAL_DELAY_MS = 500; // 0.5 seconds
        private static final long MAX_WAIT_MS = 2_000;    // 2 seconds

        private final BasicThreadFactory threadFactory = new BasicThreadFactory.Builder()
                .namingPattern("MO-PERSIST-%d")
                .daemon(true)
                .priority(Thread.NORM_PRIORITY + 2) // Bump the thread's priority
                .build();

        private final ExecutorService batchExecutor = Executors.newSingleThreadExecutor(threadFactory);

        BatchQueue()
        {
            super("MO Persist", INITIAL_DELAY_MS, MAX_WAIT_MS, HashSet::new);
        }

        @Override
        protected ExecutorService getExecutorService()
        {
            return batchExecutor;
        }

        @Override
        protected void setThreadName()
        {
            // Do nothing, the thread name generated by the thread factory is sufficient
        }

        @Override
        protected void runBatch(Set<IMapObject> batch)
        {
            logger.debug("run batch: {}", batch);

            batch.forEach(MapObjectChangeMonitor.this::persistMapObject);

            persistServiceProvider.cleanupPersistService();
        }
    }
}
