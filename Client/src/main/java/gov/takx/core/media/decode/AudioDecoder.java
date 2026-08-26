package gov.takx.core.media.decode;

import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaFrame;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swresample.SwrContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;

import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.AV_ROUND_UP;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_S16;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_get_bytes_per_sample;
import static org.bytedeco.ffmpeg.global.avutil.av_get_default_channel_layout;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_rnd;
import static org.bytedeco.ffmpeg.global.avutil.av_samples_alloc;
import static org.bytedeco.ffmpeg.global.swresample.swr_alloc_set_opts;
import static org.bytedeco.ffmpeg.global.swresample.swr_convert;
import static org.bytedeco.ffmpeg.global.swresample.swr_free;
import static org.bytedeco.ffmpeg.global.swresample.swr_get_delay;
import static org.bytedeco.ffmpeg.global.swresample.swr_init;

/**
 * Decoder for audio streams
 *
 * @since 2.8
 */
public class AudioDecoder extends Decoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SwrContext swrContext;
    private AudioFormat audioFormat;
    private AVFrame audioOut;

    private final int dstNumChannels = 2;
    private final int dstSampleFormat = AV_SAMPLE_FMT_S16;
    private final int dstSampleRate = 44100;
    private final int dstBytesPerSample = av_get_bytes_per_sample(dstSampleFormat);
    private int dstNumSamples = 4096;

    private final int srcSampleRate;

    public AudioDecoder(AVStream stream)
    {
        super(stream);
        audioOut = av_frame_alloc();

        // Needs to match the dst* fields
        audioFormat = new AudioFormat(dstSampleRate, 16, dstNumChannels, true,
                ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));

        int srcChannelLayout = (int) av_get_default_channel_layout(codecContext.channels());
        int srcSampleFormat = codecContext.sample_fmt();
        srcSampleRate = codecContext.sample_rate();

        // Need to specify the layout to the codec context
        codecContext.channel_layout(srcChannelLayout);

        // And set up the output frame to have the right parameters
        int dstChannelLayout = (int) av_get_default_channel_layout(dstNumChannels);
        audioOut.channel_layout(dstChannelLayout);
        audioOut.sample_rate(dstSampleRate);
        audioOut.format(dstSampleFormat);

        // Create and initialize the context to resample the audio
        swrContext = swr_alloc_set_opts(null,
                dstChannelLayout, dstSampleFormat, dstSampleRate,
                srcChannelLayout, srcSampleFormat, srcSampleRate,
                0, null);

        int ret;
        if ((ret = swr_init(swrContext)) < 0)
        {
            logger.error("Couldn't init resampling context {}", FfmpegUtils.formatError(ret));
            throw new RuntimeException("Couldn't init resampling context " + FfmpegUtils.formatError(ret));
        }

        if ((ret = av_samples_alloc(audioOut.extended_data(), null, dstNumChannels, dstNumSamples, dstSampleFormat, 0)) < 1)
        {
            logger.error("Failed av_samples_alloc {}", FfmpegUtils.formatError(ret));
            throw new RuntimeException("Failed av_samples_alloc " + FfmpegUtils.formatError(ret));
        }
    }

    @Override
    public Optional<MediaFrame> convert(AVFrame frame)
    {
        long delay = swr_get_delay(swrContext, srcSampleRate);
        int inSamples = frame.nb_samples();
        int outSamples = (int) av_rescale_rnd(delay + inSamples, dstSampleRate, srcSampleRate, AV_ROUND_UP);

        // Ensure our output buffer is big enough
        if (outSamples > dstNumSamples)
        {
            logger.debug("Required samples is greater than current buffer {} > {}", outSamples, dstNumSamples);

            // TODO: free audioOut.extended_data()?

            int ret;
            if ((ret = av_samples_alloc(audioOut.extended_data(), null, dstNumChannels, outSamples, dstSampleFormat, 0)) < 1)
            {
                logger.error("Failed av_samples_alloc {}", FfmpegUtils.formatError(ret));
                return Optional.empty();
            }
            dstNumSamples = outSamples;
        }

        // Convert to our desired audio
        int dstSamplesPerChannel;
        if ((dstSamplesPerChannel = swr_convert(swrContext, audioOut.extended_data(), outSamples, frame.extended_data(), inSamples)) < 0)
        {
            logger.error("Failed swr_convert {}", FfmpegUtils.formatError(dstSamplesPerChannel));
            return Optional.empty();
        }

        // Create the MediaFrame for distribution
        byte[] data = new byte[dstSamplesPerChannel * dstNumChannels * dstBytesPerSample];
        audioOut.extended_data(0).get(data);
        double pts = bestEffortMediaFramePts(stream, frame);
        MediaFrame mediaFrame = new MediaFrame(pts);
        mediaFrame.setAudioFrame(new MediaFrame.AudioFrame(audioFormat, data));
        return Optional.of(mediaFrame);
    }

    @Override
    public void releaseResources()
    {
        super.releaseResources();
        swr_free(swrContext);
    }
}
