package gov.tak.api.comms.takserver.mission.call.video;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.VideoConnections;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to create a video collection.
 * @since 5.0
 */
public class CreateVideoCollection extends AbstractEndpointCaller<Void>
{
    public CreateVideoCollection(@Nonnull FeignVideoApi api,
                                 @Nonnull VideoConnections connections)
    {
        super(devoid((headers, params) -> api.createVideoConnection(headers, params, connections)));
    }
}
