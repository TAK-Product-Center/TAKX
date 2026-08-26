package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.QueryMap;
import feign.RequestLine;
import gov.tak.api.comms.takserver.mission.call.contacts.GetClientEndpoints;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * REST interface modeling the Contact-Manager API from TAK Server.
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface FeignContactManagerApi
{
    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return List of all connected clients.
     *
     * @see GetClientEndpoints
     */
    @Nonnull
    @RequestLine("GET /clientEndPoints")
    List<ClientEndpoint> getClientEndpoints(@Nonnull @HeaderMap Map<String, Object> headers,
                                            @Nonnull @QueryMap Map<String, Object> queryMap);
}
