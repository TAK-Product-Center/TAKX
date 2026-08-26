package gov.takx.core.media;

import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.writer.ChunkWriter;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Records an input media stream to a file sequence.
 *
 * @since 5.1.0
 */
public class MediaRecorder implements PacketListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Path baseFilePath;
    private final ExternalClock clock;
    private ChunkWriter chunkWriter;
    // We hit a fatal error, ignore future packets.
    boolean errored;

    /**
     * Create recorder to write media packets to disk. Recording will start once this object
     * has been registered as a PacketListener and receives the first packet.
     *
     * @param baseFilePath Base file path (chunk filenames will be appended with -%06d.ts)
     * @param clock        Provides the time appended to the file name
     */
    public MediaRecorder(@Nonnull Path baseFilePath, @Nonnull ExternalClock clock)
    {
        this.baseFilePath = baseFilePath;
        this.clock = clock;
        ensureExists(baseFilePath.getParent());
    }

    /**
     * Create recorder to write media packets to disk. Recording will start once this object
     * has been registered as a PacketListener and receives the first packet.
     *
     * @param baseFilePath Base file path (chunk filenames will be appended with -%06d.ts)
     * @param clock        Provides the time appended to the file name
     */
    public MediaRecorder(@Nonnull String baseFilePath, @Nonnull ExternalClock clock)
    {
        this(Paths.get(baseFilePath), clock);
    }

    /**
     * Attempt to create the required directories for the path
     *
     * @param path The path of directories to create.
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
                logger.error(errorMessage, exception);
                AlwaysOnTopJOptionPane.showErrorDialog(errorMessage, "Error processing media.");
                errored = true;
            }
        }
    }

    /**
     * Stop recording the stream
     */
    public void stop()
    {
        chunkWriter.close();
    }

    @Override
    public void onPacket(@Nonnull AVFormatContext context, @Nonnull AVPacket packet)
    {
        if (!errored)
        {
            try
            {
                if (chunkWriter == null)
                {
                    // First packet
                    createNewChunk(context);
                } else if (chunkWriter.isChunkFinished())
                {
                    // Close and open a new chunk file
                    chunkWriter.close();
                    createNewChunk(context);
                }

                chunkWriter.writePacket(packet);
            } catch (IOException | MediaException exception)
            {
                errored = true;
                logger.error("Error processing media packet.", exception);
                AlwaysOnTopJOptionPane.showErrorDialog(exception.getMessage(), "Error processing media packet.");
            }
        }
    }

    /**
     * Create a new chunk, a short media file containing a part of the whole captured media.
     *
     * @param context The {@code AVFormatContext} we are reading packets from.
     * @throws IOException    Unable to open or write the output file.
     * @throws MediaException On other fatal error.
     */
    private void createNewChunk(@Nonnull AVFormatContext context) throws IOException, MediaException
    {
        // File name contains rough time of first included packet
        final String filename = baseFilePath.getFileName().toString() + String.format("-%.2f.ts", clock.getClock());
        String filePath = baseFilePath.resolveSibling(filename).toString();
        logger.debug("Starting new chunk file: {}", filePath);

        chunkWriter = new ChunkWriter(context, filePath);
    }
}
