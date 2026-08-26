package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;

import java.util.Optional;

/**
 * Represents a discrete frame of media.
 *
 * @since 2.8
 */
@Experimental
public interface IMediaFrame
{
    /**
     * @return The presentation timestamp (PTS)
     * @since 5.8
     */
    double getPresentationTimeStamp();

    /**
     * @return Get the video frame, if present
     */
    @Nonnull
    Optional<IVideoFrame> getVideoFrame();

    /**
     * @return The audio frame, if present
     */
    @Nonnull
    Optional<IAudioFrame> getAudioFrame();

    /**
     * @return The data frame, if present
     */
    @Nonnull
    Optional<IDataFrame> getDataFrame();
}
