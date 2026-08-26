package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.Mission;
import jakarta.annotation.Nonnull;

/**
 * Listener for handling data-sync mission <i>(feed)</i> updates.
 *
 * @since 5.0
 */
public interface IDataSyncMissionListener
{
    /**
     * Called when a new mission is available on a server.
     *
     * @param connection Connection to server defining the mission.
     * @param mission New mission model.
     */
    void onNewMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);

    /**
     * Called when an existing mission is updated on a server.
     *
     * @param connection Connection to server defining the mission.
     * @param oldMission Old mission model.
     * @param mission Updated mission model.
     */
    void onUpdateMission(@Nonnull ITakServerConnection connection, @Nonnull Mission oldMission, @Nonnull Mission mission);

    /**
     * Called when a mission is removed from a server.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Removed mission model.
     */
    void onRemoveMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);
}
