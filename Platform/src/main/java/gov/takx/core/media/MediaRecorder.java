package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IPacketListener;
import gov.takx.api.media.MediaException;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.writer.MediaStreamWriter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * Records an input media streams to a file sequence.  Configured using a base file path, which is used to generate
 * the file names to which media streams are written.  Each file is appended with the presentation timestamp of the
 * first packet to be written to it.  The number of packets written to a file before a new one is created is controlled
 * by {@link #setMaxPackets(int)}, defaulting to assuming 24 FPS for 60 seconds.
 *
 * @see MediaStreamWriter
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class MediaRecorder implements IPacketListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String FILENAME_PATTERN = "%s-%.2f.ts";

    private final Path baseFilePath;
    private final ExternalClock clock;
    private final BiConsumer<String, Exception> exceptionHandler;
    private MediaStreamWriter mediaStreamWriter;
    private int maxPackets = 24 * 60; // Default limit on file size assuming 24 fps, for 60 seconds
    boolean errored; // We hit a fatal error, ignore future packets.

    /**
     * Create recorder to write media packets to disk. Recording will start once this object
     * has been registered as a IPacketListener and receives the first packet.
     *
     * @param baseFilePath Base file path (chunk filenames will be appended with -%2d.ts)
     * @param clock        Provides the time appended to the file name
     * @param exceptionHandler Handler to receive any exceptions thrown by the recording process, {@code null} for
     *                         default handling, which will simply log the exceptions
     */
    public MediaRecorder(@Nonnull Path baseFilePath, @Nonnull ExternalClock clock, @Nullable BiConsumer<String, Exception> exceptionHandler)
    {
        this.baseFilePath = baseFilePath;
        this.clock = clock;
        this.exceptionHandler = exceptionHandler != null ? exceptionHandler : logger::error;
        ensureExists(baseFilePath.getParent());
    }

    /**
     * @return The number of packets to record before starting a new output file
     */
    public int getMaxPackets()
    {
        return maxPackets;
    }

    /**
     * @param maxPackets The number of packets to record before starting a new output file
     */
    public void setMaxPackets(int maxPackets)
    {
        this.maxPackets = maxPackets;
    }

    /**
     * Ensure that the given directory path exists, creating any missing parent directories.
     */
    private void ensureExists(Path path)
    {
        if (path != null)
        {
            try
            {
                Files.createDirectories(path);
            } catch (IOException exception)
            {
                String errorMessage = String.format("Unable to create directory %s.", path);
                exceptionHandler.accept(errorMessage, exception);
                errored = true;
            }
        }
    }

    @Override
    public void stop()
    {
        mediaStreamWriter.close();
    }

    @Override
    public void onPacket(@Nonnull AVFormatContext context, @Nonnull AVPacket packet)
    {
        if (!errored)
        {
            try
            {
                if (mediaStreamWriter == null)
                {
                    // First packet
                    createNewChunk(context);
                } else if (mediaStreamWriter.getPacketsWritten() >= maxPackets)
                {
                    // Close and open a new chunk file
                    mediaStreamWriter.close();
                    createNewChunk(context);
                }

                mediaStreamWriter.writePacket(packet);
            } catch (IOException | MediaException exception)
            {
                errored = true;
                exceptionHandler.accept("Error processing media packet.", exception);
            }
        }
    }

    /**
     * Create a new chunk, a short media file containing a part of the whole captured media.
     *
     * @param context The {@code AVFormatContext} we are reading packets from.
     * @throws IOException Unable to open or write the output file.
     * @throws MediaException On other fatal error.
     */
    private void createNewChunk(@Nonnull AVFormatContext context) throws IOException, MediaException
    {
        // File name contains rough time of first included packet
        final String filename = String.format(FILENAME_PATTERN, baseFilePath.getFileName(), clock.getClock());

        final var filePath = baseFilePath.resolveSibling(filename);
        logger.debug("Starting new chunk file: {}", filePath);

        mediaStreamWriter = new MediaStreamWriter(context, filePath);
    }
}
