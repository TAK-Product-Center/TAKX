package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IDecodingOptions;
import gov.takx.api.media.stream.IStreamConfiguration;
import gov.takx.api.media.stream.VideoStreamConfiguration;
import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaFrame;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.swscale.SwsContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_image_fill_arrays;
import static org.bytedeco.ffmpeg.global.avutil.av_image_get_buffer_size;
import static org.bytedeco.ffmpeg.global.avutil.av_malloc;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BICUBIC;
import static org.bytedeco.ffmpeg.global.swscale.SWS_BILINEAR;
import static org.bytedeco.ffmpeg.global.swscale.sws_freeContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_getCachedContext;
import static org.bytedeco.ffmpeg.global.swscale.sws_scale;

/**
 * Decoder for video streams
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class VideoDecoder extends ADecoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final FrameConverter frameConverter = new FrameConverter();
    private final int nativeWidth;
    private final int nativeHeight;
    private final int nativePixFormat;
    private final boolean videoValid;

    private AVFrame bgrFrame;// frame decoded to BGR24 pixel format
    private BytePointer rgbFrameBufferPointer;
    private SwsContext swsContext;

    private int interpolator = SWS_BILINEAR;

    /**
     * @param stream Stream we are decoding from
     * @param options Options controlling our decoding
     */
    public VideoDecoder(@Nonnull AVStream stream, @Nonnull IDecodingOptions options)
    {
        super(stream);

        if (super.isValid())
        {
            setDecodingEnabled(options.isDecodeVideo());

            nativeWidth = codecContext.width();
            nativeHeight = codecContext.height();
            nativePixFormat = codecContext.pix_fmt();
            videoValid = (nativeWidth > 0 && nativeHeight > 0 && nativePixFormat >= 0);

            if (videoValid)
            {
                bgrFrame = av_frame_alloc();
                setSizes(nativeWidth, nativeHeight);
            }
        } else
        {
            videoValid = false;
            nativeWidth = 0;
            nativeHeight = 0;
            nativePixFormat = -1;
        }
    }

    @Override
    public boolean isValid()
    {
        return super.isValid() && videoValid;
    }

    /**
     * Set image dimensions for rbgFrame, the AVFrame to decode into
     *
     * @param width  Frame width
     * @param height Frame height
     */
    private void setSizes(int width, int height)
    {
        int origWidth = bgrFrame.width();
        int origHeight = bgrFrame.height();

        bgrFrame.width(width);
        bgrFrame.height(height);
        bgrFrame.format(AV_PIX_FMT_BGR24);

        // Create the context for converting from native codec pixel format to BGR24
        swsContext = sws_getCachedContext(swsContext,
                nativeWidth, nativeHeight, codecContext.pix_fmt(),
                bgrFrame.width(), bgrFrame.height(), bgrFrame.format(),
                interpolator, null, null, (DoublePointer) null);

        // Reallocate only if necessary - we only grow the buffer to prevent unnecessary allocation/deallocation
        if (width > origWidth || height > origHeight)
        {
            // Allocate space for the frame holding the fully decoded BGR24 image
            int size = av_image_get_buffer_size(bgrFrame.format(), bgrFrame.width(), bgrFrame.height(), 32);
            rgbFrameBufferPointer = new BytePointer(av_malloc(size)).capacity(size);
        }

        int ret = av_image_fill_arrays(new PointerPointer<AVFrame>(bgrFrame), bgrFrame.linesize(),
                rgbFrameBufferPointer,
                bgrFrame.format(), bgrFrame.width(), bgrFrame.height(), 32);

        if (ret < 0)
        {
            logger.error("Could not allocate buffers for RGB frame {}", FfmpegUtils.formatError(ret));
            av_frame_free(bgrFrame);
            // TODO: throw an exception?
        }
    }

    /**
     * Set up the scaling context to maximally fill the available screen space.
     *
     * @param imageOutputSize Target image size
     */
    private void resizeScalingContext(@Nonnull Dimension imageOutputSize)
    {
        final int availableWidth = imageOutputSize.width;
        final int availableHeight = imageOutputSize.height;

        // Recommended approach to scaling is to use bicubic interpolation when enlarging an image,
        // bilinear when shrinking it.
        if (availableWidth > nativeWidth && availableHeight > nativeHeight)
        {
            interpolator = SWS_BICUBIC;
        } else
        {
            interpolator = SWS_BILINEAR;
        }

        // Find the largest size, preserving aspect ratio that fits in the space
        double nativeRatio = nativeWidth / (double) nativeHeight;
        double availableRatio = availableWidth / (double) availableHeight;

        if (availableRatio > nativeRatio)
        {
            // Too much available width; letterboxing will be on the sides; use all available height
            setSizes((int) (availableHeight * (nativeRatio)), availableHeight);
        } else
        {
            // Too much available height; letterboxing will be on the top and bottom; use all available width
            setSizes(availableWidth, (int) (availableWidth * (1 / nativeRatio)));
        }
    }

    @Override
    public Optional<MediaFrame> convert(@Nonnull AVFrame frame)
    {
        if (!isValid()) return Optional.empty();

        // Create the context for converting from native codec pixel format to BGR24
        swsContext = sws_getCachedContext(swsContext,
                nativeWidth, nativeHeight, nativePixFormat,
                bgrFrame.width(), bgrFrame.height(), bgrFrame.format(),
                interpolator, null, null, (DoublePointer) null);

        // Convert image from native pixel format to BGR24
        sws_scale(swsContext,
                new PointerPointer<AVFrame>(frame), frame.linesize(), 0, nativeHeight,
                bgrFrame.data(), bgrFrame.linesize());

        BufferedImage image = frameConverter.convert(bgrFrame);
        double pts = bestEffortMediaFramePts(stream, frame);
        return Optional.of(new MediaFrame(pts, new MediaFrame.VideoFrame(deepCopy(image))));
    }

    @Override
    public void configure(@Nonnull IStreamConfiguration config)
    {
        if (config instanceof VideoStreamConfiguration videConfig) resizeScalingContext(videConfig.imageOutputSize());
    }

    private static BufferedImage deepCopy(BufferedImage bi)
    {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    @Override
    public void releaseResources()
    {
        super.releaseResources();
        sws_freeContext(swsContext);
        if (bgrFrame != null) av_frame_free(bgrFrame);
    }
}
