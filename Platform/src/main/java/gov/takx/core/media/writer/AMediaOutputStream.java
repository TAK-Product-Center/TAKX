package gov.takx.core.media.writer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.MediaException;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.Utilities;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.global.avformat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;

/**
 * Abstract superclass for capturing or processing media streams.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public abstract class AMediaOutputStream implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final int mediaType;
    private final AVFormatContext outputFormatContext;

    protected AVStream inputStream;
    protected AVStream outputStream;
    protected int outputStreamIndex; // The index into the output streams
    private boolean interleaved;
    private long packetsWritten;

    /**
     * Construct a media stream of the given media type
     *
     * @param mediaType           Tye media type for this stream.
     * @param outputFormatContext The overall ffmpeg output context
     * @param inputStream         The input stream we are mapping to our output stream
     */
    protected AMediaOutputStream(int mediaType, @Nonnull AVFormatContext outputFormatContext, @Nonnull AVStream inputStream)
    {
        this.mediaType = mediaType;
        this.outputFormatContext = outputFormatContext;
        this.inputStream = inputStream;
    }

    /**
     * Open the output {@link AVStream} using the given context and encoder.
     *
     * @param outputFormatContext Output context
     * @param encoder Encoder to use, {@code null} if none
     */
    protected void openOutputStream(@Nonnull AVFormatContext outputFormatContext, @Nullable AVCodec encoder) throws MediaException
    {
        final AVStream stream = avformat_new_stream(outputFormatContext, encoder);
        if (stream == null) throw new MediaException("Unable to allocate " + getMediaTypeString() + " output stream.");

        outputStream = stream;
        outputStreamIndex = stream.index();
    }

    /**
     * @return The {@code AVMediaType} for this stream.
     */
    public int getMediaType()
    {
        return mediaType;
    }

    /**
     * @return Display string for our media type
     */
    public String getMediaTypeString()
    {
        return Utilities.getMediaTypeString(getMediaType());
    }

    /**
     * Set whether we write interleaved packets, or not.
     *
     * @param interleaved {@code true} if we invoke write interleaved frame, {@code false} if we invoke write frame
     * @see #writePacket(AVPacket)
     */
    void setInterleaved(boolean interleaved)
    {
        this.interleaved = interleaved;
    }

    /**
     * Get whether we write interleaved packets, or write packets.
     *
     * @return {@code true} if we invoke {@code av_interleaved_write_frame}, {@code false} if we invoke {@code av_write_frame}.
     * @see #writePacket(AVPacket)
     */
    public boolean isInterleaved()
    {
        return interleaved;
    }

    /**
     * @return The number of packets written by this stream
     */
    public long getPacketsWritten()
    {
        return packetsWritten;
    }

    /**
     * Write and un-reference an encoded packet to the output context.
     *
     * @param encodedPacket The encoded packet. The packet will be unreferenced before this method returns.
     * @return An int giving the return value from the last invoked ffmpeg method.
     */
    protected int writePacket(@Nonnull AVPacket encodedPacket)
    {
        packetsWritten++;
        int ret;
        if (isInterleaved())
        {
            if ((ret = avformat.av_interleaved_write_frame(outputFormatContext, encodedPacket)) < 0)
            {
                logger.error("av_interleaved_write_frame() failed: {}", FfmpegUtils.formatError(ret));
            }
        } else
        {
            if ((ret = avformat.av_write_frame(outputFormatContext, encodedPacket)) < 0)
            {
                logger.error("av_write_frame() failed: {}", FfmpegUtils.formatError(ret));
            }

            // only free this in the non interleaved case, in the interleaved case it is freed automatically.
            av_packet_unref(encodedPacket);
        }
        return ret;
    }

    /**
     * Feed a packet for processing.
     *
     * @param packet The packet to process, for example it might decode, resample, then encode
     * @return An int with the status of the last ffmpeg operation.
     * @throws MediaException On a significant error.
     */
    @SuppressWarnings("UnusedReturnValue")
    public abstract int processPacket(@Nonnull AVPacket packet) throws MediaException;

    /**
     * Flush the encoder if it has retained frames. Subclasses that are capable of flushing their stream should
     * override this method.
     */
    public void flush()
    {
    }

    /**
     * Each stream is expected to implement a version of this method that releases any needed resources.
     */
    @Override
    public void releaseResources()
    {
    }
}
