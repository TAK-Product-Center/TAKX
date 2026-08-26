package gov.tak.core.comms.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import jakarta.annotation.Nonnull;

/**
 * Listener for receiving notifications of restored data sync content.
 *
 * @since 5.3
 */
public interface IDataSyncRestorationListener
{
    /**
     * Called when a mission that was previously subscribed to has been restored.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission restored.
     */
    void onSubscribedMissionRestore(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);

    /**
     * Called when a mission that was previously downloaded has been restored.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission restored.
     */
    void onDownloadedMissionRestore(@Nonnull ITakServerConnection connection, @Nonnull Mission mission);
}
