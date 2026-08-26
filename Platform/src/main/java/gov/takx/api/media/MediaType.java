package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Supported media types and their associated file extensions.
 *
 * @since 2.8
 */
@Experimental
public enum MediaType
{
    /**
     * A still image.
     */
    Image("bmp", "gif", "jpg", "jpeg", "png", "tif", "tiff"),

    /**
     * A video stream; can include sub-streams for audio and metadata.
     */
    Video("3gp", "3g2", "avi", "dv", "flv", "m2ts", "m4v", "mjpeg", "mjpg", "mkv", "mov", "mp4", "mpeg",
            "mpg", "mxf", "swf", "ts", "webm", "wmv"),

    /**
     * An audio stream.
     */
    Audio("aac", "au", "flac", "m3u", "mp3", "wav", "wma"),

    /**
     * Unsupported media type.
     */
    Unknown;

    private static final Set<String> ALL_SUPPORTED_EXTENSIONS = new HashSet<>();

    static
    {
        for (MediaType mediaType : values()) ALL_SUPPORTED_EXTENSIONS.addAll(mediaType.extensions);
    }

    private final Set<String> extensions;

    MediaType(String... extensions)
    {
        this.extensions = Set.of(extensions);
    }

    /**
     * @return The extensions associated with this media type
     * @since 3.5
     */
    @Nonnull
    public Collection<String> getExtensions()
    {
        return extensions;
    }

    /**
     * @return All supported media file extensions
     * @since 3.5
     */
    @Nonnull
    public static Collection<String> getAllSupportedExtensions()
    {
        return Collections.unmodifiableSet(ALL_SUPPORTED_EXTENSIONS);
    }

    /**
     * Get the media type associated with the given file extension.
     *
     * @param extension Extension to check
     * @return Associated media type, or {@link #Unknown} if no association is found for the given extension or
     *         if the given extension is {@code null}.
     * @since 3.5
     */
    @Nonnull
    public static MediaType getMediaTypeForExtension(@Nullable String extension)
    {
        if (extension == null) return Unknown;

        final String lcExtension = extension.toLowerCase();

        if (Audio.extensions.contains(lcExtension)) return Audio;
        if (Video.extensions.contains(lcExtension)) return Video;
        if (Image.extensions.contains(lcExtension)) return Image;
        return Unknown;
    }

    /**
     * Derive the media type from an instance of IMedia
     *
     * @param media the media
     * @return the MediaType
     * @since 2.37
     */
    @Nonnull
    public static MediaType getMediaType(IMedia media)
    {
        if (media instanceof IImage) return Image;
        if (media instanceof IVideo) return Video;
        if (media instanceof IAudio) return Audio;
        return Unknown;
    }
}
