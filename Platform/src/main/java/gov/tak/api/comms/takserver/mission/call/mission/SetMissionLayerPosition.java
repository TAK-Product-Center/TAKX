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
 * @since 3.5
 */
public class SetMissionLayerPosition extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("afterUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     */
    public SetMissionLayerPosition(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String layerUid)
    {
        super(devoid((headers, params) -> api.setMissionLayerPosition(headers, params, missionName, layerUid)), PARAM_MAP);
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
    public SetMissionLayerPosition withAfterUid(@Nonnull String afterUid)
    {
        return setParam("afterUid", afterUid);
    }
}
