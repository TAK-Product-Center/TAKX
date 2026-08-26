package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

/**
 * @param type The type of change.
 * @param missionName Name of associated mission.
 * @param timestamp Timestamp of the change.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user responsible for the change.
 * @param contentUid The map item's UID associated with the change. Denotes the content of the change.
 * Can be present when {@link #type()} is {@link MissionChangeType#ADD_CONTENT} or {@link MissionChangeType#REMOVE_CONTENT}
 * @param missionFeed The mission feed associated with the change. Denotes the content of the change.
 * Will be present when {@link #type()} is {@link MissionChangeType#CREATE_DATA_FEED} or {@link MissionChangeType#CREATE_DATA_FEED}
 * @param externalData The external data added or removed to/from the mission. Denotes the content of the change.
 * Can be present when {@link #type()} is {@link MissionChangeType#ADD_CONTENT} or {@link MissionChangeType#REMOVE_CONTENT}
 * @param contentResource The resource added or removed to/from the mission. Denotes the content of the change.
 * Can be present when {@link #type()} is {@link MissionChangeType#ADD_CONTENT} or {@link MissionChangeType#REMOVE_CONTENT}
 * @param details Additional details for map item addition and modifications.
 * Can be present when {@link #type()} is {@link MissionChangeType#ADD_CONTENT} or {@link MissionChangeType#REMOVE_CONTENT}
 * @param logEntry Associated log entry.
 *
 * @since 3.5
 */
public record MissionChange(MissionChangeType type,
        String missionName,
        Instant timestamp,
        String creatorUid,
        String contentUid,
        MissionFeed missionFeed,
        ExternalMissionData externalData,
        Resource contentResource,
        UidDetails details,
        LogEntry logEntry)
{
}
