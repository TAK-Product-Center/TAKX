package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to update a {@link MissionLayer} instance on the server.
 *
 * @since 5.7
 */
public class SetMissionLayerPositionByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("afterUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     */
    public SetMissionLayerPositionByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull String layerUid)
    {
        super(devoid((headers, params) -> api.setMissionLayerPositionByGuid(headers, params, missionGuid, layerUid)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param afterUid The {@link MissionLayer#uid()} of the adjacent layer to associate this layer with.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionLayerPositionByGuid withAfterUid(@Nonnull String afterUid)
    {
        return setParam("afterUid", afterUid);
    }
}
