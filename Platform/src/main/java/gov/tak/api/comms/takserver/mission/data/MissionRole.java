package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.Set;

/**
 * @param type The role of the user pertaining to mission access/responsibilities.
 * @param permissions The permissions granted to users in the scope of the mission. Set value is taken as-is and is not copied.
 *
 * @since 3.5
 */
public record MissionRole(RoleType type, @JsonSetter(nulls = Nulls.AS_EMPTY) Set<MissionPermission> permissions)
{
}
