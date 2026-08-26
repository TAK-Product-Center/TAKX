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
 * @since 3.5
 */
public class GetMissionInvitations extends AbstractEndpointCaller<List<MissionInvitation>>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the invitations to.
     */
    public GetMissionInvitations(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionInvitations(headers, params, missionName));
    }
}
