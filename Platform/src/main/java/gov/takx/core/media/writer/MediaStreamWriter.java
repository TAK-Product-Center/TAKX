package gov.takx.core.media.writer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.MediaException;
import gov.takx.core.media.FfmpegUtils;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.bytedeco.ffmpeg.global.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.av_guess_format;
import static org.bytedeco.ffmpeg.global.avformat.av_write_trailer;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_write_header;
import static org.bytedeco.ffmpeg.global.avformat.avio_close;
import static org.bytedeco.ffmpeg.global.avformat.avio_open2;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

/**
 * Writes media packets to a file, tracking the number of packets that have been written.  This allows a caller to
 * determine if/when a new file should be started.  If the input contains more than one stream, the output packets will
 * be interleaved.
 *
 * @implNote The file is written in MPEG-TS format.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class MediaStreamWriter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AMediaOutputStream[] streams;
    private AVFormatContext outputFormatContext = new AVFormatContext(null);
    private AMediaOutputStream videoStream;

    /**
     * Construct default writer, using MPEG-TS output format.
     *
     * @param inputFormatContext Input format context
     * @param outputFile Path of file to write to
     */
    public MediaStreamWriter(@Nonnull AVFormatContext inputFormatContext, @Nonnull Path outputFile) throws IOException, MediaException
    {
        this(inputFormatContext, av_guess_format("mpegts", null, null), outputFile);
    }

    /**
     * Constructor to write using the given output format.
     *
     * @param inputFormatContext Input format context
     * @param outputFormat Format to use for writing packets
     * @param outputFile Path of file to write to
     */
    public MediaStreamWriter(@Nonnull AVFormatContext inputFormatContext, @Nonnull AVOutputFormat outputFormat, @Nonnull Path outputFile) throws IOException, MediaException
    {
        // Set up the output context
        int ret = avformat_alloc_output_context2(outputFormatContext, outputFormat, (String) null, null);
        if (ret < 0) throw new IOException("Could not allocate format context: " + FfmpegUtils.formatError(ret));

        // Open the file
        var ioContext = new AVIOContext(null);
        ret = avio_open2(ioContext, outputFile.toString(), AVIO_FLAG_WRITE, null, null);
        if (ret < 0) throw new IOException(String.format("Error opening %s: %s.", outputFile, FfmpegUtils.formatError(ret)));

        outputFormatContext.pb(ioContext);

        // Add streams to the output
        final int nStreams = inputFormatContext.nb_streams();
        streams = new AMediaOutputStream[nStreams];
        int streamsFound = 0;

        for (int inputStreamIndex = 0; inputStreamIndex < nStreams; inputStreamIndex++)
        {
            AVStream inputStream = inputFormatContext.streams(inputStreamIndex);
            int avMediaType = inputStream.codecpar().codec_type();
            if (logger.isDebugEnabled()) logger.debug("Found {} stream {}.", Utilities.getMediaTypeString(avMediaType), inputStreamIndex);

            if (avMediaType == AVMEDIA_TYPE_VIDEO)
            {
                // Video is processed to convert it to H.265, for better compression
                streamsFound++;
                streams[inputStreamIndex] = new VideoOutputStream(inputFormatContext, outputFormatContext, outputFormat, inputStream);

                // We will use the video stream to determine when to open a new output file chunk.
                if (videoStream == null) videoStream = streams[inputStreamIndex];
            } else
            {
                // All other types (Audio, Data, etc.) just pass through unchanged
                streamsFound++;
                streams[inputStreamIndex] = new PassThroughOutputStream(outputFormatContext, inputStream);
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
     * Set whether we write interleaved packets, or write packets.
     *
     * @param interleaved {@code true} when we invoke write interleaved frame, {@code false} if we invoke write frame.
     * @see AMediaOutputStream#writePacket(AVPacket)
     */
    private void setInterleaved(boolean interleaved)
    {
        validStreams().forEach(stream -> stream.setInterleaved(interleaved));
    }

    /**
     * Feed a packet into the processing pipeline. Decodes, resamples, and writes the packet to the file.
     *
     * @param packet The packet to write.
     */
    public void writePacket(@Nonnull AVPacket packet) throws MediaException
    {
        AMediaOutputStream stream = streams[packet.stream_index()];
        if (stream != null) stream.processPacket(packet);
    }

    /**
     * @return The number of VIDEO packets that have been written to the output file
     */
    public long getPacketsWritten()
    {
        return videoStream.getPacketsWritten();
    }

    /**
     * Flush any remaining packets from the encoder to the output stream.
     */
    private void flushEncoders()
    {
        validStreams().forEach(AMediaOutputStream::flush);
    }

    /**
     * Release contexts and other resources allocated for each stream.
     */
    private void freeStreamContexts()
    {
        validStreams().forEach(AMediaOutputStream::releaseResources);
    }

    /**
     * @return Stream of non-null output streams.
     */
    private Stream<AMediaOutputStream> validStreams()
    {
        return Arrays.stream(streams).filter(Objects::nonNull);
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
}
