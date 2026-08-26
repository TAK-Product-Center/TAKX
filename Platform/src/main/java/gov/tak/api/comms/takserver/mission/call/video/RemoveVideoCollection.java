package gov.tak.api.comms.takserver.mission.call.video;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to remove a video collection.
 * @since 5.0
 */
public class RemoveVideoCollection extends AbstractEndpointCaller<Void>
{
    public RemoveVideoCollection(@Nonnull FeignVideoApi api,
                                 @Nonnull String uid)
    {
        super(devoid((headers, params) -> api.deleteVideoConnection(headers, params, uid)));
    }
}
