package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to remove a {@link LogEntry} instance by its ID.
 *
 * @since 3.5
 */
public class RemoveLogEntry extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param id The {@link LogEntry#entryUid()} to delete.
     */
    public RemoveLogEntry(@Nonnull FeignMissionApi api, @Nonnull String id)
    {
        super(devoid((headers, params) -> api.removeLogEntry(headers, params, id)));
    }
}
