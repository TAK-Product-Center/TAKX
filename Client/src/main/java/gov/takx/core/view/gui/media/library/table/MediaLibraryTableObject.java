package gov.takx.core.view.gui.media.library.table;

import gov.takx.api.media.IImage;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IPlayable;
import gov.takx.api.media.MediaType;
import gov.takx.commons.date.DateFormatter;
import gov.takx.commons.file.SizeFormatter;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Represents a row in the Media panel's library table
 *
 * @see MediaLibraryTableModel
 * @since 2.37
 */
public class MediaLibraryTableObject
{
    private final String name;
    private final String pathURL;
    private final String type;
    private final String creationTime;
    private final String durationSize;
    private boolean onTimeline;

    private final IMedia media;

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /**
     * Constructor for an object representing a row in the Media panel's library table
     *
     * @param media Named handle for an audio, video, or image file or stream.  Even though it's not displayed
     *              in the table, it is kept around for convenience for the rest of the media API.
     */
    public MediaLibraryTableObject(IMedia media)
    {
        MediaType mediaType = MediaType.getMediaType(media);

        // Need a local variable because the class version is final
        final String localDurationSize;

        if (media instanceof IPlayable playable)
        {
            // Both IAudio and IVideo are instances of IPlayable
            Long duration = playable.getDuration();
            if (duration != null)
            {
                localDurationSize = DateFormatter.formatElapsedTime(TimeUnit.SECONDS.toMillis(duration));
            } else
            {
                localDurationSize = "Unknown";
            }
        } else if (media instanceof IImage image)
        {
            Long fileSizeBytes = image.getFileSize();
            if (fileSizeBytes != null)
            {
                localDurationSize = SizeFormatter.formatEstimate(fileSizeBytes);
            } else
            {
                localDurationSize = "Unknown";
            }
        } else
        {
            localDurationSize = "Unknown";
        }

        final String creationTimeString;

        if (media.getLocalFilePath() != null)
        {
            ZonedDateTime mediaCreationTime;

            try
            {
                final Path path = Path.of(media.getSourceUri());
                BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
                FileTime fileTimeFileCreation = attributes.creationTime();
                mediaCreationTime = ZonedDateTime.ofInstant(fileTimeFileCreation.toInstant(), ZoneId.systemDefault());
            } catch (IOException | NullPointerException e)
            {
                // Since the attributes failed, we'll assume that a stream was added
                mediaCreationTime = ZonedDateTime.now();
            }

            creationTimeString = dateTimeFormatter.format(mediaCreationTime);
        } else
        {
            creationTimeString = "Unknown";
        }

        name = media.getName();
        pathURL = media.getSourceUri().toString();
        type = mediaType.toString();
        creationTime = creationTimeString;
        durationSize = localDurationSize;
        onTimeline = false;
        this.media = media;
    }

    /**
     * @return {@code true} when the media library entry origiantes from a local file path.
     * {@code false} for any other origin.
     * @since 5.2
     */
    public boolean isLocalFile()
    {
        return pathURL.startsWith("file:");
    }

    public String getName()
    {
        return name;
    }

    String getPathURL()
    {
        return pathURL;
    }

    public String getType()
    {
        return type;
    }

    String getCreationTime()
    {
        return creationTime;
    }

    String getDurationSize()
    {
        return durationSize;
    }

    boolean isOnTimeline()
    {
        return onTimeline;
    }

    void setOnTimeline(boolean onTimeline)
    {
        this.onTimeline = onTimeline;
    }

    Color getDeviceColor()
    {
        return media.getColor();
    }

    void setDeviceColor(Color deviceColor)
    {
        media.setColor(deviceColor);
    }

    public IMedia getMedia()
    {
        return media;
    }
}
