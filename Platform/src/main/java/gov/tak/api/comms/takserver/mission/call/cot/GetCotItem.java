package gov.tak.api.comms.takserver.mission.call.cot;

import feign.Response;
import feign.Util;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.feign.FeignCotApi;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_MULTIPLE_CHOICES;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * Endpoint caller to get the CoT XML of a single map item.
 *
 * @since 5.0
 */
public class GetCotItem extends AbstractEndpointCaller<Response>
{
    private final String uid;

    /**
     * @param api API reference.
     * @param uid UID of the map item to get CoT XML of.
     */
    public GetCotItem(@Nonnull FeignCotApi api, @Nonnull String uid)
    {
        super((headers, params) -> api.getCotItem(headers, params, uid));
        this.uid = uid;
    }

    /**
     * Calls {@link #get()} and extracts the text contents.
     *
     * @return Text contents from the request.
     *
     * @throws IOException When the response body cannot be fully read.
     * @throws ApiException When the CoT could not be downloaded from the server.
     */
    @Nonnull
    public String getText() throws IOException, ApiException
    {
        Response response = get();
        if (response == null) throw new IOException("Response was empty");

        int statusCode = response.status();

        // Check specific error codes
        if (statusCode == SC_FORBIDDEN) throw new ApiException("Permission denied requesting cot: " + uid);
        if (statusCode == SC_NOT_FOUND) throw new ApiException("No such cot item exists: " + uid);

        // If the response is OK, decode the body into the CoT message
        if (statusCode >= SC_OK && statusCode < SC_MULTIPLE_CHOICES) return decode(response);

        // Any non 2XX response code is assumed to be invalid
        throw new ApiException("Received not OK code (" + statusCode + ") requesting cot: " + uid);
    }

    /**
     * Calls {@link #asyncGet()} and maps the response to {@code byte[]}.
     *
     * @return Future of raw content bytes from the request.
     */
    @Nonnull
    public CompletableFuture<String> asyncGetText()
    {
        return asyncGet().thenApply(GetCotItem::uncheckedDecode);
    }

    /**
     * Calls {@link #asyncGet(ExecutorService)} and maps the response to {@code byte[]}.
     *
     * @param executorService Executor service to use for the completable future supplier.
     *
     * @return Future of raw content bytes from the request.
     */
    @Nonnull
    public CompletableFuture<String> asyncGetText(@Nonnull ExecutorService executorService)
    {
        return asyncGet(executorService).thenApply(GetCotItem::uncheckedDecode);
    }

    @Nonnull
    private static String uncheckedDecode(@Nonnull Response response) throws UncheckedIOException
    {
        try
        {
            return decode(response);
        } catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    @Nonnull
    private static String decode(@Nonnull Response response) throws IOException
    {
        try (Response.Body body = response.body())
        {
            return Util.toString(body.asReader(StandardCharsets.UTF_8));
        }
    }
}
