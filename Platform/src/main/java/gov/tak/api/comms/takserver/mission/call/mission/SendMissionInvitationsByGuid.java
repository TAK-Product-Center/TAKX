package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to publish a list of {@link MissionInvitation invitations} to the server.
 *
 * @since 5.7
 */
public class SendMissionInvitationsByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to create invitations for.
     * @param invitations Invitations to send to the server.
     */
    public SendMissionInvitationsByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid,
                                        @Nonnull List<MissionInvitation> invitations)
    {
        super(devoid((headers, params) -> api.sendMissionInvitesByGuid(headers, params, missionGuid, invitations)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
