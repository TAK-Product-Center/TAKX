package gov.takx.core.media.writer;

import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaException;
import gov.takx.core.media.Utilities;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Objects;

import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_guess_format;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_close;
import static org.bytedeco.ffmpeg.global.avformat.avio_open2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

/**
 * Writes media packets to a file. Tracks the number of frames written to the video stream allowing the caller
 * to approximate the time span of a chunk, and start a new one when desired.
 *
 * @since 5.1.0
 */
public class ChunkWriter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // First guess 24 fps, for 60 seconds.
    private static final int DEFAULT_MAX_PACKETS = 24 * 60;
    private final AMediaStream[] streams;

    private AVFormatContext outputFormatContext;
    private AMediaStream videoStream;
    private int maxPackets = DEFAULT_MAX_PACKETS;

    /**
     * Open the specified chunk file for output.
     *
     * @param inputFormatContext Input format context
     * @param filename           Filename of the chunk file
     */
    public ChunkWriter(@Nonnull AVFormatContext inputFormatContext, @Nonnull String filename) throws IOException, MediaException
    {
        // Set up the output format
        AVOutputFormat outputFormat = av_guess_format("mpegts", null, null);
        outputFormatContext = new AVFormatContext(null);
        int ret;
        if ((ret = avformat_alloc_output_context2(outputFormatContext, outputFormat, (String) null, null)) < 0)
        {
            throw new IOException("Could not allocate format context: " + FfmpegUtils.formatError(ret));
        }
        // Open the file
        AVIOContext ioContext = new AVIOContext(null);
        if ((ret = avio_open2(ioContext, filename, AVIO_FLAG_WRITE, null, null)) < 0)
        {
            throw new IOException(String.format("Error opening %s: %s.", filename, FfmpegUtils.formatError(ret)));
        }
        outputFormatContext.pb(ioContext);

        final int nStreams = inputFormatContext.nb_streams();
        streams = new AMediaStream[nStreams];
        // Add streams to the output
        int streamsFound = 0;
        for (int inputStreamIndex = 0; inputStreamIndex < nStreams; inputStreamIndex++)
        {
            AVStream inputStream = inputFormatContext.streams(inputStreamIndex);
            int avMediaType = inputStream.codecpar().codec_type();
            if (logger.isDebugEnabled())
            {
                logger.debug("Found {} stream {}.", Utilities.getMediaTypeString(avMediaType), inputStreamIndex);
            }
            if (avMediaType == AVMEDIA_TYPE_VIDEO)
            {
                streamsFound++;
                streams[inputStreamIndex] = new VideoStream(inputFormatContext, outputFormatContext, outputFormat, inputStream);
                if (videoStream == null)
                {
                    // We will use this stream to determine the chunk size.
                    videoStream = streams[inputStreamIndex];
                }
            } else if (avMediaType == AVMEDIA_TYPE_AUDIO)
            {
                streamsFound++;
                streams[inputStreamIndex] = new AudioStream(outputFormatContext, inputStream);
            }
        }

        // If we found multiple streams write them interleaved.
        setInterleaved(streamsFound > 1);

        // Write header
        avformat_write_header(outputFormatContext, (AVDictionary) null);
        if (logger.isDebugEnabled())
        {
            av_dump_format(outputFormatContext, 0, outputFormatContext.url().getString(), 1);

            // Do this after the write header op.
            for (int inputStreamIndex = 0; inputStreamIndex < nStreams; inputStreamIndex++)
            {
                logger.debug("Output Stream {} type {}.", inputStreamIndex, Utilities.getMediaTypeString(outputFormatContext.streams(inputStreamIndex).codecpar().codec_type()));
                logger.debug("Output Stream {} time base {}.", inputStreamIndex, outputFormatContext.streams(inputStreamIndex).time_base().num() + "/" + outputFormatContext.streams(inputStreamIndex).time_base().den());
            }
        }
    }

    /**
     * Get the number of packets to process before setting finished.
     *
     * @return The number of packets expected in a chunk.
     * @see #isChunkFinished()
     */
    public int getMaxPackets()
    {
        return maxPackets;
    }

    /**
     * Set the number of packets processed before the chunk is considered finished.
     *
     * @param maxPackets The number of packets to be processed before the chunk is finished.
     * @see #isChunkFinished()
     */
    public void setMaxPackets(int maxPackets)
    {
        this.maxPackets = maxPackets;
    }

    /**
     * Set whether we write interleaved packets, or write packets.
     *
     * @param interleaved {@code true} when we invoke write interleaved frame, {@code false} if we invoke write frame.
     * @see AMediaStream#writePacket(AVPacket)
     */
    private void setInterleaved(boolean interleaved)
    {
        Arrays.stream(streams).filter(Objects::nonNull).forEach(stream -> stream.setInterleaved(interleaved));
    }

    /**
     * Feed a packet into the processing pipeline. Decodes, resamples, and writes the packet to the file.
     *
     * @param packet The packet to write.
     */
    public void writePacket(@Nonnull AVPacket packet) throws MediaException
    {
        AMediaStream stream = streams[packet.stream_index()];
        if (stream != null) stream.processPacket(packet);
    }

    /**
     * Flush any remaining packets from the encoder to the output stream.
     */
    private void flushEncoders()
    {
        for (AMediaStream stream : streams)
        {
            if (stream != null) stream.flush();
        }
    }

    /**
     * Release contexts and other resources allocated for each stream.
     */
    private void freeStreamContexts()
    {
        for (AMediaStream stream : streams)
        {
            if (stream != null) stream.releaseResources();
        }
    }

    /**
     * Close the chunk file.
     */
    public void close()
    {
        flushEncoders();
        av_write_trailer(outputFormatContext);

        freeStreamContexts();
        if (outputFormatContext != null)
        {
            if (outputFormatContext.pb() != null) avio_close(outputFormatContext.pb());

            avformat_free_context(outputFormatContext);
            outputFormatContext = null;
        }
    }

    /**
     * Check if the current chunk should be closed.
     *
     * @return {@code true} if the current chunk should be closed.
     */
    public boolean isChunkFinished()
    {
        return videoStream.getFramesWritten() >= maxPackets;
    }
}
