package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

import java.util.ArrayList;
import java.util.List;

/**
 * All Media and properties that fire changes when modified
 *
 * @since 2.15
 */
@Experimental
public abstract class MediaProperties
{
    public static class Common
    {
        public static final String NAME = "mediaNAME";
        /** @since 5.0 */
        public static final String SOURCE_URI = "mediaSourceUri";
        /** @since 5.0 */
        public static final String OPTIONS = "mediaOPTIONS";
        /** @since 5.0 */
        public static final String FORMAT = "mediaFORMAT";
        public static final String INFO = "mediaINFO";
        public static final String THUMBNAIL_URL = "mediaTHUMBNAIL_URL";
        public static final String LAST_MODIFIED = "mediaLAST_MODIFIED";
        public static final String FILE_SIZE = "mediaFILE_SIZE";
        public static final String LOCATION = "mediaLOCATION";
        public static final String TIME = "mediaTIME";
        public static final String META_DATA = "mediaMETA_DATA";
        /** @since 2.37 */
        public static final String COLOR = "mediaCOLOR";
        public static final List<String> ALL = List.of(NAME, SOURCE_URI, INFO, THUMBNAIL_URL,
                LAST_MODIFIED, FILE_SIZE, LOCATION, TIME, META_DATA, OPTIONS, COLOR, FORMAT);
    }

    public static class Audio
    {
        /** @since 5.1.0 */
        public static final String CAPTURE = "mediaAUDIO_CAPTURE";
        public static final String DURATION = "mediaAUDIO_DURATION";
        public static final List<String> ALL = List.of(DURATION, CAPTURE);
    }

    public static class Video
    {
        /** @since 5.1.0 */
        public static final String CAPTURE = "mediaVIDEO_CAPTURE";
        public static final String DURATION = "mediaVIDEO_DURATION";
        public static final List<String> ALL = List.of(DURATION, CAPTURE);
    }

    public static class Image
    {
        public static final String WIDTH = "mediaIMAGE_WIDTH";
        public static final String HEIGHT = "mediaIMAGE_HEIGHT";
        public static final List<String> ALL = List.of(WIDTH, HEIGHT);
    }

    public static final List<String> ALL = new ArrayList<>();

    static
    {
        ALL.addAll(Common.ALL);
        ALL.addAll(Audio.ALL);
        ALL.addAll(Video.ALL);
        ALL.addAll(Image.ALL);
    }
}
