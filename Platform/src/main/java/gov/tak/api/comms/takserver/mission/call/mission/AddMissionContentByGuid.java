package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionContentKeys;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to add one or more {@link Resource} entries to a {@link Mission}.
 *
 * @since 5.7
 */
public class AddMissionContentByGuid extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to update.
     * @param keys Keys of {@link Resource#uid()} and {@link Resource#hash()} items.
     */
    public AddMissionContentByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid,
                                   @Nonnull MissionContentKeys keys)
    {
        super(unpack((headers, params) -> api.addMissionContentByGuid(headers, params, missionGuid, keys)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
