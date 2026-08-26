package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Headers;
import feign.QueryMap;
import feign.RequestLine;
import feign.Response;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * API for querying {@link gov.takx.api.playback.IMarker} {@link gov.takx.api.breadcrumb.ITrack} data.
 *
 * @since 5.4
 */
@NoPluginImplementations
public interface FeignTracksApi
{
    /**
     * @param headers Additional request headers
     * @param queryMap Map of query names to values
     * @param kmlFile The KML file
     * @since 5.5
     */
    @Nonnull
    @RequestLine("POST /TracksKML")
    @Headers("Content-Type: application/vnd.google-earth.kml+xml")
    Response publishTrack(@Nonnull @HeaderMap Map<String, Object> headers, @Nonnull @QueryMap Map<String, Object> queryMap, @Nonnull byte[] kmlFile);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     */
    @Nonnull
    @RequestLine("GET /ExportMissionKML")
    Response getTracks(@Nonnull @HeaderMap Map<String, Object> headers, @Nonnull @QueryMap Map<String, Object> queryMap);
}
