package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to get a {@link MissionLayer} instance from the server.
 *
 * @since 5.7
 */
public class GetMissionLayerByGuid extends AbstractEndpointCaller<MissionLayer>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     */
    public GetMissionLayerByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull String layerUid)
    {
        super((headers, params) -> api.getMissionLayerByGuid(headers, params, missionGuid, layerUid));
    }
}
