package gov.tak.core.comms.takserver.mission;

import com.atakmap.net.AtakAuthenticationDatabase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.takx.commons.lang.ExceptionUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Data-sync component for persisting mission metadata for a given server connection.
 *
 * @since 5.7
 */
public class MissionPersistence
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final TypeReference<Map<String, Mission>> STRING_MISSION_MAP = new TypeReference<>()
    {
    };

    private static final String TYPE_PASSWORD = "DATA_SYNC_PASSWORD";
    private static final String TYPE_ACCESS_TOKEN = "DATA_SYNC_ACCESS_TOKEN";
    private static final String CACHED_DOWNLOADED_MISSIONS = "missions.json";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // This module and the timestamp=false are key for odd default handling of 'Instant'
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

    private final DataSyncManager dataSyncManager;
    private final MissionManager missionManager;
    private final MissionResources missionResources;

    MissionPersistence(@Nonnull MissionManager missionManager, @Nonnull MissionResources missionResources, @Nonnull DataSyncManager dataSyncManager)
    {
        this.missionManager = missionManager;
        this.missionResources = missionResources;
        this.dataSyncManager = dataSyncManager;
    }

    /**
     * Store downloaded mission metadata.
     */
    public void persist()
    {
        ITakServerConnection connection = missionManager.getConnection();
        File missionsDir = missionResources.getMissionsDir();

        try
        {
            if (!missionsDir.exists() && !missionsDir.mkdirs())
            {
                logger.error("Failed to persist mission data, could not create directory: {}", missionsDir);
                return;
            }

            // We will always want to write our data, even if nothing is stored.
            // If we start TAKX with data and clear it all out before we save we will want to overwrite the state that we had in the beginning.
            missionManager.getMissionPasswords().forEach((guid, password) -> AtakAuthenticationDatabase.saveCredentials(TYPE_PASSWORD, guid, guid, password, false));
            missionManager.getMissionSubscriptionTokenMap().forEach((guid, token) -> AtakAuthenticationDatabase.saveCredentials(TYPE_ACCESS_TOKEN, guid, guid, token, false));
            ObjectWriter writer = JSON_MAPPER.writerWithDefaultPrettyPrinter();
            writer.writeValue(new File(missionsDir, CACHED_DOWNLOADED_MISSIONS), missionManager.getDownloadedMissionMap());
        } catch (IOException e)
        {
            String message = "Failed to persist server models for '" + connection.getDisplayName() + "'";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Restore downloaded mission metadata.
     */
    public void restore()
    {
        ITakServerConnection connection = missionManager.getConnection();
        File missionsDir = missionResources.getMissionsDir();

        try
        {
            File downloadedMissionStore = new File(missionsDir, CACHED_DOWNLOADED_MISSIONS);
            if (downloadedMissionStore.exists())
            {
                // Read the downloaded mission models, then query the credential database for any associated passwords/tokens.
                Map<String, String> cachedTokens = new HashMap<>();
                Map<String, String> cachedPasswords = new HashMap<>();
                Map<String, Mission> cachedMissions = JSON_MAPPER.readValue(downloadedMissionStore, STRING_MISSION_MAP);
                cachedMissions.keySet().forEach(guid -> {
                    var passwordCredentials = AtakAuthenticationDatabase.getCredentials(TYPE_PASSWORD, guid);
                    var tokenCredentials = AtakAuthenticationDatabase.getCredentials(TYPE_ACCESS_TOKEN, guid);
                    if (passwordCredentials != null) cachedPasswords.put(guid, passwordCredentials.password);
                    if (tokenCredentials != null) cachedTokens.put(guid, tokenCredentials.password);
                });

                for (Mission mission : cachedMissions.values())
                {
                    String guid = mission.guid();
                    String token = cachedTokens.get(guid);
                    String password = cachedPasswords.get(guid);
                    missionManager.putDownloadedMission(mission, password, token, false);

                    // Notify internal listeners that the mission has been restored
                    ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncRestorationListeners(), (token == null) ? listener -> listener.onDownloadedMissionRestore(connection, mission) : listener -> listener.onSubscribedMissionRestore(connection, mission));
                }
            }
        } catch (IOException e)
        {
            String message = "Failed to restore server models for '" + connection.getDisplayName() + "'";
            logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
