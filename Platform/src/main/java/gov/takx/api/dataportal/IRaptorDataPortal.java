package gov.takx.api.dataportal;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * The DataPortal is the single point of entry for data flowing into TAKX from connections. The DataPortal is
 * responsible for creating a DataPortalController for each map object, ensuring that each piece of data is persisted
 * as soon as it is received, and then making the data available to the rest of TAKX through data listener interfaces.
 *
 */
@Internal
public interface IRaptorDataPortal extends IDataListenerManager
{
    /**
     * @return Get all the data controllers managed by the DataPortal.
     */
    Collection<? extends IRaptorDataPortalController> getControllers();

    /**
     * Construct a controller for the given Map Object.  This is used when the MapObjectManager is initially
     * creating a new Map Object.
     *
     * @param mapObject          Map Object to process
     * @param propagationHandler Logic to execute if a new DPC is created (i.e., it will be propagated to listeners)
     * @since 2.29
     */
    void createController(IMapObject mapObject, Consumer<IRaptorDataPortalController> propagationHandler);

    /**
     * Get the controller for a map object.
     *
     * @param deviceKey The identifier of the device whose controller is to be retrieved.
     * @return data controller for the specified device, or {@code null} if no
     * such device is founded
     */
    IRaptorDataPortalController getController(String deviceKey);

    /**
     * Delete a map object from the data portal.
     *
     * @param mapObjectKey map object key
     */
    void deleteMapObject(String mapObjectKey);

    /**
     * Called when a map object is defined.
     *
     * @param msg IMapEntity
     */
    void onMapObjectDefinition(IMapEntity msg);

    /**
     * Send an RDM into the device gateway, possibly bringing the device online.
     *
     * @param rdm RDM to send
     * @since 2.29
     */
    void sendToGateway(IPrePersistRaptorDataMessage rdm);
}
