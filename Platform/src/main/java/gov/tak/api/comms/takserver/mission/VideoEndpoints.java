package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.mission.call.video.CreateVideoCollection;
import gov.tak.api.comms.takserver.mission.call.video.GetVideoCollection;
import gov.tak.api.comms.takserver.mission.call.video.GetVideoCollections;
import gov.tak.api.comms.takserver.mission.call.video.RemoveVideoCollection;
import gov.tak.api.comms.takserver.mission.call.video.UpdateVideoCollection;
import gov.tak.api.comms.takserver.mission.data.VideoConnections;
import gov.tak.api.comms.takserver.mission.feign.FeignVideoApi;
import jakarta.annotation.Nonnull;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignVideoApi}.
 *
 * @since 5.0
 */
public class VideoEndpoints
{
    private final FeignVideoApi videoApi;

    public VideoEndpoints(@Nonnull FeignVideoApi videoApi)
    {
        this.videoApi = videoApi;
    }

    public CreateVideoCollection createVideoCollection(VideoConnections connections)
    {
        return new CreateVideoCollection(videoApi, connections);
    }

    public UpdateVideoCollection updateVideoCollection(VideoConnections connections)
    {
        return new UpdateVideoCollection(videoApi, connections);
    }

    public GetVideoCollections getVideoCollections()
    {
        return new GetVideoCollections(videoApi);
    }

    public GetVideoCollection getVideoCollection(String uid)
    {
        return new GetVideoCollection(videoApi, uid);
    }

    public RemoveVideoCollection removeVideoCollection(String uid)
    {
        return new RemoveVideoCollection(videoApi, uid);
    }
}
