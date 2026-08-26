package gov.takx.core.media;

import gov.takx.api.media.IAudioFrame;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Writes decoded audio data to the mixer.
 *
 * @since 5.8
 */
class AudioThread extends Thread
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final BlockingQueue<IAudioFrame> queue = new LinkedBlockingQueue<>(100);
    private final Map<AudioFormat, SourceDataLine> lines = new ConcurrentHashMap<>();
    private final Set<AudioFormat> failedFormats = new CopyOnWriteArraySet<>();
    private volatile boolean running = true;
    private volatile boolean muted;

    /**
     * Queue an audio frame to be sent to the mixer
     *
     * @param audio The frame to be written
     */
    public void enqueue(@Nonnull IAudioFrame audio)
    {
        if (!queue.offer(audio)) logger.debug("Dropped audio frame");
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
     * @param muted Pass {@code true} to mute audio, {@code false} to enable audio
     */
    public void setMuted(boolean muted)
    {
        this.muted = muted;
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
                final var audio = queue.poll(100, TimeUnit.MILLISECONDS);
                if (muted || audio == null) continue;

                var audioFormat = audio.getAudioFormat();
                if (isBlacklisted(audioFormat)) continue;

                var sourceDataLine = getOrOpenSource(audioFormat);
                if (sourceDataLine != null)
                {
                    final byte[] data = audio.getAudioData();
                    int size = sourceDataLine.write(data, 0, data.length);
                    logger.debug("Wrote {} bytes", size);
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

    @Nullable
    private SourceDataLine getOrOpenSource(final AudioFormat format)
    {
        var sourceDataLine = lines.keySet()
                .stream()
                .filter(f -> f.matches(format))
                .findAny()
                .map(lines::get)
                .orElse(null);

        if (sourceDataLine == null) sourceDataLine = openSource(format);
        return sourceDataLine;
    }

    @Nullable
    private SourceDataLine openSource(final AudioFormat format)
    {
        try
        {
            logger.debug("Creating SourceDataLine for {}", format);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.addLineListener(e -> logger.debug("{}", e));
            line.open(format);
            line.start();
            lines.put(format, line);
            return line;
        } catch (LineUnavailableException | IllegalArgumentException e)
        {
            logger.error("Error creating SourceDataLine for {}", format, e);
            failedFormats.add(format);
            return null;
        }
    }

    private void closeLines()
    {
        lines.values().forEach(Line::close);
        lines.clear();
    }

    private boolean isBlacklisted(final AudioFormat format)
    {
        return failedFormats.stream().anyMatch(f -> f.matches(format));
    }
}
