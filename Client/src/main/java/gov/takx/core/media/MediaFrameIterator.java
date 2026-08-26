package gov.takx.core.media;

import gov.takx.api.media.IDecodingOptions;
import gov.takx.api.media.IMediaFrameIterator;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.StreamHandler;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;

/**
 * Iterate through all frames of a media resource
 *
 * @since 2.8
 */
public class MediaFrameIterator implements IMediaFrameIterator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_QUEUE_SIZE = (15 * 1024 * 1024);
    private final String mediaUrl;
    private final IDecodingOptions options;
    private final Map<Integer, StreamHandler> handlers = new ConcurrentHashMap<>();
    private final Set<StreamHandler> handlersWithFramesLeft = new CopyOnWriteArraySet<>();
    private final Map<StreamHandler, MediaFrame> frames = new ConcurrentHashMap<>();
    private final MediaIteratorThread thread;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private boolean initializedCorrectly;

    public MediaFrameIterator(String mediaUrl)
    {
        this(mediaUrl, new DecodingOptions());
    }

    MediaFrameIterator(String mediaUrl, IDecodingOptions options)
    {
        this.mediaUrl = mediaUrl;
        this.options = options;

        // Start thread to read through the media file
        this.thread = new MediaIteratorThread();
        thread.start();

        // Wait for individual decoding threads to be spun up before returning
        try
        {
            countDownLatch.await();
            initializedCorrectly = true;
        } catch (InterruptedException e)
        {
            initializedCorrectly = false;
        }
    }

    @Override
    public boolean hasNext()
    {
        if (!initializedCorrectly)
        {
            return false;
        }

        Set<StreamHandler> tempStreamHandlers = new HashSet<>(handlersWithFramesLeft);
        for (StreamHandler streamHandler : tempStreamHandlers)
        {
            if (!frames.containsKey(streamHandler))
            {
                MediaFrame frame = streamHandler.getNextFrame();
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
        if (frames.isEmpty())
        {
            return null;
        }

        // Look for the frame with smallest PTS
        StreamHandler bestStreamHandler = null;
        MediaFrame bestFrame = null;
        double bestTime = Double.MAX_VALUE;
        for (Map.Entry<StreamHandler, MediaFrame> entry : frames.entrySet())
        {
            if (bestFrame == null || entry.getValue().getPts() < bestTime)
            {
                bestStreamHandler = entry.getKey();
                bestFrame = entry.getValue();
                bestTime = bestFrame.getPts();
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

    String toHex(byte b)
    {
        return String.format("%02X", b);
    }

    void printByte(long location, BytePointer pointer)
    {

    }

    /**
     * Thread to read packets from the media and pass them to separate threads (one per stream) to be
     * decoded. The goal is to read the media file as quickly as possible.
     */
    private class MediaIteratorThread extends Thread
    {
        private final Set<Thread> threads = new HashSet<>();
        private volatile boolean shutdown = false;

        @Override
        public void run()
        {
            Thread.currentThread().setName("MediaIterator Read Packet");

            final AVFormatContext formatContext = avformat_alloc_context();

            // Attempt to open the stream
            int ret = -1;
            if ((ret = avformat_open_input(formatContext, mediaUrl, null, null)) < 0)
            {
                logger.error("Could not open URL: {} {}", mediaUrl, FfmpegUtils.formatError(ret));
                return;
            }

            // Retrieve stream information
            if ((ret = avformat_find_stream_info(formatContext, (PointerPointer) null)) < 0)
            {
                logger.error("Could not retrieve stream information: {}", FfmpegUtils.formatError(ret));
                avformat_close_input(formatContext);
                avformat_free_context(formatContext);
                return;
            }

            ExternalClock clock = new ExternalClock();

            // Create StreamHandlers to decode each stream
            handlers.putAll(StreamFactory.createAll(formatContext, clock, options));
            handlersWithFramesLeft.addAll(handlers.values());

            // Start decoding threads for each stream
            handlers.values().stream().forEach(h -> {
                Thread thread = new Thread(h);
                thread.setDaemon(true);
                thread.start();
                threads.add(thread);
            });

            countDownLatch.countDown();

            // Loop to read all packets and pass them to the handlers
            AVPacket packet = new AVPacket();
            while (true)
            {
                if (shutdown)
                {
                    handlers.values().stream().forEach(StreamHandler::abort);
                    break;
                }

                // Ensure we have enough space in our buffers to read another packet
                int bufferedPackets = handlers.values().stream().mapToInt(StreamHandler::getPacketQueueSize).sum();
                if (bufferedPackets > MAX_QUEUE_SIZE)
                {
                    shortWait();
                    continue;
                }

                // Read a packet from the file/stream
                if ((ret = av_read_frame(formatContext, packet)) < 0)
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
                        handlers.values().stream().forEach(StreamHandler::abort);
                    }

                    // Signal to handlers that we are at EOF, causing their threads to exit
                    for (StreamHandler streamHandler : handlers.values())
                    {
                        streamHandler.queueEoFPacket();
                    }
                    break;
                }

                // If there is a handler for the stream, pass the packet to it
                StreamHandler streamHandler = handlers.get(packet.stream_index());
                if (streamHandler != null)
                {
                    streamHandler.queuePacket(packet);
                }
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Media iterator thread waiting for handler threads to finish...");
            }

            // Wait for our handler threads
            waitForHandlers();

            threads.clear();
            handlers.clear();

            avformat_close_input(formatContext);
            avformat_free_context(formatContext);

            if (logger.isDebugEnabled())
            {
                logger.debug("Media iterator thread finished");
            }
        }

        private void waitForHandlers()
        {
            for (Thread thread : threads)
            {
                try
                {
                    thread.join();
                } catch (InterruptedException e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Interrupted while waiting for handler threads");
                    }
                }
            }
        }

        private void shortWait()
        {
            try
            {
                Thread.sleep(5);
            } catch (InterruptedException e)
            {
                // don't care
            }
        }
    }
}
