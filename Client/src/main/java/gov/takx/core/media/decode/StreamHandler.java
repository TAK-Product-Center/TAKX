package gov.takx.core.media.decode;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.media.MediaFrame;
import gov.takx.core.media.Utilities;
import gov.takx.core.media.writer.MediaCleaner;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_guess_frame_rate;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_alloc;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_free;
import static org.bytedeco.ffmpeg.global.avutil.av_frame_get_best_effort_timestamp;
import static org.bytedeco.ffmpeg.global.avutil.av_inv_q;
import static org.bytedeco.ffmpeg.global.avutil.av_make_q;
import static org.bytedeco.ffmpeg.global.avutil.av_q2d;

/**
 * Thread to decode one stream of the media (audio, video, or data)
 *
 * @since 2.8
 */
public class StreamHandler implements IResourceCleaner, Runnable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /* no AV sync correction is done if below the minimum AV sync threshold */
    private static final double AV_SYNC_THRESHOLD_MIN = 0.04;
    /* AV sync correction is done if above the maximum AV sync threshold */
    private static final double AV_SYNC_THRESHOLD_MAX = 0.1;
    /* If a frame duration is longer than this, it will not be duplicated to compensate AV sync */
    private static final double AV_SYNC_FRAMEDUP_THRESHOLD = 0.1;
    /* no AV correction is done if too big error */
    private static final double AV_NOSYNC_THRESHOLD = 10.0;

    private static final double MAX_FRAME_DURATION = 10.0;

    private static final Optional<MediaFrame> EMPTY_FRAME = Optional.empty();

    private final PacketQueue packetQueue = new PacketQueue();

    private final AVStream stream;
    private final int mediaType; // The {@code AVMediaType} for this stream @since 5.1.0
    private final IDecoder decoder;
    private final FrameQueue frameQueue;
    private final Clock clock = new Clock();
    private final ExternalClock masterClock;
    private final AVRational frameRate;

    // Variables for refresh thread
    private double frameTimer = Double.NaN;
    private boolean forceRefresh;
    private boolean wasPaused;

    private final AtomicReference<HandlerConfiguration> updatedConfig = new AtomicReference<>(null);
    private Thread thread;
    private volatile boolean abort;
    private volatile boolean eof; // We hit an EOF on the input stream @since 5.1.0

    /**
     * Create a StreamHandler to decode an individual stream
     *
     * @param formatContext  The AVFormatContext
     * @param stream         The AVStream
     * @param decoder        The IDecoder
     * @param masterClock    The ExternalClock
     * @param frameQueueSize Size of the output queue
     */
    public StreamHandler(AVFormatContext formatContext,
                         AVStream stream,
                         IDecoder decoder,
                         ExternalClock masterClock,
                         int frameQueueSize)
    {
        this.stream = stream;
        this.decoder = decoder;
        this.masterClock = masterClock;
        mediaType = stream.codecpar().codec_type();
        frameQueue = new FrameQueue(frameQueueSize);
        frameRate = av_guess_frame_rate(formatContext, stream, null);
        MediaCleaner.getInstance().register(this, new StreamHandlerState(decoder)::cleanup);

        if (logger.isDebugEnabled()) logger.debug("Handler created for {} stream {}.", Utilities.getMediaTypeString(mediaType), stream.index());
    }

    public int getPacketQueueSize()
    {
        return packetQueue.size();
    }

    public void queuePacket(AVPacket packet)
    {
        packetQueue.put(packet);
    }

    public void queueEoFPacket()
    {
        packetQueue.putEoF();
    }

    public void configure(HandlerConfiguration configuration)
    {
        updatedConfig.set(configuration);
    }

    /**
     * Returns the next frame, blocking until one is available
     *
     * @return The next available frame for display, or {@code null} at EOF or if abort has been called
     */
    public MediaFrame getNextFrame()
    {
        // TODO this seems potentially dangerous
        while (frameQueue.isEmpty())
        {
            if (isDone()) return null;
            Thread.yield();
        }

        DecodedFrame decodedFrame = frameQueue.peek();
        if (decodedFrame == null || decodedFrame.isEof())
        {
            return null;
        } else
        {
            frameQueue.next();
            return decodedFrame.getMediaFrame().get();
        }
    }

    /**
     * Have we drained both the packet queue and the frame queue?
     *
     * @return {@code true} if both the packet queue and the frame queue are empty.
     * @since 5.1.0
     */
    public boolean isEmpty()
    {
        return packetQueue.isEmpty() && frameQueue.isEmpty();
    }

    /**
     * Non-blocking call to return the next frame, if one is available
     *
     * @return The next frame, or empty optional if we don't have one yet
     */
    public Optional<MediaFrame> getFrameIfReady()
    {
        if (frameQueue.isEmpty()) return EMPTY_FRAME;

        DecodedFrame lastDecodedFrame = frameQueue.peekLast();
        DecodedFrame decodedFrame = frameQueue.peek();

        // initialize frame timer if necessary
        if (Double.isNaN(frameTimer))
        {
            frameTimer = Clock.timeInSeconds();
        }

        // Make sure we skip over any latent frames in the queue after seeking
        while (decodedFrame.getSerial() != packetQueue.getSerial())
        {
            frameQueue.next();
            if (frameQueue.isEmpty()) return EMPTY_FRAME;

            lastDecodedFrame = frameQueue.peekLast();
            decodedFrame = frameQueue.peek();
        }

        // Discontinuity in the frames, restart the time
        if (lastDecodedFrame.getSerial() != decodedFrame.getSerial())
        {
            clock.setClock(decodedFrame.getPts(), decodedFrame.getSerial());
            frameTimer = Clock.timeInSeconds();
        }

        // If the master clock is disabled then we don't worry about timing and just return the next frame.
        if (!masterClock.isEnabled())
        {
            return nextFrame();
        }

        if (masterClock.isPaused())
        {
            wasPaused = true;
            if (forceRefresh && frameQueue.canShow())
            {
                forceRefresh = false;
                return decodedFrame.getMediaFrame();
            } else
            {
                forceRefresh = false;
                return EMPTY_FRAME;
            }
        } else if (wasPaused)
        {
            clock.setClock(decodedFrame.getPts(), decodedFrame.getSerial());
            frameTimer = Clock.timeInSeconds();
            wasPaused = false;
        }

        double lastDuration = decodedFrame.getDuration(lastDecodedFrame);
        double delay = computeTargetDelay(lastDuration);
        double time = Clock.timeInSeconds();

        // We are early, don't send new frame yet
        if (time < frameTimer + delay)
        {
            //displayIfRequired(listeners);
            return EMPTY_FRAME;
        }
        //if (logger.isDebugEnabled())
        //logger.debug(String.format("TIME %d delay= %.3f elapsed= %.3f lastDur=%.3f DISPLAY ", stream.index(), delay, time - frameTimer, lastDuration));

        // Difference is too great, don't try to resync, just reset the timer
        frameTimer += delay;

        // Set the clock
        clock.setClock(decodedFrame.getPts(), decodedFrame.getSerial());

        // Bring up the next frame and display it
        return nextFrame();
    }

    private Optional<MediaFrame> nextFrame()
    {
        frameQueue.next();
        if (frameQueue.canShow())
        {
            DecodedFrame frame = frameQueue.peekLast();
            Optional<MediaFrame> mediaFrame = frame.getMediaFrame();
            mediaFrame.ifPresent(f -> frame.setShown(true));
            return mediaFrame;
        } else
        {
            return EMPTY_FRAME;
        }
    }

    private double computeTargetDelay(double delay)
    {
        double syncThreshold = 0, diff = 0;
        diff = clock.getClock() - masterClock.getClock();
        syncThreshold = Math.max(AV_SYNC_THRESHOLD_MIN, Math.min(AV_SYNC_THRESHOLD_MAX, delay));
        if (!Double.isNaN(syncThreshold) && Math.abs(diff) < MAX_FRAME_DURATION)
        {
            if (diff <= -syncThreshold)
            {
                delay = Math.max(0, delay + diff);
            }
        }
        //if (logger.isDebugEnabled())
        //logger.debug(String.format("TIME %d computeTargetDelay delay=%.3f diff=%.3f sync=%.3f", stream.codecpar().codec_type(), delay, diff, syncThreshold));
        return delay;
    }

    /**
     * Shut down the stream handler
     */
    public void abort()
    {
        logger.debug("StreamHandler abort called");

        abort = true;
        packetQueue.abort();
        frameQueue.abort();

        // Shut down the thread
        if (thread != null)
        {
            // Give it a chance to complete
            Uninterruptibles.joinUninterruptibly(thread, Duration.ofMillis(500));

            // Interrupt thread as a last resort
            if (thread.isAlive())
            {
                logger.debug("Stream handler join failed; interrupting stream handler thread");
                thread.interrupt();
            }
        }
    }

    /**
     * Called from read thread
     */
    public void flushPacketQueue()
    {
        packetQueue.flushQueue();
    }

    /**
     * Release resources when the class is no longer needed.
     *
     * @since 5.1.0
     */
    @Override
    public void releaseResources()
    {
        packetQueue.flushQueue();
    }

    @Override
    public void run()
    {
        if (logger.isDebugEnabled()) logger.debug("Stream handler {} thread starting", stream.index());
        Thread.currentThread().setName("Handler " + stream.index());
        thread = Thread.currentThread();
        AVFrame frame = av_frame_alloc();

        while (!eof && !abort)
        {
            if (!decoder.isValid())
            {
                logger.warn("Invalid decoder");
                break;
            }

            // Check if the configuration setting have been updated
            HandlerConfiguration configuration = updatedConfig.getAndSet(null);
            if (configuration != null)
            {
                decoder.configure(configuration);
            }

            // Get the next packet from our input queue, blocking if queue is empty. Also end the thread
            // by breaking loop if we get an ABORT_PACKET or EOF_PACKET.
            PacketQueue.InternalPacket internalPacket = packetQueue.blockingGet();
            if (internalPacket == PacketQueue.ABORT_PACKET)
            {
                logger.debug("Packet queue returned abort signal");
                break;
            } else if (internalPacket == PacketQueue.FLUSH_PACKET)
            {
                decoder.flush();
                forceRefresh = true;
                continue;
            }
            // feed a null packet into the decoder to flush it.
            eof = internalPacket == PacketQueue.EOF_PACKET;

            int serial = internalPacket.getSerial();

            // Decode the packet, and push decoded frame into frameQueue (blocking if queue is full)
            try
            {
                decoder.decodeFrame(internalPacket.getPacket(), frame).ifPresent(mediaFrame -> {
                    long tmpPts = av_frame_get_best_effort_timestamp(frame);
                    double pts = (tmpPts == AV_NOPTS_VALUE) ? Double.NaN : tmpPts * av_q2d(stream.time_base());
                    double frameDuration = computeFrameDuration(frame, frameRate);
                    queueDecoded(frame, mediaFrame, pts, frameDuration, serial);
                });
            } catch (DecodingException e)
            {
                logger.error("Error during decoding", e);
                break;
            }

            // Free the packet, but skip the null packet used to flush the decoder on eof.
            if (!eof) av_packet_unref(internalPacket.getPacket());
        }

        // clean up
        av_frame_free(frame);
        // For EOF, the MediaCleaner empties the queues, then shuts us down.
        if (!eof) releaseResources();

        if (logger.isDebugEnabled()) logger.debug("Stream handler {} thread exiting", stream.index());
    }

    /**
     * Do we expect to do more work?
     *
     * @return {@code true} if we hit eof in the stream, or were aborted. {@code false} otherwise.
     * @since 5.1.0
     */
    public boolean isDone()
    {
        return eof || abort;
    }

    private double computeFrameDuration(AVFrame frame, AVRational frameRate)
    {
        return switch (mediaType)
        {
            case AVMEDIA_TYPE_AUDIO -> av_q2d(av_make_q(frame.nb_samples(), frame.sample_rate()));
            case AVMEDIA_TYPE_VIDEO, AVMEDIA_TYPE_DATA ->
                (frameRate.num() != 0 && frameRate.den() != 0) ? av_q2d(av_inv_q(frameRate)) : 0;
            default -> 0;
        };
    }

    /**
     * Queue a new decoded frame into frameQueue, blocking if queue is full
     *
     * @param frame Source frame
     * @param mediaFrame Decoded frame
     * @param pts presentation time stamp, indicating when the frame should be played
     * @param duration frame duration
     * @param serial frame serial number
     */
    private void queueDecoded(AVFrame frame, MediaFrame mediaFrame, double pts, double duration, int serial)
    {
        // Block until frameQueue has a free slot
        DecodedFrame decodedFrame = frameQueue.peekWritable();

        // Couldn't write frame because abort() has been called, return
        if (decodedFrame == null)
        {
            return;
        }

        // Copy frame into frameQueue's buffer, moving the AVFrame reference
        decodedFrame.setMediaFrame(mediaFrame);
        decodedFrame.setPts(pts);
        decodedFrame.setDuration(duration);
        decodedFrame.setSerial(serial);
        decodedFrame.setShown(false);
        decodedFrame.setEof(false);
        decodedFrame.moveReference(frame);

        // Notify queue and the consumer thread that the frame has been written
        frameQueue.push();
    }

    /**
     * Capture the relevant state for the cleaner
     *
     * @since 5.1.0
     */
    private record StreamHandlerState(IDecoder decoder)
    {
        void cleanup()
        {
            decoder.releaseResources();
        }
    }
}
