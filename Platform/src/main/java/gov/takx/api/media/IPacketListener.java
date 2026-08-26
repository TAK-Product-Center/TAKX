package gov.takx.api.media;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;

/**
 * Interface to listen for received {@link AVPacket packets}.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public interface IPacketListener
{
    /**
     * Callback indicating a new {@link AVPacket} has been received.
     *
     * @param context The {@code AVFormatContext} we are reading from.
     * @param packet The packet we read.
     */
    void onPacket(@Nonnull AVFormatContext context, @Nonnull AVPacket packet);

    /**
     * No more packets. Either explicit user action, or input stream is exhausted.
     */
    void stop();
}
