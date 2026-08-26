package gov.takx.core.media.decode;

import gov.takx.core.media.MediaFrame;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Decoder for metadata streams
 *
 * @since 2.8
 */
public class DataDecoder implements IDecoder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final AVStream stream;
    private final String format;
    private boolean decodingEnabled = true;

    public DataDecoder(AVStream stream)
    {
        this.stream = stream;
        format = toAscii(stream.codecpar().codec_tag());
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public void setDecodingEnabled(boolean enable)
    {
        decodingEnabled = enable;
    }

    @Override
    public Optional<MediaFrame> decodeFrame(AVPacket packet, AVFrame frame)
    {
        double pts = bestEffortMediaFramePts(packet, stream);
        MediaFrame mediaFrame = new MediaFrame(pts);
        if (!decodingEnabled)
        {
            return Optional.of(mediaFrame);
        }

        byte[] data = new byte[packet.size()];
        packet.data().get(data);

        mediaFrame.setDataFrame(new MediaFrame.DataFrame(format, data));
        return Optional.of(mediaFrame);
    }

    @Override
    public void flush()
    {
        // no-op
    }

    public static String toAscii(int i)
    {
        return new String(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array());
    }

    private static double bestEffortMediaFramePts(AVPacket packet, AVStream stream)
    {
        long tmpPts = packet.pts();
        final long startTime = stream.start_time() == AV_NOPTS_VALUE ? 0 : stream.start_time();
        return (tmpPts == AV_NOPTS_VALUE) ? Double.NaN
                : (tmpPts - startTime) * av_q2d(stream.time_base());
    }
}
