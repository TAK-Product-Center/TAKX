package gov.takx.core.media.writer;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.MediaException;
import gov.takx.core.media.FfmpegUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVOutputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_CAP_DELAY;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H265;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_rescale_ts;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_alloc_context3;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_close;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_decoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_find_encoder;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_free_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_open2;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_to_context;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_receive_packet;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_frame;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_send_packet;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.ffmpeg.global.avformat.av_guess_frame_rate;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV444P;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;
import static org.bytedeco.ffmpeg.global.avutil.av_freep;
import static org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays;
import static org.bytedeco.ffmpeg.global.avutil.av_image_get_buffer_size;
import static org.bytedeco.ffmpeg.global.avutil.av_malloc;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q_rnd;
import static org.bytedeco.ffmpeg.global.swscale.SWS_POINT;
import static org.bytedeco.ffmpeg.global.swscale.sws_freeContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_getCachedContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;
import static org.bytedeco.ffmpeg.presets.avutil.AVERROR_EAGAIN;

/**
 * Uncompress, resample, then recompress video packets into h.265 format.
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class VideoOutputStream extends AMediaOutputStream
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TARGET_PIX_FMT = AV_PIX_FMT_YUV444P; // Desired pixel format for H265 encoder

    private final AVFrame frame; // The frame we read from the decoder
    private final AVCodecContext decoderContext;
    private final boolean needsResampling;
    private final AVPacket avPacket = new AVPacket();
    private AVFrame resampledVideoFrame;
    private SwsContext swsContext;
    private final AVCodecContext encoderContext;
    private final AVPacket encodedPacket = new AVPacket();
    private final AVPacket encodedPacketRef = new AVPacket();

    /**
     * Construct a video stream to process packets from the given input stream.
     *
     * @param inputFormatContext  The defining context for our input.
     * @param outputFormatContext The defining context for our output.
     * @param outputFormat        The output format, mpegts, mp4, etc.
     * @param inputStream         The source for our packets.
     * @throws MediaException Thrown on an unrecoverable error.
     */
    public VideoOutputStream(@Nonnull AVFormatContext inputFormatContext, @Nonnull AVFormatContext outputFormatContext,
                             @Nonnull AVOutputFormat outputFormat, @Nonnull AVStream inputStream)
            throws MediaException
    {
        super(AVMEDIA_TYPE_VIDEO, outputFormatContext, inputStream);

        AVCodec decoder = avcodec_find_decoder(inputStream.codecpar().codec_id());
        if (decoder == null) throw new MediaException("Failed to find decoder for id " + inputStream.codecpar().codec_id() + ".");

        decoderContext = avcodec_alloc_context3(decoder);
        if (decoderContext == null) throw new MediaException("Failed to allocate video decoder context.");

        int ret = avcodec_parameters_to_context(decoderContext, inputStream.codecpar());
        if (ret < 0) throw new MediaException("Failed to copy video decoder parameters: " + FfmpegUtils.formatError(ret));

        if (logger.isDebugEnabled())
        {
            logger.debug("Video input stream time base {}.", av_q2d(inputStream.time_base()));
            logger.debug("Video decoder time base {}.", av_q2d(decoderContext.time_base()));
        }

        AVRational frameRate = av_guess_frame_rate(inputFormatContext, inputStream, null);
        AVCodec encoder = avcodec_find_encoder(AV_CODEC_ID_H265);
        encoderContext = avcodec_alloc_context3(encoder);
        encoderContext.framerate(frameRate);
        encoderContext.time_base(inputStream.time_base());
        encoderContext.height(decoderContext.height());
        encoderContext.width(decoderContext.width());
        encoderContext.sample_aspect_ratio(decoderContext.sample_aspect_ratio());
        encoderContext.pix_fmt(TARGET_PIX_FMT);
        encoderContext.max_b_frames(0);
        encoderContext.gop_size(30);

        if ((outputFormat.flags() & AVFMT_GLOBALHEADER) == AVFMT_GLOBALHEADER)
        {
            encoderContext.flags(encoderContext.flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
        }

        ret = avcodec_open2(decoderContext, decoder, (AVDictionary) null);
        if (ret < 0) throw new MediaException(String.format("Unable to open decoder: %s.", FfmpegUtils.formatError(ret)));

        final var options = new AVDictionary();
        // for H265 use main444-10, lossless 1 for highest quality compression
        // alternatively for H264 use high444 with crf 0
        // many players do not support this, use ffplay.
        av_dict_set(options, "profile", "main444-10", 0);
        av_dict_set(options, "preset", "medium", 0);
        av_dict_set(options, "lossless", "1", 0);
        ret = avcodec_open2(encoderContext, encoder, options);
        av_dict_free(options);

        if (ret < 0) throw new MediaException(String.format("Unable to open video encoder: %s%n", FfmpegUtils.formatError(ret)));

        openOutputStream(outputFormatContext, encoder);

        if (logger.isDebugEnabled()) logger.debug("Encoder context time base {}.", av_q2d(encoderContext.time_base()));
        frame = av_frame_alloc();

        needsResampling = decoderContext.pix_fmt() != TARGET_PIX_FMT;
        if (needsResampling) configureResamplingContext();
    }

    private void configureResamplingContext() throws MediaException
    {
        int ret;
        int encoderWidth = encoderContext.width();
        int encoderHeight = encoderContext.height();
        swsContext =
                sws_getCachedContext(swsContext,
                        decoderContext.width(), decoderContext.height(), decoderContext.pix_fmt(),
                        encoderWidth, encoderHeight, TARGET_PIX_FMT,
                        SWS_POINT, null, null, (DoublePointer) null);

        // Temporary storage for the converted video frame
        if ((resampledVideoFrame = av_frame_alloc()) == null)
        {
            throw new MediaException("av_frame_alloc() error: Could not allocate temporary video frame.");
        }
        // How many bytes do we need?
        int numBytes = av_image_get_buffer_size(TARGET_PIX_FMT, encoderWidth, encoderHeight, Integer.SIZE);
        final var buffer = new BytePointer(av_malloc(numBytes));

        ret = av_image_fill_arrays(resampledVideoFrame.data(), resampledVideoFrame.linesize(),
                buffer, TARGET_PIX_FMT, encoderWidth, encoderHeight, 32);
        if (ret < 0)
        {
            throw new MediaException(String.format("Failed to allocate resampled video buffers %s.", FfmpegUtils.formatError(ret)));
        }

        resampledVideoFrame.format(TARGET_PIX_FMT);
    }

    /**
     * Resample a frame to the desired color space. If resampled, the original frame is unreferenced.
     *
     * @param frame The frame to be resampled.
     * @return The resampled frame, or the original frame if no resampling is done.
     * @throws MediaException On sampling error.
     */
    private AVFrame resampleFrame(@Nonnull AVFrame frame) throws MediaException
    {
        if (needsResampling)
        {
            // We need to resample to another color space
            int ret = sws_scale(swsContext,          // struct SwsContext* c,
                    new PointerPointer<AVFrame>(frame), // const uint8_t* const srcSlice[],
                    frame.linesize(),                // const int srcStride[],
                    0,                               // int srcSliceY,
                    frame.height(),                  // int srcSliceH,
                    resampledVideoFrame.data(),      // uint8_t* const dst[],
                    resampledVideoFrame.linesize()); // const int dstStride[]);

            resampledVideoFrame.pts(frame.pts());
            resampledVideoFrame.pkt_duration(frame.pkt_duration());
            resampledVideoFrame.repeat_pict(frame.repeat_pict());
            av_frame_unref(frame);

            if (ret < 0) throw new MediaException(String.format("Failed sws_scale %s.", FfmpegUtils.formatError(ret)));

            return resampledVideoFrame;
        }
        return frame;
    }

    @Override
    public int processPacket(@Nonnull AVPacket packet) throws MediaException
    {
        if (logger.isDebugEnabled()) logger.debug("Video packet with pts {}, time {}.", packet.pts(), packet.pts() * av_q2d(inputStream.time_base()));

        int ret = av_packet_ref(avPacket, packet);
        if (ret < 0)
        {
            throw new MediaException(String.format("Error referencing %s packet: %s", getMediaTypeString(), FfmpegUtils.formatError(ret)));
        }

        // The packet can contain anything from part of a frame to multiple frames.
        ret = avcodec_send_packet(decoderContext, avPacket);
        if (ret < 0)
        {
            logger.error("Error decoding video packet: {}", FfmpegUtils.formatError(ret));
            return ret;
        }

        while (true)
        {
            // Try to read a frame from the decoder
            ret = avcodec_receive_frame(decoderContext, frame);
            if (ret == AVERROR_EOF || ret == AVERROR_EAGAIN())
            {
                // no more data available from the decoder at this time, it needs more packets
                break;
            } else if (ret < 0)
            {
                // Possibly a transient error, log it and try the next packet.
                logger.error("Error reading frame from video decoder: {}", FfmpegUtils.formatError(ret));
                break;
            }

            long pts = frame.pts();
            logger.debug("Decoded video packet presentation time stamp {}", pts);

            // A robust a*b/c
            pts = av_rescale_q_rnd(pts, inputStream.time_base(), encoderContext.time_base(), AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
            frame.pts(pts);
            if (logger.isDebugEnabled())
            {
                logger.debug("Adjusted video packet presentation time stamp {}, time {}.", pts, pts * av_q2d(encoderContext.time_base()));
            }

            encodeAndWriteFrame(resampleFrame(frame));
        }
        av_packet_unref(avPacket);
        return ret;
    }

    @Override
    public void flush()
    {
        super.flush();
        if ((encoderContext.codec().capabilities() & AV_CODEC_CAP_DELAY) != 0) encodeAndWriteFrame(null);
    }

    /**
     * Process a packet for a stream. The packet may be {@code null} if we are flushing the end of the stream.
     *
     * @param frame Compress the frame and write it out.
     */
    public void encodeAndWriteFrame(@Nullable AVFrame frame)
    {
        int ret = avcodec_send_frame(encoderContext, frame);
        if (ret < 0)
        {
            // Probably a transient error, log it and get the next frame.
            logger.error("Error encoding frame: {}.", FfmpegUtils.formatError(ret));
            return;
        }

        // Now read any frames generated by the encoder
        while (true)
        {
            ret = avcodec_receive_packet(encoderContext, encodedPacket);
            if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF) return;

            encodedPacket.stream_index(outputStreamIndex);

            av_packet_rescale_ts(encodedPacket,
                    encoderContext.time_base(),
                    outputStream.time_base());

            if (logger.isDebugEnabled())
            {
                logger.debug("Writing packet to stream {} with timestamp {}, time {}", outputStreamIndex,
                        encodedPacket.pts(), encodedPacket.pts() * av_q2d(outputStream.time_base()));
            }

            ret = av_packet_ref(encodedPacketRef, encodedPacket);
            if (ret < 0)
            {
                logger.error("Error copying packet: {}.", FfmpegUtils.formatError(ret));
                return;
            }
            writePacket(encodedPacketRef);
            av_packet_unref(encodedPacket);
        }
    }

    @Override
    public void releaseResources()
    {
        super.releaseResources();
        if (encoderContext != null)
        {
            avcodec_close(encoderContext);
            avcodec_free_context(encoderContext);
        }

        if (decoderContext != null)
        {
            avcodec_close(decoderContext);
            avcodec_free_context(decoderContext);
        }

        if (frame != null)
        {
            av_freep(frame.data());
            av_frame_free(frame);
        }

        if (resampledVideoFrame != null)
        {
            av_freep(resampledVideoFrame.data());
            av_frame_free(resampledVideoFrame);
        }

        if (swsContext != null) sws_freeContext(swsContext);
    }
}
