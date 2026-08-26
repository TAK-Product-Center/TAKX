package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove all keywords from a {@link Mission} instance.
 *
 * @see SetMissionKeywords
 * @see RemoveMissionKeyword
 * @since 3.5
 */
public class ClearMissionKeywords extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     */
    public ClearMissionKeywords(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(unpack((headers, params) -> api.clearMissionKeywords(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
