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
 * @since 5.7
 */
public class SendMissionInvitationByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            paramOptional("role", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     */
    public SendMissionInvitationByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid,
                                       @Nonnull InvitationType type, @Nonnull String invitee)
    {
        super(devoid((headers, params) -> api.inviteToMissionByGuid(headers, params, missionGuid, type.getApiName(), invitee)), PARAM_MAP);
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
    public SendMissionInvitationByGuid withRole(@Nonnull RoleType role)
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
