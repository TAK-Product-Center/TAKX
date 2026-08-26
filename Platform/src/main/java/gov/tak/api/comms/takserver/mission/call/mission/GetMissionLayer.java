package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to get a {@link MissionLayer} instance from the server.
 *
 * @since 3.5
 */
public class GetMissionLayer extends AbstractEndpointCaller<MissionLayer>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     */
    public GetMissionLayer(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String layerUid)
    {
        super((headers, params) -> api.getMissionLayer(headers, params, missionName, layerUid));
    }
}
