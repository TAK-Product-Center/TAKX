package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get the list of all invites to a {@link Mission}.
 *
 * @since 5.7
 */
public class GetMissionInvitationsByGuid extends AbstractEndpointCaller<List<MissionInvitation>>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get the invitations to.
     */
    public GetMissionInvitationsByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionInvitationsByGuid(headers, params, missionGuid));
    }
}
