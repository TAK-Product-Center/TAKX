package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to unlink the parent-child relation of a given {@link Mission} instance on the server.
 * This change is not visible in the serialized {@link Mission} model.
 *
 * @see GetMissionChildren
 * @see GetMissionParent
 * @see SetMissionParent
 * @since 3.5
 */
public class RemoveMissionParent extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to remove parent link of.
     */
    public RemoveMissionParent(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.removeMissionParent(headers, params, missionName)));
    }
}
