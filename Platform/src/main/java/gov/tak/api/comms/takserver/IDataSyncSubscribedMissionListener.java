package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.Mission;
import jakarta.annotation.Nonnull;

/**
 * Listener for handling data-sync mission <i>(feed)</i> updates for subscribed missions.
 *
 * @since 5.0
 */
public interface IDataSyncSubscribedMissionListener
{
    /**
     * Called when the given mission is subscribed to.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission model.
     */
    void onSubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);

    /**
     * Called when the given mission is unsubscribed from.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission model.
     */
    void onUnsubscribe(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);

    /**
     * Called when an existing mission that we are subscribed to is updated on a server.
     * The mission model in this case has more details filled in than the other update call.
     *
     * @param connection Connection to server defining the mission.
     * @param oldMission Old mission model.
     * @param mission Updated mission model.
     */
    void onSubscribedMissionUpdate(@Nonnull ITakServerConnection connection, @Nonnull Mission oldMission, @Nonnull Mission mission);
}
