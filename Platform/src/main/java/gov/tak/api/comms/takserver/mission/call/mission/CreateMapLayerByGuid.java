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
 * @since 5.7
 */
public class CreateMapLayerByGuid extends AbstractEndpointCaller<MapLayer> implements IClientAssociatedEndpointCaller<MapLayer>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to hold the layer.
     * @param layer Layer to publish on the server.
     */
    public CreateMapLayerByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull MapLayer layer)
    {
        super((headers, params) -> api.createMapLayerByGuid(headers, params, missionGuid, layer), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
