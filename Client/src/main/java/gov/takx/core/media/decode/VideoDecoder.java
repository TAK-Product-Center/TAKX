package gov.takx.core.media.decode;

import gov.takx.core.media.FfmpegUtils;
import gov.takx.core.media.MediaFrame;
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
 * @since 2.8
 */
public class VideoDecoder extends Decoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final FrameConverter frameConverter = new FrameConverter();
    private final int nativeWidth;
    private final int nativeHeight;
    private final int nativePixFormat;
    private final boolean isVideoValid;
    private AVFrame bgrFrame;// frame decoded to BGR24 pixel format
    private BytePointer rgbFrameBufferPointer;
    private SwsContext swsContext = null;

    private int interpolator = SWS_BILINEAR;
    private int streamIndex;

    public VideoDecoder(AVStream stream)
    {
        super(stream);

        isVideoValid = isVideoValid(stream);

        nativeWidth = codecContext.width();
        nativeHeight = codecContext.height();
        nativePixFormat = codecContext.pix_fmt();

        if (isValid())
        {
            bgrFrame = av_frame_alloc();
            this.streamIndex = stream.index();
            setSizes(nativeWidth, nativeHeight);
        }
    }

    private boolean isVideoValid(AVStream stream)
    {
        return (codecContext.width() > 0 && codecContext.height() > 0 && codecContext.pix_fmt() >= 0);
    }

    @Override
    public boolean isValid()
    {
        return isValid && isVideoValid;
    }

    /**
     * Set image dimensions for rbgFrame, the AVFrame to decode into
     *
     * @param width  Frame width
     * @param height Frame height
     */
    private void setSizes(int width, int height)
    {
        if (!isValid())
        {
            logger.warn("Video data invalid");
            return;
        }

        int origWidth = bgrFrame.width();
        int origHeight = bgrFrame.height();

        bgrFrame.width(width);
        bgrFrame.height(height);
        bgrFrame.format(AV_PIX_FMT_BGR24);

        // Create the context for converting from native codec pixel format to BGR24
        swsContext = sws_getCachedContext(swsContext,
                codecContext.width(), codecContext.height(), codecContext.pix_fmt(),
                bgrFrame.width(), bgrFrame.height(), bgrFrame.format(),
                interpolator, null, null, (DoublePointer) null);

        // Reallocate only if necessary - we only grow the buffer to prevent unnecessary allocation/deallocation
        if (width > origWidth || height > origHeight)
        {
            // Allocate space for the frame holding the fully decoded BGR24 image
            int size = av_image_get_buffer_size(bgrFrame.format(), bgrFrame.width(), bgrFrame.height(), 32);
            rgbFrameBufferPointer = new BytePointer(av_malloc(size)).capacity(size);
        }

        int ret = av_image_fill_arrays(new PointerPointer(bgrFrame), bgrFrame.linesize(),
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
     * Set up the scaling context to maximally fill the available screen space
     *
     * @param availableWidth  The available screen width, in pixels
     * @param availableHeight The available screen height, in pixels
     */
    private void resizeScalingContext(int availableWidth, int availableHeight)
    {
        // current size
        int cw = bgrFrame.width();
        int ch = bgrFrame.height();

        // native size
        int nativeWidth = codecContext.width();
        int nativeHeight = codecContext.height();

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
        double nativeRatio = codecContext.width() / (double) codecContext.height();
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
    public Optional<MediaFrame> convert(AVFrame frame)
    {
        if (!isValid())
        {
            return Optional.empty();
        }

        // Create the context for converting from native codec pixel format to BGR24
        swsContext = sws_getCachedContext(swsContext,
                nativeWidth, nativeHeight, nativePixFormat,
                bgrFrame.width(), bgrFrame.height(), bgrFrame.format(),
                interpolator, null, null, (DoublePointer) null);

        // Convert image from native pixel format to BGR24
        sws_scale(swsContext,
                new PointerPointer(frame), frame.linesize(), 0, nativeHeight,
                bgrFrame.data(), bgrFrame.linesize());

        BufferedImage image = frameConverter.convert(bgrFrame);
        double pts = bestEffortMediaFramePts(stream, frame);
        MediaFrame mediaFrame = new MediaFrame(pts);
        mediaFrame.setVideoFrame(new MediaFrame.VideoFrame(deepCopy(image)));
        return Optional.of(mediaFrame);
    }

    @Override
    public void configure(HandlerConfiguration config)
    {
        Optional<Dimension> maxSize = config.getMaxImageOutputSize();
        maxSize.ifPresent(d -> resizeScalingContext(d.width, d.height));
    }

    static BufferedImage deepCopy(BufferedImage bi)
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
    }
}
