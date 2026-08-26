package gov.takx.core.media;

import jakarta.enterprise.context.ApplicationScoped;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_TIME_BASE;

/**
 * Utility to analyze the length of an audio/video clip.
 *
 * @since 3.0
 */
@ApplicationScoped
public class MediaLengthAnalyzer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Open a mediaUrl and find the duration of the clip, in seconds. If the URL is not a local file then empty is
     * returned
     *
     * @param mediaUrl The url of the file to analyze
     * @return duration of the media, in seconds
     */
    public Optional<Long> analyzeDuration(String mediaUrl)
    {
        // Only analyze if this is a file; network streams should return null
        if (new File(mediaUrl).exists())
        {
            final AVFormatContext formatContext = avformat_alloc_context();

            try
            {
                // Attempt to open the stream
                int ret;
                if ((ret = avformat_open_input(formatContext, mediaUrl, null, null)) < 0)
                {
                    logger.error("Could not open URL: {} {}", mediaUrl, FfmpegUtils.formatError(ret));
                } else if ((ret = avformat_find_stream_info(formatContext, (PointerPointer<?>) null)) < 0)
                {
                    logger.error("Could not retrieve stream information: {}", FfmpegUtils.formatError(ret));
                } else if (formatContext.duration() != AV_NOPTS_VALUE)
                {
                    return Optional.of(formatContext.duration() / AV_TIME_BASE);
                }
            } catch (Exception e)
            {
                logger.error("Failed to extract duration from stream: {}", mediaUrl, e);
            } finally
            {
                avformat_close_input(formatContext);
                avformat_free_context(formatContext);
            }
        }
        return Optional.empty();
    }
}
