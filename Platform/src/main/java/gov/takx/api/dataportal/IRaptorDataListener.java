package gov.takx.api.dataportal;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;

/**
 * Implement this interface to receive data from an {@link IRaptorDataPortalController}. The listener can be registered
 * by calling {@link IDataListenerManager#addListener(IRaptorDataListener)}.
 *
 * @see IRaptorDataPortalController
 */
public interface IRaptorDataListener
{
    /**
     * Override to match only map objects that meet certain criteria.
     *
     * @param mapObject The new map object.
     * @return true if the listener wants to receive data from {@code mapObject}.
     */
    boolean matchMapObject(IMapObject mapObject);

    /**
     * Override to match data (RDMs) coming only from map objects that meet certain criteria.
     * <p>
     * If you only care about Map Object online/offline events, but not the RDMs, then return
     * {@code true} from {@link #matchMapObject(IMapObject)} and {@code false} from this method.
     * <p>
     * By default, this method redirects to {@link #matchMapObject(IMapObject)} for backward compatibility.
     *
     * @param mapObject The map object
     * @return true if the listener wants to receive data from the Map Object
     * @since 2.29
     */
    default boolean matchMapObjectForData(IMapObject mapObject)
    {
        return matchMapObject(mapObject);
    }

    /**
     * Called when a new map object is defined.
     *
     * @param message Definition of new object.
     */
    void onMapObjectDefined(IMapEntity message);

    /**
     * Called when a map object is added.
     *
     * @param mapObject  The new map object.
     * @param controller The controller that is handling the data feed for the new map object.
     */
    void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller);

    /**
     * This method is called when a message is received.
     *
     * @param topic  the topic to which this message was published.
     * @param msg    the message itself.
     * @param struct the data structure associated with the message.
     */
    void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct);

    /**
     * This method is called when a data feed is disconnected. The
     * disconnect may be user initiated, or may be because a connection
     * was lost.
     *
     * @param controller The controller for the feed that was disconnected.
     */
    void onDisconnect(IRaptorDataPortalController controller);

    /**
     * The Map Object is going offline (being deleted).
     *
     * @param msg                Message with the details of the map object that is going offline
     * @param controller         The controller for the map object that is going offline
     * @param wipeHistoricalData always {@code true}
     */
    void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData);
}
