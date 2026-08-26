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
public class SetMissionLayerParent extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("parentUid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission containing the layer.
     */
    public SetMissionLayerParent(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.setMissionLayerParent(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param parentUid The {@link MissionLayer#uid()} of the parent layer to associate this layer with.
     *
     * @return Self.
     */
    @Nonnull
    public SetMissionLayerParent withParentUid(@Nonnull String parentUid)
    {
        return setParam("parentUid", parentUid);
    }
}
