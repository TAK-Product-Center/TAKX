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
 * @since 3.5
 */
public class GetRemoteSubscriptions extends AbstractEndpointCaller<List<RemoteSubscription>>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the remote subscriptions of.
     */
    public GetRemoteSubscriptions(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getRemoteSubscriptions(headers, params, missionName));
    }
}
