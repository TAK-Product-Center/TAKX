package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.RoleType;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to update a user's role within a {@link Mission}.
 *
 * @since 3.5
 */
public class SetMissionRole extends AbstractEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("clientUid", ""),
            paramOptional("username", ""),
            param("role", RoleType.MISSION_SUBSCRIBER.name()));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update roles within.
     */
    public SetMissionRole(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.setMissionRole(headers, params, missionName)), PARAM_MAP);
    }

    /**
     * Marks which user should have their role updated.
     *
     * @param username User to update within the mission.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionRole withUsername(@Nonnull String username)
    {
        // While the server technically allows both without throwing an error, it will only pick one.
        return setParam("username", username)
                .unsetParam("clientUid");
    }

    /**
     * Marks which user should have their role updated.
     *
     * @param clientUid User to update within the mission.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionRole withClientUid(@Nonnull String clientUid)
    {
        // While the server technically allows both without throwing an error, it will only pick one.
        return setParam("username", "")
                .setParam("clientUid", clientUid);
    }

    /**
     * Marks which role is to be set for the user.
     *
     * @param role Role type to assign.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionRole withRole(@Nonnull RoleType role)
    {
        return setParam("role", role.name());
    }
}
