package gov.tak.api.comms.takserver.mission.call.video;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.VideoConnection;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to retrieve a single {@link VideoConnection}.
 * @since 5.0
 */
public class GetVideoCollection extends AbstractEndpointCaller<VideoConnection>
{
    public GetVideoCollection(@Nonnull FeignVideoApi api, @Nonnull String uid)
    {
        super((headers, params) -> api.getVideoCollection(headers, params, uid));
    }
}
