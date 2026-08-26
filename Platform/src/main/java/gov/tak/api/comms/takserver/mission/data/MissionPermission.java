package gov.tak.api.comms.takserver.mission.data;

/**
 * Server-side permission roles.
 *
 * @since 3.5
 */
public enum MissionPermission
{
    /**
     * Grants access to read-only actions.
     */
    MISSION_READ,
    /**
     * Grants access to actions that modify missions.
     */
    MISSION_WRITE,
    /**
     * Grants access to mission deletion.
     */
    MISSION_DELETE,
    /**
     * Grants access to allow changing mission roles.
     */
    MISSION_SET_ROLE,
    /**
     * Grants access to allow changing mission passwords.
     */
    MISSION_SET_PASSWORD,
    /**
     * Grants access to allow changing mission groups.
     */
    MISSION_UPDATE_GROUPS,
    /**
     * Grants access to allow changing mission feeds.
     */
    MISSION_MANAGE_FEEDS,
    /**
     * Grants access to allow changing mission layers.
     */
    MISSION_MANAGE_LAYERS
}
