package gov.takx.core.globe.tak;

import android.graphics.PointF;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore4;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.map.opengl.GLMapView;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.map.layer.IPickContext;
import gov.tak.core.map.layer.feature.IFeatureRendererManager;
import gov.tak.core.map.layer.feature.IPickedFeatureProvider;
import gov.tak.core.map.layer.feature.datastore.SharedFeatureDataStore;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.util.LimitingThread;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeAnnotationMapObject;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectChangeListener;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IGlobeRangeFanMapObject;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectSelectListener;
import gov.takx.api.mapobject.MapObjectSelectEvent;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.GlobeBullseyeMapObject;
import gov.takx.core.globe.GlobeIconMapObject;
import gov.takx.core.globe.GlobeRouteMapObject;
import gov.takx.core.globe.GlobeShapeMapObject;
import gov.takx.core.globe.StateIconsChanged;
import gov.takx.core.globe.tak.FeatureUtility.FeatureSetRegistration;
import gov.takx.core.globe.tak.features.FeatureFactory;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureFactoryState;
import gov.takx.core.globe.tak.features.FeatureFactory.FeatureSetResult;
import gov.takx.core.globe.tak.features.MilStdGlobeShapeFeatureProvider;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.AShapeMapObject;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.collections4.ListUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A service that manages a {@link FeatureLayer3} for rendering {@link IGlobeMapObject} objects as TAK features.
 *
 * @since 3.5
 */
@ApplicationScoped
public class GlobeMapObjectFeatureManager implements IGlobeMapObjectChangeListener, PropertyChangeListener, IMapObjectSelectListener, IPickedFeatureProvider, MapRenderer.OnCameraChangedListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static
    {
        // Register the marshals needed by shapes, shape editor control points, and Surface Images

        MarshalManager.registerMarshal(new AbstractMarshal(IShape.AltitudeMode.class, Feature.AltitudeMode.class)
        {
            @Override
            protected <T, V> T marshalImpl(V in)
            {
                //noinspection unchecked
                return (T) switch ((IShape.AltitudeMode) in)
                {
                    case ClampToGround -> Feature.AltitudeMode.ClampToGround;
                    case Absolute -> Feature.AltitudeMode.Absolute;
                    case Relative -> Feature.AltitudeMode.Relative;
                };
            }
        }, IShape.AltitudeMode.class, Feature.AltitudeMode.class);

        MarshalManager.registerMarshal(new AbstractMarshal(Feature.AltitudeMode.class, IShape.AltitudeMode.class)
        {
            @Override
            protected <T, V> T marshalImpl(V in)
            {
                //noinspection unchecked
                return (T) switch ((Feature.AltitudeMode) in)
                {
                    case ClampToGround -> IShape.AltitudeMode.ClampToGround;
                    case Absolute -> IShape.AltitudeMode.Absolute;
                    case Relative -> IShape.AltitudeMode.Relative;
                };
            }
        }, Feature.AltitudeMode.class, IShape.AltitudeMode.class);
    }

    private static final int PROPERTY_MASK = FeatureDataStore4.PROPERTY_FEATURE_NAME | FeatureDataStore4.PROPERTY_FEATURE_GEOMETRY
            | FeatureDataStore4.PROPERTY_FEATURE_STYLE | FeatureDataStore4.PROPERTY_FEATURE_ATTRIBUTES
            | FeatureDataStore4.PROPERTY_FEATURE_ALTITUDE_MODE | FeatureDataStore4.PROPERTY_FEATURE_EXTRUDE;

    /* Copied from GlobeShapeMapObject */
    private static final List<String> SHAPE_PROPERTIES = List.of(
            IMapObject.NAME, ShapeProperties.ANGLE, ShapeProperties.BEARING, ShapeProperties.EDITABLE,
            ShapeProperties.FILL_COLOR, ShapeProperties.GROUND_REFERENCE, ShapeProperties.HEIGHT, ShapeProperties.HOLE_ADDED,
            ShapeProperties.HOLE_REMOVED, ShapeProperties.LINE_COLOR, ShapeProperties.LINE_WIDTH, ShapeProperties.LOCATION, ShapeProperties.LOCATIONS,
            ShapeProperties.SEMI_MAJOR_AXIS, ShapeProperties.SEMI_MINOR_AXIS, ShapeProperties.ORIENTATION,
            ShapeProperties.PUBLISHED, ShapeProperties.RADIUS, ShapeProperties.RANGE, ShapeProperties.RING_COUNT,
            ShapeProperties.SWEEP, ShapeProperties.VERTICAL_HEIGHT, ShapeProperties.VERTICAL_HEIGHT_ENABLED, ShapeProperties.VISIBILITY,
            ShapeProperties.WIDTH, IMapObject.ATTRIBUTE_SET, GlobeShapeMapObject.IS_EDITING);
    /* Copied from GlobeRangeFanMapObject */
    private static final List<String> RANGE_FAN_PROPERTIES = List.of(
            ShapeProperties.EDITABLE, ShapeProperties.INTERVAL, ShapeProperties.LOCATION, ShapeProperties.LOCATIONS,
            ShapeProperties.MAX_DISTANCE, ShapeProperties.MIN_DISTANCE, ShapeProperties.ORIENTATION,
            ShapeProperties.RANGE_COLORS, ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS,
            ShapeProperties.RANGE_TYPE, ShapeProperties.SAMPLE_DISTANCE, ShapeProperties.SHOW_EDGES,
            ShapeProperties.SHOW_ORIENTATION, ShapeProperties.SHOW_RANGE_VALUES, ShapeProperties.SWEEP,
            ShapeProperties.VISIBILITY, GlobeShapeMapObject.IS_EDITING);
    /* Concatenation of IBullseye properties and GlobeShapeMapObject properties */

    private static final List<String> BULLSEYE_PROPERTIES = ListUtils.union(List.of(GlobeShapeMapObject.IS_EDITING), IBullseye.PROPERTIES_TO_WATCH);
    /* Copied from GlobeRouteMapObject */
    private static final List<String> ROUTE_PROPERTIES = List.of(
            IMapObject.NAME, ShapeProperties.EDITABLE, ShapeProperties.FILL_COLOR, ShapeProperties.LINE_COLOR,
            ShapeProperties.LINE_WIDTH, ShapeProperties.LOCATIONS, ShapeProperties.VISIBILITY, StandardAttributeNames.CALLSIGN,
            IRoute.METHOD, IRoute.DIRECTION, IRoute.ROUTE_PRECEDENCE_TYPE, IRoute.ROUTE_ORDER,
            IRoute.WAYPOINT_ADDED, IRoute.WAYPOINT_REMOVED, IRoute.WAYPOINT_NAME, IRoute.PREFIX, GlobeShapeMapObject.IS_EDITING);

    private static final List<String> ICON_PROPERTIES = List.of(
            IIconMapObject.CONTAINED, IMapObject.ELEVATION_MODE, IMapObject.HEADING, IMapObject.ICON, IMapObject.ICON_SCALE,
            IMapObject.LOCATION, IMapObject.NAME, IMapObject.PLAYBACK_MODE, IMapObject.SNAP_ICON_TO_SURFACE, IMapObject.VISIBILITY,
            Marker.STYLE);

    private static final List<String> GLOBE_ICON_PROPERTIES = List.of(
            IMapObject.LOCATION, GlobeIconMapObject.IMAGE_SOURCE, GlobeIconMapObject.SIZE,
            GlobeIconMapObject.SHOW_TOOLTIP, GlobeIconMapObject.TOOLTIP_TEXT,
            GlobeIconMapObject.BACKGROUND_IMAGE, GlobeIconMapObject.BACKGROUND_SCALE,
            GlobeIconMapObject.SELECTED);

    /* Copied from GlobeAnnotationMapObject */
    private static final List<String> ANNOTATION_PROPERTIES = List.of(
            IMapObject.LOCATION,
            IAnnotationMapObject.ANNOTATION_TEXT, IAnnotationMapObject.ANNOTATION_TEXT_COLOR,
            IAnnotationMapObject.ANNOTATION_TEXT_ALIGN, IAnnotationMapObject.ANNOTATION_FONT,
            IAnnotationMapObject.ANNOTATION_BACKGROUND_COLOR, IAnnotationMapObject.ANNOTATION_OPACITY,
            IAnnotationMapObject.ANNOTATION_MIN_ACTIVE_ALTITUDE, IAnnotationMapObject.ANNOTATION_MAX_ACTIVE_ALTITUDE,
            IAnnotationMapObject.ANNOTATION_BORDER_COLOR, IAnnotationMapObject.ANNOTATION_BORDER_LEAD_ENABLE,
            IAnnotationMapObject.ANNOTATION_BORDER_SHAPE, IAnnotationMapObject.ANNOTATION_BORDER_RADIUS,
            IAnnotationMapObject.ANNOTATION_BORDER_THICKNESS, IAnnotationMapObject.ANNOTATION_ENABLE_FIXED_HEIGHT,
            IAnnotationMapObject.ANNOTATION_FIXED_HEIGHT, IAnnotationMapObject.ANNOTATION_DRAW_OFFSET_X,
            IAnnotationMapObject.ANNOTATION_DRAW_OFFSET_Y);

    private final FeatureFactory featureFactory;
    private IGlobeMapObjectManager globeMapObjectManager;
    private final EventBus uiEventBus;
    private final boolean headless;
    private final ApplicationGlobeManager globeManager;
    private final IFeatureRendererManager featureManager;
    private final FeatureDataStore4 dataStore;

    // synchronized access only for registeredMapObjects, and dataStore.acquireModifyLock())
    private final HashMap<IGlobeMapObject, FeatureSetRegistration> registeredMapObjects = new HashMap<>();
    private IMapObject hoveringMapObject;

    private volatile Map<Integer, Double> lastGsdMap = new HashMap<>();
    private volatile Map<Integer, GeoBounds> lastSceneBoundsMap = new HashMap<>();

    private final Map<IGlobeMapObject, FeatureFactoryState<IGlobeMapObject, Object>> registrationQueue = new HashMap<>();
    private final Set<IGlobeMapObject> unregistrationQueue = new HashSet<>();
    private LimitingThread registrationThread;
    private LimitingThread onCameraChangedHandlerThread;
    private volatile boolean stopping;

    // Preferences that affect marker rendering
    private final IPreferenceMonitor<Double> iconScaleMonitor = UserPreferenceEntry.GLOBAL_ICON_SCALE.newDoubleMonitor(v -> forceRender(IGlobeMarkerMapObject.class), false);
    private final IPreferenceMonitor<Boolean> showLiveMarkersMonitor = PreferenceEntry.PLAYBACK_SHOW_LIVE_MARKERS.newBooleanMonitor(v -> forceRender(IGlobeMarkerMapObject.class), false);

    // Preferences that affect shape rendering
    private final IPreferenceMonitor<Boolean> showShapeLabelsMonitor = UserPreferenceEntry.SHOW_SHAPE_LABELS.newBooleanMonitor(v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<Boolean> showRangeLabelsMonitor = UserPreferenceEntry.SHOW_RANGE_BEARING_LABELS.newBooleanMonitor(v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<NorthReference> northReferenceMonitor = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, v -> forceRender(IGlobeShapeMapObject.class), false);
    private final IPreferenceMonitor<SpeedUnits> speedUnitsMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, v -> forceRender(IGlobeShapeMapObject.class), false);

    @Inject
    GlobeMapObjectFeatureManager(FeatureFactory featureFactory, ApplicationGlobeManager globeManager,
                                 @UiEventBus EventBus uiEventBus, IFeatureRendererManager featureManager,
                                 @SharedFeatureDataStore FeatureDataStore4 featureDataStore,
                                 @Headless boolean headless)
    {
        this.featureFactory = featureFactory;
        this.uiEventBus = uiEventBus;
        this.headless = headless;
        this.globeManager = globeManager;
        this.featureManager = featureManager;
        dataStore = featureDataStore;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        if (headless) return;

        stopping = false;

        event.getProgressMonitor().setText("Starting TAK feature services");
        featureManager.addPickedFeatureProvider(this);

        registrationThread = new LimitingThread("GlobeMapObjectFeatureManager registration thread", this::processQueuedRegistrations);
        onCameraChangedHandlerThread = new LimitingThread("GlobeMapObjectFeatureManager onCameraChanged thread", () -> forceRenderMilStdObjects(lastGsdMap, lastSceneBoundsMap));

        final var primaryGlobeDelegate = globeManager.getPrimaryGlobeDelegate();
        globeMapObjectManager = primaryGlobeDelegate.getGlobeMapObjectManager();

        globeManager.doWhenGlobeReady(g -> g.getPortableMapRenderer().addOnCameraChangedListener(this));

        primaryGlobeDelegate.addGlobeMapObjectChangeListener(this);
        primaryGlobeDelegate.addSelectListener(this);
        iconScaleMonitor.start();
        showShapeLabelsMonitor.start();
        northReferenceMonitor.start();
        bearingUnitsMonitor.start();
        altitudeUnitsMonitor.start();
        rangeUnitsMonitor.start();
        speedUnitsMonitor.start();
        showRangeLabelsMonitor.start();
        showLiveMarkersMonitor.start();
        uiEventBus.register(this);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        if (headless) return;

        stopping = true;

        final ApplicationGlobe globeDelegate = globeManager.getPrimaryGlobeDelegate();
        globeDelegate.removeSelectListener(this);
        globeDelegate.removeGlobeMapObjectChangeListener(this);
        iconScaleMonitor.stop();
        showShapeLabelsMonitor.stop();
        northReferenceMonitor.stop();
        bearingUnitsMonitor.stop();
        altitudeUnitsMonitor.stop();
        rangeUnitsMonitor.stop();
        speedUnitsMonitor.stop();
        showRangeLabelsMonitor.stop();
        showLiveMarkersMonitor.stop();

        if (registrationThread != null)
        {
            registrationThread.dispose();
            registrationThread = null;
        }

        if (onCameraChangedHandlerThread != null)
        {
            onCameraChangedHandlerThread.dispose();
            onCameraChangedHandlerThread = null;
        }

        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

        if (dataStore != null) dataStore.dispose();
        registeredMapObjects.clear();
    }

    /**
     * Ingest the provided {@link GlobeMapObjectAndState} and write to the features database.
     * @param registrations the globe map objects and state objects
     * @since 5.1
     */
    private synchronized void register(Iterable<GlobeMapObjectAndState> registrations)
    {
        if (headless) return;

        for (var pr : registrations)
        {
            if (stopping) return; // Stop processing registrations if we're shutting down

            IGlobeMapObject globeMapObject = pr.globeMapObject;
            if (featureFactory.isSupported(globeMapObject))
            {
                FeatureSetResult featureSetResult = featureFactory.create(pr.featureFactoryState, PROPERTY_MASK);
                FeatureSetRegistration featureSetRegistration = registeredMapObjects.get(globeMapObject);
                // If we are currently editing this IGlobeShapeMapObject, then let the shape editor draw it and don't draw it here.
                if (globeMapObject instanceof IGlobeShapeMapObject globeShapeMapObject && globeShapeMapObject.isEditing())
                {
                    featureSetResult = new FeatureSetResult(featureSetResult.featureResults(), false);
                }
                featureSetRegistration = createOrUpdate(featureSetResult, pr.featureFactoryState.state(), featureSetRegistration);
                if (featureSetRegistration != null) registeredMapObjects.put(globeMapObject, featureSetRegistration);
            } else if (globeMapObject instanceof IFlatAnnotationMapObject)
            {
                // We want to ignore flat annotation GMOs since they will be handled by GlobeAnnotationMapObjectManager.
            } else if (globeMapObject instanceof IGlobeShapeMapObject)
            {
                // Any shape map object that can't be rendered by the TakFeatureFactory we will log.
                logger.debug("Could not render globe map object of type {}", globeMapObject.getClass().getName());
            }
        }
    }

    /**
     * Ingest the provided {@link IGlobeMapObject} and remove from the features database.
     * @param globeMapObjects the globe map objects
     */
    private synchronized void unregister(Iterable<IGlobeMapObject> globeMapObjects)
    {
        for (var globeMapObject : globeMapObjects)
        {
            if (stopping) return; // Stop processing unregistrations if we're shutting down

            var featureSetRegistration = registeredMapObjects.remove(globeMapObject);
            if (featureSetRegistration != null) destroy(featureSetRegistration);
        }
    }

    private synchronized FeatureSetRegistration createOrUpdate(@Nonnull FeatureSetResult featureSetResult, @Nullable Object state,
                                                               @Nullable FeatureSetRegistration featureSetRegistration)
    {
        return FeatureUtility.applyFeatureResults(featureSetResult, state, dataStore, featureSetRegistration);
    }

    private void destroy(FeatureSetRegistration featureSetRegistration)
    {
        try
        {
            for (Long fsid : featureSetRegistration.fsidMap().values())
            {
                dataStore.deleteFeatureSet(fsid);
            }
        } catch (DataStoreException e)
        {
            logger.error("Error deleting feature set from datastore.", e);
        }
    }

    private void startMonitoring(IGlobeMapObject globeMapObject)
    {
        if (globeMapObject instanceof IGlobeRangeFanMapObject globeRangeFanMapObject)
        {
            globeRangeFanMapObject.addPropertyChangeListener(this, RANGE_FAN_PROPERTIES);
        } else if (globeMapObject instanceof GlobeBullseyeMapObject globeBullseyeMapObject)
        {
            globeBullseyeMapObject.addPropertyChangeListener(this, BULLSEYE_PROPERTIES);
        } else if (globeMapObject instanceof GlobeRouteMapObject globeRouteMapObject)
        {
            globeRouteMapObject.addPropertyChangeListener(this, ROUTE_PROPERTIES);
        } else if (globeMapObject instanceof IGlobeShapeMapObject globeShapeMapObject)
        {
            globeShapeMapObject.addPropertyChangeListener(this, SHAPE_PROPERTIES);
        } else if (globeMapObject instanceof IGlobeMarkerMapObject globeMarkerMapObject)
        {
            globeMarkerMapObject.addPropertyChangeListener(this, GLOBE_ICON_PROPERTIES);
            globeMapObject.getMapObject().addPropertyChangeListener(ICON_PROPERTIES, this);
        } else if (globeMapObject instanceof IGlobeAnnotationMapObject)
        {
            globeMapObject.getMapObject().addPropertyChangeListener(ANNOTATION_PROPERTIES, this);
        }
    }

    private void stopMonitoring(IGlobeMapObject globeMapObject)
    {
        if (globeMapObject instanceof IGlobeRangeFanMapObject globeRangeFanMapObject)
        {
            globeRangeFanMapObject.removePropertyChangeListener(this, RANGE_FAN_PROPERTIES);
        } else if (globeMapObject instanceof GlobeBullseyeMapObject globeBullseyeMapObject)
        {
            globeBullseyeMapObject.removePropertyChangeListener(this, BULLSEYE_PROPERTIES);
        } else if (globeMapObject instanceof GlobeRouteMapObject globeRouteMapObject)
        {
            globeRouteMapObject.removePropertyChangeListener(this, ROUTE_PROPERTIES);
        } else if (globeMapObject instanceof IGlobeShapeMapObject globeShapeMapObject)
        {
            globeShapeMapObject.removePropertyChangeListener(this, SHAPE_PROPERTIES);
        } else if (globeMapObject instanceof IGlobeMarkerMapObject globeMarkerMapObject)
        {
            globeMarkerMapObject.removePropertyChangeListener(this, GLOBE_ICON_PROPERTIES);
            globeMapObject.getMapObject().removePropertyChangeListener(ICON_PROPERTIES, this);
        } else if (globeMapObject instanceof IGlobeAnnotationMapObject)
        {
            globeMapObject.getMapObject().removePropertyChangeListener(ANNOTATION_PROPERTIES, this);
        }
    }

    @Override
    public synchronized Object getPickObject(@Nonnull IPickContext context, long fid)
    {
        // Micro-optimization for this high traffic method to use for loops rather than streams which incur unnecessary overhead.
        for (var entry : registeredMapObjects.entrySet())
        {
            for (var featureRegistration : entry.getValue().featureRegistrations().values())
            {
                if (featureRegistration.fid() == fid) return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Handle a change in the state icons for a Globe Map Object.
     * @since 3.5
     */
    @Subscribe
    public void onStateIconsChanged(StateIconsChanged event)
    {
        queueRegistration(event.globeMarkerMapObject());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final var source = evt.getSource();
        IGlobeMapObject globeMapObject = null;

        if (source instanceof IGlobeMapObject aGlobeMapObject)
        {
            globeMapObject = aGlobeMapObject;
        } else if (source instanceof IMapObject mapObject)
        {
            globeMapObject = globeMapObjectManager.getGlobeMapObject(mapObject.getKey());
        } else if (source instanceof IShape shape)
        {
            globeMapObject = globeMapObjectManager.getGlobeMapObject(shape.getKey());
        }

        if (globeMapObject != null) queueRegistration(globeMapObject);
    }

    @Override
    public void selected(MapObjectSelectEvent event)
    {
        if (event.isRollover() || event.isDrag())
        {
            // Select events (like a hover) happen rapidly when the mouse stays over the same object.
            // There's no reason to re-register the same MO on every call, so check for that.
            boolean sameMapObject = false;

            final IMapObject mapObject;

            if (event.getMapObjects().isEmpty())
            {
                mapObject = hoveringMapObject;
                hoveringMapObject = null;
            } else
            {
                mapObject = event.getMapObjects().get(0);
                if (mapObject == hoveringMapObject)
                {
                    sameMapObject = true;
                } else
                {
                    // Refresh the previously hovering map object
                    if (hoveringMapObject != null) queueRegistrations(hoveringMapObject.getKey());
                    hoveringMapObject = mapObject;
                }
            }

            if (mapObject != null && !sameMapObject) queueRegistrations(mapObject.getKey());
        }
    }

    /**
     * Queue registrations for both the live and playback globe map objects.
     * @since 5.7
     */
    private void queueRegistrations(@Nonnull String key)
    {
        var globeMapObject = globeMapObjectManager.getGlobeMapObject(key);
        if (globeMapObject != null) queueRegistration(globeMapObject);

        globeMapObject = globeMapObjectManager.getGlobeMapObject(key, true);
        if (globeMapObject != null) queueRegistration(globeMapObject);
    }

    @Override
    public void onCameraChanged(MapRenderer renderer)
    {
        HashMap<Integer, Double> localGsdMap = new HashMap<>();
        HashMap<Integer, GeoBounds> localSceneBoundsMap = new HashMap<>();
        boolean[] viewChanged = {false};

        globeManager.getAllGlobeDelegates().forEach(appGlobe -> {
            GLMapView glMapView = LegacyAdapters.adapt(renderer);
            GLMapView.State state = glMapView.currentScene;

            int globeId = appGlobe.getGlobeId();
            int currentLod = OSMUtils.mapnikTileLevel(state.scene.gsd, Math.toRadians(state.drawLat));
            double lastGsd = lastGsdMap.getOrDefault(globeId, 0.0);
            int lastLod = OSMUtils.mapnikTileLevel(lastGsd, Math.toRadians(state.drawLat));
            GeoBounds lastSceneBounds = lastSceneBoundsMap.get(globeId);
            GeoPoint upperLeft = state.scene.inverse(new PointF(0, 0), null, false);
            GeoPoint lowerRight = state.scene.inverse(new PointF(state.scene.width, state.scene.height), null, false);
            GeoBounds currentSceneBounds = null;
            if (upperLeft != null && lowerRight != null && !Double.isNaN(upperLeft.getLatitude()) && !Double.isNaN(upperLeft.getLongitude()) &&
                    !Double.isNaN(lowerRight.getLatitude()) && !Double.isNaN(lowerRight.getLongitude()))
            {
                currentSceneBounds = new GeoBounds(upperLeft, lowerRight);
                // Initial unscaled GeoBounds should recognize IDL crossing
                currentSceneBounds.setWrap180(true);
            }
            localGsdMap.put(globeId, state.scene.gsd);
            // Inflate the scene bounds to cover a larger area of interest and reduce the need to refresh
            localSceneBoundsMap.put(globeId, scaleBounds(currentSceneBounds, 3));
            boolean lodChanged = Math.abs(currentLod - lastLod) >= 1;
            boolean sceneChanged = lastSceneBounds == null && currentSceneBounds != null || lastSceneBounds != null && currentSceneBounds == null
                    || (lastSceneBounds != null && !lastSceneBounds.contains(currentSceneBounds));
            viewChanged[0] |= lodChanged || sceneChanged;
        });
        if (!viewChanged[0]) return;
        lastGsdMap = localGsdMap;
        lastSceneBoundsMap = localSceneBoundsMap;

        onCameraChangedHandlerThread.exec();
    }

    /**
     * Force render any on-screen milstd map objects.
     * @param gsdMap the current map of globes to on-screen gsd
     * @param sceneBoundsMap the current map of globes to on-screen visible bounds
     * @since 5.2
     */
    private synchronized void forceRenderMilStdObjects(@Nonnull Map<Integer, Double> gsdMap, @Nonnull Map<Integer, GeoBounds> sceneBoundsMap)
    {
        forceRender(gmo -> {
            // Make sure the GMO is handled by the MilStd feature provider
            if (MilStdGlobeShapeFeatureProvider.SPI.create(gmo) == null) return false;
            // Make sure we have a proper milStdState object
            FeatureSetRegistration featureSetRegistration = registeredMapObjects.get(gmo);
            var stateKey = featureSetRegistration != null ? featureSetRegistration.stateKey() : null;
            if (!(stateKey instanceof MilStdGlobeShapeFeatureProvider.MilStdShapeState milStdState)) return false;
            // Must be visible
            if (!milStdState.isVisible()) return false;
            // Try to build a bounding box for the GMO
            GeoBounds.Builder builder = new GeoBounds.Builder();
            milStdState.geoPoints().forEach(g -> builder.add(g.getLatitude(), g.getLongitude()));
            GeoBounds gmoBounds = builder.build();
            if (gmoBounds == null) return false;

            // Make sure GMO is on-screen
            boolean isOnScreen = sceneBoundsMap.values().stream().anyMatch(sceneBounds -> sceneBounds == null || sceneBounds.intersects(gmoBounds));
            if (!isOnScreen) return false;
            if (gmo.getMapObject() instanceof AShapeMapObject<?> mo)
            {
                mo.setMilStdGsdHintMap(gsdMap);
                mo.setMilStdSceneBoundsHintMap(sceneBoundsMap);
            }
            return true;
        });
    }

    @Nullable
    private GeoBounds scaleBounds(@Nullable GeoBounds bounds, double scale)
    {
        if (bounds == null) return null;
        // If we're crossing the IDL, don't try to scale
        if (bounds.crossesIDL()) return new GeoBounds(bounds);
        double width = bounds.getEast() - bounds.getWest();
        double height = bounds.getNorth() - bounds.getSouth();
        double dw = ((width * scale) - width) * 0.5;
        double dh = ((height * scale) - height) * 0.5;
        double west = Math.max(bounds.getWest() - dw, -180);
        double east = Math.min(bounds.getEast() + dw, 180);
        double south = Math.max(bounds.getSouth() - dh, -90);
        double north = Math.min(bounds.getNorth() + dh, 90);
        GeoBounds result = new GeoBounds(south, west, north, east);
        // Now that we've scaled, we don't want to consider IDL wrapping in case we've scaled to more than half the globe
        result.setWrap180(false);
        return result;
    }

    /**
     * Force a {@link #queueRegistration(IGlobeMapObject)} for all map objects of the specified class types.
     * This must be invoked on the EDT, and preference changes can happen on any thread.
     *
     * @since 5.2
     */
    @InvokeOnEDT
    private void forceRender(Class<?>... clazz)
    {
        forceRender(gmo -> Arrays.stream(clazz).anyMatch(c -> c.isAssignableFrom(gmo.getClass())));
    }

    /**
     * Force a {@link #queueRegistration(IGlobeMapObject)} for all map objects that pass the specified filter.
     * @since 5.2
     */
    private synchronized void forceRender(Predicate<IGlobeMapObject> filter)
    {
        registeredMapObjects.keySet().stream()
                .filter(filter)
                .forEach(this::queueRegistration);
    }

    @Override
    public void globeMapObjectChanged(IGlobeMapObject globeMapObject, MapObjectChangeEvent mapObjectChangeEvent)
    {
        switch (mapObjectChangeEvent)
        {
            case ADDED ->
            {
                startMonitoring(globeMapObject);
                if (globeMapObject.getMapObject() instanceof AShapeMapObject<?> mo)
                {
                    mo.setMilStdGsdHintMap(lastGsdMap);
                    mo.setMilStdSceneBoundsHintMap(lastSceneBoundsMap);
                }
                queueRegistration(globeMapObject);
            }
            case REMOVED ->
            {
                stopMonitoring(globeMapObject);
                queueUnregistration(globeMapObject);
            }
            case CHANGED -> queueUnregistration(globeMapObject);
        }
    }

    /**
     * Take an {@link IGlobeMapObject} that has been added or changed and queue it for processing. This method can be called from any thread.
     * @param globeMapObject the globe map object
     */
    private void queueRegistration(IGlobeMapObject globeMapObject)
    {
        synchronized (registrationQueue)
        {
            registrationQueue.put(globeMapObject, featureFactory.createState(globeMapObject));
        }
        if (registrationThread != null) registrationThread.exec();
    }

    /**
     * Take an {@link IGlobeMapObject} that has been removed from the globe and queue it for processing. This method can be called from any thread.
     * @param globeMapObject the globe map object
     */
    private void queueUnregistration(IGlobeMapObject globeMapObject)
    {
        synchronized (registrationQueue)
        {
            unregistrationQueue.add(globeMapObject);
        }
        if (registrationThread != null) registrationThread.exec();
    }

    /**
     * Called by {@link #registrationThread} to process any queued registrations or unregistrations.
     */
    private synchronized void processQueuedRegistrations()
    {
        List<GlobeMapObjectAndState> registrations;
        List<IGlobeMapObject> unregistrations;
        synchronized (registrationQueue)
        {
            registrations = registrationQueue.entrySet().stream()
                    .map(e -> new GlobeMapObjectAndState(e.getKey(), e.getValue()))
                    .toList();
            registrationQueue.clear();
            unregistrations = new ArrayList<>(unregistrationQueue);
            unregistrationQueue.clear();
        }
        try
        {
            dataStore.acquireModifyLock(true);
            register(registrations);
            unregister(unregistrations);
        } catch (InterruptedException e)
        {
            logger.warn("Interrupted while acquiring modify lock on feature database.", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * Holds an {@link IGlobeMapObject} and its state to be processed on a background thread.
     * @param globeMapObject the globe map object
     * @param featureFactoryState the feature factory state
     * @since 5.1
     */
    private record GlobeMapObjectAndState(IGlobeMapObject globeMapObject, @Nonnull FeatureFactoryState<IGlobeMapObject, Object> featureFactoryState)
    {
    }
}
