package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to create a {@link LogEntry} instance on the server.
 *
 * @since 3.5
 */
public class CreateLogEntry extends AbstractEndpointCaller<LogEntry>
{
    /**
     * @param api API reference.
     * @param entry Entry to publish.
     */
    public CreateLogEntry(@Nonnull FeignMissionApi api, @Nonnull LogEntry entry)
    {
        super((headers, params) -> api.createLogEntry(headers, params, entry));
    }
}
