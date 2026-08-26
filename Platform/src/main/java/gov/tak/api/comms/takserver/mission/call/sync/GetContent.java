package gov.tak.api.comms.takserver.mission.call.sync;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Endpoint caller to fetch a file from enterprise sync by its SHA26 hash.
 *
 * @since 3.5
 */
public class GetContent extends AbstractEndpointCaller<Response>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("hash", ""));

    /**
     * @param api API reference.
     */
    public GetContent(@Nonnull FeignSyncApi api)
    {
        super(api::content, PARAM_MAP);
    }

    /**
     * Sets the SHA256 file hash to fetch.
     *
     * @param hash SHA256 hash.
     *
     * @return Self.
     */
    @Nonnull
    public GetContent withHash(@Nonnull String hash)
    {
        return setParam("hash", hash);
    }

    /**
     * Calls {@link #get()} and extracts the {@code byte[]}.
     *
     * @return Raw content bytes from the request.
     *
     * @throws IOException When the response body stream cannot be fully read.
     * @throws ApiException When the file contents could not be downloaded from the server.
     */
    @Nonnull
    public byte[] getRaw() throws IOException, ApiException
    {
        Response response = get();
        if (response == null) throw new IOException("Response was empty");

        int statusCode = response.status();

        // Check specific error codes
        String hash = getParameterValues().getOrDefault("hash", "?").toString();
        if (statusCode == SC_FORBIDDEN) throw new ApiException("Permission denied requesting file by hash: " + hash);
        if (statusCode == SC_NOT_FOUND) throw new ApiException("No such file by hash exists: " + hash);

        // If the response is OK, unpack the body
        if (statusCode >= SC_OK && statusCode < SC_MULTIPLE_CHOICES) return unpack(response);

        // Any non 2XX response code is assumed to be invalid
        throw new ApiException("Received not OK code (" + statusCode + ") requesting file by hash: " + hash);
    }

    /**
     * Calls {@link #asyncGet()} and maps the response to {@code byte[]}.
     *
     * @return Future of raw content bytes from the request.
     */
    @Nonnull
    public CompletableFuture<byte[]> asyncGetRaw()
    {
        return asyncGet().thenApply(GetContent::uncheckedUnpack);
    }

    /**
     * Calls {@link #asyncGet(ExecutorService)} and maps the response to {@code byte[]}.
     *
     * @param executorService Executor service to use for the completable future supplier.
     *
     * @return Future of raw content bytes from the request.
     */
    @Nonnull
    public CompletableFuture<byte[]> asyncGetRaw(@Nonnull ExecutorService executorService)
    {
        return asyncGet(executorService).thenApply(GetContent::uncheckedUnpack);
    }

    @Nonnull
    private static byte[] uncheckedUnpack(@Nonnull Response response) throws UncheckedIOException
    {
        try
        {
            return unpack(response);
        } catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Nonnull
    private static byte[] unpack(@Nonnull Response response) throws IOException
    {
        try (Response.Body body = response.body())
        {
            return body.asInputStream().readAllBytes();
        }
    }
}
