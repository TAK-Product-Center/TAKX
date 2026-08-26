package gov.takx.core.media;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IPlayable;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.HandlerConfiguration;
import gov.takx.core.media.decode.StreamHandler;
import gov.takx.dataservices.persist.Audio;
import gov.takx.dataservices.persist.Video;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Container;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_seek_file;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_DEBUG;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;
import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_TRACE;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_TIME_BASE;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

/**
 * Thread to manage decoding of media. Decoding of sub-streams is farmed off to handler threads.
 *
 * @since 2.8
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MediaProducer implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_QUEUE_SIZE = 32 * 1024; // Limit on frames queued for playback
    private static final Map<Integer, Integer> errorCounts = new ConcurrentHashMap<>();
    private static final int SUPPRESS_LIMIT = 10;

    @Inject
    private IMediaManager mediaManager;

    private final IMedia media;
    private final String mediaUrl;
    private final List<MediaProducerListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, StreamHandler> handlers = new ConcurrentHashMap<>();
    // Listeners for packets as they are read from the input streams @since 5.1.0
    private final Map<String, PacketListener> packetListeners = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<MediaCommand> commandQueue = new LinkedBlockingQueue<>();
    private final List<Thread> handlerThreads = new ArrayList<>();
    private final ExternalClock masterClock = new ExternalClock();

    // We may stop for either of two reasons.
    // We reached the eof on the incoming stream and the handler queues are empty
    private volatile boolean eof;
    // Or an external event that closes the player
    private volatile boolean shutdown;

    private Thread readThread;
    private final Integer initialMaxWidth;
    private final Integer initialMaxHeight;

    private Container container;

    /**
     * Thread for querying each handler for a frame to display.
     * Terminate on explicit shutdown, or on EOF after queues are empty.
     */
    private final Thread refreshThread = new Thread(() -> {
        // Have we found any frames on this loop iteration?
        boolean empty = false;
        while (!shutdown && !(eof && empty))
        {
            empty = true;
            for (StreamHandler streamHandler : handlers.values())
            {
                empty = empty && streamHandler.isEmpty();
                streamHandler.getFrameIfReady()
                        .ifPresent(mediaFrame -> listeners.forEach(l -> l.onMediaFrame(mediaFrame)));
            }
            shortWait();
        }
    });

    /**
     * Construct a media producer for the given media with max height and width.
     *
     * @param media            The media to be displayed.
     * @param mediaUrl         The source for the media, file: converted to a file path, other adjusted as needed.
     * @param initialMaxWidth  The max width for the media display.
     * @param initialMaxHeight The max height for the media display.
     */
    public MediaProducer(@Nonnull IMedia media, @Nonnull String mediaUrl, int initialMaxWidth, int initialMaxHeight)
    {
        this.media = media;
        this.mediaUrl = mediaUrl;
        this.initialMaxWidth = initialMaxWidth;
        this.initialMaxHeight = initialMaxHeight;
    }

    /**
     * Construct a media producer for the given media with a passed in parent container object. The container is passed
     * in because at the time the MediaProducer is created the container does not have a set size. The container size
     * will get set after the video stream is initialized and added to the container. At that point the MediaProducer
     * can set the proper max image size so that the video can be scaled to properly fix inside the container.
     *
     *
     * @param media    The media to be displayed.
     * @param mediaUrl The source for the media, file: converted to a file path, other adjusted as needed.
     * @param container {@link Container} component that contains this media player
     */
    public MediaProducer(@Nonnull IMedia media, @Nonnull String mediaUrl, @Nullable Container container)
    {
        this.media = media;
        this.mediaUrl = mediaUrl;
        initialMaxWidth = null;
        initialMaxHeight = null;
        this.container = container;
    }

    public void addMediaProducerListener(MediaProducerListener listener)
    {
        listeners.add(listener);
    }

    public void removeMediaProducerListener(MediaProducerListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Add a listener to packets read from an input stream.
     *
     * @param key      The identifier for the listener.
     * @param listener Receives packets read from an input stream.
     * @since 5.1.0
     */
    public void addPacketListener(@Nonnull String key, @Nonnull PacketListener listener)
    {
        packetListeners.put(key, listener);
    }

    /**
     * Remove a packet listener.
     *
     * @param key The identifier for the packet listener.
     * @return The removed {@code PacketListener}, or {@code null} if there is no packet listener for the key.
     * @since 5.1.0
     */
    public PacketListener removePacketListener(@Nonnull String key)
    {
        return packetListeners.remove(key);
    }

    public void onCommand(@Nonnull MediaCommand command)
    {
        logger.debug("onCommand: {}", command);
        commandQueue.offer(command);
    }

    public void setMaxSize(int width, int height)
    {
        final HandlerConfiguration sc = new HandlerConfiguration();
        sc.setMaxImageOutputSize(width, height);
        handlers.values().forEach(h -> h.configure(sc));
    }

    public boolean isPlaying()
    {
        return !masterClock.isPaused();
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("Media Producer");

        // Keep a handle on this thread so that it can be correctly interrupted if shutdown() is called
        // while the playback is paused.
        readThread = Thread.currentThread();

        // Propagate the log level into the av library
        av_log_set_level(logger.isTraceEnabled() ? AV_LOG_TRACE : logger.isDebugEnabled() ? AV_LOG_DEBUG : AV_LOG_ERROR);

        final AVFormatContext formatContext = avformat_alloc_context();
        String formatName = media.getFormat();
        AVInputFormat inputFormat = formatName == null ? null : avformat.av_find_input_format(formatName);
        final AVDictionary options = new AVDictionary();
        Map<String, String> decodingOptions = media.getOptions();
        if (decodingOptions != null)
        {
            decodingOptions.forEach((k, v) -> av_dict_set(options, k, v, 0));
        }

        if (media instanceof Video || media instanceof Audio)
        {
            if (StringUtils.startsWithAny(mediaUrl.toLowerCase(), List.of("https", "udp", "rtsp")))
            {
                av_dict_set(options, "fflags", "nobuffer", 0);
            }
        }

        // Attempt to open the stream
        int ret;
        if ((ret = avformat_open_input(formatContext, mediaUrl, inputFormat, options)) < 0)
        {
            String message = String.format("Could not open URL: %s with format: %s, %s", mediaUrl, formatName, FfmpegUtils.formatError(ret));
            logger.error(message);
            AlwaysOnTopJOptionPane.showErrorDialog(message, "Error opening media");
            return;
        }

        av_dict_free(options);

        // Retrieve stream information
        formatContext.max_analyze_duration(30 * AV_TIME_BASE);
        if ((ret = avformat_find_stream_info(formatContext, (PointerPointer<?>) null)) < 0)
        {
            String message = String.format("Could not retrieve stream information: %s.", FfmpegUtils.formatError(ret));
            logger.error(message);
            AlwaysOnTopJOptionPane.showErrorDialog(message, "Error opening media");
            avformat_close_input(formatContext);
            avformat_free_context(formatContext);
            return;
        }

        // Create handlers for each stream, which will be in charge of decoding and deciding when
        // to display each frame
        masterClock.reset();
        masterClock.setPaused(true);
        handlers.putAll(StreamFactory.createAll(formatContext, masterClock));

        // No streams were successfully created, abort
        if (handlers.isEmpty())
        {
            String message = "Could not find readable video, audio or data stream, aborting";
            logger.error(message);
            AlwaysOnTopJOptionPane.showErrorDialog(message, "No playable streams");
            avformat_close_input(formatContext);
            avformat_free_context(formatContext);
            return;
        }

        // Start decoding threads for each stream
        handlers.values().forEach(h -> {
            final HandlerConfiguration sc = new HandlerConfiguration();

            // If we have the parent container, set the image max size to the container's bounds otherwise use the initialMaxWidth
            // and initialMaxHeight if they have been set.
            if (container != null)
            {
                final int width = container.getWidth();
                final int height = container.getHeight();
                if (width > 0 && height > 0) sc.setMaxImageOutputSize(width, height);
            } else if (initialMaxWidth != null && initialMaxHeight != null)
            {
                sc.setMaxImageOutputSize(initialMaxWidth, initialMaxHeight);
            }

            h.configure(sc);

            Thread thread = new Thread(h);
            thread.setDaemon(true);
            thread.setName("media decoder #" + handlerThreads.size() + " for " + media.getName());
            thread.start();
            handlerThreads.add(thread);
        });

        // Create a thread to ask the handlers if it is time to display their decoded frames
        refreshThread.setName("media player for " + media.getName());
        refreshThread.setDaemon(true);
        refreshThread.start();

        final AVPacket packet = new AVPacket();
        boolean firstEof = true;
        while (!shutdown && refreshThread.isAlive())
        {
            // Handle pause/play/seek
            handleCommands(formatContext);

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
                if (ret == AVERROR_EOF)
                {
                    // Has the underlying AVIOContext hit EOF
                    if (formatContext.pb().eof_reached() == 1)
                    {
                        eof = true;
                        if (firstEof)
                        {
                            handlers.values().forEach(StreamHandler::queueEoFPacket);
                            firstEof = false;
                        } else
                        {
                            // Once EOF is reached, we can slow down since we're now just handling user commands
                            shortWait(250);
                        }
                    } else
                    {
                        shortWait(10);
                    }
                    continue;
                } else if (ret == -11) // AVERROR(EAGAIN), just need to wait a moment and try again
                {
                    shortWait();
                } else
                {
                    final int count = errorCounts.merge(ret, 1, Integer::sum);
                    if (count < SUPPRESS_LIMIT)
                    {
                        logger.error("av_read_frame returned an error {}", FfmpegUtils.formatError(ret));
                    } else if (count == SUPPRESS_LIMIT)
                    {
                        logger.info("Suppressing further reports of this error");
                    }
                }
            }

            // If there is a handler for the stream, pass the packet to it
            var streamHandler = handlers.get(packet.stream_index());
            if (streamHandler != null) streamHandler.queuePacket(packet);

            if (!packetListeners.isEmpty()) packetListeners.values().forEach(listener -> listener.onPacket(formatContext, packet));

            av_packet_unref(packet);
        }

        commandQueue.clear();
        readThread = null;

        handlers.values().forEach(StreamHandler::abort);
        packetListeners.values().forEach(PacketListener::stop);

        avformat_close_input(formatContext);
        avformat_free_context(formatContext);

        logger.debug("MediaProducer thread exiting");
    }

    public void shutdown()
    {
        shutdown = true;
        if (readThread != null) readThread.interrupt();
        handlerThreads.forEach(Thread::interrupt);
    }

    private void handleCommands(AVFormatContext formatContext)
    {
        MediaCommand command;
        while ((command = commandQueue.poll()) != null)
        {
            logger.debug("process: {}", command);

            switch (command.getType())
            {
                case PLAY:
                    masterClock.setPaused(false);
                    break;
                case PAUSE:
                    masterClock.setPaused(true);
                    break;
                case SEEK:
                {
                    long zeroReferencedTimestamp = (long) (((Double) command.requirePayload()) * AV_TIME_BASE);
                    final long startTime = formatContext.start_time() == AV_NOPTS_VALUE ? 0 : formatContext.start_time();
                    final long timestamp = startTime + zeroReferencedTimestamp;
                    int ret;
                    if ((ret = avformat_seek_file(formatContext, -1, 0, timestamp, Long.MAX_VALUE, 0)) < 0)
                    {
                        logger.error("Failed seeking {}", FfmpegUtils.formatError(ret));
                    } else
                    {
                        handlers.values().forEach(StreamHandler::flushPacketQueue);
                        masterClock.reset();
                    }
                }
                    break;
                case START_CAPTURE:
                    if (media instanceof IPlayable playable)
                    {
                        String capturePath = (String) command.requirePayload();
                        playable.setCaptureLocation(capturePath);
                        MediaRecorder recorder = new MediaRecorder(capturePath, masterClock);
                        addPacketListener(capturePath, recorder);
                    } else
                    {
                        logger.warn("Attempted to capture a non playable media {}.", media.getName());
                    }
                    break;
                case STOP_CAPTURE:
                    PacketListener recorder = removePacketListener((String) command.requirePayload());
                    if (recorder != null)
                    {
                        recorder.stop();
                    } else
                    {
                        logger.warn("No such media capture {} for {}.", command.getPayload(), media.getName());
                    }
                    break;
            }
        }
    }

    private void shortWait()
    {
        shortWait(5);
    }

    private void shortWait(long millis)
    {
        Uninterruptibles.sleepUninterruptibly(Duration.ofMillis(millis));
    }
}
