package gov.takx.core.media.writer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.MediaException;
import gov.takx.core.media.FfmpegUtils;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Simplest stream processing, copy packets from the input stream to the output stream unchanged.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class PassThroughOutputStream extends AMediaOutputStream
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AVPacket avPacket = new AVPacket();

    /**
     * Construct a stream drawing content from the given inputStream.
     *
     * @param outputFormatContext The containing output {@code AVFormatContext}.
     * @param inputStream         The input stream we are reading packets from.
     * @throws MediaException On a severe error.
     */
    public PassThroughOutputStream(@Nonnull AVFormatContext outputFormatContext, @Nonnull AVStream inputStream) throws MediaException
    {
        super(inputStream.codecpar().codec_type(), outputFormatContext, inputStream);

        openOutputStream(outputFormatContext, null);

        avcodec_parameters_copy(outputStream.codecpar(), inputStream.codecpar());
        outputStream.time_base(inputStream.time_base());
    }

    @Override
    public int processPacket(@Nonnull AVPacket packet) throws MediaException
    {
        int ret = av_packet_ref(avPacket, packet); // will be un-referenced in writePacket
        if (ret < 0) throw new MediaException(String.format("Error referencing audio packet: %s.", FfmpegUtils.formatError(ret)));

        avPacket.stream_index(outputStreamIndex);
        if (logger.isDebugEnabled())
        {
            logger.debug("Writing {} packet to stream {} with timestamp {}, time {}", getMediaTypeString(),
                    outputStreamIndex, avPacket.pts(), avPacket.pts() * av_q2d(outputStream.time_base()));
        }
        return writePacket(avPacket);
    }
}
