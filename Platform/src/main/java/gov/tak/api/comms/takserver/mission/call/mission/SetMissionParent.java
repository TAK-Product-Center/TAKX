package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to link one {@link Mission} instance to another in a parent-child relation on the server.
 * This change is not visible in the serialized {@link Mission} model. Users must use {@link GetMissionParent}
 * and {@link GetMissionChildren} to determine parent-child relations.
 *
 * @see GetMissionChildren
 * @see GetMissionParent
 * @see RemoveMissionParent
 * @since 3.5
 */
public class SetMissionParent extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     * @param parentMissionName Name of the parent to assign to the mission.
     */
    public SetMissionParent(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nonnull String parentMissionName)
    {
        super(devoid((headers, params) -> api.setMissionParent(headers, params, missionName, parentMissionName)));
    }
}
