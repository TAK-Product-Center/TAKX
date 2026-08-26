package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to delete an existing {@link Mission} instance on the server.
 *
 * @since 5.7
 */
public class RemoveMissionByGuid extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("guid", ""),
            paramOptional("creatorUid", ""),
            paramOptional("deepDelete", false));

    /**
     * @param api API reference.
     */
    public RemoveMissionByGuid(@Nonnull FeignMissionApi api)
    {
        super(unpack(api::removeMissionByGuid), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Enables or disables deletion of related resources of the mission.
     *
     * @param deepDelete {@code true} to delete all {@link Mission#contents() associated contents} attached to the mission.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionByGuid withDeepDelete(boolean deepDelete)
    {
        return setParam("deepDelete", deepDelete);
    }

    /**
     * @param guid Guid specifying which mission to remove.
     *
     * @return Self.
     */
    @Nonnull
    public RemoveMissionByGuid withGuid(@Nonnull String guid)
    {
        return setParam("guid", guid);
    }
}
