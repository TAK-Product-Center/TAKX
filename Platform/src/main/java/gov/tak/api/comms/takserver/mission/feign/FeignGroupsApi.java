package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Headers;
import feign.QueryMap;
import feign.RequestLine;
import gov.tak.api.comms.takserver.mission.call.groups.GetAllGroups;
import gov.tak.api.comms.takserver.mission.call.groups.SetActiveGroups;
import gov.tak.api.comms.takserver.mission.data.Group;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * REST interface modeling the Groups API from TAK Server.
 *
 * @since 5.0
 */
public interface FeignGroupsApi
{
    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return The available groups on the server.
     *
     * @see GetAllGroups
     */
    @Nonnull
    @RequestLine("GET /groups/all")
    List<Group> getAllGroups(@Nonnull @HeaderMap Map<String, Object> headers,
                             @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param groups Groups to self-assign.
     *
     * @see SetActiveGroups
     */
    @RequestLine("PUT /groups/active")
    @Headers("Content-Type: application/json")
    void setActiveGroups(@Nonnull @HeaderMap Map<String, Object> headers,
                         @Nonnull @QueryMap Map<String, Object> queryMap,
                         @Nonnull List<Group> groups);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param groups Bits of groups to self-assign. See {@link Group#bitpos()}.
     *
     * @see SetActiveGroups
     */
    @RequestLine("PUT /groups/activebits")
    @Headers("Content-Type: application/json")
    void setActiveGroups(@Nonnull @HeaderMap Map<String, Object> headers,
                         @Nonnull @QueryMap Map<String, Object> queryMap,
                         @Nonnull int[] groups);

    /**
     * This endpoint is effectively used to check if groups/channels are supported in a given server.
     *
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return {@code true} indicates the server actively uses groups.
     * {@code false} indicates the server does not use groups.
     */
    @RequestLine("GET /groups/groupCacheEnabled")
    boolean isGroupCacheEnabled(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap);
}
