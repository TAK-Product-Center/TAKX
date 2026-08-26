package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller get all child missions of a given parent {@link Mission}.
 * The relation is not serialized into the {@link Mission} model, so there is no way to locally differentiate
 * which missions do and do not have parents assigned.
 *
 * @see GetMissionParent
 * @see SetMissionParent
 * @see RemoveMissionParent
 * @since 3.5
 */
public class GetMissionChildren extends AbstractEndpointCaller<List<Mission>>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get the children of.
     */
    public GetMissionChildren(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> api.getChildMissions(headers, params, missionName));
    }
}
