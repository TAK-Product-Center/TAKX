package gov.takx.core.view.gui.media;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.AMediaPlayer;
import gov.takx.api.media.IAudioFrame;
import gov.takx.api.media.IDataFrame;
import gov.takx.api.media.IImage;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaPlayerListener;
import gov.takx.api.media.IPlayable;
import gov.takx.api.media.IVideoFrame;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.api.ui.UISpaceStateChangeEvent;
import gov.takx.core.media.MediaCommand;
import gov.takx.core.media.MediaFrame;
import gov.takx.core.media.MediaProducer;
import gov.takx.core.media.MediaProducerListener;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import java.awt.Dimension;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A graphical media player
 *
 * @since 2.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MediaPlayer extends AMediaPlayer implements MediaProducerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AudioThread audioThread = new AudioThread();
    private Optional<MediaProducer> mediaProducer = Optional.empty();
    private Optional<Future<?>> mediaProducerFuture = Optional.empty();

    private boolean videoReceived;

    /**
     * Constructor
     *
     * @param options Media player options
     */
    MediaPlayer(MediaPlayerOptions options, IMedia media)
    {
        super(options, media);

        audioThread.setDaemon(true);
        audioThread.start();
    }

    @Override
    protected void componentResizeListener()
    {
        Dimension d = getSize();
        mediaProducer.ifPresent(p -> p.setMaxSize(d.width, d.height));
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
            // If we don't have a URL then there is no reason to start a producer thread.
            // Most likely this is a custom player in this case
            mediaProducer = Optional.of(validSize
                    ? new MediaProducer(media, source, d.width, d.height)
                    : new MediaProducer(media, source, this));
            mediaProducer.ifPresent(producer -> producer.addMediaProducerListener(this));
            mediaProducerFuture = mediaProducer.map(executorService::submit);
        }
    }

    @Override
    public void unloadMedia()
    {
        final String mediaName = media != null ? media.getName() : "null";

        if (logger.isDebugEnabled())
        {
            logger.debug("Unloading media: \"{}\" producer:{}", mediaName, mediaProducer.isPresent() ? "yes" : "no");
        }
        super.unloadMedia();

        mediaProducer.ifPresent(producer -> {
            producer.removeMediaProducerListener(this);
            producer.shutdown();
        });

        mediaProducerFuture.ifPresent(future -> {
            try
            {
                future.get(3, TimeUnit.SECONDS);
            } catch (ExecutionException e)
            {
                logger.error("Error shutting down media", e);
            } catch (InterruptedException | TimeoutException e)
            {
                logger.debug("Timeout waiting on media producer: {}", mediaName);
            }
        });
        mediaProducer = Optional.empty();
        mediaProducerFuture = Optional.empty();

        audioThread.pause();
    }

    @Override
    public void play()
    {
        // Unpause media producer
        mediaProducer.ifPresent(producer -> producer.onCommand(new MediaCommand(MediaCommand.Type.PLAY)));

        super.play();
    }

    @Override
    public boolean isPlaying()
    {
        return mediaProducer.map(MediaProducer::isPlaying).orElse(false);
    }

    @Override
    public void pause()
    {
        super.pause();

        // Pause the producer
        mediaProducer.ifPresent(producer -> producer.onCommand(new MediaCommand(MediaCommand.Type.PAUSE)));

        // Pause the audio
        audioThread.pause();
    }

    @Override
    public void seek(double timestamp)
    {
        // No-op for images
        if (getMedia().isPresent())
        {
            final MediaCommand command = new MediaCommand(MediaCommand.Type.SEEK, timestamp);
            mediaProducer.ifPresent(producer -> producer.onCommand(command));
        }

        super.seek(timestamp);
    }

    /**
     * Start the capture of the media to the given path.
     *
     * @param media The currently playing media.
     * @param path  The path to capture the media.
     * @since 5.1.0
     */
    private void capture(IMedia media, String path)
    {
        if (media instanceof IPlayable playableMedia)
        {
            playableMedia.setCaptureLocation(path);
            final MediaCommand command = new MediaCommand(MediaCommand.Type.START_CAPTURE, path);
            mediaProducer.ifPresent(producer -> producer.onCommand(command));
        } else
        {
            logger.warn("Attempted to capture a non playable media {}.", media.getName());
        }
    }

    @Override
    public void capture(String path)
    {
        getMedia().ifPresent(m -> capture(m, path));
    }

    @Override
    public void stopCapture(String path)
    {
        if (getMedia().isPresent())
        {
            final MediaCommand command = new MediaCommand(MediaCommand.Type.STOP_CAPTURE, path);
            mediaProducer.ifPresent(producer -> producer.onCommand(command));
        }
    }

    @Override
    public void onMediaFrame(MediaFrame mediaFrame)
    {
        final double pts = mediaFrame.getPts();
        mediaFrame.getVideoFrame().ifPresent(f -> onVideo(pts, f));
        mediaFrame.getAudioFrame().ifPresent(f -> onAudio(pts, f));
        mediaFrame.getDataFrame().ifPresent(f -> onData(pts, f));
    }

    private void onVideo(double pts, IVideoFrame videoFrame)
    {
        videoReceived = true;
        image.set(videoFrame.getImage());

        // If media is a still image, the player is responsible for resizing it to fit the window. For video, the
        // resizing is done natively for efficiency.
        if (getMedia().isPresent() && getMedia().get() instanceof IImage)
        {
            generateScaledCopy();
        } else
        {
            updateTime(pts);
        }

        repaint();
    }

    private void onAudio(double pts, IAudioFrame audioFrame)
    {
        if (!videoReceived)
        {
            image.set(AUDIO_IMAGE);
            updateTime(pts);
        }

        if (!sliderDragging)
        {
            audioThread.enqueue(audioFrame);
        }
    }

    private void onData(double pts, IDataFrame dataFrame)
    {

    }

    private void updateTime(double pts)
    {
        if (getMedia().isEmpty() || !(getMedia().get() instanceof IPlayable playable)) return;

        Long dur = playable.getDuration();
        long durationSeconds = (dur != null) ? dur : 0;

        String sb = String.format("%.2f", pts) +
                " / " +
                String.format("%d", durationSeconds);
        elapsedAndDurationLabel.setText(sb);
        if (!sliderDragging)
        {
            slider.updateThumbPosition(pts / durationSeconds, false);
        }
    }

    /**
     * Observe UISpaceStateChanged events to capture when user closes our window
     *
     * @param event The event
     */
    public void uiStateChanged(@Observes UISpaceStateChangeEvent event)
    {
        if (event.getState() == UISpaceStateChangeEvent.DockingState.Closed && event.getTitle().equals(getName()))
        {
            // Note that this event may be fired multiple times, so ensure close() is safe to call repeatedly.
            logger.debug("User closed media player");
            close();
        }
    }

    /**
     * Close the file/stream
     */
    private void close()
    {
        unloadMedia();
        mediaPlayerManager.removeMediaPlayer(this);
    }

    /**
     * Called internally by media manager to release any resources and notify listeners that we closed
     */
    public void destroy()
    {
        audioThread.shutdown();

        // Notify listeners
        for (IMediaPlayerListener listener : listeners)
        {
            listener.onPlayerClosed(this);
        }
        listeners.clear();
    }

    /**
     * Writes decoded audio data to the mixer
     */
    private class AudioThread extends Thread
    {
        LinkedBlockingQueue<IAudioFrame> queue = new LinkedBlockingQueue<>(100);
        Map<AudioFormat, SourceDataLine> lines = new ConcurrentHashMap<>();
        Set<AudioFormat> failedFormats = new CopyOnWriteArraySet<>();
        boolean running = true;

        /**
         * Queue an audio frame to be sent to the mixer
         *
         * @param audio The frame to be written
         */
        void enqueue(IAudioFrame audio)
        {
            queue.offer(audio);
        }

        /**
         * Pause the audio playback by clearing queue & closing all lines
         */
        public void pause()
        {
            queue.clear();
            closeLines();
        }

        /**
         * Terminate the thread
         */
        public void shutdown()
        {
            running = false;
        }

        @Override
        public void run()
        {
            while (running)
            {
                try
                {
                    final IAudioFrame audio = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (audio == null)
                    {
                        continue;
                    }

                    AudioFormat audioFormat = audio.getAudioFormat();
                    if (isBlacklisted(audioFormat))
                    {
                        continue;
                    }

                    Optional<SourceDataLine> sourceDataLine = getSource(audioFormat);
                    if (sourceDataLine.isEmpty())
                    {
                        sourceDataLine = openSource(audioFormat);
                    }

                    if (sourceDataLine.isPresent())
                    {
                        SourceDataLine line = sourceDataLine.get();
                        final byte[] data = audio.getAudioData();
                        if (!isMuted)
                        {
                            int size = line.write(data, 0, data.length);
                            logger.debug("Wrote {} bytes", size);
                        }
                    }
                } catch (InterruptedException e)
                {
                    logger.warn("Audio thread interrupted exiting...");
                    running = false;
                }
            }

            closeLines();
            failedFormats.clear();
            queue.clear();
        }

        private Optional<SourceDataLine> getSource(final AudioFormat format)
        {
            return lines.keySet()
                    .stream()
                    .filter(f -> f.matches(format))
                    .findAny()
                    .map(f -> lines.get(f));
        }

        private Optional<SourceDataLine> openSource(final AudioFormat format)
        {
            try
            {
                logger.debug("Creating SourceDataLine for {}", format);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.addLineListener(e -> logger.debug("{}", e));
                line.open(format);
                line.start();
                lines.put(format, line);
                return Optional.of(line);
            } catch (LineUnavailableException | IllegalArgumentException e)
            {
                logger.error("Error creating SourceDataLine for {}", format, e);
                failedFormats.add(format);
                return Optional.empty();
            }
        }

        private void closeLines()
        {
            for (SourceDataLine line : lines.values())
            {
                line.close();
            }
            lines.clear();
        }

        private boolean isBlacklisted(final AudioFormat format)
        {
            for (AudioFormat f : failedFormats)
            {
                if (f.matches(format))
                {
                    return true;
                }
            }
            return false;
        }
    }
}
