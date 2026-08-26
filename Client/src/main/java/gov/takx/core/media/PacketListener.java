package gov.takx.core.media;

import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;

/**
 * Interface to listen for received AVPackets
 *
 * @since 5.1.0
 */
public interface PacketListener
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
