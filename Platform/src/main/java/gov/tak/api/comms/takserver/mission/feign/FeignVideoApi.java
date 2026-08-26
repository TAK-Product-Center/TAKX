package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.data.VideoConnection;
import gov.tak.api.comms.takserver.mission.data.VideoConnections;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * REST interface modeling the Video API from TAK Server.
 * <p/>
 * For internal use, see {@link gov.tak.api.comms.takserver.mission.VideoEndpoints},
 * accessible via {@link TakServerApiClient}
 *
 * @since 5.0
 */
@SuppressWarnings("NewClassNamingConvention")
@NoPluginImplementations
public interface FeignVideoApi
{
    @Nonnull
    @RequestLine("GET /video")
    VideoConnections getVideoCollections(@Nonnull @HeaderMap Map<String, Object> headers,
                                         @Nonnull @QueryMap Map<String, Object> queryMap);

    @Nonnull
    @RequestLine("GET /video/{uid}")
    VideoConnection getVideoCollection(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("uid") String uid);

    @RequestLine("POST /video")
    @Headers("Content-Type: application/json")
    void createVideoConnection(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull VideoConnections connections);

    @RequestLine("POST /video")
    @Headers("Content-Type: application/json")
    void updateVideoConnection(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull VideoConnections connections);

    @RequestLine("DELETE /video/{uid}")
    void deleteVideoConnection(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("uid") String uid);
}
