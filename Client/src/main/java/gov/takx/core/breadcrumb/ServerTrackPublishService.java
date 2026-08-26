package gov.takx.core.breadcrumb;

import feign.Response;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes {@link gov.takx.api.breadcrumb.ITrack tracks} to TAK servers.
 *
 * @since 5.5
 */
@ApplicationScoped
public class ServerTrackPublishService
{
    private final TakServerApiClient apiClient;

    @Inject
    ServerTrackPublishService(TakServerApiClient apiClient)
    {
        this.apiClient = apiClient;
    }

    /**
     * @param connection The connection to the server that should be published to
     * @param markerUid The track owner marker's UID
     * @param callsign The track owner's callsign
     * @param cotType The track owner's CoT type
     * @param groupName The track owner's group
     * @param groupRole The track owner's group role
     * @param kmlFile The track KML file to publish
     * @throws IOException if there are errors reading {@code kmlFile}
     */
    public CompletableFuture<Response> publishTrack(@Nonnull ITakServerConnection connection, @Nonnull String markerUid,
                                                    @Nonnull String callsign, @Nonnull String cotType,
                                                    @Nonnull String groupName, @Nonnull String groupRole,
                                                    @Nonnull File kmlFile)
            throws IOException
    {
        return apiClient.trackEndpoints(connection)
                .publishTrack(kmlFile)
                .withUid(markerUid)
                .withCallsign(callsign)
                .withCotType(cotType)
                .withGroupName(groupName)
                .withGroupRole(groupRole)
                .asyncGet();
    }
}
