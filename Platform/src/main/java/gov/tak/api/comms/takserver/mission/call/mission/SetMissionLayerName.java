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
public class SetMissionLayerName extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("name", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     */
    public SetMissionLayerName(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String layerUid)
    {
        super(devoid((headers, params) -> api.setMissionLayerName(headers, params, missionName, layerUid)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param name Layer name to set.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionLayerName withName(@Nonnull String name)
    {
        return setParam("name", name);
    }
}
