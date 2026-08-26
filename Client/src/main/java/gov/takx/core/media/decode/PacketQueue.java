package gov.takx.core.media.decode;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_clone;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;

/**
 * Queue for AVPackets
 *
 * @since 2.8
 */
public class PacketQueue
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final InternalPacket FLUSH_PACKET = new InternalPacket(null, 0);
    static final InternalPacket ABORT_PACKET = new InternalPacket(null, 0);
    static final InternalPacket EOF_PACKET = new InternalPacket(null, 0);

    private final BlockingQueue<InternalPacket> packetQueue = new LinkedBlockingQueue<>();
    private volatile boolean abort = false;
    private volatile int serial = 0;

    public int size()
    {
        return packetQueue.size();
    }

    /**
     * Is the queue empty?
     *
     * @return {@code true} if the queue is empty, {@code false} if there are entries in the queue.
     * @since 5.1.0
     */
    public boolean isEmpty()
    {
        return packetQueue.isEmpty();
    }

    public void put(AVPacket packet)
    {
        packetQueue.offer(new InternalPacket(av_packet_clone(packet), serial));
    }

    void putEoF()
    {
        packetQueue.offer(EOF_PACKET);
    }

    void flushQueue()
    {
        serial++;
        List<InternalPacket> packets = new ArrayList<>();
        packetQueue.drainTo(packets);
        for (InternalPacket packet : packets)
        {
            av_packet_free(packet.getPacket());
        }
        packets.clear();
        packetQueue.offer(FLUSH_PACKET);
    }

    /**
     * Get the next packet in the queue, waiting as necessary for it to arrive
     *
     * @return The next packet on the queue, or ABORT_PACKET if processing is finished, or FLUSH_PACKET if all previous data is stale
     */
    InternalPacket blockingGet()
    {
        while (true)
        {
            if (abort)
            {
                return ABORT_PACKET;
            }

            try
            {
                InternalPacket packet = packetQueue.poll(100, TimeUnit.MILLISECONDS);
                if (packet != null)
                {
                    return packet;
                }
            } catch (InterruptedException e)
            {

            }
        }
    }

    public void abort()
    {
        abort = true;
        if (logger.isDebugEnabled())
        {
            logger.debug("PacketQueue.abort");
        }
    }

    public int getSerial()
    {
        return serial;
    }

    public static class InternalPacket
    {
        final AVPacket packet;
        final int serial;

        InternalPacket(AVPacket packet, int serial)
        {
            this.packet = packet;
            this.serial = serial;
        }

        public AVPacket getPacket()
        {
            return packet;
        }

        public int getSerial()
        {
            return serial;
        }
    }
}
