package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.MissionSubEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to all missions.
 *
 * @since 3.5
 */
public class GetAllMissionSubscribers extends AbstractEndpointCaller<List<MissionSubEntry>>
{
    /**
     * @param api API reference.
     */
    public GetAllMissionSubscribers(@Nonnull FeignMissionApi api)
    {
        super(api::getAllMissionSubscriptions);
    }
}
