package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to create a {@link MapLayer} instance on the mission.
 *
 * @since 3.5
 */
public class CreateMapLayer extends AbstractEndpointCaller<MapLayer> implements IClientAssociatedEndpointCaller<MapLayer>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to hold the layer.
     * @param layer Layer to publish on the server.
     */
    public CreateMapLayer(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull MapLayer layer)
    {
        super((headers, params) -> api.createMapLayer(headers, params, missionName, layer), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
