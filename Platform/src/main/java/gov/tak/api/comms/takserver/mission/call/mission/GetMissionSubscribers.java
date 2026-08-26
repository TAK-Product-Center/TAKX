package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to a given mission.
 *
 * @since 3.5
 */
public class GetMissionSubscribers extends AbstractEndpointCaller<List<String>>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get subscriptions of.
     */
    public GetMissionSubscribers(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionSubscribers(headers, params, missionName));
    }
}
