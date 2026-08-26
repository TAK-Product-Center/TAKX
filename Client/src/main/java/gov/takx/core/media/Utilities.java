package gov.takx.core.media;

import org.bytedeco.javacpp.BytePointer;

import static org.bytedeco.ffmpeg.global.avutil.av_get_media_type_string;

/**
 * Common utilities for media package
 *
 * @since 5.1.0
 */
public final class Utilities
{
    private Utilities()
    {
        // No instantiation of this class
    }

    /**
     * Translate a member of {@code AVMediaType} C enum to a string name.
     *
     * @param avMediaType The media type identifier.
     * @return The media type name corresponding the media type identifier.
     */
    public static String getMediaTypeString(int avMediaType)
    {
        try (BytePointer typeNamePointer = av_get_media_type_string(avMediaType))
        {
            return typeNamePointer.getString();
        }
    }
}
