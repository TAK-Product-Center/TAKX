package gov.takx.core.media;

import gov.takx.api.media.IAudioFrame;
import gov.takx.api.media.IDataFrame;
import gov.takx.api.media.IMediaFrame;
import gov.takx.api.media.IVideoFrame;

import javax.sound.sampled.AudioFormat;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Represents single frame of media
 *
 * @since 2.8
 */
public class MediaFrame implements IMediaFrame
{
    private final double pts;
    private Optional<IVideoFrame> videoFrame;
    private Optional<IAudioFrame> audioFrame;
    private Optional<IDataFrame> dataFrame;

    public MediaFrame(double pts)
    {
        this.pts = pts;
        this.videoFrame = Optional.empty();
        this.audioFrame = Optional.empty();
        this.dataFrame = Optional.empty();
    }

    public double getPts()
    {
        return pts;
    }

    @Override
    public Optional<IVideoFrame> getVideoFrame()
    {
        return videoFrame;
    }

    public void setVideoFrame(IVideoFrame videoFrame)
    {
        this.videoFrame = Optional.of(videoFrame);
    }

    @Override
    public Optional<IAudioFrame> getAudioFrame()
    {
        return audioFrame;
    }

    public void setAudioFrame(IAudioFrame audioFrame)
    {
        this.audioFrame = Optional.of(audioFrame);
    }

    @Override
    public Optional<IDataFrame> getDataFrame()
    {
        return dataFrame;
    }

    public void setDataFrame(IDataFrame dataFrame)
    {
        this.dataFrame = Optional.of(dataFrame);
    }

    public static class VideoFrame implements IVideoFrame
    {
        private final BufferedImage image;

        public VideoFrame(BufferedImage image)
        {
            this.image = image;
        }

        @Override
        public BufferedImage getImage()
        {
            return image;
        }
    }

    public static class AudioFrame implements IAudioFrame
    {
        private final AudioFormat audioFormat;
        private final byte[] data;

        public AudioFrame(AudioFormat audioFormat, byte[] data)
        {
            this.audioFormat = audioFormat;
            this.data = data;
        }

        @Override
        public AudioFormat getAudioFormat()
        {
            return audioFormat;
        }

        @Override
        public byte[] getAudioData()
        {
            return data;
        }
    }

    public static class DataFrame implements IDataFrame
    {
        private final String dataFormat;
        private final byte[] data;

        public DataFrame(String dataFormat, byte[] data)
        {
            this.dataFormat = dataFormat;
            this.data = data;
        }

        @Override
        public byte[] getData()
        {
            return data;
        }

        @Override
        public String getDataFormat()
        {
            return dataFormat;
        }
    }
}
