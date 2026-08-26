
package gov.takx.core.globe.tak;

import com.atakmap.map.layer.LegacyAdapters;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.control.FeatureEditControl;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.render.Renderable;
import gov.tak.api.util.Visitor;
import gov.tak.platform.util.LimitingThread;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.layer.feature.IFeatureRendererManager;
import gov.takx.api.layer.feature.ISharedFeatureDataStore;
import gov.takx.api.layer.pick.IPickContext;
import gov.takx.api.layer.pick.IPickedFeatureProvider;
import gov.takx.core.globe.tak.FeatureUtility.FeatureRegistration;
import gov.takx.core.globe.tak.FeatureUtility.FeatureSetRegistration;
import gov.takx.core.globe.tak.features.FeatureFactory;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureFactoryState;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureSetResult;
import gov.takx.core.globe.tak.layer.RenderableFeatureLayer;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A service that manages a {@link FeatureLayer3} for rendering WW renderables as TAK features. It is designed to collect renderables during the WW draw cycle via
 * {@link #offerRenderable(Renderable, IApplicationGlobe)}, and then submit them for processing during the call to {@link #render(IApplicationGlobe)} as a collective set.
 *
 * @since 3.4
 */
@ApplicationScoped
public class RenderableFeatureManager implements IGlobeLifeCycleListener, IPickedFeatureProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int PROPERTY_MASK = FeatureDataStore4.PROPERTY_FEATURE_NAME | FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY
            | FeatureDataStore4.PROPERTY_FEATURE_STYLE | FeatureDataStore4.PROPERTY_FEATURE_ATTRIBUTES
            | FeatureDataStore4.PROPERTY_FEATURE_ALTITUDE_MODE | FeatureDataStore4.PROPERTY_FEATURE_EXTRUDE;

    /**
     * A {@link gov.takx.api.globe.IGlobeMapObject} being edited that is associated with this renderable.
     *
     * @since 5.0
     */
    public static final String IS_EDITING_MAP_OBJECT = "IsEditingMapObject";
    /**
     * If present, indicates that the feature associated with this renderable is currently registered as being edited in the {@link FeatureEditControl}.
     */
    private static final String IS_EDITING_FEATURE = "IsEditingFeature";

    private final FeatureFactory featureFactory;
    private final boolean headless;
    private final IExtendedGlobeManager globeManager;
    private final IFeatureRendererManager featureManager;

    private final FeatureDataStore4 dataStore;
    private RenderableFeatureLayer layer;
    // synchronized access only for registeredRenderables
    private final HashMap<Renderable, RenderableRegistration> registeredRenderables = new HashMap<>();
    // EDT access only for queuedRenderables
    private final HashMap<IApplicationGlobe, List<Renderable>> queuedRenderables = new HashMap<>();
    // Atomic access for incomingRenderables
    private final AtomicReference<HashMap<IApplicationGlobe, List<RenderableAndState>>> incomingRenderables = new AtomicReference<>(new HashMap<>());
    private LimitingThread registrationThread;

    @Inject
    RenderableFeatureManager(FeatureFactory featureFactory, IExtendedGlobeManager globeManager,
                             IFeatureRendererManager featureManager, ISharedFeatureDataStore featureDataStore,
                             @Headless boolean headless)
    {
        this.featureFactory = featureFactory;
        this.headless = headless;
        this.globeManager = globeManager;
        this.featureManager = featureManager;
        this.dataStore = featureDataStore;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        if (headless) return;

        event.getProgressMonitor().setText("Starting raster services");
        featureManager.addPickedFeatureProvider(this);

        layer = new RenderableFeatureLayer();

        registrationThread = new LimitingThread("RenderableFeatureManager registration thread", this::processQueuedRenderableList);

        globeManager.addGlobeLifeCycleListener(this);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        if (headless) return;
        globeManager.removeGlobeLifeCycleListener(this);

        if (registrationThread != null)
        {
            registrationThread.dispose();
            registrationThread = null;
        }

        if (dataStore != null) dataStore.dispose();
        queuedRenderables.clear();
        registeredRenderables.clear();
    }

    /**
     * Get the layer for this manager.
     * @return the layer
     * @since 3.5
     */
    public RenderableFeatureLayer getLayer()
    {
        return layer;
    }

    /**
     * Offer a {@link Renderable} to be rendered using TAK features. If the renderable is accepted this method will return true
     * and render its representation during the next render cycle. If the renderable is not offered during the next render cycle
     * then the renderable representation will no longer be drawn. This method is designed to only be called on the EDT.
     * @param renderable the renderable to be offered
     * @param globe the globe
     * @return true if the renderable is accepted
     * @since 3.5
     */
    public boolean offerRenderable(Renderable renderable, @Nonnull IApplicationGlobe globe)
    {
        if (featureFactory.isSupported(renderable))
        {
            List<Renderable> renderables = queuedRenderables.computeIfAbsent(globe, k -> new ArrayList<>());
            renderables.add(renderable);
            return true;
        }
        return false;
    }

    /**
     * Takes the queued list of {@link Renderable} for the current globe as a snapshot and push to background thread for further processing.
     * @param globe the globe
     */
    public void render(@Nonnull IApplicationGlobe globe)
    {
        // Push queued Renderables for this globe to the queuedRenderableList and fire thread
        List<Renderable> renderables = queuedRenderables.remove(globe);
        List<RenderableAndState> incomingRenderableList = renderables != null ? renderables.stream().map(r -> new RenderableAndState(r, featureFactory.createState(r))).toList() : Collections.emptyList();
        incomingRenderables.updateAndGet(map -> {
            map.put(globe, incomingRenderableList);
            return map;
        });
        // Even if the renderable list is empty, we want to signal the processing thread.
        // We may not have new renderables to subscribe to, however there could still be renderables that need to be unsubscribed.
        registrationThread.exec();
    }

    private FeatureSetRegistration createOrUpdate(Renderable renderable, @Nonnull FeatureSetResult featureSetResult, @Nullable Object state, @Nullable FeatureSetRegistration featureSetRegistration)
    {
        featureSetRegistration = FeatureUtility.applyFeatureResults(featureSetResult, state, dataStore, featureSetRegistration);

        if (featureSetRegistration != null && renderable instanceof AVList avList)
        {
            boolean isEditingGmo = AVListImpl.getBooleanValue(avList, IS_EDITING_MAP_OBJECT, false);
            boolean isEditingFeature = AVListImpl.getBooleanValue(avList, IS_EDITING_FEATURE, false);
            if (!isEditingGmo && isEditingFeature)
            {
                stopEditingFeature(featureSetRegistration);
                avList.setValue(IS_EDITING_FEATURE, false);
            }
            if (isEditingGmo && !isEditingFeature)
            {
                startEditingFeature(featureSetRegistration);
                avList.setValue(IS_EDITING_FEATURE, true);
            }
            if (isEditingGmo) updateEditingFeature(featureSetRegistration, featureSetResult);
        }
        return featureSetRegistration;
    }

    private void destroy(long fsid)
    {
        try
        {
            dataStore.deleteFeatureSet(fsid);
        } catch (DataStoreException e)
        {
            logger.error("Error deleting feature set from datastore.", e);
        }
    }

    /**
     * Ingest the provided {@link RenderableAndState} and write to the features database.
     * @param incomingRenderableIter the renderables and state objects
     * @param globe the globe
     */
    private void register(Iterable<RenderableAndState> incomingRenderableIter, IApplicationGlobe globe)
    {
        if (headless) return;
        synchronized (registeredRenderables)
        {
            incomingRenderableIter.forEach(rs -> {
                Renderable renderable = rs.renderable;
                Object state = rs.featureFactoryState.state();
                if (!registeredRenderables.containsKey(renderable))
                {
                    FeatureSetResult featureSetResult = featureFactory.create(rs.featureFactoryState, PROPERTY_MASK);
                    FeatureSetRegistration featureSetRegistration = createOrUpdate(renderable, featureSetResult, state, null);
                    if (featureSetRegistration != null)
                    {
                        RenderableRegistration registration = new RenderableRegistration(featureSetRegistration, new HashSet<>());
                        registration.references.add(globe);
                        registeredRenderables.put(renderable, registration);
                    }
                } else
                {
                    RenderableRegistration registration = registeredRenderables.get(renderable);
                    registration.references.add(globe);
                    if (registration.featureSetRegistration.stateKey() == null || !Objects.deepEquals(registration.featureSetRegistration.stateKey(), state))
                    {
                        FeatureSetResult featureSetResult = featureFactory.create(rs.featureFactoryState, PROPERTY_MASK);
                        FeatureSetRegistration featureSetRegistration = createOrUpdate(renderable, featureSetResult, state, registration.featureSetRegistration);
                        registration = new RenderableRegistration(featureSetRegistration, registration.references);
                        registeredRenderables.put(renderable, registration);
                    }
                }
            });
        }
    }

    /**
     * Ingest the provided {@link Renderable} and remove from the features database.
     * @param renderables the renderables
     * @param globe the globe
     */
    private void unregister(Iterable<Renderable> renderables, IApplicationGlobe globe)
    {
        synchronized (registeredRenderables)
        {
            renderables.forEach(renderable -> {
                RenderableRegistration registration = registeredRenderables.get(renderable);
                if (registration != null)
                {
                    if (renderable instanceof AVList avList && AVListImpl.getBooleanValue(avList, IS_EDITING_FEATURE, false))
                    {
                        stopEditingFeature(registration.featureSetRegistration);
                        avList.setValue(IS_EDITING_FEATURE, false);
                    }
                    registration.references.remove(globe);
                    if (registration.references.isEmpty())
                    {
                        registration.featureSetRegistration.fsidMap().values().forEach(this::destroy);
                        registeredRenderables.remove(renderable);
                    }
                }
            });
        }
    }

    @Override
    public Object getPickObject(@Nonnull IPickContext context, long fid)
    {
        // Micro-optimization for this high traffic method to use for loops rather than streams which incur unnecessary overhead.
        synchronized (registeredRenderables)
        {
            for (var entry : registeredRenderables.entrySet())
            {
                for (var featureRegistration : entry.getValue().featureSetRegistration.featureRegistrations().values())
                {
                    if (featureRegistration.fid() == fid) return !PickUtils.isNotPickable(entry.getKey()) ? entry.getKey() : null;
                }
            }
            return null;
        }
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        synchronized (registeredRenderables)
        {
            unregister(new ArrayList<>(registeredRenderables.keySet()), (IApplicationGlobe) globe);
        }
    }

    /**
     * Visit the {@link FeatureEditControl} for each globe.
     *
     * @param visitor the visitor
     * @since 5.0
     */
    private void visitControl(Visitor<FeatureEditControl> visitor)
    {
        globeManager.getAllGlobeDelegates().forEach(globe -> {
            FeatureLayer3 featureLayer = featureManager.getFeatureRenderer(globe);
            if (featureLayer != null) globe.getPortableMapRenderer().visitControl(LegacyAdapters.adapt2(featureLayer), visitor, FeatureEditControl.class);
        });
    }

    /**
     * Inform the feature renderer that the features listed in the provided {@link FeatureSetRegistration} are being edited.
     *
     * @param featureSetRegistration the feature set registration
     * @since 5.0
     */
    private void startEditingFeature(@Nonnull FeatureSetRegistration featureSetRegistration)
    {
        long[] selectedFeatureIds = featureSetRegistration.featureRegistrations().values().stream().mapToLong(FeatureRegistration::fid).toArray();
        visitControl(control -> Arrays.stream(selectedFeatureIds).forEach(control::startEditing));
    }

    /**
     * Inform the feature renderer that the features listed in the provided {@link FeatureSetRegistration} are no longer being edited.
     * @since 5.0
     */
    private void stopEditingFeature(@Nonnull FeatureSetRegistration featureSetRegistration)
    {
        long[] selectedFeatureIds = featureSetRegistration.featureRegistrations().values().stream().mapToLong(FeatureRegistration::fid).toArray();
        visitControl(control -> Arrays.stream(selectedFeatureIds).forEach(control::stopEditing));
    }

    /**
     * Called by {@link #registrationThread} to process any queued registrations or unregistrations for all globes.
     */
    private void processQueuedRenderableList()
    {
        HashMap<IApplicationGlobe, List<RenderableAndState>> incomingRenderablesMap = incomingRenderables.getAndSet(new HashMap<>());
        try
        {
            dataStore.acquireModifyLock(true);
            for (Map.Entry<IApplicationGlobe, List<RenderableAndState>> entry : incomingRenderablesMap.entrySet())
            {
                Set<Renderable> renderables = entry.getValue().stream().map(rs -> rs.renderable).collect(Collectors.toCollection(HashSet::new));
                List<Renderable> removedRenderables;
                synchronized (registeredRenderables)
                {
                    removedRenderables = registeredRenderables.keySet().stream()
                            .filter(l -> !renderables.contains(l))
                            .toList();
                }

                IApplicationGlobe globe = entry.getKey();
                if (globe == null) return;

                register(entry.getValue(), globe);
                unregister(removedRenderables, globe);
            }
        } catch (InterruptedException e)
        {
            logger.warn("Interrupted while acquiring modify lock on feature database.", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Inform the feature renderer that the provided {@link FeatureSetRegistration} for the current editing feature
     * should be updated to reflect the provided {@link FeatureSetResult}.
     *
     * @param featureSetRegistration the feature set registration
     * @param featureSetResult the feature set result
     * @since 5.0
     */
    private void updateEditingFeature(@Nonnull FeatureSetRegistration featureSetRegistration, FeatureSetResult featureSetResult)
    {
        Arrays.stream(featureSetResult.featureResults()).forEach(fr -> {
            FeatureRegistration featureRegistration = featureSetRegistration.featureRegistrations().get(fr.objectId());
            if (featureRegistration != null) visitControl(control -> control.updateFeature(featureRegistration.fid(), fr.updatePropertyMask(), fr.name(), fr.geometry(), fr.style(), fr.altitudeMode(), fr.extrude()));
        });
    }

    private record RenderableRegistration(FeatureSetRegistration featureSetRegistration, HashSet<IApplicationGlobe> references)
    {
    }

    /**
     * Holds a {@link Renderable} and its state to be processed on a background thread.
     * @param renderable the renderable
     * @param featureFactoryState the feature factory state
     * @since 5.1
     */
    private record RenderableAndState(Renderable renderable, @Nonnull FeatureFactoryState<Renderable, Object> featureFactoryState)
    {
    }
}
