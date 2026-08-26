package gov.takx.core.globe;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwindx.examples.util.ExtentVisibilitySupport;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.api.ui.event.MapObjectTrackingEvent.TrackingState;
import gov.takx.api.ui.event.MapObjectTrackingEvent.TrackingType;
import gov.takx.core.playback.PlaybackSystem;
import gov.takx.platform.globe.CameraEyeFactory;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class handles tracking map objects and keeping them in view.  Both "Center on" and "keep in view" operations
 * are handled here in a coordinated fashion.
 * <p/>
 * Currently, only icon map objects and shapes are supported in the tracking algorithms.  Any object that implements
 * the {@link ExtentHolder} interface can be tracked, so we are limited by what objects have this implemented.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TrackingGlobeViewController implements PropertyChangeListener, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> MAP_OBJECT_PROPERTIES = List.of(IMapObject.LOCATION, IMapObject.PLAYBACK_MODE);
    private static final List<String> GLOBE_MAP_OBJECT_PROPERTIES = List.of(IMapObject.LOCATION);

    @Inject
    private PlaybackSystem playback;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    private final IApplicationGlobe applicationGlobe;
    private final Globe wwGlobe;
    private final OrbitView orbitView;

    private final Set<IMapObject> objectsToTrack = ConcurrentHashMap.newKeySet(); // All objects being tracked
    private final Set<IMapObject> keepInView = ConcurrentHashMap.newKeySet();
    private final Set<ExtentHolder> extentHolders = ConcurrentHashMap.newKeySet();
    private final Set<IMapObject> listenersRegistered = ConcurrentHashMap.newKeySet();
    private final Map<IMapObject, IGlobeMarkerMapObject> globeMapObjects = new ConcurrentHashMap<>();

    private IMapObject centerOn;
    private Position centerPosition;

    /**
     * @param applicationGlobe The globe we are controlling
     */
    public TrackingGlobeViewController(@Nonnull IApplicationGlobe applicationGlobe)
    {
        this.applicationGlobe = applicationGlobe;

        final var wwd = applicationGlobe.getWorldWindow();

        if (wwd.getView() instanceof OrbitView wwOrbitView)
        {
            orbitView = wwOrbitView;
        } else
        {
            throw new IllegalArgumentException("Application Globe view is not an OrbitView");
        }

        wwGlobe = Objects.requireNonNull(wwd.getModel().getGlobe(), "WW Globe must not be null");
    }

    /**
     * Start centering the view on the given Map Object on this globe. Only one Map Object can have the "centered on"
     * setting for a globe.  If a previous Map Object is already configured as "center on" for this globe, then it
     * will be removed, as if a call to {@link #stopCenterOn()}} was made prior to calling this method.
     * <p/>
     * Whenever the location of the Map Object that is "centered on" for a given globe changes, that globe will have
     * its view adjusted so that the given Map Object is kept at the center of the view.
     *
     * @param mapObject The map object to center on
     */
    public void startCenterOn(@Nonnull IMapObject mapObject)
    {
        stopCenterOn(); // Clear any previous center on configuration

        logger.debug("Set centerOn={}", mapObject);

        centerOn = mapObject;
        startTracking(mapObject, TrackingType.CENTER_ON);
    }

    /**
     * Stop centering the view on a Map Object.  If no Map Object is currently being tracked, then this call
     * does nothing.  Otherwise, it removes the previously configured Map Object.
     */
    public void stopCenterOn()
    {
        // If there was no center on configured, just bail
        if (centerOn == null)
        {
            logger.debug("stopCenterOn called with no MO being centered on");
            return;
        }

        // Stop watching for changes if it is not also being kept in view
        var priorCenterOn = centerOn;
        centerOn = null;
        stopTracking(priorCenterOn, TrackingType.CENTER_ON, !isKeepInView(priorCenterOn));

        // Reset the view center position in case the center-on object's elevation was above the terrain, which will
        // prevent the user from zooming past that point
        final var viewCenter = orbitView.getCenterPosition();
        final var lat = viewCenter.getLatitude().degrees;
        final var lon = viewCenter.getLongitude().degrees;
        orbitView.setCenterPosition(Position.fromDegrees(lat, lon, ElevationManager.getElevation(lat, lon, null)));
    }

    /**
     * @return The Map Object that is configured as the "center on" for this globe, may be {@code null}
     * @see #startCenterOn(IMapObject)
     * @see #stopCenterOn()
     */
    @Nullable
    public IMapObject getCenterOn()
    {
        return centerOn;
    }

    /**
     * Test to see if the given Map Object is the currently configured "center on" Map Object for this globe.
     *
     * @param mapObject Map Object of interest
     * @return {@code true} if configured as center onm {@code false} if not
     */
    public boolean isCenterOn(@Nonnull IMapObject mapObject)
    {
        return mapObject.equals(centerOn);
    }

    /**
     * Add a Map Object to the set of objects to be kept in the view of this globe.
     * <p/>
     * Any time a Map Object that has been marked for "keep in view" has a location or extent change, the globe view will
     * be adjusted so that all Map Objects that are to be kept in view are visible.
     * This may involve panning and/or zooming the globe's view.
     *
     * @param mapObject The map object to keep in view
     */
    public void addKeepInView(@Nonnull IMapObject mapObject)
    {
        if (keepInView.add(mapObject))
        {
            startTracking(mapObject, TrackingType.KEEP_IN_VIEW);
        } else
        {
            logger.warn("Tried to add the same MapObject to the tacking set twice: {}", mapObject);
        }
    }

    /**
     * Remove the given Map Object from the set of Map Objects being kept in view on this globe.  If the given Map
     * Object is not being tracked, then this call does nothing.
     *
     * @param mapObject The map object to remove from the tracking set
     */
    public void removeKeepInView(@Nonnull IMapObject mapObject)
    {
        if (keepInView.remove(mapObject))
        {
            // Stop listening for change if it is not also center on
            stopTracking(mapObject, TrackingType.KEEP_IN_VIEW, !isCenterOn(mapObject));
        } else
        {
            logger.warn("Map Object not in tracking set: {}", mapObject);
        }
    }

    /**
     * Determine if a given Map Object is being kept in view on this globe.
     *
     * @param mapObject The Map Object of interest
     * @return {@code true} if this Map Object is being kept in view
     * @see #addKeepInView(IMapObject)
     * @see #removeKeepInView(IMapObject)
     */
    public boolean isKeepInView(@Nonnull IMapObject mapObject)
    {
        return keepInView.contains(mapObject);
    }

    /**
     * Add the given map object to our extents, start listening for property changes, and fire the appropriate STARTED event.
     * @since 5.7
     */
    private void startTracking(IMapObject mapObject, TrackingType trackingType)
    {
        objectsToTrack.add(mapObject);
        logger.debug("Added MO to tracking set: {}", mapObject);

        final var extentHolder = getExtentHolder(getGlobeMapObject(mapObject));
        if (extentHolder != null) extentHolders.add(extentHolder);

        addListeners(mapObject);
        sceneChanged(); // Force a recalculation of the scene
        fireTrackingEvent(mapObject, TrackingState.STARTED, trackingType);
    }

    /**
     * Stop tracking the given map object, and fire the appropriate STOPPED event.
     *
     * @param mapObject Map object to stop tracking
     * @param trackingType type of event to fire
     * @param noLongerTracked Pass {@code true} if this MO is no longer of interest
     * @since 5.7
     */
    private void stopTracking(IMapObject mapObject, TrackingType trackingType, boolean noLongerTracked)
    {
        if (noLongerTracked)
        {
            objectsToTrack.remove(mapObject);
            logger.debug("Removed MO from tracking set: {}", mapObject);

            removeListeners(mapObject);

            final var extentHolder = getExtentHolder(getGlobeMapObject(mapObject));
            if (extentHolder != null) extentHolders.remove(extentHolder);
        }

        fireTrackingEvent(mapObject, TrackingState.STOPPED, trackingType);
    }

    /**
     * Fire the tracking event so the rest of the UI can react accordingly.
     *
     * @param mapObject  The MO involved in the change
     * @param state The state (start or stop)
     * @param type  The operation type (keep in view or center on)
     */
    private void fireTrackingEvent(IMapObject mapObject, TrackingState state, TrackingType type)
    {
        final var trackingEvent = new MapObjectTrackingEvent(applicationGlobe.getGlobeId(), mapObject.getController(), state, type);
        logger.debug("Publish: {}", trackingEvent);
        uiEventBus.post(trackingEvent);
    }

    /**
     * Add the needed listeners so we can track this Map Objects movement and screen extent.
     *
     * @param mapObject MO of interest
     */
    private void addListeners(IMapObject mapObject)
    {
        if (listenersRegistered.add(mapObject))
        {
            mapObject.addPropertyChangeListener(MAP_OBJECT_PROPERTIES, this);
            addPlaybackListener(mapObject);

            // If this is a shape, then we monitor ALL properties on the GlobeShapeMapObject.  There are just too
            // many properties that affect the extent of various shapes to worry about listing every one.
            if (getGlobeMapObject(mapObject) instanceof IGlobeShapeMapObject gsmo) gsmo.addPropertyChangeListener(this);
        }

        logger.debug("Registered listener on {}", mapObject);
    }

    /**
     * Remove the listeners registered on the Map Object.
     *
     * @param mapObject MO of interest
     */
    private void removeListeners(IMapObject mapObject)
    {
        mapObject.removePropertyChangeListener(MAP_OBJECT_PROPERTIES, this);
        removePlaybackListener(mapObject);
        if (getGlobeMapObject(mapObject) instanceof IGlobeShapeMapObject gsmo) gsmo.removePropertyChangeListener(this);

        listenersRegistered.remove(mapObject);
        logger.debug("Unregistered listener on {}", mapObject);
    }

    /**
     * Add the playback listener for the given map object, if it is a marker (i.e., not a shape).
     * @since 5.7
     */
    private void addPlaybackListener(@Nonnull IMapObject mapObject)
    {
        if (mapObject.inPlaybackMode() && getGlobeMapObject(mapObject) instanceof IGlobeMarkerMapObject gmo)
        {
            gmo.addPropertyChangeListener(this, GLOBE_MAP_OBJECT_PROPERTIES);
            globeMapObjects.put(mapObject, gmo);
        }
    }

    /**
     * Remove the playback listener for the given map object.
     * @since 5.7
     */
    private void removePlaybackListener(@Nonnull IMapObject mapObject)
    {
        final var gmo = globeMapObjects.remove(mapObject);
        if (gmo != null) gmo.removePropertyChangeListener(this, GLOBE_MAP_OBJECT_PROPERTIES);
    }

    /**
     * Get the globe map object associated with the given map object.
     *
     * @param mapObject map object of interest
     * @return GlobeMapObject representing it on the globe
     */
    private IGlobeMapObject getGlobeMapObject(IMapObject mapObject)
    {
        return applicationGlobe.getGlobeMapObjectManager().getGlobeMapObject(mapObject.getKey(), mapObject.inPlaybackMode());
    }

    /**
     * If a globe map object represents a shape that is an extent holder, then return it.  Otherwise, return {@code null}.
     *
     * @param gmo GlobeMapObject to inspect
     * @return ExtentHolder instance if present, {@code null} otherwise
     */
    private ExtentHolder getExtentHolder(IGlobeMapObject gmo)
    {
        return gmo instanceof IGlobeShapeMapObject gsmo && gsmo.getShapeModel() instanceof ExtentHolder extentHolder
                ? extentHolder
                : null;
    }

    /**
     * Handle a property change on one of our map objects.  Deal with location/size changes by updating the view
     * to contain our tracking set.
     *
     * @param evt event to process
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getPropertyName().equals(IMapObject.PLAYBACK_MODE))
        {
            final var mapObject = (IMapObject) evt.getSource();
            removePlaybackListener(mapObject);
            addPlaybackListener(mapObject);
        } else
        {
            sceneChanged();
        }
    }

    /**
     * Shutdown our processing.  Unregister all listeners and event subscriptions.
     *
     * @param progressMonitor Progress monitor to update (ignored)
     */
    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        // Remove the listeners on all the objects being tracked
        objectsToTrack.forEach(this::removeListeners);

        // Clear out the lists
        centerOn = null;
        objectsToTrack.clear();
        globeMapObjects.clear();
        listenersRegistered.clear();
        extentHolders.clear();
        keepInView.clear();
    }

    /**
     * The scene has change (objects we are tracking have changed locations). Update our view as needed.
     */
    @InvokeOnEDT
    private void sceneChanged()
    {
        logger.debug("Scene changed, update view on {}", applicationGlobe);

        boolean doCenterOn = centerOn != null;
        boolean centerChanged = false;

        ILocation newCenterLoc = null;
        if (doCenterOn)
        {
            final var gmo = getGlobeMapObject(centerOn);
            if (gmo != null)
            {
                // Don't try to change center if our map object doesn't have a valid location
                final var location = gmo.getLocation();
                if (GeoPoint.isValid(location.getLatitude(), location.getLongitude()))
                {
                    newCenterLoc = location;
                    centerChanged = true;
                }
            }
        }

        // If we are centering on an object, then note that so the animator can reference it
        centerPosition = null;
        if (newCenterLoc != null)
        {
            final double lat = newCenterLoc.getLatitude();
            final double lon = newCenterLoc.getLongitude();
            double elevation = centerOn.isSnapIconToSurface() || !newCenterLoc.hasElevation()
                    ? ElevationManager.getElevation(lat, lon, null)
                    : newCenterLoc.getElevation();

            centerPosition = Position.fromDegrees(lat, lon, elevation);
        }

        // Update our view, if needed.
        final var trackedObjectExtents = new TrackedObjectExtents();
        if (centerChanged || !trackedObjectExtents.areExtentsContained(orbitView)) updateView(trackedObjectExtents);
    }

    /**
     * Goto a new camera eye that contains the scene including all tracked objects.
     * @since 5.7
     */
    private void updateView(TrackedObjectExtents trackedObjectExtents)
    {
        final var lookAt = trackedObjectExtents.computeViewLookAt();
        if (lookAt == null || lookAt.length != 3) return;

        // When centerPosition is null, use the calculated center of the tracked objects
        if (centerPosition == null) centerPosition = orbitView.getGlobe().computePositionFromPoint(lookAt[1]);

        final double zoom = Math.max(orbitView.getZoom(), lookAt[0].distanceTo3(lookAt[1]));
        logger.trace("updateView: center={}, zoom={}", centerPosition, zoom);

        final var cameraEye = CameraEyeFactory.createCameraEye(centerPosition.latitude.degrees, centerPosition.longitude.degrees,
                centerPosition.elevation, orbitView.getHeading().degrees, orbitView.getPitch().degrees, zoom);
        applicationGlobe.getGlobeViewController().goTo(cameraEye, false);
    }

    /**
     * Extension to properly interpret our center position and all our tracked object extents.
     * @since 5.7
     */
    private class TrackedObjectExtents extends ExtentVisibilitySupport
    {
        private static final int PADDING = 10; // Pixels of padding on icons for view calculation

        TrackedObjectExtents()
        {
            setScreenExtents(screenExtentsFromTrackedObjects());
            setExtents(extentsFromExtentHolders(extentHolders, wwGlobe, 1));
        }

        /**
         * Override the calculation of center point so it uses the value from our "center on" object, if there
         * is one.  Otherwise, delegate to the base class to calculate the geometric mean of the object center points.
         */
        @Override
        protected Vec4 computeCenterPoint(Globe globe, double ve, Iterable<? extends Extent> modelExtents, Iterable<? extends ScreenExtent> screenExtents)
        {
            // If we have a center on location, use that, otherwise calculate the geometric mean
            return centerPosition != null
                    ? globe.computePointFromPosition(centerPosition)
                    : super.computeCenterPoint(globe, ve, modelExtents, screenExtents);
        }

        /**
         * @see #computeViewLookAtContainingExtents(Globe, double, View)
         */
        @Nullable
        Vec4[] computeViewLookAt()
        {
            try
            {
                return computeViewLookAtContainingExtents(wwGlobe, 1, orbitView);
            } catch (IllegalArgumentException e)
            {
                // Catch occasional "Eye point and center point are coincident" exception
                if (e.getMessage().contains("coincident"))
                {
                    logger.debug("Ignored eye/center coincident exception");
                } else
                {
                    logger.error("Failed to compute view", e);
                }
            }
            return null;
        }

        /**
         * @return List of the screen extents from all our tracked objects.
         */
        @Nonnull
        private List<ScreenExtent> screenExtentsFromTrackedObjects()
        {
            return objectsToTrack.stream()
                    .map(this::getMapObjectScreenExtent)
                    .filter(Objects::nonNull)
                    .toList();
        }

        /**
         * @return the screen extent for the given map object if it is a marker, {@code null} otherwise
         */
        @Nullable
        private ScreenExtent getMapObjectScreenExtent(@Nonnull IMapObject mapObject)
        {
            final var gmo = getGlobeMapObject(mapObject);

            if (gmo.isViewable() && gmo instanceof IGlobeMarkerMapObject globeMarkerMapObject)
            {
                final var referencePoint = wwGlobe.computePointFromPosition(WWUtils.locationToPosition(globeMarkerMapObject.getLocation()));
                final var screenPoint = orbitView.project(referencePoint);
                final var imageSize = globeMarkerMapObject.getCurrentImageSize();
                final double width = imageSize.getWidth();
                final double height = imageSize.getHeight();
                final double x = screenPoint.x - (width / 2.0);
                final double y = screenPoint.y - (height / 2.0);
                final var bounds = new Rectangle((int) x, (int) y, (int) (width + PADDING), (int) (height + PADDING));

                return new ScreenExtent(referencePoint, bounds);
            } else
            {
                return null;
            }
        }
    }
}
