package gov.tak.api.comms.takserver.mission.data;

/**
 * Type of role for {@link MissionRole}.
 *
 * @since 3.5
 */
public enum RoleType
{
    /**
     * Full access and control over the mission, barring and server-side config preventing certain actions.
     */
    MISSION_OWNER,
    /**
     * Access to mission capabilities allowed by {@link MissionRole#permissions()}.
     */
    MISSION_SUBSCRIBER,
    /**
     * Access to mission read-only capabilities.
     */
    MISSION_READONLY_SUBSCRIBER
}
