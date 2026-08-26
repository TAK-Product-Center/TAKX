package gov.takx.core.media;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IDecodingOptions;
import gov.takx.core.media.decode.AudioDecoder;
import gov.takx.core.media.decode.DataDecoder;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.StreamHandler;
import gov.takx.core.media.decode.VideoDecoder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_ATTACHMENT;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_NB;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_UNKNOWN;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

/**
 * Factory to create handlers for media sub-streams.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public final class StreamFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Create appropriate handlers for each of the streams using default decoding options.
     *
     * @param formatContext The format context
     * @param masterClock   The master clock
     * @return Map of handlers, one per stream, keyed by stream index
     */
    public static Map<Integer, StreamHandler> createAll(@Nonnull AVFormatContext formatContext, @Nonnull ExternalClock masterClock)
    {
        return createAll(formatContext, masterClock, new DecodingOptions());
    }

    /**
     * Create appropriate handlers for each of the streams within the format context.
     *
     * @param formatContext The format context
     * @param masterClock   The master clock
     * @param options       Decoding options
     * @return Map of handlers, one per stream, keyed by stream index
     */
    public static Map<Integer, StreamHandler> createAll(@Nonnull AVFormatContext formatContext, @Nonnull ExternalClock masterClock, @Nonnull IDecodingOptions options)
    {
        Map<Integer, StreamHandler> handlers = new HashMap<>();
        for (int i = 0; i < formatContext.nb_streams(); ++i)
        {
            final int index = i;
            create(formatContext, formatContext.streams(i), masterClock, options).ifPresent(h -> handlers.put(index, h));
        }
        return handlers;
    }

    /**
     * Create the appropriate handler to decode an individual stream.
     *
     * @param formatContext The format context
     * @param stream        The stream
     * @param masterClock   Master clock
     * @param options       Decoding options
     * @return The stream handler, or empty if there is no appropriate decoder
     */
    public static Optional<StreamHandler> create(@Nonnull AVFormatContext formatContext, @Nonnull AVStream stream,
                                                 @Nonnull ExternalClock masterClock, @Nonnull IDecodingOptions options)
    {
        final int codecType = stream.codecpar().codec_type();
        final int streamIndex = stream.index();

        return switch (codecType)
        {
            case AVMEDIA_TYPE_VIDEO -> Optional.ofNullable(createVideoStreamHandler(formatContext, stream, masterClock, options, streamIndex));
            case AVMEDIA_TYPE_AUDIO -> Optional.ofNullable(createAudioStreamHandler(formatContext, stream, masterClock, options, streamIndex));
            case AVMEDIA_TYPE_DATA -> Optional.of(createDataStreamHandler(formatContext, stream, masterClock, options, streamIndex));
            case AVMEDIA_TYPE_SUBTITLE, AVMEDIA_TYPE_ATTACHMENT, AVMEDIA_TYPE_NB, AVMEDIA_TYPE_UNKNOWN ->
            {
                logger.debug("Found {} stream at {}; ignoring", Utilities.getMediaTypeString(codecType), streamIndex);
                yield Optional.empty();
            }
            default ->
            {
                logger.error("Unknown codec type {} for stream {}", Utilities.getMediaTypeString(codecType), streamIndex);
                yield Optional.empty();
            }
        };
    }

    @Nullable
    private static StreamHandler createVideoStreamHandler(@Nonnull AVFormatContext formatContext, @Nonnull AVStream stream,
                                                          @Nonnull ExternalClock masterClock, @Nonnull IDecodingOptions options, int streamIndex)
    {
        logger.debug("Found VIDEO stream at {}", streamIndex);

        final var decoder = new VideoDecoder(stream, options);
        if (decoder.isValid())
        {
            return new StreamHandler(formatContext, stream, decoder, masterClock, 4);
        } else
        {
            logger.warn("Could not create decoder for video stream at index {}", streamIndex);
        }
        return null;
    }

    @Nullable
    private static StreamHandler createAudioStreamHandler(@Nonnull AVFormatContext formatContext, @Nonnull AVStream stream,
                                                          @Nonnull ExternalClock masterClock, @Nonnull IDecodingOptions options, int streamIndex)
    {
        logger.debug("Found AUDIO stream at {}", streamIndex);
        final var decoder = new AudioDecoder(stream, options);

        if (decoder.isValid())
        {
            return new StreamHandler(formatContext, stream, decoder, masterClock, 9);
        } else
        {
            logger.warn("Could not create audio decoder for stream at index {}", streamIndex);
        }
        return null;
    }

    @Nonnull
    private static StreamHandler createDataStreamHandler(@Nonnull AVFormatContext formatContext, @Nonnull AVStream stream,
                                                         @Nonnull ExternalClock masterClock, @Nonnull IDecodingOptions options, int streamIndex)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Found DATA stream at {}", streamIndex);
            logger.debug("Fourcc {}", Utilities.tagToFourCc(stream.codecpar().codec_tag()));
        }

        return new StreamHandler(formatContext, stream, new DataDecoder(stream, options), masterClock, 4);
    }

    private StreamFactory()
    {
    }
}
