package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.annotation.Nullable;
import org.bytedeco.javacpp.BytePointer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.bytedeco.ffmpeg.global.avutil.av_get_media_type_string;

/**
 * Common utilities for media package
 *
 * @since 5.1.0 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public final class Utilities
{
    /**
     * Translate a member of {@code AVMediaType} C enum to a string name.
     *
     * @param avMediaType The media type identifier.
     * @return The media type name corresponding the media type identifier.
     */
    @Nullable
    public static String getMediaTypeString(int avMediaType)
    {
        try (BytePointer typeNamePointer = av_get_media_type_string(avMediaType))
        {
            return typeNamePointer != null ? typeNamePointer.getString() : null;
        }
    }

    /**
     * Convert 32-bit integer codec tag to a four CC string.
     *
     * @param tag The codec tag
     * @return 4-character string of the four CC
     */
    public static String tagToFourCc(int tag)
    {
        return new String(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(tag).array(), StandardCharsets.US_ASCII);
    }

    private Utilities()
    {
    }
}
