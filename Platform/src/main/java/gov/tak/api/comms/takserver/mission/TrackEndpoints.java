package gov.tak.api.comms.takserver.mission;

import com.google.common.io.Files;
import gov.tak.api.comms.takserver.mission.call.mission.GetTrackKml;
import gov.tak.api.comms.takserver.mission.call.mission.PublishTrackKml;
import gov.tak.api.comms.takserver.mission.feign.FeignTracksApi;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.io.IOException;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignTracksApi}.
 *
 * @since 5.4
 */
public class TrackEndpoints
{
    private final FeignTracksApi tracksApi;

    /**
     * @param tracksApi The API to wrap
     */
    public TrackEndpoints(@Nonnull FeignTracksApi tracksApi)
    {
        this.tracksApi = tracksApi;
    }

    /**
     * @param markerUid The marker's UID
     * @return The endpoint caller that will return a KML track file
     */
    public GetTrackKml getTracks(@Nonnull String markerUid)
    {
        return new GetTrackKml(tracksApi, markerUid);
    }

    /**
     * @param kmlFile The {@link gov.takx.api.breadcrumb.ITrack} KML file
     * @return The endpoint caller that will post the KML track file
     * @throws IOException if there are errors reading the provided track file
     * @since 5.5
     */
    public PublishTrackKml publishTrack(@Nonnull File kmlFile) throws IOException
    {
        return new PublishTrackKml(tracksApi, Files.toByteArray(kmlFile));
    }
}
