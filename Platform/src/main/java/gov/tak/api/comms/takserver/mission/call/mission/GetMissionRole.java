package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionRole;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to get the current user's role within a {@link Mission}.
 *
 * @since 3.5
 */
public class GetMissionRole extends AbstractEndpointCaller<MissionRole>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the role within.
     */
    public GetMissionRole(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionRole(headers, params, missionName));
    }
}
