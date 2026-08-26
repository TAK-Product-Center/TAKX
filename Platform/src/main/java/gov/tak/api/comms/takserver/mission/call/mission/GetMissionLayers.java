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
 * @since 3.5
 */
public class GetMissionLayers extends AbstractEndpointCaller<List<MissionLayer>>
{

    /**
     * @param api API reference.
     * @param missionName Name of the mission that contains layers to look up.
     */
    public GetMissionLayers(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getMissionLayers(headers, params, missionName));
    }
}
