package gov.takx.core.media;

import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

/**
 * Utility methods for FFmpeg
 *
 * @since 2.8
 */
public class FfmpegUtils
{
    private FfmpegUtils()
    {

    }

    public static String formatError(int error)
    {
        byte[] bytes = new byte[256];

        // av_strerror leaves 0 bytes at the end of the buffer, so we need to strip out any unprintable chars
        return av_strerror(error, bytes, 256) < 0
                ? "(" + error + ") Unknown error"
                : "(" + error + ") " + new String(bytes).replaceAll("[^ -~]+", "");
    }
}
