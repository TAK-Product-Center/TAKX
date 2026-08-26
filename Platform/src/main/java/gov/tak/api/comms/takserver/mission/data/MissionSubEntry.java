package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used by {@link gov.tak.api.comms.takserver.mission.call.mission.GetAllMissionSubscribers} to correlate
 * missions with subscribers.
 * @since 3.5
 * @param missionName Name of the mission.
 * @param clientUid Associated {@link ClientEndpoint#uid()}.
 */
public record MissionSubEntry(@JsonProperty("key") String missionName, @JsonProperty("value") String clientUid)
{
    // TAK-Server serializes a raw 'Map.Entry' rather than a proper data type, hence the property name mappings
}
