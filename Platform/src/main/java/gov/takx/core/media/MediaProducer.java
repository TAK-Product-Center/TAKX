package gov.takx.core.media;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaProducerListener;
import gov.takx.api.media.IPacketListener;
import gov.takx.api.media.IPlayable;
import gov.takx.api.media.stream.VideoStreamConfiguration;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.media.decode.ExternalClock;
import gov.takx.core.media.decode.StreamHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.javacpp.PointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Container;
import java.awt.Dimension;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_seek_file;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
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
 * @since 2.8 (moved to Platform in 5.8)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
@IncubatingApi(since = "5.8")
public class MediaProducer implements Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MAX_QUEUE_SIZE = 512 * 1024 * 1024;  // Limit on memory used in our frame queues
    private static final Map<Integer, Integer> errorCounts = new ConcurrentHashMap<>();
    private static final int SUPPRESS_LIMIT = 10;
    private static final List<String> STREAMING_PROTOCOLS = List.of("https", "udp", "rtsp");

    @Inject
    private IMediaManager mediaManager;

    private final IMedia media;
    private final String mediaUrl;
    private final List<IMediaProducerListener> mediaProducerListeners = new CopyOnWriteArrayList<>();
    private final Map<String, IPacketListener> packetListeners = new ConcurrentHashMap<>();
    private final Map<Integer, StreamHandler> handlers = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<MediaCommand> commandQueue = new LinkedBlockingQueue<>();
    private final List<Thread> handlerThreads = new ArrayList<>();
    private final ExternalClock masterClock = new ExternalClock();

    // We may stop for either of two reasons.
    // We reached the eof on the incoming stream and the handler queues are empty
    private volatile boolean eof;

    // Or an external event that closes the player
    private volatile boolean shutdown;

    private Thread readThread;

    private Container container;
    private final int initialMaxWidth;
    private final int initialMaxHeight;
    private final Consumer<String> onStartupFailure;

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
            for (var streamHandler : handlers.values())
            {
                empty = empty && streamHandler.isEmpty();
                streamHandler.getFrameIfReady().ifPresent(mediaFrame -> mediaProducerListeners.forEach(l -> l.onMediaFrame(mediaFrame)));
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
     * @param onStartupFailure Optional consumer to receive an error message if initialization fails
     */
    public MediaProducer(@Nonnull IMedia media, @Nonnull String mediaUrl, int initialMaxWidth, int initialMaxHeight,
                         @Nullable Consumer<String> onStartupFailure)
    {
        this.media = media;
        this.mediaUrl = mediaUrl;
        this.initialMaxWidth = initialMaxWidth;
        this.initialMaxHeight = initialMaxHeight;
        this.onStartupFailure = onStartupFailure;
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
     * @param onStartupFailure Optional consumer to receive an error message if initialization fails
     */
    public MediaProducer(@Nonnull IMedia media, @Nonnull String mediaUrl, @Nullable Container container,
                         @Nullable Consumer<String> onStartupFailure)
    {
        this.media = media;
        this.mediaUrl = mediaUrl;
        initialMaxWidth = initialMaxHeight = 0;
        this.container = container;
        this.onStartupFailure = onStartupFailure;
    }

    /**
     * Add a listener for produced media frames.
     */
    public void addMediaProducerListener(@Nonnull IMediaProducerListener listener)
    {
        mediaProducerListeners.add(listener);
    }

    /**
     * Remove a previously added listener
     */
    public void removeMediaProducerListener(@Nonnull IMediaProducerListener listener)
    {
        mediaProducerListeners.remove(listener);
    }

    /**
     * Add a listener to raw packets read from an input stream.
     *
     * @param key      The unique key for the listener
     * @param listener Receives packets read from an input stream
     * @since 5.1.0
     */
    public void addPacketListener(@Nonnull String key, @Nonnull IPacketListener listener)
    {
        packetListeners.put(key, listener);
    }

    /**
     * Remove a packet listener.
     *
     * @param key The unique key for the packet listener
     * @return The removed {@link IPacketListener}, or {@code null} if there is no packet listener for the given {@code key}
     * @since 5.1.0
     */
    public IPacketListener removePacketListener(@Nonnull String key)
    {
        return packetListeners.remove(key);
    }

    /**
     * Queue a command (play, pause, seek, etc.) to be executed.
     */
    public void queueCommand(@Nonnull MediaCommand command)
    {
        logger.debug("queue: {}", command);
        commandQueue.offer(command);
    }

    /**
     * Start capturing the media we are producing.
     * 
     * @param path Base path and file name to use for generated files as recording continues
     * @param exceptionHandler Handler called when any exception is thrown by the recording process
     * @throws IllegalArgumentException If the current media is not {@link IPlayable}
     */
    public void startCapture(@Nonnull String path, @Nullable BiConsumer<String, Exception> exceptionHandler)
    {
        if (media instanceof IPlayable playable)
        {
            playable.setCaptureLocation(path);
            addPacketListener(path, new MediaRecorder(Paths.get(path), masterClock, exceptionHandler));
        } else
        {
            throw new IllegalArgumentException("Attempted to capture a non playable media: " + media.getName());
        }
    }

    /**
     * Stop capturing packets from the media being produced.
     * 
     * @param path Capture path, must eb the same path as passed to {@link #startCapture(String, BiConsumer)}
     */
    public void stopCapture(@Nonnull String path)
    {
        final var recorder = removePacketListener(path);
        if (recorder != null)
        {
            recorder.stop();
        } else
        {
            throw new IllegalArgumentException(String.format("No such media capture [%s] for %s", path, media.getName()));
        }
    }

    /**
     * Set the desired size of decoded video frames.  This enables native resizing by the video decoder to improve performance.
     */
    public void setVideoSize(@Nonnull Dimension dimension)
    {
        final var config = new VideoStreamConfiguration(dimension);
        handlers.values().stream().filter(h -> h.getMediaType() == AVMEDIA_TYPE_VIDEO).forEach(h -> h.configure(config));
    }

    /**
     * @return {@code true} if the master clock is not paused, {@code false} if it is
     */
    public boolean isPlaying()
    {
        return !masterClock.isPaused();
    }

    @Override
    public void run()
    {
        Thread.currentThread().setName("Media Producer for " + media.getName());

        // Keep a handle on this thread so that it can be correctly interrupted if shutdown() is called
        // while the playback is paused.
        readThread = Thread.currentThread();

        // Propagate the log level into the av library
        av_log_set_level(logger.isTraceEnabled() ? AV_LOG_TRACE : logger.isDebugEnabled() ? AV_LOG_DEBUG : AV_LOG_ERROR);

        final var formatContext = initAvFormatContext();
        if (formatContext == null) return;

        createHandlers(formatContext);
        if (handlers.isEmpty()) // No streams were successfully created, abort
        {
            reportErrorAndCleanup("Could not find readable video, audio or data stream, aborting", formatContext, true);
            return;
        }

        // Start the thread to pull decoded frames from the handlers and play them
        refreshThread.setName("Media Player for " + media.getName());
        refreshThread.setDaemon(true);
        refreshThread.start();

        processMedia(formatContext); // Read, decode, and queue all media streams

        commandQueue.clear();

        handlers.values().forEach(StreamHandler::abort);
        packetListeners.values().forEach(IPacketListener::stop);

        avformat_close_input(formatContext);
        avformat_free_context(formatContext);

        logger.debug("MediaProducer thread exiting");
    }

    /**
     * @return Initialized {@link AVFormatContext}, configured according to our media, {@code null} on any error
     */
    @Nullable
    private AVFormatContext initAvFormatContext()
    {
        final var formatContext = avformat_alloc_context();
        String formatName = media.getFormat();
        final var inputFormat = formatName == null ? null : avformat.av_find_input_format(formatName);

        final var options = new AVDictionary();
        Map<String, String> decodingOptions = media.getOptions();
        if (decodingOptions != null) decodingOptions.forEach((k, v) -> av_dict_set(options, k, v, 0));

        if ((media instanceof IPlayable) && StringUtils.startsWithAny(mediaUrl.toLowerCase(), STREAMING_PROTOCOLS))
        {
            av_dict_set(options, "fflags", "nobuffer", 0);
        }

        // Attempt to open the stream
        int ret = avformat_open_input(formatContext, mediaUrl, inputFormat, options);
        av_dict_free(options);

        if (ret < 0)
        {
            String message = String.format("Could not open URL: %s with format: %s, %s", mediaUrl, formatName, FfmpegUtils.formatError(ret));
            reportErrorAndCleanup(message, formatContext, false);
            return null;
        }

        // Retrieve stream information
        formatContext.max_analyze_duration(30 * AV_TIME_BASE);
        ret = avformat_find_stream_info(formatContext, (PointerPointer<?>) null);
        if (ret < 0)
        {
            String message = String.format("Could not retrieve stream information: %s.", FfmpegUtils.formatError(ret));
            reportErrorAndCleanup(message, formatContext, true);
            return null;
        }
        return formatContext;
    }

    /**
     * Create media handlers for each stream in our source media.  A thread will be created and started to run each handler.
     */
    private void createHandlers(AVFormatContext formatContext)
    {
        masterClock.reset();
        masterClock.setPaused(true);

        // Create handlers for each stream, which will be in charge of decoding and deciding when
        // to display each frame
        handlers.putAll(StreamFactory.createAll(formatContext, masterClock));

        // Start decoding threads for each handler
        handlers.values().forEach(handler -> {
            if (handler.getMediaType() == AVMEDIA_TYPE_VIDEO) configureVideoHandler(handler);

            final var thread = new Thread(handler);
            thread.setDaemon(true);
            thread.setName("media decoder #" + handlerThreads.size() + " for " + media.getName());
            thread.start();
            handlerThreads.add(thread);
        });
    }

    /**
     * Install our desired video output size on the given handler.
     */
    private void configureVideoHandler(StreamHandler handler)
    {
        // If we have the parent container, set the image size to the container's size otherwise use the initialMaxWidth
        // and initialMaxHeight if they have been set.
        Dimension videoSize = null;

        if (container != null)
        {
            final int width = container.getWidth();
            final int height = container.getHeight();
            if (width > 0 && height > 0) videoSize = new Dimension(width, height);
        } else if (initialMaxWidth > 0 && initialMaxHeight > 0)
        {
            videoSize = new Dimension(initialMaxWidth, initialMaxHeight);
        }

        if (videoSize != null) handler.configure(new VideoStreamConfiguration(videoSize));
    }

    /**
     * Read packets from the media input and pass them to the handler threads for decoding.  This method will not
     * return until we are shutdown, or the refresh thread exits (meaning all media frames have been processed).
     */
    private void processMedia(@Nonnull AVFormatContext formatContext)
    {
        final AVPacket packet = new AVPacket();
        boolean firstEof = true;

        while (!shutdown && refreshThread.isAlive())
        {
            handleCommands(formatContext); // Handle pause/play/seek

            // Ensure we have enough space in our buffers to read another packet
            long bufferedPackets = handlers.values().stream().mapToLong(StreamHandler::getPacketQueueSizeInBytes).sum();
            if (bufferedPackets > MAX_QUEUE_SIZE)
            {
                shortWait();
                continue;
            }

            // Read a packet from the file/stream
            int ret = av_read_frame(formatContext, packet);
            if (ret < 0)
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

            // And notify packet listeners
            if (!packetListeners.isEmpty()) ExceptionUtils.safeDoOperation(packetListeners.values(), l -> l.onPacket(formatContext, packet));

            av_packet_unref(packet);
        }
    }

    private void reportErrorAndCleanup(String message, AVFormatContext formatContext, boolean isOpen)
    {
        logger.error(message);

        if (onStartupFailure != null) onStartupFailure.accept(message);

        if (isOpen) avformat_close_input(formatContext);
        avformat_free_context(formatContext);
    }

    /**
     * Stop processing media streams, interrupt all our processing threads.
     */
    public void shutdown()
    {
        shutdown = true;
        if (readThread != null && readThread.isAlive()) readThread.interrupt();
        handlerThreads.forEach(Thread::interrupt);
    }

    private void handleCommands(AVFormatContext formatContext)
    {
        MediaCommand command;
        while ((command = commandQueue.poll()) != null)
        {
            logger.debug("process: {}", command);

            switch (command.type())
            {
                case PLAY -> masterClock.setPaused(false);
                case PAUSE -> masterClock.setPaused(true);
                case SEEK -> doSeek(formatContext, command);
            }
        }
    }

    private void doSeek(AVFormatContext formatContext, MediaCommand command)
    {
        long zeroReferencedTimestamp = (long) (((Double) command.requirePayload()) * AV_TIME_BASE);
        final long startTime = formatContext.start_time() == AV_NOPTS_VALUE ? 0 : formatContext.start_time();
        final long timestamp = startTime + zeroReferencedTimestamp;
        int ret = avformat_seek_file(formatContext, -1, 0, timestamp, Long.MAX_VALUE, 0);
        if (ret < 0)
        {
            logger.error("Failed seeking {}", FfmpegUtils.formatError(ret));
        } else
        {
            handlers.values().forEach(StreamHandler::flushPacketQueue);
            masterClock.reset();
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
