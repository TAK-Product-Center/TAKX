package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get a list of all {@link MissionSubscription subscriptions} to a given mission.
 *
 * @since 3.5
 */
public class GetMissionSubscriptions extends AbstractEndpointCaller<List<MissionSubscription>>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get subscriptions of.
     */
    public GetMissionSubscriptions(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionSubscriptions(headers, params, missionName));
    }
}
