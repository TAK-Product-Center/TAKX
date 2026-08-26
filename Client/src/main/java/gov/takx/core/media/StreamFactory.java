package gov.takx.core.media;

import gov.takx.api.media.IDecodingOptions;
import gov.takx.core.media.decode.AudioDecoder;
import gov.takx.core.media.decode.DataDecoder;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.IDecoder;
import gov.takx.core.media.decode.StreamHandler;
import gov.takx.core.media.decode.VideoDecoder;
import gov.takx.core.media.freja.GeoTriggerDecoder;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEG;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_ATTACHMENT;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_NB;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_UNKNOWN;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;

/**
 * Factory to create handlers for media sub-streams
 *
 * @since 2.8
 */
public class StreamFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Create the appropriate handler to decode an individual stream
     *
     * @param formatContext The format context
     * @param stream        The stream
     * @param masterClock   Master clock
     * @param options       Decoding options
     * @return The stream handler, or empty if there is no appropriate decoder
     */
    public static Optional<StreamHandler> create(AVFormatContext formatContext, AVStream stream, ExternalClock masterClock, IDecodingOptions options)
    {
        final int codecType = stream.codecpar().codec_type();
        final int streamIndex = stream.index();

        switch (codecType)
        {
            case AVMEDIA_TYPE_VIDEO:
            {
                logger.debug("Found VIDEO stream at {}", streamIndex);

                // If this is an AVI and video is encoded as MJPG, check to see if Freja metadata is present
                boolean isAvi = "avi".equals(formatContext.iformat().name().getString());
                boolean isMjpg = stream.codecpar().codec_id() == AV_CODEC_ID_MJPEG;

                final IDecoder decoder;
                if (isAvi && isMjpg && GeoTriggerDecoder.hasGeoTriggerMetadata(formatContext.filename().getString()))
                {
                    logger.debug("Freja AVI file detected, will pull out metadata, using GeoTriggerDecoder");
                    decoder = new GeoTriggerDecoder(stream);
                } else
                {
                    logger.debug("Standard video format detected, using VideoDecoder");
                    decoder = new VideoDecoder(stream);
                }

                if (decoder.isValid())
                {
                    decoder.setDecodingEnabled(options.isDecodeVideo());
                    return Optional.of(new StreamHandler(formatContext, stream, decoder, masterClock, 4));
                } else
                {
                    logger.warn("Could not decode video stream at index {}", streamIndex);
                }
                break;
            }
            case AVMEDIA_TYPE_AUDIO:
            {
                logger.debug("Found AUDIO stream at {}", streamIndex);
                IDecoder decoder = new AudioDecoder(stream);

                if (decoder.isValid())
                {
                    decoder.setDecodingEnabled(options.isDecodeAudio());
                    return Optional.of(new StreamHandler(formatContext, stream, decoder, masterClock, 9));
                } else
                {
                    logger.warn("Could not decode audio stream at index {}", streamIndex);
                }
                break;
            }
            case AVMEDIA_TYPE_DATA:
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Found DATA stream at {}", streamIndex);
                    logger.debug("Fourcc {}", DataDecoder.toAscii(stream.codecpar().codec_tag()));
                }
                IDecoder decoder = new DataDecoder(stream);

                if (decoder.isValid())
                {
                    decoder.setDecodingEnabled(options.isDecodeData());
                    return Optional.of(new StreamHandler(formatContext, stream, decoder, masterClock, 3));
                } else
                {
                    logger.warn("Could not decode data stream at index {}", streamIndex);
                }
                break;
            }
            case AVMEDIA_TYPE_SUBTITLE:
            {
                logger.debug("Found SUBTITLE stream at {}", streamIndex);
                break;
            }
            case AVMEDIA_TYPE_ATTACHMENT:
                logger.debug("Found ATTACHMENT stream with index {}; decoding currently unsupported, skipping...", streamIndex);
                break;
            case AVMEDIA_TYPE_NB:
                logger.debug("Found NB stream with index {}; decoding currently unsupported, skipping...", streamIndex);
                break;
            case AVMEDIA_TYPE_UNKNOWN:
                logger.debug("Found UNKNOWN stream with index {}; decoding currently unsupported, skipping...", streamIndex);
                break;
            default:
                logger.error("No decoder for stream {}", streamIndex);
        }
        return Optional.empty();
    }

    /**
     * Create appropriate handlers for each of the streams within the format context
     *
     * @param formatContext The format context
     * @param masterClock   The master clock
     * @param options       Decoding options
     * @return Map of handlers, one per stream
     */
    public static Map<Integer, StreamHandler> createAll(AVFormatContext formatContext, ExternalClock masterClock, IDecodingOptions options)
    {
        Map<Integer, StreamHandler> handlers = new HashMap<>();
        for (int i = 0; i < formatContext.nb_streams(); ++i)
        {
            final int index = i;
            AVStream stream = formatContext.streams(i);
            create(formatContext, stream, masterClock, options).ifPresent(h -> handlers.put(index, h));
        }
        return handlers;
    }

    /**
     * Create appropriate handlers for each of the streams using default decoding options
     *
     * @param formatContext The format context
     * @param masterClock   The master clock
     * @return Map of handlers, one per stream
     */
    public static Map<Integer, StreamHandler> createAll(AVFormatContext formatContext, ExternalClock masterClock)
    {
        return createAll(formatContext, masterClock, new DecodingOptions());
    }
}
