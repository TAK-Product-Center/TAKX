package gov.takx.api.media;

import jakarta.annotation.Nullable;

/**
 * @since 2.15
 */
public interface IPlayable
{
    /**
     * Sets the duration of the media in case the duration can not be calculated
     *
     * @param duration The duration of the media, or {@code null} if the media is a stream
     */
    void setDuration(@Nullable Long duration);

    /**
     * Get the duration, in seconds, of the IMedia
     *
     * @return Duration, in seconds, or {@code null} if the media is a stream or duration could not be analyzed
     */
    @Nullable
    Long getDuration();

    /**
     * Set the location for a capture of this media. This will be a copy of the media in whole or in part.
     * Especially targeted to making a local copy of live media. A null value removes the association between
     * the media and the on disk copy, for example, when it is deleted.
     *
     * @param captureLocation The path to the captured copy of this media.
     * @since 5.1.0
     */
    void setCaptureLocation(@Nullable String captureLocation);

    /**
     * Get the location for a capture of this media. This will be a copy of the media in whole or in part.
     * Especially targeted to making a local copy of live media.
     *
     * @return The path to the captured copy of this media, or null if none.
     * @since 5.1.0
     */
    @Nullable
    String getCaptureLocation();
}
