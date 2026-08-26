package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

import java.util.Optional;

/**
 * Represents a discrete frame of media
 *
 * @since 2.8
 */
@Experimental
public interface IMediaFrame
{
    /**
     * Access the presentation timestamp (PTS)
     *
     * @return The PTS
     */
    double getPts();

    /**
     * Access the video frame
     *
     * @return Get the video frame, if present
     */
    Optional<IVideoFrame> getVideoFrame();

    /**
     * Access the audio frame
     *
     * @return The audio frame, if present
     */
    Optional<IAudioFrame> getAudioFrame();

    /**
     * Access the data frame
     *
     * @return The data frame, if present
     */
    Optional<IDataFrame> getDataFrame();
}
