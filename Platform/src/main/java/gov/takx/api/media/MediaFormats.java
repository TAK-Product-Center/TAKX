package gov.takx.api.media;

import java.util.Arrays;
import java.util.List;

/**
 * Media plugin constants
 *
 * @since 2.8
 */
public class MediaFormats
{
    public static final String[] IMAGE_FORMATS = MediaType.Image.getExtensions().toArray(new String[0]);
    public static final List<String> IMAGE_FORMATS_LIST = Arrays.asList(IMAGE_FORMATS);

    public static final String[] AUDIO_FORMATS = MediaType.Audio.getExtensions().toArray(new String[0]);
    public static final List<String> AUDIO_FORMATS_LIST = Arrays.asList(AUDIO_FORMATS);

    public static final String[] VIDEO_FORMATS = MediaType.Video.getExtensions().toArray(new String[0]);
    public static final List<String> VIDEO_FORMATS_LIST = Arrays.asList(VIDEO_FORMATS);

    // Convenience array of all formats
    public static final String[] ALL_FORMATS = MediaType.getAllSupportedExtensions().toArray(new String[0]);
}
