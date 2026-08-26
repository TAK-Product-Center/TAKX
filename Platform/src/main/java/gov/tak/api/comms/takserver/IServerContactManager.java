package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.UUID;

/**
 * Contract for the service that manages server contacts from each TAK server connection established.
 *
 * @since 3.1
 */
@InjectableService
@NoPluginImplementations
public interface IServerContactManager
{
    /**
     * Get the callsign associated with the given {@code contactUid} for the given server.
     * If no contact is known for that UID, then the original {@code contactUid} is returned.
     *
     * @param serverUuid The server's UUID
     * @param contactUid UID of contact to query
     * @return Associated callsign, or {@code contactUid} if no callsign found
     */
    @Nonnull
    String getContactCallsign(@Nonnull UUID serverUuid, @Nonnull String contactUid);

    /**
     * @return Map of server UUIDs to the contacts on that server represented as a map of contact uids to callsigns.
     *
     * @since 3.5
     */
    @Nonnull
    Map<UUID, Map<String, String>> getContactsPerServer();

    /**
     * @param serverUuid The server's UUID
     *
     * @return Map of client uids to client endpoints associated with the server.
     * If the server is unknown, returns an empty map.
     *
     * @since 5.0
     */
    @Nonnull
    Map<String, ClientEndpoint> getServerClientEndpoints(@Nonnull UUID serverUuid);
}
