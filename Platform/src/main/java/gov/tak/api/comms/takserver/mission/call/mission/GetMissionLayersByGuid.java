package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get all {@link MissionLayer} instances associated with a {@link Mission}.
 *
 * @since 5.7
 */
public class GetMissionLayersByGuid extends AbstractEndpointCaller<List<MissionLayer>>
{

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission that contains layers to look up.
     */
    public GetMissionLayersByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionLayersByGuid(headers, params, missionGuid));
    }
}
