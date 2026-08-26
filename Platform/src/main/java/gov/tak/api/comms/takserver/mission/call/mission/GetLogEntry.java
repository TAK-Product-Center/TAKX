package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to get a {@link LogEntry} instance by its ID.
 *
 * @since 3.5
 */
public class GetLogEntry extends AbstractEndpointCaller<LogEntry>
{
    /**
     * @param api API reference.
     * @param id The {@link LogEntry#entryUid()} to look-up.
     */
    public GetLogEntry(@Nonnull FeignMissionApi api, @Nonnull String id)
    {
        super((headers, params) -> api.getLogEntry(headers, params, id));
    }
}
