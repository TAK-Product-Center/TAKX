package gov.takx.core.media.decode;

import jakarta.annotation.Nonnull;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_clone;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;

/**
 * Queue for AVPackets.  The queue has no pre-determined size limit.  The total size of all non-command packets is
 * maintained, and can be obtained via {@link #sizeInBytes()}.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
public class PacketQueue
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final PacketHolder FLUSH_PACKET = new PacketHolder(null, 0);
    static final PacketHolder ABORT_PACKET = new PacketHolder(null, 0);
    static final PacketHolder EOF_PACKET = new PacketHolder(null, 0);

    private final BlockingQueue<PacketHolder> packetQueue = new LinkedBlockingQueue<>();
    private volatile boolean abort;
    private final AtomicInteger serial = new AtomicInteger(0);
    private final AtomicLong sizeInBytes = new AtomicLong(); // Total size of all packets in our queue

    /**
     * @return Number of packets in our queue
     */
    public int size()
    {
        return packetQueue.size();
    }

    /**
     * @return Total number of bytes in all packets in this queue
     */
    public long sizeInBytes()
    {
        return sizeInBytes.get();
    }

    /**
     * @return {@code true} if the queue is empty, {@code false} if there are entries in the queue.
     * @since 5.1.0
     */
    public boolean isEmpty()
    {
        return packetQueue.isEmpty();
    }

    /**
     * Add a packet to the queue.
     */
    public void put(@Nonnull AVPacket packet)
    {
        put(new PacketHolder(av_packet_clone(packet), serial.get()));
    }

    /**
     * Add an EOF packet to the queue.
     */
    void putEoF()
    {
        put(EOF_PACKET);
    }

    /**
     * Flush the queue, disposing of all packets that were in the queue, and increment our serial number.
     */
    void flushQueue()
    {
        serial.incrementAndGet();

        final var packets = new ArrayList<PacketHolder>();
        packetQueue.drainTo(packets);
        for (var packet : packets) av_packet_free(packet.packet());
        packets.clear();

        put(FLUSH_PACKET);
        sizeInBytes.set(0);
    }

    /**
     * Add a packet to our queue and update our total size.
     */
    private void put(PacketHolder packet)
    {
        try
        {
            packetQueue.put(packet);
            sizeInBytes.addAndGet(packet.sizeInBytes());
        } catch (InterruptedException ignored)
        {
            // Can't happen
        }
    }

    /**
     * Get the next packet in the queue, waiting as necessary for it to arrive
     *
     * @return The next packet on the queue, or ABORT_PACKET if processing is finished, or FLUSH_PACKET if all previous data is stale
     */
    public PacketHolder blockingGet()
    {
        while (true)
        {
            if (abort) return ABORT_PACKET;

            try
            {
                var packet = packetQueue.poll(100, TimeUnit.MILLISECONDS);
                if (packet != null)
                {
                    sizeInBytes.addAndGet(-packet.sizeInBytes());
                    return packet;
                }
            } catch (InterruptedException ignored)
            {
            }
        }
    }

    public void abort()
    {
        abort = true;
        logger.debug("PacketQueue.abort");
    }

    /**
     * @return Our serial number.  This is incremented every time the queue is flushed, which is used when the user
     *         seeks to a different position in the media.
     */
    public int getSerial()
    {
        return serial.get();
    }

    /**
     * Holds packets in the queue so a serial number can be attached.
     *
     * @param packet The actual AVPacket, may be {@code null} for our special command packets
     * @param serial Serial number, incremented each time this queue is flushed/reset
     */
    public record PacketHolder(AVPacket packet, int serial)
    {
        /**
         * @return Size of our contained packet in bytes
         */
        public long sizeInBytes()
        {
            return packet != null ? packet.size() : 0;
        }
    }
}
