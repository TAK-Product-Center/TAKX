package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove a {@link MissionSubscription} from a {@link Mission}.
 *
 * @see CreateMissionSubscription
 * @see GetCurrentMissionSubscription
 * @since 3.5
 */
public class RemoveMissionSubscription extends AbstractEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("uid", ""),
            paramOptional("topic", ""),
            paramOptional("disconnectOnly", true));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to create a subscription to.
     */
    public RemoveMissionSubscription(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.removeMissionSubscription(headers, params, missionName)), PARAM_MAP);
    }

    /**
     * @param uid The {@link ClientEndpoint#uid()} to use as the removal lookup.
     *
     * @return Self.
     *
     * @see #withTopic(String) For subscriptions not targeting specific users.
     */
    @Nonnull
    public RemoveMissionSubscription withUid(@Nonnull String uid)
    {
        // Can only provide either 'uid' or 'topic'
        return setParam("uid", uid)
                .unsetParam("topic");
    }

    /**
     * @param topic The topic name to use as the removal lookup
     *
     * @return Self.
     *
     * @see #withUid(String) For subscriptions targeting specific users.
     */
    @Nonnull
    public RemoveMissionSubscription withTopic(@Nonnull String topic)
    {
        // Can only provide either 'uid' or 'topic'
        return unsetParam("uid")
                .setParam("topic", topic);
    }

    // TODO: What does this flag do? The back-end logic doesn't make sense.
    //  - In 'DistributedSubscriptionManager' having disconnectOnly=true removes the association in the subscription-store,
    //     but keeps the association in the missionSubscriptionRepository... Why? I have no clue what the semantics of each
    //     are because nothing is documented and at the lowest level its just a bunch of SQL with generic column names.
    @Nonnull
    public RemoveMissionSubscription withDisconnectOnly(boolean disconnectOnly)
    {
        return setParam("disconnectOnly", disconnectOnly);
    }
}
