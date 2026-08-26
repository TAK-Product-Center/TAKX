package gov.takx.core.drifter;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.drifter.DrifterConfiguration;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.mapobject.CancelableMapObjectSelectionListener;
import gov.takx.core.view.gui.drifter.EditDrifterConfigurationDialog;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.platform.utils.StringKeyLockSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.EventListenerList;

import java.lang.invoke.MethodHandles;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Controller for {@link DrifterConfiguration} data on {@link IMapObject}. This class handles operations on the map objects
 * themselves and updates to the {@link DrifterConfiguration}. The map object updates run on a fixed delay (see
 * {@link ICoreExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)}). That schedule is cancelled if there
 * are no active, unpaused drifter configurations and is re-created when necessary. Future adjustments to this class should
 * take care to retain that so that we are not burdening the executor service with empty processes.
 *
 * @since 5.6
 */
@ApplicationScoped
public class DrifterController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long STEP_DELAY_MS = 1000;

    private final StringKeyLockSupport lockSupport = new StringKeyLockSupport();
    private final EventListenerList eventListeners = new EventListenerList();
    private final List<IMapObject> registeredMapObjects = new CopyOnWriteArrayList<>();
    private final Map<String, Set<IMapObject>> registeredRoutesMap = new HashMap<>();
    private final ICoreExecutorService executorService;
    private final IMapObjectFinder mapObjectFinder;
    private ScheduledFuture<?> movementFuture;

    @Inject
    DrifterController(@Nonnull IMapObjectFinder mapObjectFinder, @Nonnull ICoreExecutorService executorService)
    {
        this.mapObjectFinder = mapObjectFinder;
        this.executorService = executorService;
    }

    /**
     * @param paused {@code true} to pause all drifter configurations or {@code false} to unpause them
     */
    public void setPausedAllConfigurations(boolean paused)
    {
        getMapObjects().forEach(mapObject -> setPaused(mapObject, paused));
    }

    /**
     * Sets the paused state of the drifter configuration for a {@link IMapObject}. 
     * 
     * @param mapObject the mapObject
     * @param paused {@code true} to pause drifter or {@code false} to unpause
     */
    public void setPaused(@Nonnull IMapObject mapObject, boolean paused)
    {
        final var lock = lockSupport.lock(mapObject.getUid());
        try
        {
            var currentConfiguration = requireDrifterConfiguration(mapObject);
            if (paused != currentConfiguration.paused())
            {
                mapObject.setDrifterConfiguration(DrifterConfiguration.builder(currentConfiguration).paused(paused).build());
                if (!paused) startSchedule();
            }
        } finally
        {
            lock.unlock();
        }
    }

    /**
     * @param mapObject the map object to toggle the paused state of drifter configuration on
     */
    public void togglePaused(@Nonnull IMapObject mapObject)
    {
        setPaused(mapObject, !requireDrifterConfiguration(mapObject).paused());
    }

    /**
     * Restart route navigation for all applicable map objects. See {@link #restart(IMapObject)}.
     */
    public void restartAll()
    {
        getMapObjects().stream().filter(mo -> getRoute(requireDrifterConfiguration(mo)) != null).forEach(this::restart);
    }

    /**
     * Restart a map object at the beginning of the route specified in its drifter configuration.
     *
     * @param mapObject the map object
     */
    public void restart(@Nonnull IMapObject mapObject)
    {
        final var lock = lockSupport.lock(mapObject.getUid());
        try
        {
            var currentConfig = requireDrifterConfiguration(mapObject);
            var route = getRoute(currentConfig);
            if (route == null) throw new IllegalStateException("Cannot restart non-route drifter configuration");
            var firstPoint = route.getPointByIndex(0);
            mapObject.setLocation(firstPoint.get(), false);
            mapObject.setDrifterConfiguration(DrifterConfiguration.builder(currentConfig).routePoint(0).build());
        } finally
        {
            lock.unlock();
        }
    }

    /**
     * Adds a new drifter configuration by allowing the user to select a map object and then fill out the fields in {@link EditDrifterConfigurationDialog}.
     */
    public void addConfiguration()
    {
        new CancelableMapObjectSelectionListener(event -> {
            if (event != null) EditDrifterConfigurationDialog.show(event.get(0));
        }, IMarkerMapObject.class::isInstance, "Select a map item to configure drifter on.").register();
    }

    /**
     * Update an existing drifter configuration.
     * 
     * @param mapObject the map object to update
     * @param config the drifter configuration
     */
    public void updateConfiguration(@Nonnull IMapObject mapObject, @Nullable DrifterConfiguration config)
    {
        final var lock = lockSupport.lock(mapObject.getUid());
        try
        {
            if (config == null)
            {
                deleteConfiguration(mapObject, false);
            } else
            {
                var currentConfig = mapObject.getDrifterConfiguration();
                IRouteMapObject currentRoute = null;
                if (currentConfig != null)
                {
                    setPaused(mapObject, true);
                    currentRoute = getRoute(currentConfig);
                }
                var newRoute = getRoute(config);
                if (newRoute != null)
                {
                    if (newRoute == currentRoute)
                    {
                        // route did not change so keep current route point
                        config = DrifterConfiguration.builder(config).routePoint(currentConfig.routePoint()).build();
                    } else
                    {
                        var firstPoint = newRoute.getPointByIndex(0);
                        mapObject.setLocation(firstPoint.get(), false);
                        registerRoute(mapObject, newRoute);
                    }
                }
                if (currentRoute != null && currentRoute != newRoute)
                {
                    unregisterRoute(mapObject, currentRoute);
                }
                mapObject.setDrifterConfiguration(config);
                if (currentConfig == null) registerMapObject(mapObject);
                if (!config.paused()) startSchedule();
            }
        } finally
        {
            lock.unlock();
        }
    }

    /**
     * Prompt the user to confirm and then delete the drifter configuration for a map object if confirmed.
     * 
     * @param mapObject the map object
     * @return {@code true} if the object was deleted, otherwise {@code false}
     */
    public boolean deleteConfiguration(@Nonnull IMapObject mapObject)
    {
        return deleteConfiguration(mapObject, true);
    }

    /**
     * Delete the drifter configuration for a map object, optionally showing a confirmation prompt.
     * 
     * @param mapObject the map object
     * @param confirm {@code true} to show a confirm dialog, otherwise {@code false}
     * @return {@code true} if the object was deleted, otherwise {@code false}
     */
    public boolean deleteConfiguration(@Nonnull IMapObject mapObject, boolean confirm)
    {
        if (registeredMapObjects.contains(mapObject))
        {
            var displayName = mapObject.getName();
            if (!confirm || AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], "<html><center>Are you sure you want to delete the Drifter configuration for <b>" + displayName + "</b>?" + "</center></html>", "Confirm Delete Drifter Configuration - " + displayName, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            {
                final var lock = lockSupport.lock(mapObject.getUid());
                try
                {
                    if (registeredMapObjects.contains(mapObject))
                    {
                        registeredMapObjects.remove(mapObject);
                        List.of(eventListeners.getListeners(DrifterConfigurationListener.class)).forEach(l -> l.removed(mapObject));
                        mapObject.setHeading(Double.NaN);
                        mapObject.setSpeedInMps(0);
                        var config = mapObject.getDrifterConfiguration();
                        mapObject.setDrifterConfiguration(null);
                        if (config != null)
                        {
                            var route = getRoute(config);
                            if (route != null)
                            {
                                unregisterRoute(mapObject, route);
                            }
                        }
                        return true;
                    }
                } finally
                {
                    lock.unlock();
                }
            }
        }
        return false;
    }

    /**
     * @return a List of all map objects with drifter configurations
     */
    @Nonnull
    public List<IMapObject> getMapObjects()
    {
        return List.copyOf(registeredMapObjects);
    }

    /**
     * @param listener the listener to add
     */
    public void addDrifterConfigurationListener(@Nonnull DrifterConfigurationListener listener)
    {
        eventListeners.add(DrifterConfigurationListener.class, listener);
    }

    /**
     * Simulate the passage of time for a {@link IMapObject} or all map objects based on drifter configuration.
     *
     * @param mapObject the map object to move
     * @param durationMs the number of milliseconds to fast-forward
     */
    public void simulateTime(@Nonnull IMapObject mapObject, long durationMs)
    {
        executorService.submit(() -> {
            Thread.currentThread().setName(getClass().getSimpleName());
            moveDuration(mapObject, durationMs);
        });
    }

    /**
     * Simulate the passage of time for all registered {@link IMapObject IMapObjects}.
     *
     * @param durationMs the number of milliseconds to fast-forward
     */
    public void simulateTimeAll(long durationMs)
    {
        executorService.submit(() -> {
            Thread.currentThread().setName(getClass().getSimpleName());
            getMapObjects().forEach(mo -> moveDuration(mo, durationMs));
        });
    }

    /**
     * @param mapObject the map object that must contain a drifter configuration
     * @return the drifter configuration for the given map object
     * @throws IllegalStateException if the map object does not have a drifter configuration
     * @since 5.7
     */
    @Nonnull
    public DrifterConfiguration requireDrifterConfiguration(@Nonnull IMapObject mapObject)
    {
        return Objects.requireNonNull(mapObject.getDrifterConfiguration(), () -> "Map object: '" + mapObject.getName() + "' has no DrifterConfiguration");
    }

    /**
     * @param configuration a drifter configuration
     * @return the Route object for the drifter configuration, if it exists
     * @since 5.7
     */
    @Nullable
    public IRouteMapObject getRoute(@Nonnull DrifterConfiguration configuration)
    {
        var uid = configuration.routeUid();
        if (uid != null)
        {
            var route = mapObjectFinder.findMapObjectByUid(uid);
            if (route != null)
            {
                if (route instanceof IRouteMapObject routeMapObject)
                {
                    return routeMapObject;
                } else
                {
                    logger.warn("DrifterConfiguration route: {} is not a route", uid);
                }
            } else
            {
                logger.warn("DrifterConfiguration route: {} does not exist", uid);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        stopSchedule();
        registeredMapObjects.clear();
    }

    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices ignored)
    {
        startSchedule();
    }

    private void registerMapObject(IMapObject mapObject)
    {
        if (!registeredMapObjects.contains(mapObject))
        {
            registeredMapObjects.add(mapObject);
            var drifter = requireDrifterConfiguration(mapObject);
            var route = getRoute(drifter);
            if (route != null) registerRoute(mapObject, route);
            List.of(eventListeners.getListeners(DrifterConfigurationListener.class)).forEach(l -> l.added(mapObject));
            if (!drifter.paused()) startSchedule();
        }
    }

    private void registerRoute(IMapObject mapObject, IRouteMapObject route)
    {
        registeredRoutesMap.computeIfAbsent(route.getUid(), k -> new CopyOnWriteArraySet<>()).add(mapObject);
    }

    private void unregisterRoute(IMapObject mapObject, IRouteMapObject route)
    {
        registeredRoutesMap.get(route.getUid()).remove(mapObject);
    }

    private void unregisterRoute(IRouteMapObject route)
    {
        if (registeredRoutesMap.containsKey(route.getUid()))
        {
            registeredRoutesMap.get(route.getUid()).forEach(mo -> deleteConfiguration(mo, false));
        }
    }

    /**
     * Move the map object based on the configuration speed.
     *
     * @param mapObject the map object
     * @param durationMs the duration to move, or {@code -1} to calculate based on current time and last communication time
     */
    private void moveDuration(IMapObject mapObject, long durationMs)
    {
        final var lock = lockSupport.lock(mapObject.getUid());
        try
        {
            var current = System.currentTimeMillis();
            durationMs = durationMs == -1 ? current - mapObject.getMostRecentCommunicationTime() : durationMs;
            final var startLocation = mapObject.getLocation();
            final var startBearing = mapObject.getHeading();

            if (durationMs <= 0) return;
            final var configuration = requireDrifterConfiguration(mapObject);
            if (configuration.speed() == 0) return;

            var distanceMeters = configuration.speed() * (durationMs / 1000d);
            var route = getRoute(configuration);

            if (route == null)
            {
                mapObject.setTrack(configuration.bearing(), configuration.speed());
                mapObject.setLocation(ShapeUtils.rhumbEndPositionMeters(startLocation, configuration.bearing(), distanceMeters), current, false);
            } else
            {
                var controlPoints = route.getControlPoints();
                var routePoint = configuration.routePoint();
                var distanceLeftToMove = distanceMeters;
                var nextLocation = startLocation;
                var nextBearing = startBearing;

                while (routePoint < controlPoints.size())
                {
                    var targetLocation = route.getPointByIndex(routePoint).get();
                    var distanceToTarget = GeoCalculations.distance(nextLocation, targetLocation);
                    if (distanceToTarget < distanceLeftToMove)
                    {
                        if (distanceToTarget > 0) nextBearing = GeoCalculations.bearing(nextLocation, targetLocation);
                        routePoint++;
                        nextLocation = targetLocation;
                        distanceLeftToMove -= distanceToTarget;
                    } else
                    {
                        nextBearing = GeoCalculations.bearing(nextLocation, targetLocation);
                        nextLocation = new GeoPoint(GeoCalculations.pointAtDistance(nextLocation, nextBearing, distanceLeftToMove));
                        break;
                    }
                }

                if (routePoint == controlPoints.size()) routePoint--;

                if (configuration.routePoint() != routePoint)
                {
                    // Use the latest the config so we don't override any external changes made
                    mapObject.setDrifterConfiguration(DrifterConfiguration.builder(mapObject.getDrifterConfiguration()).routePoint(routePoint).build());
                }
                mapObject.setTrack(nextBearing, configuration.speed());
                mapObject.setLocation(nextLocation, current, false);
            }
        } finally
        {
            lock.unlock();
        }
    }

    private void stepSafe(IMapObject mapObject)
    {
        try
        {
            moveDuration(mapObject, -1);
        } catch (Exception e)
        {
            logger.error("Pausing after error for drifter configuration: {} ", mapObject, e);
            setPaused(mapObject, true);
        }
    }

    private void stepAll()
    {
        Thread.currentThread().setName(getClass().getSimpleName());
        var count = new AtomicInteger(0);
        getMapObjects()
                .stream()
                .filter(mo -> !requireDrifterConfiguration(mo).paused())
                .peek(dc -> count.incrementAndGet())
                .forEach(this::stepSafe);
        if (count.get() == 0) stopSchedule();
    }

    @SuppressWarnings("unused")
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (event.getMapObject() != null && event.getMapObject().getDrifterConfiguration() != null)
        {
            registerMapObject(event.getMapObject());
        }
    }

    @SuppressWarnings("unused")
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject() instanceof IRouteMapObject route)
        {
            unregisterRoute(route);
        } else
        {
            deleteConfiguration(event.getMapObject(), false);
        }
    }

    private synchronized void startSchedule()
    {
        if (movementFuture == null)
        {
            logger.debug("Starting drifter schedule");
            movementFuture = executorService.scheduleWithFixedDelay(this::stepAll, 0, STEP_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void stopSchedule()
    {
        if (movementFuture != null)
        {
            logger.debug("Stopping drifter schedule");
            movementFuture.cancel(false);
            movementFuture = null;
        }
    }

    /**
     * Listener for changes to drifter configurations.
     */
    public interface DrifterConfigurationListener extends EventListener
    {
        /**
         * @param mapObject the map object that a drifter configuration was added to
         */
        void added(@Nonnull IMapObject mapObject);

        /**
         * @param mapObject the map object that a drifter configuration was removed from
         */
        void removed(@Nonnull IMapObject mapObject);
    }
}
