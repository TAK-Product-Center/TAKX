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
public class SetMissionLayerNameByGuid extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("name", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     */
    public SetMissionLayerNameByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull String layerUid)
    {
        super(devoid((headers, params) -> api.setMissionLayerName(headers, params, missionGuid, layerUid)), PARAM_MAP);
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
    public SetMissionLayerNameByGuid withName(@Nonnull String name)
    {
        return setParam("name", name);
    }
}
