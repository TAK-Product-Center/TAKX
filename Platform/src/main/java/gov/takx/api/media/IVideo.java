package gov.takx.api.media;

import gov.tak.api.video.ConnectionEntry;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nullable;

/**
 * Reference to a video file or stream
 *
 * @since 2.15
 */
@NoPluginImplementations
public interface IVideo extends IMedia, IPlayable
{
    /**
     * @return The {@link ConnectionEntry} associated with this media, if any
     * @since 5.3
     */
    @Nullable
    ConnectionEntry getConnectionEntry();
}
