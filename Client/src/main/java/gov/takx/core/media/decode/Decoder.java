package gov.takx.core.media.decode;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaFrame;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_flush_buffers;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EAGAIN;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Abstract base class for decoders
 *
 * @since 2.8
 */
public abstract class Decoder implements IDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected final AVStream stream;
    protected final boolean isValid;
    protected AVCodecContext codecContext = null;
    private boolean decodingEnabled;

    /**
     * Convert the AVFrame to a MediaFrame (resize, convert pixel format, etc.)
     *
     * @param frame The AVFrame to convert
     * @return The MediaFrame, if any
     */
    protected abstract Optional<MediaFrame> convert(AVFrame frame);

    Decoder(AVStream stream)
    {
        this.stream = stream;
        this.isValid = initialize(stream);
        this.decodingEnabled = true;
    }

    @Override
    public boolean isValid()
    {
        return isValid;
    }

    @Override
    public void setDecodingEnabled(boolean decodingEnabled)
    {
        this.decodingEnabled = decodingEnabled;
    }

    private boolean initialize(AVStream stream)
    {
        AVCodecContext cc = avcodec_alloc_context3(null);
        int ret = -1;
        if ((ret = avcodec_parameters_to_context(cc, stream.codecpar())) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Couldn't create AVCodecContext for stream {} codec: {} error {}", stream.index(), stream.codecpar().codec_tag(), FfmpegUtils.formatError(ret));
            }
            avcodec_free_context(cc);
            return false;
        }

        AVCodec codec = avcodec_find_decoder(stream.codecpar().codec_id());
        if (codec == null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Couldn't find AVCodec for stream {} codec: {} error {}", stream.index(), stream.codecpar().codec_tag(), FfmpegUtils.formatError(ret));
            avcodec_free_context(cc);
            return false;
        }

        AVDictionary options = new AVDictionary(null);
        if ((ret = avcodec_open2(cc, codec, options)) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Couldn't open AVCodec for stream {} codec: {} error {}", stream.index(), stream.codecpar().codec_tag(), FfmpegUtils.formatError(ret));
            }

            avcodec_free_context(cc);
            return false;
        }
        av_dict_free(options);
        codecContext = cc;
        return true;
    }

    @Override
    public Optional<MediaFrame> decodeFrame(AVPacket packet, AVFrame frame) throws DecodingException
    {
        if (!decodingEnabled)
        {
            return packet == null ? Optional.empty() : Optional.of(new MediaFrame(bestEffortMediaFramePts(packet, stream)));
        }

        int ret;
        if ((ret = avcodec_send_packet(codecContext, packet)) < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("avcodec_send_packet error {}", FfmpegUtils.formatError(ret));
            }
            throw new DecodingException("avcodec_send_packet error " + FfmpegUtils.formatError(ret));
        }

        if ((ret = avcodec_receive_frame(codecContext, frame)) < 0)
        {
            if (ret == AVERROR_EAGAIN()
                    || ret == -35 // AVERROR(EDEADLK)
                    || ret == AVERROR_EOF)
            {
                return Optional.empty();
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("avcodec_receive_frame error {}", FfmpegUtils.formatError(ret));
            }
            throw new DecodingException("avcodec_receive_frame error " + FfmpegUtils.formatError(ret));
        }

        return convert(frame);
    }

    @Override
    public void flush()
    {
        logger.debug("Flushing codec buffers for {}", codecContext);
        avcodec_flush_buffers(codecContext);
    }

    /**
     * Get the presentation time stamp (PTS) value for the given AVFrame
     *
     * @param stream The stream
     * @param frame  The AVFrame
     * @return PTS value, or Double.NaN if the AVFrame did not have a PTS value
     */
    protected static double bestEffortMediaFramePts(AVStream stream, AVFrame frame)
    {
        final long tmpPts = frame.best_effort_timestamp();
        final long startTime = stream.start_time() == AV_NOPTS_VALUE ? 0 : stream.start_time();
        return (tmpPts == AV_NOPTS_VALUE) ? Double.NaN : (tmpPts - startTime) * av_q2d(stream.time_base());
    }

    /**
     * Get the presentation time stamp (PTS) value for the given AVPacket
     *
     * @param packet The AVPacket
     * @param stream The stream
     * @return PTS value, or Double.NaN if the AVPacket did not have a PTS value
     */
    private static double bestEffortMediaFramePts(AVPacket packet, AVStream stream)
    {
        long tmpPts = packet.pts();

        // Some screwed-up codecs do not store PTS but use DTS in its place
        if (tmpPts == AV_NOPTS_VALUE)
        {
            if (packet.dts() != AV_NOPTS_VALUE)
            {
                tmpPts = packet.dts();
            }
        }

        final long startTime = stream.start_time() == AV_NOPTS_VALUE ? 0 : stream.start_time();
        return (tmpPts == AV_NOPTS_VALUE) ? Double.NaN : (tmpPts - startTime) * av_q2d(stream.time_base());
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void releaseResources()
    {
        avcodec_free_context(codecContext);
    }
}
