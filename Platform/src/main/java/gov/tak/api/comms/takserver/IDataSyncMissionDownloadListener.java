package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.Resource;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * Listener for handling data-sync resource download updates.
 *
 * @since 5.0
 */
public interface IDataSyncMissionDownloadListener
{
    /**
     * Called when an attempt to download a mission resource has completed. If the file at the target path is empty, the
     * resource data has <em>not</em> been downloaded and the user still needs to manually complete the download request.
     * This method will be called again with a non-empty file if the user makes the request.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission holding the resource.
     * @param resource The resource associated with the mission.
     * @param localPath The local file path where the resource should be stored. The file may be empty if the user must
     *                  manually complete the download
     */
    void onDownloadResource(@Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                            @Nonnull Resource resource, @Nonnull File localPath);

    /**
     * Called when an attempt to download mission CoT XML has completed. If the file at the target path is empty, the
     * CoT XML has <em>not</em> been downloaded and the user still needs to manually complete the download request. This
     * method will be called again with a non-empty file if the user makes the request.
     *
     * @param connection Connection to server defining the mission.
     * @param mission Mission holding the resource.
     * @param cot The CoT outline downloaded.
     * @param localPath The local file path where the CoT XML should be stored. The file may be empty if the user must 
     *                  manually complete the download
     */
    void onDownloadCot(@Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                       @Nonnull MissionAdd<String> cot, @Nonnull File localPath);
}
