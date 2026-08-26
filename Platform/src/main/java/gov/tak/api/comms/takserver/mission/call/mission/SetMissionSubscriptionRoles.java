package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to update roles of some given {@link MissionSubscription} instances.
 *
 * @since 3.5
 */
public class SetMissionSubscriptionRoles extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update subscriptions of.
     * @param subscriptions Subscriptions to update.
     * Each subscription must declare both {@link MissionSubscription#clientUid()} and {@link MissionSubscription#role()}.
     */
    public SetMissionSubscriptionRoles(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                       @Nonnull List<MissionSubscription> subscriptions)
    {
        super(devoid((headers, params) -> api.setMissionSubscriptionRoles(headers, params, missionName, subscriptions)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
