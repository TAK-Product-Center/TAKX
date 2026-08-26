package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IDecoder;
import gov.takx.api.media.IDecodingOptions;
import gov.takx.core.media.MediaFrame;
import gov.takx.core.media.Utilities;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Decoder for metadata streams.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class DataDecoder implements IDecoder
{
    private final AVStream stream;
    private final String format;
    private boolean decodingEnabled;

    /**
     * @param stream Stream we are decoding from
     * @param options decoding options
     */
    public DataDecoder(@Nonnull AVStream stream, @Nonnull IDecodingOptions options)
    {
        this.stream = stream;
        format = Utilities.tagToFourCc(stream.codecpar().codec_tag());
        decodingEnabled = options.isDecodeData();
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
    @Nonnull
    public Optional<MediaFrame> decodeFrame(@Nullable AVPacket packet, @Nonnull AVFrame frame)
    {
        if (packet == null) return Optional.empty();
        if (!decodingEnabled) return Optional.of(new MediaFrame(bestEffortPacketFramePts(packet, stream)));

        double pts = bestEffortPacketFramePts(packet, stream);

        byte[] data = new byte[packet.size()];
        packet.data().get(data);

        return Optional.of(new MediaFrame(pts, new MediaFrame.DataFrame(format, data)));
    }

    @Override
    public void flush()
    {
        // no-op
    }

    private static double bestEffortPacketFramePts(AVPacket packet, AVStream stream)
    {
        long tmpPts = packet.pts();
        final long startTime = stream.start_time() == AV_NOPTS_VALUE ? 0 : stream.start_time();
        return tmpPts == AV_NOPTS_VALUE ? Double.NaN : (tmpPts - startTime) * av_q2d(stream.time_base());
    }

    @Override
    public void releaseResources()
    {
        // nothing to release
    }
}
