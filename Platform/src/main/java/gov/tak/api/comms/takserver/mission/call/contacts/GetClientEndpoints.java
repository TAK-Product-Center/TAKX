package gov.tak.api.comms.takserver.mission.call.contacts;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.feign.FeignContactManagerApi;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Endpoint caller to fetch connected clients.
 *
 * @since 3.5
 */
public class GetClientEndpoints extends AbstractEndpointCaller<List<ClientEndpoint>>
{
    /**
     * @param api API reference.
     */
    public GetClientEndpoints(@Nonnull FeignContactManagerApi api)
    {
        super(api::getClientEndpoints);
    }
}
