package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller get the parent {@link Mission} of another {@link Mission} if a parent-child relation exists.
 * The relation is not serialized into the {@link Mission} model, so there is no way to locally differentiate
 * which missions do and do not have parents assigned.
 *
 * @see GetMissionChildrenByGuid
 * @see SetMissionParentByGuid
 * @see RemoveMissionParentByGuid
 * @since 5.7
 */
public class GetMissionParentByGuid extends AbstractEndpointCaller<Mission>
{
    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to get the parent of.
     */
    public GetMissionParentByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super((headers, params) -> api.getMissionParentByGuid(headers, params, missionGuid));
    }
}
