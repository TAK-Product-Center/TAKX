package gov.takx.core.globe.render;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfaceSector;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectControllerListener;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IMapPointPersist;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentListener;
import gov.takx.api.tracks.internal.ISmartTrackAssignmentManager;
import gov.takx.api.ui.event.RenderingModelChangedEvent;
import gov.takx.core.globe.utils.IVisibilityHelper;
import gov.takx.core.globe.utils.StylingContextKey;
import gov.takx.core.globe.utils.SummaryCounter;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.core.mapdata.MapPointCache;
import gov.takx.core.mapdata.TrackPoint;
import gov.takx.platform.globe.renderer.ARenderingEngine;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.tracks.Groupability;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Base implementation of a rendering engine that operates on Tracks that can be styled using a
 * {@link ITrackStyleDefinition}.
 *
 * @param <SG> Type of scene generator in use for this renderer
 * @since 1.49
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AStyledRenderingEngine<SG extends ISceneGenerator>
        extends ARenderingEngine
        implements IMapObjectControllerListener, ISmartTrackAssignmentListener, IVisibilityHelper, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Turn on logging for this logger namespace to enable debug rendering for the rendering engines such as
     * drawing the sector outlines on the globe when it is tilted past 45 degrees.
     *
     * @since 2.36
     */
    public static final String RENDERING_DEBUG_NAMESPACE = "rendering.debug.";

    @Inject
    protected IPreferenceManager preferenceManager;

    @Inject
    protected ISmartTrackAssignmentManager smartTrackAssignmentManager;

    @Inject
    protected ICoreExecutorService executorService;

    @Inject
    protected IGlobeManager globeManager;

    @Inject
    protected EventBus eventBus;

    @Inject
    protected IMapPointPersist mapPointPersist;

    protected final RenderingContext renderingContext;
    protected final RenderingStyleContextManager scManager;

    // The maximum number of objects we want to render in any scene
    protected int renderingGoal;

    // The renderer for our markers
    protected GlobeMapPointRenderer markerRenderer = new GlobeMapPointRenderer();

    // Current rendering set
    protected final Map<Integer, MapObjectMarkerList> markerLists = new ConcurrentHashMap<>();
    protected final List<Renderable> renderables = new ArrayList<>();

    // Map of MapObject Id to Z order
    private final Map<Integer, Integer> zOrdering = new ConcurrentHashMap<>(50);
    private final ZOrderedComparator zOrderedComparator = new ZOrderedComparator();
    private Collection<MapObjectMarkerList> orderedMarkerLists;

    protected long lastVisibleSCKey; // The last SC key we tested that was visible

    // The set of map point ids we are currently in the process of loading
    protected final IntSet pendingMapPoints = HashIntSets.newMutableSet(500);

    // A cache for MapPoints to ensure we don't grow memory without bound
    // NOTE: This is static so it lives across rendering engine instantiations
    private static MapPointCache mapPointCache;

    protected int frame; // Current frame (calculated scene)

    // Future holding any pending scene generation currently under way
    protected Future<?> currentSceneGenerator;

    private final IPreferenceMonitor<String> trackDetailLevelMonitor = UserPreferenceEntry.TRACK_DETAIL_LEVEL.newStringMonitor(this::trackDetailLevelChanged);

    /**
     * Constructor.
     *
     * @param globe Globe we are rendering for
     * @param renderingContext The rendering context we are working within
     */
    protected AStyledRenderingEngine(IApplicationGlobe globe, RenderingContext renderingContext)
    {
        super(globe);

        this.renderingContext = renderingContext;

        // Believe it or not, calls to renderingContext.getStyleContextManager later account for a lot of time.
        // These operations are called millions of times per rendering cycle.
        scManager = renderingContext.getStyleContextManager();
    }

    @PostConstruct
    private void postConstruct()
    {
        renderingGoal = getMaxTrackPointsUserPreference();
//        renderingGoal = 200;

        if (mapPointCache == null)
        {
            mapPointCache = new MapPointCache(calculateMapPointCacheSize(renderingGoal));
        } else
        {
            mapPointCache.setMaxEntries(calculateMapPointCacheSize(renderingGoal));
        }

        logger.debug("Rendering Goal = {}", renderingGoal);

        smartTrackAssignmentManager.addAssignmentListener(this);
        eventBus.register(this);
    }

    /**
     * Calculate the size of the map point cache based on the given rendering goal.
     *
     * @param renderingGoal Target rendering goal
     * @return The size to use for the map point cache
     * @since 2.8
     */
    private int calculateMapPointCacheSize(int renderingGoal)
    {
        // If the rendering goal is really low (like < 500), then we want to ensure the cache is large enough to be
        // useful with locally dense data.  So, set a minimum size.

        return Math.max(5000, renderingGoal * 5);
    }

    /**
     * Handle a change in the data model, mark the scene as stale.
     *
     * @param evt The event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onRenderingModelChanged(RenderingModelChangedEvent evt)
    {
        regenerateView(evt.getReason());
    }

    @Override
    public void updateView(DrawContext dc)
    {
        if (shutdown) return;

        // Keep in mind that this method is always invoked on the EDT

        try
        {
            // Clear the stale setting right away.  This will ensure that if another event is fired while we
            // are in the process of generating this frame, we will immediately re-generate on the next frame.
            // If we don't do this, we can end up with partially rendered scenes.

            setViewStale(false); // We've fixed it now

            final int currentFrame = ++frame; // At next frame

            final SG scene = createSceneGeneratorIfNeeded(currentFrame, dc);

            if (scene != null)
            {
                // Since we're going to generate a new scene, we can cancel the generation of the prior scene
                if (currentSceneGenerator != null)
                {
                    currentSceneGenerator.cancel(true);
                    currentSceneGenerator = null;
                }

                currentSceneGenerator = executorService.submit(() -> {
                    Thread.currentThread().setName(getClass().getSimpleName() + "SceneGenerator-" + currentFrame);

                    try
                    {
                        final long start = System.currentTimeMillis();

                        scene.generateScene();

                        // If the scene generation wasn't interrupted, then install the scene results
                        if (scene.isInterrupted())
                        {
                            logger.warn("Scene generator for frame {} interrupted - but we still got a result!", currentFrame);
                        } else
                        {
                            installSceneOnEDT(scene); // Get the scene installed (on the EDT)
                        }

                        long duration = System.currentTimeMillis() - start;
                        logger.debug("frame {} took {} ms", frame, duration);
                    } catch (InterruptedException e)
                    {
                        logger.debug("Generator frame {} interrupted", currentFrame);
                    } catch (Exception e)
                    {
                        logger.warn("Scene generator for frame {} failed", currentFrame, e);
                    }
                });
            }
        } catch (Exception e)
        {
            logger.error("Failed to render scene", e);
        }
    }

    /**
     * Determine if a new scene needs to be generated, and if so, construct the properly initialized scene generator
     * and return it, otherwise, return {@code null}.
     *
     * @param frame The frame being processed
     * @param dc    The DrawContext (for accessing scene geometry)
     * @return Scene generator if a new scene should be generated, else {@code null}
     * @since 2.8
     */
    protected abstract SG createSceneGeneratorIfNeeded(int frame, DrawContext dc);

    /**
     * Install the generated scene onto our view, ensuring coordination with shutdown handling and that
     * processing takes place on the EDT.
     *
     * @param scene The scene to install
     * @since 2.8
     */
    @InvokeOnEDT
    protected void installSceneOnEDT(SG scene)
    {
        synchronized (shutdownLock) // Ensure clean interaction with shutdown handling
        {
            if (shutdown) return; // Bail if we've already been shutdown

            clearSceneObjects(); // Remove prior scene contents
            installScene(scene); // Add new scene contents
            raptorGlobe.redraw(); // Ensure we redraw the globe
        }
    }

    /**
     * Install the generated scene onto our view, i.e., create the renderables to be shown on the globe.
     * This method will be called on the EDT.
     *
     * @param scene The scene to install
     * @since 2.8
     */
    protected abstract void installScene(SG scene);

    /**
     * Add renderables to the scene that represent a collection of sector boundaries.
     *
     * @param sectors Sectors for which to create renderables
     * @param color   Color to use for the renderable (line color)
     * @since 2.8
     */
    protected void addSectorRenderables(Collection<Sector> sectors, Material color)
    {
        sectors.stream()
                .map(sector -> makeSectorBoundsShape(sector, color))
                .forEach(renderables::add);
    }

    /**
     * Add a collection of lists of TrackPoints to the rendered scene.  Recompute the ordered marker lists
     * after the points have been added.
     *
     * @param trackPointLists Collection of lists of TrackPoints to process
     * @since 2.8
     */
    protected void addTrackPointsToScene(Collection<List<TrackPoint>> trackPointLists)
    {
        // We gather up RDM ids that are not in the cache so we can load them all at once
        IntSet neededMapPointIds = HashIntSets.newMutableSet(renderingGoal * 2);

        trackPointLists.stream()
                .flatMap(List::stream)
                .forEach(tp -> addPointRenderable(tp, neededMapPointIds));

        // Ok, we may have a list of map point ids that we need to pull into memory and add to our scene
        if (!neededMapPointIds.isEmpty())
        {
            loadMapPoints(neededMapPointIds);
        }

        makeOrderedMarkerLists();
    }

    @Override
    public void setZOrder(@Nonnull IMapObject mapObject, int zOrder)
    {
        final Integer moId = mapObject.getId();

        // The Id can be null in some cases (usually when the MO has been deleted and an operation to deal with
        // that deletion is happening on a different thread (like the EDT)).
        if (moId == null)
        {
            logger.debug("Ignored Z order setting on null ID: {}", mapObject);
            return;
        }

        zOrdering.put(moId, zOrder);
        regenerateView("updated Z order");
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

        if (propName.equals(IMapObject.TRACK_STYLE))
        {
            // Force a regeneration so we re-render with updated track style
            regenerateView("TSD changed on MO");
        }
    }

    /**
     * Handle a change to track detail level.
     */
    @InvokeOnEDT
    private void trackDetailLevelChanged(String trackDetailLevel)
    {
        // Update state.
        renderingGoal = getMaxTrackPointsUserPreference();
        mapPointCache.setMaxEntries(calculateMapPointCacheSize(renderingGoal));

        // Redraw.
        regenerateView("User changed track detail level.");
    }

    @Override
    public void preRender(DrawContext dc)
    {
        renderables.stream()
                .filter(renderable -> renderable instanceof PreRenderable)
                .forEach(renderable -> ((PreRenderable) renderable).preRender(dc));
    }

    @Override
    public void render(DrawContext dc)
    {
        for (Renderable renderable : renderables)
        {
            renderable.render(dc);
        }

        // Just in case no detail rendering has occurred
        if (orderedMarkerLists == null)
        {
            orderedMarkerLists = markerLists.values();
        }

        markerRenderer.renderMarkerLists(dc, orderedMarkerLists);
    }

    /**
     * Clear out any previous renderables and the marker lists from the previous scene.
     */
    protected void clearSceneObjects()
    {
        renderables.clear();

        markerLists.values().forEach(MapObjectMarkerList::clear);

        orderedMarkerLists = null;
    }

    @Override
    protected void clearRenderableState()
    {
        // Clear out anything from the last scene setup
        renderables.clear();

        markerLists.values().forEach(MapObjectMarkerList::clear);

        markerLists.clear();

        if (orderedMarkerLists != null)
        {
            orderedMarkerLists.clear();
        }

        // Clear our pending map points since we don't need them anymore
        synchronized (pendingMapPoints)
        {
            pendingMapPoints.clear();
        }
    }

    /**
     * Create the ordered marker lists based on the Z ordering currently applied
     */
    protected void makeOrderedMarkerLists()
    {
        if (zOrdering.isEmpty())
        {
            orderedMarkerLists = markerLists.values(); // No ordering, use as is
        } else
        {
            Set<MapObjectMarkerList> ordered = new TreeSet<>(zOrderedComparator);
            ordered.addAll(markerLists.values());
            orderedMarkerLists = ordered;
        }
    }

    @Override
    protected void processSingleMessage(IRaptorDataMessage rdm)
    {
        // If this new point is within our rendered scene, then mark the view as stale and redraw
        if (isPointInScene(rdm))
        {
            // Since it's in our view, make a map point for it right away
            MapPoint mapPoint = new MapPoint(rdm);

            final GlobeMarkerAttributes markerAttributes = renderingContext.getMarkerAttrTracker()
                    .getMarkerAttributesForMapPoint(mapPoint, rdm.getTrackStyleDefinitionId(), rdm.getOverrideTrackStyleDefinitionId());
            mapPoint.setAttributes(markerAttributes);
            mapPointCache.put(mapPoint.getRdmId(), mapPoint);

            // Ping the regen task, it will handle timing of actual processing of new data
            sceneRegenThrottle.request();
        }
    }

    /**
     * Determine if an RDM's map point is in the current scene.
     *
     * @param rdm The RDM to test
     * @return {@code true} if the RDMs location is within the current scene boundaries
     */
    protected abstract boolean isPointInScene(IRaptorDataMessage rdm);

    /**
     * Add a track point marker as a renderable to our scene.  If the map point was not in the cache, then add it
     * to the list of needed map points.
     *
     * @param point             The point to add
     * @param neededMapPointIds The collection of map points that need to be pulled into memory (will be updated with
     *                          the map points not already in the cache)
     */
    protected void addPointRenderable(TrackPoint point, IntSet neededMapPointIds)
    {
        MapPoint mapPoint = mapPointCache.get(point.rdmId);

        if (mapPoint != null)
        {
            markerLists.get(point.moId).addMapPoint(mapPoint);
        } else
        {
            // Add it to the list of needed points (if not already pending)
            final int rdmId = point.rdmId;
            synchronized (pendingMapPoints)
            {
                if (!pendingMapPoints.contains(rdmId))
                {
                    neededMapPointIds.add(rdmId);
                }
            }
        }
    }

    @Override
    protected void handleMapObjectAdded(IMapObject mapObject)
    {
        mapObject.getController().addMapObjectControllerListener(this);

        mapObject.addPropertyChangeListener(IMapObject.TRACK_STYLE, this);

        // Construct the marker list for this map object
        markerLists.put(mapObject.getId(), new MapObjectMarkerList(mapObject));
    }

    @Override
    protected void cleanupMapObject(@Nonnull IMapObject mapObject, boolean beingDeleted)
    {
        super.cleanupMapObject(mapObject, beingDeleted);

        removeMapObjectListeners(mapObject);

        final Integer moId = mapObject.getId();

        markerLists.remove(moId);
        zOrdering.remove(moId);

        if (beingDeleted)
        {
            // And clean out any MapPoints that we have cached for this MO
            mapPointCache.cleanMapObject(moId);
        }
    }

    /**
     * Remove listeners associated with the given Map Object.
     *
     * @since 3.0
     */
    private void removeMapObjectListeners(@Nonnull IMapObject mapObject)
    {
        final var moc = mapObject.getController();
        if (moc != null) moc.removeMapObjectControllerListener(this);

        mapObject.removePropertyChangeListener(IMapObject.TRACK_STYLE, this);
    }

    @Override
    protected void handleMapObjectDeleted(IMapObject mapObject)
    {
        // Nothing to do here, base class did it all
    }

    @Override
    protected void handleMapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
        mapPointCache.cleanMapObject(mapObject.getId(), timeOfDeletion); // Clean the cache
    }

    @Override
    public synchronized void receiveMessage(IRaptorDataMessage rdm)
    {
        if (renderingContext.getRenderingModel().isMessageExcluded(rdm)) return;

        logger.trace("receiveMessage(): {}", rdm);

        // If we don't have the MO for this RDM in our data, then assume it has been deleted
        final Integer moId = rdm.getMapObject().getId();
        if (!mapObjectExists(moId))
        {
            logger.debug("No MO for RDM: id={}", moId);
            return;
        }

        // NOTE: The rendering model doesn't fire a model change event (on purpose) for single message receives,
        // so we need to update our scene accordingly.
        processSingleMessage(rdm);
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, ITrackStyleDefinition tsd, IRaptorDataMessage rdm)
    {
        updateMapPointWithTsd(rdm.getTrackStyleDefinitionId(), rdm.getOverrideTrackStyleDefinitionId(), rdm.getIdAsInt());
    }

    @Override
    public void onTSDAssignedToRDM(Integer oldTsdId, Integer tsdId, int moId, int rdmId)
    {
        // Since we are called with the tsdId as the current *active* TSD Id, we don't need to worry about the actual
        // override TSD Id.  Just pass in null and that will cause the active TSD Id to be used.

        updateMapPointWithTsd(tsdId, null, rdmId);
    }

    /**
     * Update the MapPoint for the given RDM (by Id) with the appropriate new marker attributes based on the
     * permanent and override TSD Ids.
     *
     * @param tsdId         New permanent TSD Id
     * @param overrideTsdId Current override TSD Id
     * @param rdmId         RDM Id
     */
    private void updateMapPointWithTsd(Integer tsdId, Integer overrideTsdId, int rdmId)
    {
        // We need to update the MapPoint for this RDM to use a new styling context.
        final MapPoint mapPoint = mapPointCache.get(rdmId);

        if (mapPoint != null)
        {
            logger.debug("reset attrs for map point: {}", mapPoint);

            final MarkerAttributeTracker mat = renderingContext.getMarkerAttrTracker();

            // This method is called with the active TSD id as the tsdId, so we don't need to go figure out
            // if the override TSD is involved, our caller has done that for us.

            final GlobeMarkerAttributes attrs = mat.getMarkerAttributesForMapPoint(mapPoint, tsdId, overrideTsdId);

            if (attrs != null) mapPoint.setAttributes(attrs);
        }
    }

    @Override
    public int getVisibleCount(SummaryCounter summaryCounter)
    {
        int[] countData = new int[2];
        getVisibleCountData(summaryCounter, countData);
        return countData[TOTAL_COUNT];
    }

    @Override
    public void getVisibleCountData(SummaryCounter summaryCounter, int[] countData)
    {
        countData[GROUP_COUNT] = countData[TOTAL_COUNT] = 0;

        summaryCounter.forEachEntry((mapKey, counter) -> {
            // To be visible, the map object must be visible and the styling context must be visible
            final int moId = StylingContextKey.getMapObjectIdFromMapKey(mapKey);
            final int rdmTsdId = StylingContextKey.getRdmTsdIdFromMapKey(mapKey);

            final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, rdmTsdId);

            if (mapObjectVisible.get(moId) && tsc.isVisible())
            {
                final int count = counter.count;
                if (count != 0)
                {
                    // Keep track of the last SC key that we found to be visible in this counters data.  This will
                    // be used in the detail layout to determine the SC key for the singleton to render in the
                    // case that we calculate a visible count of 1.
                    lastVisibleSCKey = mapKey;

                    countData[TOTAL_COUNT] += count;

                    // If this group is marked as "ungroupable" then we need to treat is as "count" groups

                    if (tsc.getGroupability() == Groupability.Ungroupable)
                    {
                        countData[GROUP_COUNT] += count;
                    } else
                    {
                        countData[GROUP_COUNT] += 1;
                    }
                }
            }
            return true;
        });
    }

    @Override
    public long getLastVisibleStylingContextKey()
    {
        return lastVisibleSCKey;
    }

    @Override
    public boolean isStylingContextVisible(StylingContextKey scKey)
    {
        final ITrackStylingContext tsc = scManager.getTrackStylingContext(scKey);
        final int moId = scKey.mapObjectId;

        return (mapObjectVisible.get(moId) && tsc.isVisible());
    }

    @Override
    public boolean isStylingContextVisible(long scMapKey)
    {
        final int moId = StylingContextKey.getMapObjectIdFromMapKey(scMapKey);
        final int rdmTsdId = StylingContextKey.getRdmTsdIdFromMapKey(scMapKey);

        final ITrackStylingContext tsc = scManager.getTrackStylingContext(moId, rdmTsdId);

        return (mapObjectVisible.get(moId) && tsc.isVisible());
    }

    /**
     * Get the max number of allowable track points based on the user's preference. If unable to obtain
     * the user's preference, the value of {@link RendererConstants#LOW_TRACK_DETAIL_LEVEL} will be used.
     *
     * @return The max number of track points that should be displayed before applying grouping logic.
     */
    protected int getMaxTrackPointsUserPreference()
    {
        // Check the user's preference.
        final String userSetting = UserPreferenceEntry.TRACK_DETAIL_LEVEL.getStringValue();

        final int max;

        // Check for ludicrous mode.
        if ("ludicrous".equals(userSetting))
        {
            max = 1_000_000;
        } else
        {
            // Otherwise, check the definition of the ProgramProperties' listing of the detail levels.

            max = PreferenceEntry.TRACK_DETAIL_LEVELS_NODE.node().getInt(userSetting, RendererConstants.DEFAULT_LOW_DETAIL_VISIBLE_TRACKS);
        }

        return max;
    }

    /**
     * Create a renderable to outline a given sector.
     *
     * @param sector   Sector to outline
     * @param material The material (color) to use for the shape outline
     * @return renderable showing that outline
     */
    protected Renderable makeSectorBoundsShape(Sector sector, Material material)
    {
        final ShapeAttributes attributes = new BasicShapeAttributes();

        attributes.setDrawInterior(false);
        attributes.setDrawOutline(true);
        attributes.setOutlineMaterial(material);
        attributes.setOutlineOpacity(1.0);
        attributes.setOutlineWidth(2);

        final SurfaceSector ss = new SurfaceSector(attributes, sector);
        ss.setPathType(AVKey.RHUMB_LINE);
        return ss;
    }

    @Override
    protected void handleDeactivate()
    {
        eventBus.unregister(this);

        getMapObjects().forEach(this::removeMapObjectListeners); // Stop listening to all MOCs

        // Shutdown our scene generator if we have one active
        if (currentSceneGenerator != null)
        {
            currentSceneGenerator.cancel(true);
            currentSceneGenerator = null;
        }

        smartTrackAssignmentManager.removeAssignmentListener(this);
        trackDetailLevelMonitor.dispose();
    }

    @Override
    public void shutdown(boolean isPrimary)
    {
        super.shutdown(isPrimary);

        // Since we are shutting down (as part of a project-level clear state/shutdown) we need to flush the MP cache
        mapPointCache.clear();
    }

    /**
     * Load the map points for the given RDM ids into memory and into our rendering set.  This is done in the background
     * to avoid hanging the EDT.
     *
     * @param neededMapPointIds List of RDM ids for which we need map points loaded
     */
    protected void loadMapPoints(final IntSet neededMapPointIds)
    {
        // Add the list of ids to the pending set so we avoid trying to retrieve them more than once
        synchronized (pendingMapPoints)
        {
            pendingMapPoints.addAll(neededMapPointIds);
        }

        // Toss this into the background

        executorService.execute(() -> {
            Thread.currentThread().setName("MapPoint Loader: " + neededMapPointIds.size() + " points");

            final MarkerAttributeTracker mat = renderingContext.getMarkerAttrTracker();

            final int sz = neededMapPointIds.size();
            final IntCursor cursor = neededMapPointIds.cursor();

            // We can't just fire off a select with N thousand ids in the "in" clause - it performs
            // like crap.  So, we batch them up.  More selects with smaller in clauses is much faster.

            final int BATCH_SIZE = 400;
            final Set<Integer> ids = new HashSet<>(BATCH_SIZE);
            int index = 0;
            long start = System.currentTimeMillis();

            while (index < sz)
            {
                long innerStart = System.currentTimeMillis();

                int batch = index + BATCH_SIZE < sz ? BATCH_SIZE : sz - index;
                ids.clear();

                for (int i = 0; i < batch; i++)
                {
                    cursor.moveNext();
                    ids.add(cursor.elem());
                }

                index += batch;

                // Find the MapPoints by the RDM Ids
                final List<MapPoint> batchPoints = mapPointPersist.getByIds(ids);

                // Synchronized to ensure clean access to data in the renderer (which may be shutting down)
                synchronized (shutdownLock)
                {
                    if (shutdown) return; // Bail if we've been shutdown

                    if (batchPoints.size() != batch)
                    {
                        logger.info("MapPoint load mismatch: expected {}, got {}", batch, batchPoints.size());

                        // In the case of a failure to load the desired MapPoints, we need to clean up pendingMapPoints
                        // so that it doesn't think we are still waiting on them (since they're never coming at
                        // this point).

                        synchronized (pendingMapPoints)
                        {
                            pendingMapPoints.removeAll(ids);
                        }
                    }

                    // Prepare this batch of results and add it to the rendering lists
                    for (MapPoint mapPoint : batchPoints)
                    {
                        final int moId = mapPoint.getMoId();

                        // If the MO doesn't exist, we should just ditch this new MapPoint.
                        if (!mapObjectExists(moId)) continue;

                        // Or, if we have a delete time recorded for it, test against that
                        final Long deleteTime = deleteTimes.get(moId);
                        if (deleteTime != null && deleteTime >= mapPoint.getTime()) continue;

                        // Configure the map point with the rendering attributes
                        mapPoint.setAttributes(mat.getMarkerAttributesForMapPoint(mapPoint));

                        // Add the map point to the cache and remove it from the pending set
                        final int rdmId = mapPoint.getRdmId();
                        mapPointCache.put(rdmId, mapPoint);
                        synchronized (pendingMapPoints)
                        {
                            pendingMapPoints.removeInt(rdmId);
                        }

                        // Add the map point to the rendering set
                        markerLists.get(moId).addMapPoint(mapPoint);
                    }
                }

                if (logger.isTraceEnabled())
                {
                    long innerEnd = System.currentTimeMillis();
                    logger.trace("Batch of {} took {} ms", batch, innerEnd - innerStart);
                }

                // Get these new points rendered
                globeManager.requestRedraw("MapPoints loaded");
            }

            if (logger.isDebugEnabled())
            {
                long end = System.currentTimeMillis();
                logger.debug("Loading {} map points took {} ms", sz, end - start);
            }
        });
    }

    /**
     * Internal class to provide an ordering on MapObjectMarkerList entries in a collection.  This
     * sorting enforces the Z ordering imposed on our rendering.
     */
    private class ZOrderedComparator implements Comparator<MapObjectMarkerList>
    {
        /**
         * Compare two MOMLs and determine their ordering.  We invert the normal ordering to handle
         * OpenGL only rendering the first iterated object at a given depth.
         *
         * @param o1 first list
         * @param o2 second list
         * @return 1 for less, 0 for equal, -1 for greater (swapped vs. normal comparison)
         */
        @Override
        public int compare(MapObjectMarkerList o1, MapObjectMarkerList o2)
        {
            final int moId1 = o1.getMapObject().getId();
            final int moId2 = o2.getMapObject().getId();

            Integer zo1int = zOrdering.get(moId1);
            int zo1 = zo1int == null ? -1 : zo1int;

            Integer zo2int = zOrdering.get(moId2);
            int zo2 = zo2int == null ? -1 : zo2int;

            // Only equal if moIds are equal (or the set will fail to contain all the entries)
            return moId1 == moId2
                    ? 0
                    : zo1 <= zo2 ? 1 : -1;
        }
    }
}
