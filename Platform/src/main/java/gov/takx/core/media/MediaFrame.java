package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IAudioFrame;
import gov.takx.api.media.IDataFrame;
import gov.takx.api.media.IMediaFrame;
import gov.takx.api.media.IVideoFrame;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.sound.sampled.AudioFormat;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Represents single frame of media.  Although supported, it is rare to have more than a single inner frame type.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class MediaFrame implements IMediaFrame
{
    private final double pts;
    private IVideoFrame videoFrame;
    private IAudioFrame audioFrame;
    private IDataFrame dataFrame;

    /**
     * @param pts Presentation time stamp of this frame.
     * @param videoFrame Optional Decoded video frame
     * @param audioFrame Optional Decoded audio frame
     * @param dataFrame Optional Raw data frame
     */
    public MediaFrame(double pts, @Nullable IVideoFrame videoFrame, @Nullable IAudioFrame audioFrame, @Nullable IDataFrame dataFrame)
    {
        this.pts = pts;
        this.videoFrame = videoFrame;
        this.audioFrame = audioFrame;
        this.dataFrame = dataFrame;
    }

    public MediaFrame(double pts, @Nonnull IVideoFrame videoFrame)
    {
        this(pts, videoFrame, null, null);
    }

    public MediaFrame(double pts, @Nonnull IAudioFrame audioFrame)
    {
        this(pts, null, audioFrame, null);
    }

    public MediaFrame(double pts, @Nonnull IDataFrame dataFrame)
    {
        this(pts, null, null, dataFrame);
    }

    /**
     * @param pts Presentation timestamp
     */
    public MediaFrame(double pts)
    {
        this.pts = pts;
    }

    @Override
    public double getPresentationTimeStamp()
    {
        return pts;
    }

    @Override
    @Nonnull
    public Optional<IVideoFrame> getVideoFrame()
    {
        return Optional.ofNullable(videoFrame);
    }

    @Override
    @Nonnull
    public Optional<IAudioFrame> getAudioFrame()
    {
        return Optional.ofNullable(audioFrame);
    }

    @Override
    @Nonnull
    public Optional<IDataFrame> getDataFrame()
    {
        return Optional.ofNullable(dataFrame);
    }

    @Override
    public String toString()
    {
        return "MediaFrame{" +
                "pts=" + pts +
                ", contains=[" +
                (videoFrame != null ? "Video," : "") +
                (audioFrame != null ? "Audio," : "") +
                (dataFrame != null ? "Data," : "") +
                "]}";
    }

    public record VideoFrame(@Nonnull BufferedImage getImage) implements IVideoFrame
    {
    }

    public record AudioFrame(@Nonnull AudioFormat getAudioFormat, @Nonnull byte[] getAudioData) implements IAudioFrame
    {
    }

    public record DataFrame(@Nonnull String getDataFormat, @Nonnull byte[] getData) implements IDataFrame
    {
    }
}
