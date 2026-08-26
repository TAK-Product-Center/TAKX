package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to update a {@link MapLayer} instance on the mission.
 *
 * @since 5.7
 */
public class UpdateMapLayerByGuid extends AbstractEndpointCaller<MapLayer> implements IClientAssociatedEndpointCaller<MapLayer>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layer Layer to update on the server. Must share a {@link MapLayer#uid()} with an existing layer.
     */
    public UpdateMapLayerByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull MapLayer layer)
    {
        super((headers, params) -> api.updateMapLayerByGuid(headers, params, missionGuid, layer), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
