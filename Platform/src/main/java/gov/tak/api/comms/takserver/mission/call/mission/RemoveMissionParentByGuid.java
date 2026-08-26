package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to unlink the parent-child relation of a given {@link Mission} instance on the server.
 * This change is not visible in the serialized {@link Mission} model.
 *
 * @see GetMissionChildrenByGuid
 * @see GetMissionParentByGuid
 * @see SetMissionParentByGuid
 * @since 5.7
 */
public class RemoveMissionParentByGuid extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to remove parent link of.
     */
    public RemoveMissionParentByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super(devoid((headers, params) -> api.removeMissionParentByGuid(headers, params, missionGuid)));
    }
}
