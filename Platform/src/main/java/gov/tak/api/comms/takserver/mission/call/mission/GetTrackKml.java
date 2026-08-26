package gov.tak.api.comms.takserver.mission.call.mission;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.feign.FeignTracksApi;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zeroturnaround.zip.ZipUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static org.apache.http.HttpStatus.SC_OK;

/**
 * Endpoint caller to fetch track data by {@link gov.takx.api.playback.IMarker} UID.
 *
 * @since 5.4
 */
public class GetTrackKml extends AbstractEndpointCaller<Response>
{
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(StandardTimeFormatting.ISO_INSTANT_SHORT).withZone(ZoneOffset.UTC);

    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("extendedData", true),
            param("format", "kmz"),
            param("optimizeExport", true));

    /**
     * @param api The API
     * @param markerUid The marker's UID
     */
    public GetTrackKml(@Nonnull FeignTracksApi api, @Nonnull String markerUid)
    {
        super(api::getTracks, PARAM_MAP);
        setParam("uid", markerUid);
    }

    /**
     * @param threshold The max duration (in minutes) between breadcrumbs before a track will be split into multiple tracks
     * @return this
     */
    public GetTrackKml withMultiTrackThreshold(int threshold)
    {
        setParam("multiTrackThreshold", threshold);
        return this;
    }

    /**
     * @param start The start time bound or {@code null} if unbounded
     * @param end The end time bound or {@code null} if unbounded
     * @return this
     */
    public GetTrackKml withTimeBounds(@Nullable Instant start, @Nullable Instant end)
    {
        if (start != null && end != null && start.isAfter(end)) throw new IllegalArgumentException("start is after end");

        if (start != null) setParam("startTime", DATE_TIME_FORMATTER.format(start));
        if (end != null) setParam("endTime", DATE_TIME_FORMATTER.format(end));

        return this;
    }

    /**
     * @return KML file bytes that may contain track data
     * @throws IOException thrown if a response was not obtained from the server
     * @throws ApiException thrown if the server's response is invalid
     */
    @Nonnull
    public byte[] getKmlFileBytes() throws IOException, ApiException
    {
        final var response = get();
        if (response == null) throw new IOException("Response was empty");

        final var status = response.status();
        if (status != SC_OK) throw new ApiException("Received response status code " + status);

        return unpack(response);
    }

    /**
     * @return KML file bytes that may contain track data
     */
    @Nonnull
    public CompletableFuture<byte[]> asyncGetKmlFileBytes()
    {
        return asyncGet().thenApply(this::uncheckedUnpack);
    }

    /**
     * @param executorService The executor to use for asynchronous execution
     * @return A future that will return the KML file bytes that may contain track data
     */
    public CompletableFuture<byte[]> asyncGetKmlFileBytes(@Nonnull ExecutorService executorService)
    {
        return asyncGet(executorService).thenApply(this::uncheckedUnpack);
    }

    /**
     * @param response The response to process
     * @return The KML file bytes extracted from the response
     * @throws UncheckedIOException if there are issues reading data from the response
     */
    @Nonnull
    private byte[] uncheckedUnpack(@Nonnull Response response) throws UncheckedIOException
    {
        try
        {
            return unpack(response);
        } catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param response The response to process
     * @return The KML file bytes extracted from the response
     * @throws IOException if there are issues reading data from the response
     */
    @Nonnull
    private byte[] unpack(@Nonnull Response response) throws IOException
    {
        try (Response.Body body = response.body())
        {
            return ZipUtil.unpackEntry(body.asInputStream(), "doc.kml");
        }
    }
}
