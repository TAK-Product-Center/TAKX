package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to remove a {@link MissionLayer} instance on the server.
 *
 * @since 3.5
 */
public class RemoveMissionLayer extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("uid", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission containing the layer.
     */
    public RemoveMissionLayer(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.removeMissionLayer(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * @param uid Unique id of the layer to remove.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionLayer withUid(@Nonnull String uid)
    {
        return setParam("uid", uid);
    }
}
