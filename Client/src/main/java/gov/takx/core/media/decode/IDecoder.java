package gov.takx.core.media.decode;

import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.media.MediaFrame;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.util.Optional;

/**
 * Common interface for decoders
 *
 * @since 2.8
 */
public interface IDecoder extends IResourceCleaner
{
    /**
     * Whether this decoder was initialized properly.
     *
     * @return True if initialization was successful and this decoder is ready for packets
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
     * Decode data from a packet into an ffmpeg frame.
     *
     * @param packet The packet
     * @param frame  The decoded ffmpeg frame
     * @return The MediaFrame; may be {@code null} if more packets are required to create a full frame, or on error
     */
    Optional<MediaFrame> decodeFrame(AVPacket packet, AVFrame frame) throws DecodingException;

    /**
     * Flush any data queued for decoding
     */
    void flush();

    /**
     * Configure parameters controlling the output MediaFrames
     *
     * @param configuration Configuration parameters
     */
    default void configure(HandlerConfiguration configuration)
    {
        // default to do nothing
    }

    @Override
    default void releaseResources()
    {
    }
}
