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
 * @see GetMissionChildrenByGuid
 * @see GetMissionParentByGuid
 * @see RemoveMissionParentByGuid
 * @since 5.7
 */
public class SetMissionParentByGuid extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to update.
     * @param parentMissionGuid Guid of the parent to assign to the mission.
     */
    public SetMissionParentByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid, @Nonnull String parentMissionGuid)
    {
        super(devoid((headers, params) -> api.setMissionParentByGuid(headers, params, missionGuid, parentMissionGuid)));
    }
}
