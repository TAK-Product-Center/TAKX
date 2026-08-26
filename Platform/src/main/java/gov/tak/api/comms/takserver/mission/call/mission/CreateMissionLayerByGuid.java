package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.data.MissionLayerType;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to create a {@link MissionLayer} instance on the server.
 *
 * @since 5.7
 */
public class CreateMissionLayerByGuid extends AbstractEndpointCaller<MissionLayer> implements IClientAssociatedEndpointCaller<MissionLayer>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("name", ""),
            param("type", ""),
            paramOptional("parentUid", ""),
            paramOptional("afterUid", ""),
            paramOptional("creatorUid", ""));

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to hold the layer.
     */
    public CreateMissionLayerByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.createMissionLayerByGuid(headers, params, missionGuid), PARAM_MAP);
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
    public CreateMissionLayerByGuid withName(@Nonnull String name)
    {
        return setParam("name", name);
    }

    /**
     * @param type Layer type to set.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMissionLayerByGuid withType(@Nonnull MissionLayerType type)
    {
        return setParam("type", type.name());
    }

    /**
     * @param uid Unique id to assign to the layer. If not provided, the server will auto-generate one.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMissionLayerByGuid withUid(@Nonnull String uid)
    {
        return setParam("uid", uid);
    }

    /**
     * @param parentUid The {@link MissionLayer#uid()} of the parent layer to associate this layer with.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMissionLayerByGuid withParentUid(@Nonnull String parentUid)
    {
        return setParam("parentUid", parentUid);
    }

    /**
     * @param afterUid The {@link MissionLayer#uid()} of the adjacent layer to associate this layer with.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMissionLayerByGuid withAfterUid(@Nonnull String afterUid)
    {
        return setParam("afterUid", afterUid);
    }
}
