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
 * @since 3.5
 */
public class RemoveMissionInvitation extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     */
    public RemoveMissionInvitation(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                   @Nonnull InvitationType type, @Nonnull String invitee)
    {
        super(devoid((headers, params) -> api.uninviteToMission(headers, params, missionName, type.name(), invitee)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
