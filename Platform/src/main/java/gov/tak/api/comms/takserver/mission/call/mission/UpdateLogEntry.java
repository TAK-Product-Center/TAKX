package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to update a {@link LogEntry} instance on the server.
 *
 * @since 3.5
 */
public class UpdateLogEntry extends AbstractEndpointCaller<LogEntry>
{
    /**
     * @param api API reference.
     * @param entry Entry to update.
     */
    public UpdateLogEntry(@Nonnull FeignMissionApi api, @Nonnull LogEntry entry)
    {
        super((headers, params) -> api.updateLogEntry(headers, params, entry));
    }
}
