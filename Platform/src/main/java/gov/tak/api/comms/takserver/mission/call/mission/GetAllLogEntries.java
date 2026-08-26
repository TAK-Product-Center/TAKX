package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to get all {@link LogEntry} instances across all missions.
 *
 * @since 3.5
 */
public class GetAllLogEntries extends AbstractEndpointCaller<List<LogEntry>>
{
    /**
     * @param api API reference.
     */
    public GetAllLogEntries(@Nonnull FeignMissionApi api)
    {
        super(api::getAllLogEntries);
    }
}
