package gov.takx.api.dataportal;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import jakarta.annotation.Nonnull;

/**
 * This interface provides access to data coming from a connection. Clients can add message listeners to get access
 * to the data as it is received. Each listener is assigned a priority, and data events are dispatched in order of
 * priority.
 *
 * @see IRaptorDataListener
 */
@NoPluginImplementations
public interface IRaptorDataPortalController
{
    int HIGH_PRIORITY = 100;
    int LOW_PRIORITY = 0;
    int MEDIUM_PRIORITY = (HIGH_PRIORITY - LOW_PRIORITY) / 2;

    /**
     * Add a data listener to this controller.   Listeners are given a priority, and are notified of
     * data in order of priority (high to low).
     *
     * @param listener Listener to add
     * @param priority the priority of the new listener, in the range [LOW_PRIORITY:HIGH_PRIORITY]
     */
    void addListener(@Nonnull IRaptorDataListener listener, int priority);

    /**
     * Remove a data listener from this controller.
     *
     * @param listener Listener to remove.
     */
    void removeListener(@Nonnull IRaptorDataListener listener);

    /**
     * Send a data message upward.
     *
     * @param rdm Message to send
     */
    void sendDataMessage(@Nonnull IPrePersistRaptorDataMessage rdm);

    /**
     * @return the MapObject for this controller
     * @since 3.0
     */
    @Nonnull
    IMapObject getMapObject();
}
