package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to clear all mission-local keywords of a contained {@link Mission#contents() resource}.
 *
 * @see SetMissionResourceKeywords
 * @since 3.5
 */
public class ClearMissionResourceKeyword extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission with a resource to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     */
    public ClearMissionResourceKeyword(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String hash)
    {
        super(devoid((headers, params) -> api.clearMissionContentKeywords(headers, params, missionName, hash)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
