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
 * @since 3.5
 */
public class SendMissionInvitations extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to create invitations for.
     * @param invitations Invitations to send to the server.
     */
    public SendMissionInvitations(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                  @Nonnull List<MissionInvitation> invitations)
    {
        super(devoid((headers, params) -> api.sendMissionInvites(headers, params, missionName, invitations)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
