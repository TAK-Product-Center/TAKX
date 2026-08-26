package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to retract a {@link MissionInvitation}.
 *
 * @since 5.7
 */
public class RemoveMissionInvitationByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     */
    public RemoveMissionInvitationByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid,
                                         @Nonnull InvitationType type, @Nonnull String invitee)
    {
        super(devoid((headers, params) -> api.uninviteToMissionByGuid(headers, params, missionGuid, type.name(), invitee)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
