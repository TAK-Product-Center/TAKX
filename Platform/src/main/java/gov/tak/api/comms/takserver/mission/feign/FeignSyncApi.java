package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.Response;
import gov.tak.api.comms.takserver.mission.SyncEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.call.sync.DeleteContent;
import gov.tak.api.comms.takserver.mission.call.sync.GetContent;
import gov.tak.api.comms.takserver.mission.call.sync.SearchResources;
import gov.tak.api.comms.takserver.mission.call.sync.UploadResource;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * REST interface modeling the enterprise sync endpoints <i>(From various APIs)</i> in TAK Server.
 * <p>
 * For internal use, see {@link SyncEndpoints},
 * accessible via {@link TakServerApiClient}
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface FeignSyncApi
{
    // Internal note: TAK-Server has other '/sync/...' which are scattered among other API's
    //  There is no documented reason for this. These operations belong to "Enterprise Sync".
    //  Finding documentation for what this feature is and why its scattered may be useful
    //  for developer documentation. Perhaps similar to @see in JavaDocs.
    //
    // In some instances, there are endpoints which do not appear in the Swagger UI at all.
    //  One such case is '/sync/content' which is used to download files.
    //
    // In other instances, there are duplicate endpoints with different paths, but similar/identical functionality.
    //  The '/sync/search' vs '/api/sync/search' endpoints for instance are defined in two separate controllers,
    //  offer the same parameters (but with a few formatting differences) and the same results (in a different format)
    //  Despite the '/api/ prefixed one being defined in a separate controller than the rest of these, it belongs
    //  to the same greater API/functionality group.

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return Search results of matching resources.
     *
     * @see SearchResources
     */
    @Nonnull
    @RequestLine("GET api/sync/search")
    List<Resource> search(@Nonnull @HeaderMap Map<String, Object> headers,
                          @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return HTTP response <i>(Not a special type so that automatic deserialization doesn't try to kick in)</i>
     *
     * @see GetContent
     */
    @Nonnull
    @RequestLine("GET /sync/content")
    Response content(@Nonnull @HeaderMap Map<String, Object> headers,
                     @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param hash SHA256 hash of the file.
     *
     * @see DeleteContent
     * @since 5.7
     */
    @RequestLine("DELETE api/files/{hash}")
    void delete(@Nonnull @HeaderMap Map<String, Object> headers,
                @Nonnull @QueryMap Map<String, Object> queryMap,
                @Nonnull @Param("hash") String hash);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param fileContent Raw file contents to upload.
     *
     * @return Minimal resource model detailing info about the file uploaded.
     * @see UploadResource
     */
    @Nonnull
    default UploadedResourceResponse upload(@Nonnull Map<String, Object> headers,
                                            @Nonnull Map<String, Object> queryMap,
                                            @Nonnull byte[] fileContent)
    {
        return upload(headers, queryMap, "application/octet-stream", fileContent);
    }

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param contentType Mime type of file contents.
     * @param fileContent Raw file contents to upload.
     *
     * @return Minimal resource model detailing info about the file uploaded.
     * @since 5.3
     * @see UploadResource
     */
    @Nonnull
    @Headers("Content-Type: {contentType}")
    @RequestLine("POST /sync/upload")
    UploadedResourceResponse upload(@Nonnull @HeaderMap Map<String, Object> headers,
                                    @Nonnull @QueryMap Map<String, Object> queryMap,
                                    @Nonnull @Param("contentType") String contentType,
                                    @Nonnull byte[] fileContent);
}
