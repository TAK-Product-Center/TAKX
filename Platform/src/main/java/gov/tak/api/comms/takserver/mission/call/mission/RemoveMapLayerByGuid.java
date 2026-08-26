package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove a {@link MapLayer} instance from a mission.
 *
 * @since 5.7
 */
public class RemoveMapLayerByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(param("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MapLayer#uid() Unique identifier} of a layer to remove from the mission.
     */
    public RemoveMapLayerByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull String layerUid)
    {
        super(devoid((headers, params) -> api.removeMapLayerByGuid(headers, params, missionGuid, layerUid)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }
}
