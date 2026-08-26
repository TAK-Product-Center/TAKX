package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.data.RoleType;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to publish a {@link MissionInvitation} to the server.
 *
 * @since 3.5
 */
public class SendMissionInvitation extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            paramOptional("role", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     */
    public SendMissionInvitation(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                 @Nonnull InvitationType type, @Nonnull String invitee)
    {
        super(devoid((headers, params) -> api.inviteToMission(headers, params, missionName, type.getApiName(), invitee)), PARAM_MAP);
    }

    /**
     * Marks which role should be applied to users of the invite,
     * otherwise {@link Mission#defaultRole()} is applied by the user on the server-side.
     *
     * @param role Role to assign.
     *
     * @return Self.
     */
    @Nonnull
    public SendMissionInvitation withRole(@Nonnull RoleType role)
    {
        return setParam("role", role.name());
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
