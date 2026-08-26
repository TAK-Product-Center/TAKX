package gov.takx.core.media.writer;

import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaException;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Simplest audio stream processing, copy packets from the input stream to the output stream.
 *
 * @since 5.1.0
 */
public class AudioStream extends AMediaStream
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final AVPacket packetRef;

    /**
     * Construct an audio stream drawing content from the given inputStream.
     *
     * @param outputFormatContext The containing output {@code AVFormatContext}.
     * @param inputStream         The input stream we are reading packets from.
     * @throws MediaException On a severe error.
     */
    public AudioStream(@Nonnull AVFormatContext outputFormatContext, @Nonnull AVStream inputStream)
            throws MediaException
    {
        super(AVMEDIA_TYPE_AUDIO, outputFormatContext, inputStream);

        AVStream outputStream = avformat_new_stream(outputFormatContext, null);

        if (outputStream == null)
        {
            throw new MediaException("Unable to allocate audio output stream.");
        }

        setOutputStream(outputStream);
        avcodec_parameters_copy(outputStream.codecpar(), inputStream.codecpar());
        outputStream.time_base(inputStream.time_base());
        packetRef = new AVPacket();
    }

    @Override
    public int processPacket(@Nonnull AVPacket packet) throws MediaException
    {
        int ret;
        if ((ret = av_packet_ref(packetRef, packet)) < 0)
        {
            throw new MediaException(String.format("Error referencing audio packet: %s.", FfmpegUtils.formatError(ret)));
        }

        packetRef.stream_index(outputStreamIndex);
        if (logger.isDebugEnabled())
        {
            logger.debug("Writing audio packet to stream {} with timestamp {}, time {}",
                    outputStreamIndex, packetRef.pts(), packetRef.pts() * av_q2d(outputStream.time_base()));
        }
        return writePacket(packetRef);
    }
}
