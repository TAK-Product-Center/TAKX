package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to set mission-local keywords of a contained {@link Mission#contents() resource}.
 *
 * @see ClearMissionResourceKeyword
 * @since 3.5
 */
public class SetMissionResourceKeywords extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission with a resource to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     * @param keywords Keywords to set.
     */
    public SetMissionResourceKeywords(@Nonnull FeignMissionApi api, @Nonnull String missionName,
                                      @Nonnull String hash, @Nonnull List<String> keywords)
    {
        super(devoid((headers, params) -> api.setMissionContentKeywords(headers, params, missionName, hash, keywords)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
