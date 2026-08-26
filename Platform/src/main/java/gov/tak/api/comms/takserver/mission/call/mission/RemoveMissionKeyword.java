package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove one keyword from a {@link Mission} instance.
 *
 * @see ClearMissionKeywords
 * @see SetMissionKeywords
 * @since 3.5
 */
public class RemoveMissionKeyword extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param keyword Keyword to remove from the mission.
     */
    public RemoveMissionKeyword(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String keyword)
    {
        super(unpack((headers, params) -> api.removeMissionKeyword(headers, params, missionName, keyword)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
