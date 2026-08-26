package gov.tak.api.comms.takserver.mission.call.video;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.VideoConnections;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to update a video collection.
 * @since 5.0
 */
public class UpdateVideoCollection extends AbstractEndpointCaller<Void>
{
    public UpdateVideoCollection(@Nonnull FeignVideoApi api,
                                 @Nonnull VideoConnections connections)
    {
        super(devoid((headers, params) -> api.updateVideoConnection(headers, params, connections)));
    }
}
