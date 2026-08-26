package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to replace all keywords of a {@link Mission} instance.
 *
 * @see ClearMissionKeywords
 * @see RemoveMissionKeyword
 * @since 3.5
 */
public class SetMissionKeywords extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param keywords Keywords to set.
     */
    public SetMissionKeywords(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull List<String> keywords)
    {
        super(unpack((headers, params) -> api.setMissionKeywords(headers, params, missionName, keywords)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
