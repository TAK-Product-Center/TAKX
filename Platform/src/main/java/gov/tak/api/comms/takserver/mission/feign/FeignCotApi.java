package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.Response;
import gov.tak.api.comms.takserver.mission.call.cot.GetCotItem;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * REST interface modeling the CoT API from TAK Server.
 *
 * @since 5.0
 */
public interface FeignCotApi
{
    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param uid UID of the map item to get CoT XML of.
     *
     * @return CoT representation of map item.
     *
     * @see GetCotItem
     */
    @Nonnull
    @RequestLine("GET /cot/xml/{uid}")
    Response getCotItem(@Nonnull @HeaderMap Map<String, Object> headers,
                        @Nonnull @QueryMap Map<String, Object> queryMap,
                        @Nonnull @Param("uid") String uid);
}
