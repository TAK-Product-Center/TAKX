package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionFeed;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove an {@link MissionFeed} item from a {@link Mission} instance.
 *
 * @see AddFeed
 * @since 3.5
 */
public class RemoveFeed extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param uid The {@link MissionFeed#uid()} of feed to remove.
     */
    public RemoveFeed(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String uid)
    {
        super(devoid((headers, params) -> api.removeFeed(headers, params, missionName, uid)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
