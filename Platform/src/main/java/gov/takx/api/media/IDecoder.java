package gov.takx.api.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.stream.IStreamConfiguration;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.media.MediaFrame;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.util.Optional;

/**
 * Common interface for decoders
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public interface IDecoder extends IResourceCleaner
{
    /**
     * @return {@code true} if initialization was successful and this decoder is ready for packets
     */
    boolean isValid();

    /**
     * Set whether this {@link IDecoder} should actually perform the decoding. If set to false then
     * {@link IDecoder#decodeFrame(AVPacket, AVFrame)} will generally return an empty MediaFrame
     * (except for a PTS value).
     *
     * @param enable True to enable decoding
     */
    void setDecodingEnabled(boolean enable);

    /**
     * Decode data from a packet into a ffmpeg frame.
     *
     * @param packet The packet
     * @param frame  The decoded ffmpeg frame
     * @return The MediaFrame; may be empty if more packets are required to create a full frame, or on error
     */
    @Nonnull
    Optional<MediaFrame> decodeFrame(@Nullable AVPacket packet, @Nonnull AVFrame frame) throws DecodingException;

    /**
     * Flush any data queued for decoding
     */
    void flush();

    /**
     * Configure parameters controlling the decoding of media frames.
     *
     * @param configuration Configuration parameters
     */
    default void configure(@Nonnull IStreamConfiguration configuration)
    {
        // default to do nothing
    }
}
