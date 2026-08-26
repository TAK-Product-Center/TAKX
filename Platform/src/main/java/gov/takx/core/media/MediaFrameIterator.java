package gov.takx.core.media;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.media.IDecodingOptions;
import gov.takx.api.media.IMediaFrameIterator;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.StreamHandler;
import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;

/**
 * An iterator over all frames of a media resource.  Generally used to rapidly read and process media frames.  No
 * delay for presentation timestamps is performed.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class MediaFrameIterator implements IMediaFrameIterator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_QUEUE_SIZE = 512 * 1024 * 1024; // Limit on memory used in our frame queues
    private final String mediaUrl;
    private final IDecodingOptions options;
    private final Map<Integer, StreamHandler> handlers = new ConcurrentHashMap<>();
    private final Set<StreamHandler> handlersWithFramesLeft = new CopyOnWriteArraySet<>();
    private final Map<StreamHandler, MediaFrame> frames = new ConcurrentHashMap<>();
    private final MediaIteratorThread thread = new MediaIteratorThread();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean initializedCorrectly;

    /**
     * Constructor using default {@link IDecodingOptions}.
     *
     * @param mediaUrl URL of media to process
     */
    public MediaFrameIterator(@Nonnull String mediaUrl)
    {
        this(mediaUrl, new DecodingOptions());
    }

    /**
     * @param mediaUrl URL of media to process
     * @param options Decoding options
     */
    public MediaFrameIterator(@Nonnull String mediaUrl, @Nonnull IDecodingOptions options)
    {
        this.mediaUrl = mediaUrl;
        this.options = options;

        thread.start(); // Start thread to read through the media file

        // Wait for individual decoding threads to be spun up before returning
        try
        {
            countDownLatch.await();
            initializedCorrectly = true;
        } catch (InterruptedException ignored)
        {
            initializedCorrectly = false;
        }
    }

    @Override
    public boolean hasNext()
    {
        if (!initializedCorrectly) return false;

        for (var streamHandler : new HashSet<>(handlersWithFramesLeft))
        {
            if (!frames.containsKey(streamHandler))
            {
                var frame = streamHandler.getNextFrame();
                if (frame == null)
                {
                    handlersWithFramesLeft.remove(streamHandler);
                } else
                {
                    frames.put(streamHandler, frame);
                }
            }
        }

        return !frames.isEmpty();
    }

    @Override
    public MediaFrame next()
    {
        if (frames.isEmpty()) throw new NoSuchElementException();

        // Look for the frame with the smallest PTS
        StreamHandler bestStreamHandler = null;
        MediaFrame bestFrame = null;
        double bestTime = Double.MAX_VALUE;
        for (var entry : frames.entrySet())
        {
            if (bestFrame == null || entry.getValue().getPresentationTimeStamp() < bestTime)
            {
                bestStreamHandler = entry.getKey();
                bestFrame = entry.getValue();
                bestTime = bestFrame.getPresentationTimeStamp();
            }
        }

        frames.remove(bestStreamHandler);
        return bestFrame;
    }

    @Override
    public void close() throws Exception
    {
        thread.shutdown = true;
    }

    /**
     * Thread to read packets from the media and pass them to separate threads (one per stream) to be
     * decoded. The goal is to read the media file as quickly as possible.
     */
    private class MediaIteratorThread extends Thread
    {
        private final Set<Thread> threads = new HashSet<>();
        private volatile boolean shutdown;

        @Override
        public void run()
        {
            currentThread().setName("MediaIterator");

            final AVFormatContext formatContext = avformat_alloc_context();

            // Attempt to open the stream
            int ret = avformat_open_input(formatContext, mediaUrl, null, null);
            if (ret < 0)
            {
                logger.error("Could not open URL: {} {}", mediaUrl, FfmpegUtils.formatError(ret));
                avformat_free_context(formatContext);
                return;
            }

            // Retrieve stream information
            ret = avformat_find_stream_info(formatContext, (PointerPointer<AVDictionary>) null);
            if (ret < 0)
            {
                logger.error("Could not retrieve stream information: {}", FfmpegUtils.formatError(ret));
                avformat_close_input(formatContext);
                avformat_free_context(formatContext);
                return;
            }

            final var clock = new ExternalClock();

            // Create StreamHandlers to decode each stream
            handlers.putAll(StreamFactory.createAll(formatContext, clock, options));
            handlersWithFramesLeft.addAll(handlers.values());

            // Start the decoding threads for each stream
            handlers.values().forEach(h -> {
                Thread thread = new Thread(h);
                thread.setName("media decoder #" + h.getStreamIndex() + " for " + mediaUrl);
                thread.setDaemon(true);
                thread.start();
                threads.add(thread);
            });

            countDownLatch.countDown(); // Signal all handler threads have been started

            // Loop to read all packets and pass them to the handlers
            final var packet = new AVPacket();
            while (true)
            {
                if (shutdown)
                {
                    handlers.values().forEach(StreamHandler::abort);
                    break;
                }

                // Ensure we have enough space in our buffers to read another packet
                long bufferedPackets = handlers.values().stream().mapToLong(StreamHandler::getPacketQueueSizeInBytes).sum();
                if (bufferedPackets > MAX_QUEUE_SIZE)
                {
                    shortWait();
                    continue;
                }

                // Read a packet from the file/stream
                ret = av_read_frame(formatContext, packet);
                if (ret < 0)
                {
                    // ret < 0 means EOF or error
                    if (ret == -11) // AVERROR(EAGAIN) is normal - loop and retry
                    {
                        shortWait();
                        continue;
                    }

                    if (ret != AVERROR_EOF)
                    {
                        logger.error("av_read_frame returned error {}", FfmpegUtils.formatError(ret));
                        handlers.values().forEach(StreamHandler::abort);
                    }

                    // Signal to handlers that we are at EOF, causing their threads to exit
                    handlers.values().forEach(StreamHandler::queueEoFPacket);
                    break;
                }

                // If there is a handler for the stream, pass the packet to it
                StreamHandler streamHandler = handlers.get(packet.stream_index());
                if (streamHandler != null) streamHandler.queuePacket(packet);

                av_packet_unref(packet);
            }

            logger.debug("Media iterator thread waiting for handler threads to finish...");

            // Wait for our handler threads
            waitForHandlers();

            threads.clear();
            handlers.clear();

            avformat_close_input(formatContext);
            avformat_free_context(formatContext);

            logger.debug("Media iterator thread finished");
        }

        private void waitForHandlers()
        {
            for (var thread : threads)
            {
                try
                {
                    thread.join();
                } catch (InterruptedException e)
                {
                    logger.debug("Interrupted while waiting for handler threads");
                }
            }
        }

        private void shortWait()
        {
            Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(5));
        }
    }
}
