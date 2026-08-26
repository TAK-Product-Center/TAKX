package gov.takx.api.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.core.media.MediaFrame;
import jakarta.annotation.Nonnull;

/**
 * Interface to listen for newly decoded frames
 *
 * @since 2.8 (moved to Platform in 5.8 and renamed)
 */
@IncubatingApi(since = "5.8")
public interface IMediaProducerListener
{
    /**
     * Callback indicating a new {@link MediaFrame} has been read and decoded.
     *
     * @param mediaFrame The media frame
     */
    void onMediaFrame(@Nonnull MediaFrame mediaFrame);
}
