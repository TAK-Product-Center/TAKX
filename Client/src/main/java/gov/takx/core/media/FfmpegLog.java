package gov.takx.core.media;

import org.bytedeco.ffmpeg.avutil.LogCallback;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_DEBUG;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_FATAL;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_PANIC;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_WARNING;

/**
 * Simple class for redirecting all the Ffmpeg logging to application log file.
 * Used in conjunction with {@link avutil#setLogCallback(LogCallback)}.
 *
 */
public class FfmpegLog extends LogCallback
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final FfmpegLog instance = new FfmpegLog();

    private static final Map<String, Integer> errorCounts = new ConcurrentHashMap<>();
    private static final int SUPPRESS_LIMIT = 10;

    public static FfmpegLog getInstance()
    {
        return instance;
    }

    @Override
    public void call(int level, BytePointer msg)
    {
        final String message = msg.getString().trim();

        // Filter out messages that have occurred too many times
        // Some messages have a common prefix, and some non-unique portions at the end, e.g.,:
        //   [mpjpeg @ 0000022163229cc0] Expected boundary '--' not found, instead found a line of 69 bytes
        // So, somewhat arbitrarily, lets just use the first 40 characters as the key.

        final String messagePrefix = message.substring(0, Math.min(message.length(), 40));
        final int count = errorCounts.merge(messagePrefix, 1, Integer::sum);

        if (count > SUPPRESS_LIMIT)
        {
            logger.trace("Suppressed: {}", message);
            return;
        }

        switch (adjust(level, message))
        {
            case AV_LOG_PANIC, AV_LOG_FATAL, AV_LOG_ERROR -> logger.error(message);
            case AV_LOG_WARNING -> logger.warn(message);
            case AV_LOG_INFO -> logger.info(message);
            default -> logger.debug(message);
        }

        if (count == SUPPRESS_LIMIT) logger.info("Suppressing further reports of this error");
    }

    /**
     * Adjust the FFmpeg logging level based on heuristics
     *
     * @param origLevel The original FFmpeg log level
     * @param message   The message string
     * @return The adjusted log level
     */
    private int adjust(int origLevel, String message)
    {
        // H.264 codec is ridiculously verbose
        return message.startsWith("[h264") ? AV_LOG_DEBUG : origLevel;
    }
}
