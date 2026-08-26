package gov.takx.core.media.decode;

import gov.takx.core.media.MediaFrame;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_move_ref;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;

/**
 * Represents a decoded frame
 *
 * @since 2.8
 */
class DecodedFrame
{
    AVFrame frame;
    private MediaFrame mediaFrame = null;
    private double pts = 0L;
    private double duration = 0L;
    private int serial = -1;
    private boolean shown = false;
    private boolean isEof = false;

    /**
     * Allocate an AVFrame
     */
    DecodedFrame()
    {
        frame = av_frame_alloc();
    }

    /**
     * Ensure the AVFrame is deallocated
     */
    void destroy()
    {
        if (frame != null)
        {
            av_frame_unref(frame);
            av_frame_free(frame);
            frame = null;
        }
    }

    void moveReference(AVFrame srcFrame)
    {
        av_frame_move_ref(frame, srcFrame);
    }

    public AVFrame getFrame()
    {
        return frame;
    }

    Optional<MediaFrame> getMediaFrame()
    {
        return Optional.ofNullable(mediaFrame);
    }

    void setMediaFrame(MediaFrame mediaFrame)
    {
        this.mediaFrame = mediaFrame;
    }

    public double getPts()
    {
        return pts;
    }

    public void setPts(double pts)
    {
        this.pts = pts;
    }

    public double getDuration()
    {
        return duration;
    }

    public void setDuration(double duration)
    {
        this.duration = duration;
    }

    public int getSerial()
    {
        return serial;
    }

    public void setSerial(int serial)
    {
        this.serial = serial;
    }

    public boolean isShown()
    {
        return shown;
    }

    public void setShown(boolean shown)
    {
        this.shown = shown;
    }

    public double getDuration(DecodedFrame previous)
    {
        if (serial == previous.serial)
        {
            double dur = pts - previous.pts;
            if (Double.isNaN(dur) || dur < 0)
            {
                return previous.duration;
            } else
            {
                return dur;
            }
        } else
        {
            return 0.0;
        }
    }

    boolean isEof()
    {
        return isEof;
    }

    void setEof(boolean eof)
    {
        isEof = eof;
    }
}
