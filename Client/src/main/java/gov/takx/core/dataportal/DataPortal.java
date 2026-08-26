package gov.takx.core.dataportal;

import gov.takx.api.dataportal.IRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.MapObjectOfflineMessage;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 */
@Singleton
public class DataPortal implements IRaptorDataPortal
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private volatile boolean acceptingData;

    private final Map<String, DataPortalController> controllers = new ConcurrentHashMap<>();
    private final ConcurrentSkipListSet<PrioritizedEntry<IRaptorDataListener>> listeners = new ConcurrentSkipListSet<>();
    private final IDeviceGateway deviceGateway;

    /**
     * CDI Constructor.
     */
    @Inject
    DataPortal(IDeviceGateway deviceGateway)
    {
        this.deviceGateway = deviceGateway;
    }

    @Override
    public void addListener(@Nonnull IRaptorDataListener listener, int priority)
    {
        // Don't allow a listener to be added twice
        if (listeners.stream().anyMatch(entry -> entry.delegate == listener)) return;

        var prioritizedEntry = new PrioritizedEntry<>(listener, priority);

        listeners.add(prioritizedEntry);
        controllers.values().forEach(controller -> controller.addListener(prioritizedEntry));
    }

    @Override
    public void removeListener(@Nonnull IRaptorDataListener listener)
    {
        listeners.removeIf(entry -> entry.delegate == listener);
        controllers.values().forEach(controller -> controller.removeListener(listener));
    }

    /**
     * Return all the data controllers managed by the data portal.
     */
    @Override
    public Collection<? extends IRaptorDataPortalController> getControllers()
    {
        return Collections.unmodifiableCollection(controllers.values());
    }

    @Override
    public IRaptorDataPortalController getController(String deviceKey)
    {
        return controllers.get(deviceKey);
    }

    /**
     * Start up.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_DataPortal) StartProjectServices event)
    {
        acceptingData = true;
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_DataPortal) StopProjectServices event)
    {
        acceptingData = false;
        clear();
    }

    /**
     * A new MapEntity has been created (or updated with new structures). Notify our listeners.
     *
     * @param mapEntity new MapEntity
     */
    @Override
    public void onMapObjectDefinition(IMapEntity mapEntity)
    {
        ensureValidMapEntity(mapEntity);

        listeners.forEach(entry -> entry.safeApply(listener -> listener.onMapObjectDefined(mapEntity), "new MapEntity"));
    }

    /**
     * Ensure that the given MapEntity has been properly constructed (i.e., has a non-null structure list).
     *
     * @param mapEntity MapEntity to test
     * @since 3.0
     */
    private void ensureValidMapEntity(IMapEntity mapEntity)
    {
        if (mapEntity.getDataStructures() == null)
        {
            // If you're working on the core, and you get here, then you need to ensure that the MapEntity was
            // created using IMapObjectPersist.getOrCreateMapEntity (which is what the DataDelegate does).
            // This error message is primarily for plugin developers that may have somehow managed to directly
            // construct a MapEntity.

            throw new IllegalArgumentException("mapEntity '" + mapEntity + "'was not properly created." +
                    " See IDataDelegate.getOrCreateMapEntity");
        }
    }

    @Override
    public void createController(IMapObject mapObject, Consumer<IRaptorDataPortalController> propagationHandler)
    {
        logger.debug("Create controller for: {}, accept={}", mapObject, acceptingData);

        if (!acceptingData) return;

        if (mapObject.isTransient())
        {
            propagationHandler.accept(null);
            return;
        }

        final var propagateNewController = new AtomicBoolean(false);

        // Check if controller has already been created
        var controller = controllers.computeIfAbsent(mapObject.getKey(), k -> {
            propagateNewController.set(true);
            return new DataPortalController(mapObject);
        });

        logger.debug("prop={}", propagateNewController);

        // Check acceptingData flag in case a shutdown was initiated while we were mucking about with the map object.
        if (propagateNewController.get() && acceptingData)
        {
            propagationHandler.accept(controller);
            propagateMapObjectOnline(mapObject, controller);
        }
    }

    @Override
    public void sendToGateway(IPrePersistRaptorDataMessage rdm)
    {
        final String uniqueKey = rdm.getUniqueKey();
        final IGatewayController controller = deviceGateway.getController(uniqueKey, true);

        if (controller != null)
        {
            controller.processConnectionMessage(rdm);
        } else
        {
            final IRaptorDataPortalController dpc = getController(uniqueKey);

            if (dpc != null)
            {
                dpc.sendDataMessage(rdm);
            } else
            {
                logger.error("Error trying to find Controller for device type = {}", uniqueKey);
            }
        }
    }

    @Override
    public void deleteMapObject(String mapObjectKey)
    {
        logger.debug("Deleting map object: {}", mapObjectKey);

        removeMapObject(controllers.remove(mapObjectKey), mapObjectKey);
    }

    private void removeMapObject(DataPortalController controller, String mapObjectKey)
    {
        if (controller == null)
        {
            // This can happen in tests if we are just running in a container or the MO doesn't have a controller.
            logger.debug("No Controller found for key: {}", mapObjectKey);
            return;
        }

        final IMapObject mapObject = controller.getMapObject();
        final MapObjectOfflineMessage msg = new MapObjectOfflineMessage(mapObject);

        // Notify listeners and remove listeners from the controller
        listeners.forEach(entry -> entry.safeApply(listener -> {
            controller.removeListener(listener);
            listener.onMapObjectOffline(msg, controller, true);
        }, "map object offline"));
    }

    private void clear()
    {
        listeners.clear();

        controllers.values().forEach(DataPortalController::close);
        controllers.clear();
    }

    private void propagateMapObjectOnline(IMapObject mapObject, DataPortalController controller)
    {
        listeners.forEach(entry -> entry.safeApply(listener -> {
            if (listener.matchMapObject(mapObject))
            {
                listener.onMapObjectAdded(mapObject, controller);
                controller.addListener(entry);
            }
        }, "new map object"));
    }
}
