package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.core.media.MediaFrame;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avutil.AVFrame;

import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_move_ref;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_unref;

/**
 * Represents a decoded media frame.  "Empty" instances of this class are used in the {@link FrameQueue}, and then later
 * filled out by the {@link StreamHandler} as the media frames are decoded.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class DecodedFrame
{
    private AVFrame frame = av_frame_alloc();
    private MediaFrame mediaFrame;
    private double pts;
    private double duration;
    private int serial = -1;
    private boolean shown;

    /**
     * Set the values on this placeholder with real decoded values.  Move our AV frame reference to the given frame.
     *
     * @param frame Frame allocated for decoding
     * @param mediaFrame Decoded media frame
     * @param pts Presentation time stamp
     * @param duration Frame duration
     * @param serial Stream serial number
     */
    public void setFrameValues(@Nonnull AVFrame frame, @Nonnull MediaFrame mediaFrame, double pts, double duration, int serial)
    {
        this.frame = frame;
        this.mediaFrame = mediaFrame;
        this.pts = pts;
        this.duration = duration;
        this.serial = serial;

        av_frame_move_ref(this.frame, frame);
    }

    /**
     * Deallocate our AVFrame.
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

    /**
     * Remove the reference we hold on our frame.
     */
    void unReference()
    {
        av_frame_unref(frame);
    }

    /**
     * @return the media frame decoded from our {@link AVFrame}.
     */
    @Nonnull
    public Optional<MediaFrame> getMediaFrame()
    {
        return Optional.ofNullable(mediaFrame);
    }

    /**
     * @return Presentation timestamp of this frame
     */
    public double getPts()
    {
        return pts;
    }

    /**
     * @return The duration of this frame, in fractional seconds
     */
    public double getDuration()
    {
        return duration;
    }

    /**
     * @return The serial number from the stream handler that generated this frame
     */
    public int getSerial()
    {
        return serial;
    }

    /**
     * @return if this frame has been shown
     */
    public boolean isShown()
    {
        return shown;
    }

    /**
     * @param shown Pass {@code true} if this frame has been shown
     */
    public void setShown(boolean shown)
    {
        this.shown = shown;
    }

    /**
     * @return The time since the PTS of the given previous frame, or {@code 0} if the previous frame has a different
     *         {@link #getSerial()} than this frame.
     */
    public double getTimeSincePrevious(@Nonnull DecodedFrame previous)
    {
        if (serial == previous.serial)
        {
            double dur = pts - previous.pts;
            return Double.isNaN(dur) || dur < 0 ? previous.duration : dur;
        } else
        {
            return 0.0;
        }
    }
}
