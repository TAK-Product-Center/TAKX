package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.sync.DeleteContent;
import gov.tak.api.comms.takserver.mission.call.sync.GetContent;
import gov.tak.api.comms.takserver.mission.call.sync.SearchResources;
import gov.tak.api.comms.takserver.mission.call.sync.UploadResource;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignSyncApi}.
 *
 * @since 3.5
 */
public class SyncEndpoints
{
    private final FeignSyncApi syncApi;
    private final UUID clientUid;

    /**
     * @param apiProvider Provider of client instances.
     * @param clientUid Unique client identifier.
     * @param connection TAK server connection to use to initialize an API client with.
     */
    public SyncEndpoints(@Nonnull ApiProvider apiProvider,
                         @Nonnull UUID clientUid,
                         @Nonnull ITakServerConnection connection)
    {
        syncApi = apiProvider.getSyncApi(connection);
        this.clientUid = clientUid;
    }

    /**
     * @return Endpoint to get file contents from enterprise sync.
     */
    @Nonnull
    public GetContent getContent()
    {
        return new GetContent(syncApi);
    }

    /**
     * @param hash SHA256 hash of the file.
     * @return Endpoint to remove a file from enterprise sync.
     * @since 5.7
     */
    @Nonnull
    public DeleteContent deleteContent(@Nonnull String hash)
    {
        return new DeleteContent(syncApi, hash);
    }

    /**
     * @return Endpoint to search for file contents on enterprise sync.
     */
    @Nonnull
    public SearchResources searchResources()
    {
        return new SearchResources(syncApi);
    }

    /**
     * @param filePath Path to file to upload.
     *
     * @return Endpoint to search for file contents on enterprise sync.
     *
     * @throws IOException When the file path cannot be read from.
     */
    @Nonnull
    public UploadResource upload(@Nonnull Path filePath) throws IOException
    {
        return upload(Files.readAllBytes(filePath));
    }

    /**
     * @param raw Raw file contents to upload.
     *
     * @return Endpoint to upload a file to enterprise sync.
     */
    @Nonnull
    public UploadResource upload(@Nonnull byte[] raw)
    {
        return upload("application/octet-stream", raw);
    }

    /**
     * @param text Text file contents to upload.
     *
     * @return Endpoint to upload a file to enterprise sync.
     * @since 5.3
     */
    @Nonnull
    public UploadResource upload(@Nonnull String text)
    {
        return interceptClientUid(new UploadResource(syncApi, "text/plain", text.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * @param contentType Mime type of file contents.
     * @param raw Raw file contents to upload.
     *
     * @return Endpoint to upload a file to enterprise sync.
     * @since 5.3
     */
    @Nonnull
    public UploadResource upload(@Nonnull String contentType, @Nonnull byte[] raw)
    {
        return interceptClientUid(new UploadResource(syncApi, contentType, raw));
    }

    @Nonnull
    private <T, E extends IEndpointCaller<T>> E interceptClientUid(@Nonnull E endpoint)
    {
        return IClientAssociatedEndpointCaller.interceptClientUid(endpoint, clientUid);
    }
}
