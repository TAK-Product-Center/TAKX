package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint caller to create a new {@link MissionSubscription} to a {@link Mission}.
 *
 * @see RemoveMissionSubscriptionByGuid
 * @see GetCurrentMissionSubscriptionByGuid
 * @since 5.7
 */
public class CreateMissionSubscriptionByGuid extends AbstractEndpointCaller<MissionSubscription>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("uid", ""),
            paramOptional("topic", ""),
            paramOptional("password", ""),
            paramOptional("secago", ""),
            paramOptional("start", ""),
            paramOptional("end", ""));

    /**
     * @param api API reference.
     * @param missionGuid Name of the mission to create a subscription to.
     */
    public CreateMissionSubscriptionByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.createMissionSubscriptionByGuid(headers, params, missionGuid), PARAM_MAP);
    }

    /**
     * @param uid The {@link ClientEndpoint#uid()} to assign as the subscription's holder.
     *
     * @return Self.
     *
     * @see #withTopic(String) For subscriptions not targeting specific users.
     */
    @Nonnull
    public CreateMissionSubscriptionByGuid withUid(@Nonnull String uid)
    {
        // Can only provide either 'uid' or 'topic'
        return setParam("uid", uid)
                .unsetParam("topic");
    }

    /**
     * @param topic The topic name to create a subscription for.
     *
     * @return Self.
     *
     * @see #withUid(String) For subscriptions targeting specific users.
     */
    @Nonnull
    public CreateMissionSubscriptionByGuid withTopic(@Nonnull String topic)
    {
        // Can only provide either 'uid' or 'topic'
        return unsetParam("uid")
                .setParam("topic", topic);
    }

    // TODO: The lack of documentation makes it unclear what these are supposed to be for (in terms of why the sub has these attributes)
    //  - Why would 'secago' exist? What is the point of a subscription that starts back in time, but ends at the current time?

    @Nonnull
    public CreateMissionSubscriptionByGuid withStart(@Nonnull Instant start)
    {
        return setParam("start", start);
    }

    @Nonnull
    public CreateMissionSubscriptionByGuid withEnd(@Nonnull Instant end)
    {
        return setParam("end", end);
    }

    @Nonnull
    public CreateMissionSubscriptionByGuid withSecondsAgo(long secago)
    {
        return setParam("secago", secago);
    }

    /**
     * @param password The password of the mission, if one exists.
     *
     * @return Self.
     * @since 5.1
     */
    @Nonnull
    public CreateMissionSubscriptionByGuid withPassword(@Nonnull String password)
    {
        return setParam("password", password);
    }
}
