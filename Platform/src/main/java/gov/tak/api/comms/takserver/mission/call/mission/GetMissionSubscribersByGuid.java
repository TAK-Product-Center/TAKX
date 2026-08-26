package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to a given mission.
 *
 * @since 5.7
 */
public class GetMissionSubscribersByGuid extends AbstractEndpointCaller<List<String>>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get subscriptions of.
     */
    public GetMissionSubscribersByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionSubscribersByGuid(headers, params, missionGuid));
    }
}
