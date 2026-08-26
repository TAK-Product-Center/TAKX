package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.RemoteSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get the associated {@link RemoteSubscription} items attached to a {@link Mission}.
 *
 * @since 5.7
 */
public class GetRemoteSubscriptionsByGuid extends AbstractEndpointCaller<List<RemoteSubscription>>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get the remote subscriptions of.
     */
    public GetRemoteSubscriptionsByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getRemoteSubscriptionsByGuid(headers, params, missionGuid));
    }
}
