package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get a list of all {@link MissionSubscription subscriptions} to a given mission.
 *
 * @since 5.7
 */
public class GetMissionSubscriptionsByGuid extends AbstractEndpointCaller<List<MissionSubscription>>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get subscriptions of.
     */
    public GetMissionSubscriptionsByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionSubscriptionsByGuid(headers, params, missionGuid));
    }
}
