package gov.tak.api.comms.takserver.mission.call.groups;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.feign.FeignGroupsApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to check if groups are enabled on a server.
 *
 * @since 5.0
 */
public class GetAreGroupsSupported extends AbstractEndpointCaller<Boolean>
{
    /**
     * @param api API reference.
     */
    public GetAreGroupsSupported(@Nonnull FeignGroupsApi api)
    {
        super(api::isGroupCacheEnabled);
    }
}
