package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionRole;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to get the current user's role within a {@link Mission}.
 *
 * @since 5.7
 */
public class GetMissionRoleByGuid extends AbstractEndpointCaller<MissionRole>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get the role within.
     */
    public GetMissionRoleByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionRoleByGuid(headers, params, missionGuid));
    }
}
