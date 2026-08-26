package gov.takx.platform.globe.renderer;

import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IPluginRendererManager;
import gov.takx.api.globe.IRenderingEngine;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic implementation of an {@link IRenderingEngine}.
 *
 * @since 2.29
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class ARenderingEngine implements IRenderingEngine, IMapObjectManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Min time between scene regeneration from live data in milliseconds.
    private static final long LIVE_REGEN_LIMIT_MS = 800;

    @Inject
    protected IMapObjectManager mapObjectManager;

    @Inject
    private IGlobeManager globeManager;

    @Inject
    protected IPluginRendererManager pluginRendererManager;

    protected final IGlobeDelegate raptorGlobe;

    // Whether the regionMap data structure is stale and should be updated.
    private volatile boolean viewStale = true;

    // All the map objects we are monitoring
    private final Map<Integer, IMapObject> mapObjects = new HashMap<>();
    private final PropertyChangeListener visibilityChangeListener = this::mapObjectVisibilityChanged;

    protected final BitSet mapObjectVisible = new BitSet(); // High level visibility control for map objects
    protected final Map<Integer, Long> deleteTimes = new ConcurrentHashMap<>();

    // This task is used to ensure a scene regeneration after the last RDM or MO add/delete when data is arriving
    // faster than the throttling limit.
    private final Runnable regenRunnable = () -> regenerateView("Catch up on pending changes");

    protected final TimeBoundedTask sceneRegenThrottle =
            new TimeBoundedTask("Scene Regen", LIVE_REGEN_LIMIT_MS / 2, LIVE_REGEN_LIMIT_MS, regenRunnable);

    protected final Object shutdownLock = new Object();
    protected volatile boolean shutdown = false;

    /**
     * Constructor.
     *
     * @param raptorGlobe Globe we are rendering for
     */
    protected ARenderingEngine(IGlobeDelegate raptorGlobe)
    {
        this.raptorGlobe = raptorGlobe;
    }

    /**
     * Set up our world.  Load in all the existing map object data.
     *
     * <em>Note: </em> this method must be called by the subclass constructor when it is ready to initialize.
     */
    protected void initialize()
    {
        // Register as a map object manager listener (so we get notified of map object creation/deletion)
        mapObjectManager.addMapObjectManagerListener(this);

        // Loop through all existing map objects and add them right away
        for (var mapObject : mapObjectManager.getMapObjects()) clientMapObjectAdded(mapObject);
    }

    /**
     * @return All the MapObjects in our tracking map
     * @since 3.0
     */
    protected Collection<IMapObject> getMapObjects()
    {
        return mapObjects.values();
    }

    @Override
    public boolean isViewStale()
    {
        return viewStale;
    }

    @Override
    public void setViewStale(boolean viewStale)
    {
        this.viewStale = viewStale;
    }

    @Override
    public abstract void preRender(DrawContext dc);

    @Override
    public abstract void render(DrawContext dc);

    @Override
    public abstract void setZOrder(@Nonnull IMapObject mapObject, int zOrder);

    private void mapObjectVisibilityChanged(PropertyChangeEvent pce)
    {
        final IMapObject mapObject = (IMapObject) pce.getSource();
        final boolean visible = (boolean) pce.getNewValue();

        mapObjectVisible.set(mapObject.getId(), visible);

        // Force a regeneration so we re-render with updated visibility settings
        regenerateView("MO Visibility change");
    }

    /**
     * Force a regeneration of the scene (mark it as stale and force a repaint of the globes).
     *
     * @param reason The reason for the regeneration, used in the GlobeDirtyEvent
     * @see IGlobeManager#requestRedraw(String)
     */
    protected void regenerateView(String reason)
    {
        if (shutdown) return;

        logger.debug("regenerateView: {}", reason);

        setViewStale(true);
        discardCurrentScene();

        globeManager.requestRedraw(reason);
    }

    /**
     * Called when the scene needs to be regenerated.  If the subclass has any saved scene state, it should be discarded.
     */
    protected void discardCurrentScene()
    {
    }

    /**
     * Process a single message coming into the renderer.  Since the rendering model purposefully doesn't fire model
     * changes for a single message, we need to handle it internally.
     *
     * @param rdm New message
     */
    protected abstract void processSingleMessage(IRaptorDataMessage rdm);

    @Override
    public boolean canHaveMapData(@Nonnull IMapObject mapObject)
    {
        return mapObject.canHaveStyledTrackData();
    }

    @Override
    public void clientMapObjectAdded(@Nonnull IMapObject mapObject)
    {
        final boolean renderingExcluded = pluginRendererManager.isRenderingExcluded(getClass(), mapObject);

        logger.debug("MO: {}, excluded = {}", mapObject, renderingExcluded);

        // Only process map objects that can produce track points, and are not configured to be excluded for this renderer.
        if (canHaveMapData(mapObject) && !renderingExcluded)
        {
            final Integer moId = mapObject.getId();
            mapObjects.put(moId, mapObject);
            mapObjectVisible.set(moId, mapObject.isVisible());
            mapObject.addPropertyChangeListener(IMapObject.VISIBILITY, visibilityChangeListener);
            sceneRegenThrottle.request();

            handleMapObjectAdded(mapObject);
        }
    }

    /**
     * Handle a newly discovered Map Object.  This may occur at startup time as we load data from the database,
     * or as a new MO comes online.
     *
     * @param mapObject Map Object to process
     * @since 1.49
     */
    protected abstract void handleMapObjectAdded(IMapObject mapObject);

    @Override
    public void clientMapObjectRemoved(@Nonnull IMapObject mapObject)
    {
        if (canHaveMapData(mapObject))
        {
            cleanupMapObject(mapObject, true);

            sceneRegenThrottle.request();

            handleMapObjectDeleted(mapObject);
        }
    }

    /**
     * Perform any renderer-specific handling of a Map Object deletion.
     *
     * @param mapObject MapObject being affected
     * @since 1.49
     */
    protected abstract void handleMapObjectDeleted(IMapObject mapObject);

    @Override
    public void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
        final Integer moId = mapObject.getId();

        deleteTimes.put(moId, timeOfDeletion); // Track deletion time
        handleMapObjectPartialDelete(mapObject, timeOfDeletion);
    }

    /**
     * Perform any renderer-specific handling of a partial data delete for a Map Object.
     *
     * @param mapObject      MapObject being affected
     * @param timeOfDeletion The time of deletion, all RDMs earlier than this cutoff are being deleted
     * @since 1.49
     */
    protected abstract void handleMapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion);

    /**
     * Clean up all our handling on this map object.  Remove all the listeners we added.
     *
     * @param mapObject    Map Object to process
     * @param beingDeleted Pass {@code true} if this MO is being deleted, {@code false} if we're just cleaning up on deactivation
     */
    protected void cleanupMapObject(@Nonnull IMapObject mapObject, boolean beingDeleted)
    {
        final Integer moId = mapObject.getId();

        mapObjects.remove(moId);
        mapObjectVisible.set(moId, false); // just mark it as invisible

        mapObject.removePropertyChangeListener(IMapObject.VISIBILITY, visibilityChangeListener);
    }

    @Override
    public void deactivate()
    {
        logger.debug("deactivating");

        shutdown = true; // We are now

        mapObjectManager.removeMapObjectManagerListener(this);
        mapObjects.values().forEach(mo -> mo.removePropertyChangeListener(IMapObject.VISIBILITY, visibilityChangeListener));

        // Synchronized to ensure clean management of the background data loading threads
        synchronized (shutdownLock)
        {
            sceneRegenThrottle.shutdown();

            handleDeactivate();
            clearRenderableState();

            mapObjects.clear();
            mapObjectVisible.clear();
            deleteTimes.clear();
        }
    }

    /**
     * Renderer-specific handling for deactivation.
     *
     * @since 1.49
     */
    protected abstract void handleDeactivate();

    /**
     * Renderer-specific handling of how to clear the current renderable state.
     *
     * @since 1.49
     */
    protected abstract void clearRenderableState();

    @Override
    public void shutdown(boolean isPrimary)
    {
        logger.debug("shutting down");

        deactivate();
    }

    /**
     * Determine if we know about a given MapObject.  If it has been deleted, we no longer "know" about it.
     *
     * @param moId ID of MO of interest
     * @return {@code true} if we know about this MO
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean mapObjectExists(Integer moId)
    {
        return mapObjects.containsKey(moId);
    }
}
