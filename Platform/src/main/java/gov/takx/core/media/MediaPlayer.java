package gov.takx.core.media;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.media.AMediaPlayer;
import gov.takx.api.media.IAudioFrame;
import gov.takx.api.media.IDataFrame;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaProducerListener;
import gov.takx.api.media.IPlayable;
import gov.takx.api.media.IVideoFrame;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.ui.UISpaceStateChangeEvent;
import gov.takx.commons.lang.ExceptionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.Dimension;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * A graphical media player that can process and display audio and video media.  A {@link MediaProducer} is used
 * to process the media source, and decode the frames within it for display here.
 *
 * @see MediaProducer
 * @since 2.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MediaPlayer extends AMediaPlayer implements IMediaProducerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private INotificationService notificationService;

    private final AudioThread audioThread = new AudioThread();
    private MediaProducer mediaProducer;
    private Thread mediaProducerThread;

    private boolean videoReceived;

    /**
     * @param options Media player options
     * @param media Media to play
     * @see #loadMedia(IMedia)
     */
    public MediaPlayer(@Nonnull MediaPlayerOptions options, @Nullable IMedia media)
    {
        super(options, media);

        audioThread.setDaemon(true);
        audioThread.start();
    }

    @Override
    protected void componentResizeListener()
    {
        if (mediaProducer != null) mediaProducer.setVideoSize(getSize());
        super.componentResizeListener();
    }

    @Override
    public void loadMedia(@Nonnull IMedia media)
    {
        super.loadMedia(media);

        Dimension d = getSize();
        boolean validSize = d.width > 0 && d.height > 0;

        // For some reason, the MediaProducer doesn't like file: URIs, so we convert that to a file path
        final String localFile = media.getLocalFilePath();
        final String source = localFile != null ? localFile : media.getSourceUri().toString();
        final boolean playable = media instanceof IPlayable;

        logger.debug("Loading Media: {}, playable {}", source, playable);

        // Special handling for local file images.  Using FFMPEG to read an image doesn't always seem to work (leading
        // to images with portions clipped), and it doesn't handle TIFF files.  So, for non-playable media (images),
        // read the image directly and install it into this player, skipping the whole media producer route.

        if (!playable && localFile != null)
        {
            try
            {
                image.set(ImageIO.read(new File(localFile)));
            } catch (Exception e)
            {
                logger.warn("Failed to load image from {}", localFile, e);
            }
        } else
        {
            mediaProducer = validSize
                    ? new MediaProducer(media, source, d.width, d.height, this::reportMediaProducerError)
                    : new MediaProducer(media, source, this, this::reportMediaProducerError);

            mediaProducer.addMediaProducerListener(this);
            mediaProducerThread = new Thread(mediaProducer);
            mediaProducerThread.start();
        }
    }

    /**
     * Override this method if you need to change the default use of the {@link INotificationService} to display an
     * error reported from the media producer.
     *
     * @param message Error message
     */
    protected void reportMediaProducerError(@Nonnull String message)
    {
        notificationService.show(new Notification(message, Notification.Type.Error));
    }

    @Override
    public void unloadMedia()
    {
        final String mediaName = media != null ? media.getName() : "null";

        if (logger.isDebugEnabled())
        {
            logger.debug("Unloading media: \"{}\" producer:{}", mediaName, mediaProducer != null ? "yes" : "no");
        }
        super.unloadMedia();

        if (mediaProducer != null)
        {
            mediaProducer.removeMediaProducerListener(this);
            mediaProducer.shutdown();

            try
            {
                mediaProducerThread.join(TimeUnit.SECONDS.toMillis(3));
            } catch (InterruptedException e)
            {
                logger.debug("Timeout waiting on media producer: {}", mediaName);
            }

            mediaProducer = null;
            mediaProducerThread = null;
        }

        if (audioThread != null) audioThread.pause();
    }

    @Override
    public void play()
    {
        // Unpause media producer
        if (mediaProducer != null) mediaProducer.queueCommand(new MediaCommand(MediaCommand.Type.PLAY));

        super.play();
    }

    @Override
    public boolean isPlaying()
    {
        return mediaProducer != null && mediaProducer.isPlaying();
    }

    @Override
    public void pause()
    {
        super.pause();

        // Pause the producer
        if (mediaProducer != null) mediaProducer.queueCommand(new MediaCommand(MediaCommand.Type.PAUSE));

        // Pause the audio
        audioThread.pause();
    }

    @Override
    protected void mute(boolean mute)
    {
        super.mute(mute);
        audioThread.setMuted(mute);
    }

    @Override
    public void seek(double timestamp)
    {
        // No-op for images
        if (mediaProducer != null) mediaProducer.queueCommand(new MediaCommand(MediaCommand.Type.SEEK, timestamp));

        super.seek(timestamp);
    }

    @Override
    public void capture(@Nonnull String path, @Nullable BiConsumer<String, Exception> exceptionHandler)
    {
        if (mediaProducer != null && media instanceof IPlayable)
        {
            mediaProducer.startCapture(path, exceptionHandler);
        } else
        {
            final String msg = media == null
                    ? "No media has been loaded"
                    : "Attempted to capture a non playable media " + media.getName();
            logger.warn(msg);
            throw new IllegalStateException(msg);
        }
    }

    @Override
    public void stopCapture(@Nonnull String path)
    {
        if (mediaProducer != null) mediaProducer.stopCapture(path);
    }

    @Override
    public void onMediaFrame(@Nonnull MediaFrame mediaFrame)
    {
        logger.debug("onMediaFrame {}", mediaFrame);
        final double pts = mediaFrame.getPresentationTimeStamp();
        mediaFrame.getVideoFrame().ifPresent(f -> onVideo(pts, f));
        mediaFrame.getAudioFrame().ifPresent(f -> onAudio(pts, f));
        mediaFrame.getDataFrame().ifPresent(f -> onData(pts, f));
    }

    /**
     * Process a decoded video frame.  Display the image.
     *
     * @param pts Presentation timestamp of frame
     * @param videoFrame frame containing the decoded image
     */
    protected void onVideo(double pts, IVideoFrame videoFrame)
    {
        videoReceived = true;
        final var frameImage = videoFrame.getImage();
        if (logger.isDebugEnabled()) logger.debug("PLAY Video pts {} {}x{}", pts, frameImage.getWidth(), frameImage.getHeight());

        image.set(frameImage);

        // If media is a still image, the player is responsible for resizing it to fit the window. For video, the
        // resizing is done natively for efficiency.
        if (isStillImage())
        {
            generateScaledCopy();
        } else
        {
            updateTime(pts);
        }

        repaint();
    }

    /**
     * Process a decoded audio frame.  Hand the frame to the audio thread.
     *
     * @param pts Presentation timestamp of frame
     * @param audioFrame frame containing the decoded audio
     */
    protected void onAudio(double pts, IAudioFrame audioFrame)
    {
        if (logger.isDebugEnabled()) logger.debug("PLAY Audio pts {} format {} size {}", pts, audioFrame.getAudioFormat(), audioFrame.getAudioData().length);

        if (!videoReceived)
        {
            image.set(AUDIO_IMAGE);
            updateTime(pts);
        }

        if (!sliderDragging) audioThread.enqueue(audioFrame);
    }

    /**
     * Process a decoded data frame.
     *
     * @param pts Presentation timestamp of frame
     * @param dataFrame frame containing the decoded tagged data
     */
    protected void onData(double pts, IDataFrame dataFrame)
    {
        if (logger.isDebugEnabled()) logger.debug("PLAY Data '{}' pts {} size {}", dataFrame.getDataFormat(), pts, dataFrame.getData().length);
    }

    /**
     * Update the time display on the slider, if our media is playable.
     *
     * @param pts Presentation timestamp to indicating the current time (offset from the media start)
     */
    protected void updateTime(double pts)
    {
        if (!(media instanceof IPlayable playable)) return;

        Long duration = playable.getDuration();
        long durationSeconds = duration != null ? duration : 0;

        String elapsed = String.format("%.2f / %d", pts, durationSeconds);
        elapsedAndDurationLabel.setText(elapsed);
        if (!sliderDragging) slider.updateThumbPosition(pts / durationSeconds, false);
    }

    /**
     * Observe UISpaceStateChanged events to capture when user closes our window
     *
     * @param event The event
     */
    private void uiStateChanged(@Observes UISpaceStateChangeEvent event)
    {
        if (event.getState() == UISpaceStateChangeEvent.DockingState.Closed && event.getTitle().equals(getName()))
        {
            // Note that this event may be fired multiple times, so ensure close() is safe to call repeatedly.
            logger.debug("User closed media player");

            unloadMedia();
            mediaPlayerManager.removeMediaPlayer(this);
        }
    }

    /**
     * Called by media manager to release any resources and notify listeners that we closed.
     */
    @Override
    @RemoveObserver
    public void destroy()
    {
        super.destroy();
        audioThread.shutdown();
        ExceptionUtils.safeDoOperation(listeners, listener -> listener.onPlayerClosed(this));
        listeners.clear();
    }
}
