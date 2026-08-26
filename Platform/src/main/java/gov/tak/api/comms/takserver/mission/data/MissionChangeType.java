package gov.tak.api.comms.takserver.mission.data;

/**
 * The type of change in a {@link MissionChange}.
 *
 * @since 3.5
 */
public enum MissionChangeType
{
    /**
     * Denotes the creation of the mission.
     */
    CREATE_MISSION,
    /**
     * Denotes the removal of the mission.
     */
    DELETE_MISSION,
    /**
     * Denotes content additions. Can include adding a {@link Resource}, {@link ExternalMissionData}
     * or another non {@link MissionFeed} value.
     * <br>
     * This type also covers updates to existing contents. You should check {@link MissionChange#contentUid()} being equal
     * to a past entry in the changelog to determine if this is the case.
     */
    ADD_CONTENT,
    /**
     * Denotes content removal. Can include removing a {@link Resource}, {@link ExternalMissionData}
     * or another non {@link MissionFeed} value.
     */
    REMOVE_CONTENT,
    /**
     * Denotes creating an associated {@link MissionFeed}.
     */
    CREATE_DATA_FEED,
    /**
     * Denotes removing an associated {@link MissionFeed}.
     */
    DELETE_DATA_FEED
}
