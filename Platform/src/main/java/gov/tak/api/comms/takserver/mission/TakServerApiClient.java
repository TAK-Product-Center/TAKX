package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NodeUUID;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Tool for integration with TAK Server.
 *
 * @since 3.5
 */
@InjectableService
@ApplicationScoped
public class TakServerApiClient
{
    private final ApiProvider apiProvider;
    private final UUID clientUid;

    /**
     * @param apiProvider Provider service to create API clients with.
     * @param clientUid Unique client identifier.
     */
    @Inject
    public TakServerApiClient(@Nonnull ApiProvider apiProvider, @NodeUUID UUID clientUid)
    {
        this.apiProvider = apiProvider;
        this.clientUid = clientUid;
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     *
     * @return New endpoint utility instance for mission-centric API calls, targeting the given server.
     */
    @Nonnull
    public MissionEndpoints missionEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new MissionEndpoints(apiProvider, clientUid, connection);
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     *
     * @return New endpoint utility instance for cot-centric API calls, targeting the given server.
     */
    @Nonnull
    public CotEndpoints cotEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new CotEndpoints(apiProvider, connection);
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     *
     * @return New endpoint utility instance for group-centric API calls, targeting the given server.
     */
    @Nonnull
    public GroupEndpoints groupEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new GroupEndpoints(apiProvider, connection);
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     *
     * @return New endpoint utility instance for contact-management-centric API calls, targeting the given server.
     */
    @Nonnull
    public ContactManagerEndpoints contactManagerEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new ContactManagerEndpoints(apiProvider, connection);
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     *
     * @return New endpoint utility instance for enterprise-sync-centric API calls, targeting the given server.
     */
    @Nonnull
    public SyncEndpoints syncEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new SyncEndpoints(apiProvider, clientUid, connection);
    }

    /**
     * @param connection Connection instance to pull details from when needed.
     * @return New endpoint utility instance for video-centric API calls, targeting the given server.
     * @since 5.0
     */
    @Nonnull
    public VideoEndpoints videoEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new VideoEndpoints(apiProvider.getVideoApi(connection));
    }

    /**
     * @param connection The server connection
     * @return The endpoints instance used to make track-specific calls to the specified server
     * @since 5.4
     */
    @Nonnull
    public TrackEndpoints trackEndpoints(@Nonnull ITakServerConnection connection)
    {
        return new TrackEndpoints(apiProvider.getTracksApi(connection));
    }

    /**
     * The value that will represent the current client.
     *
     * @return The current client uid.
     *
     * @see ClientEndpoint#uid()
     */
    @Nonnull
    public UUID getClientUid()
    {
        return clientUid;
    }
}
