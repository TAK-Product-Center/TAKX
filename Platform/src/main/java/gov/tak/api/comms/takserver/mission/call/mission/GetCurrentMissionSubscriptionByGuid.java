package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to get the {@link MissionSubscription} of a {@link Mission} for the current user.
 *
 * @see RemoveMissionSubscriptionByGuid
 * @see CreateMissionSubscriptionByGuid
 * @since 5.7
 */
public class GetCurrentMissionSubscriptionByGuid extends AbstractEndpointCaller<MissionSubscription>
        implements IClientAssociatedEndpointCaller<MissionSubscription>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("uid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get the subscription info of.
     */
    public GetCurrentMissionSubscriptionByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getSubscriptionForCurrentUserByGuid(headers, params, missionGuid), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "uid";
    }
}
