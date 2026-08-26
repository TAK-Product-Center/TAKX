package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.nio.ByteBuffer;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_BGR24;

/**
 * Converts video frames between AVFrame and BufferedImage.
 * (Adapted from javacv's Java2DFrameConverter: <a href="https://github.com/bytedeco/javacv">javacv</a>)
 *
 * @since 2.37 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class FrameConverter
{
    private BufferedImage bufferedImage;

    /**
     * Convert an AVFrame to a BufferedImage
     *
     * @param frame Input AVFrame; pixel format must be packed 3-byte BGR
     * @return The BufferedImage
     * @throws IllegalArgumentException if the input frame has the wrong pixel format
     */
    public BufferedImage convert(@Nonnull AVFrame frame)
    {
        if (frame.format() != AV_PIX_FMT_BGR24) throw new IllegalArgumentException("Input frame format must be BGR24");

        // (Re-)allocate bufferedImage as needed
        if (bufferedImage == null || bufferedImage.getWidth() != frame.width() || bufferedImage.getHeight() != frame.height())
        {
            bufferedImage = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_3BYTE_BGR);
        }

        // Output buffer
        SampleModel sampleModel = bufferedImage.getSampleModel();
        Raster raster = bufferedImage.getRaster();
        DataBuffer out = raster.getDataBuffer();

        final int x = -raster.getSampleModelTranslateX();
        final int y = -raster.getSampleModelTranslateY();
        final int step = ((ComponentSampleModel) sampleModel).getScanlineStride();
        final int channels = ((ComponentSampleModel) sampleModel).getPixelStride();

        final int start = y * step + x * channels;

        final byte[] a = ((DataBufferByte) out).getData();

        final ByteBuffer src = frame.data(0).capacity((long) frame.width() * frame.linesize(0)).asBuffer();
        copy(src, frame.linesize(0), ByteBuffer.wrap(a, start, a.length - start), step);

        return bufferedImage;
    }

    /**
     * Copy image scan lines.
     *
     * @param srcBuf  ByteBuffer containing the source image
     * @param srcStep Size of a source image scanline, in bytes
     * @param dstBuf  ByteBuffer containing the destination image
     * @param dstStep Size of a destination image scanline, in bytes
     */
    private static void copy(ByteBuffer srcBuf, int srcStep, ByteBuffer dstBuf, int dstStep)
    {
        int srcLine = srcBuf.position();
        int dstLine = dstBuf.position();
        final int w = Math.min(srcStep, dstStep);

        while (srcLine < srcBuf.capacity() && dstLine < dstBuf.capacity())
        {
            srcBuf.position(srcLine);
            dstBuf.position(dstLine);

            final ByteBuffer srcRow = srcBuf.slice();
            final ByteBuffer dstRow = dstBuf.slice();

            srcRow.limit(w);
            dstRow.limit(w);

            dstRow.put(srcRow);

            srcLine += srcStep;
            dstLine += dstStep;
        }
    }
}
