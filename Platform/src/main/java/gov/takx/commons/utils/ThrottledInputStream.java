package gov.takx.commons.utils;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Stack;

/**
 * Throttles the delivery of data read from the backing input stream.  The primary metaphor of the throttled
 * input stream is that of a cycle, where only a fixed number of bytes can be read per cycle.  The duration
 * of the cycle and the number of bytes that can be read per cycle is defined by the developer.  As an analogy,
 * consider a queue at a roller coaster ride at an amusement park.  Each time the roller coaster leaves the station,
 * at most it can hold 1 body in every seat.  The ride will typically take a static period of time to complete, at
 * which time the riders who completed the ride will exit, and new riders will enter.  Likewise, on a slow day the
 * coaster may leave the station with empty seats and no one in the queue.
 * <p/>
 * Each cycle will serve up to a maximum of number of bytes per cycle, as defined by the bytesPerCycle constructor
 * parameter.  In the unlikely event that the number of available bytes in a given cycle are not read, the bytes
 * remaining will automatically seed the next cycle, ensuring that during any given cycle there will never be
 * more then bytesPerCycle presented to the consumer of the input stream.  The behavior is especially
 * critical in a bandwidth constrained environment and by a number of GSM modems sending data using SMS
 * PDU where the maximum number of bytes in a given message are well-defined and not expected to be exceeded.  Going
 * back to the roller coaster analogy, if 3 riders decide not to exit the ride, those 3 seats are automatically filled
 * for the next trip and (n-3) new riders can only be accommodated from the queue, where n is the maximum number of seats
 * on the roller coaster.
 * <p/>
 * Only the {@code read()} method is supported at this time.
 *
 * @author weissj
 * @since 1.2
 */
public class ThrottledInputStream extends InputStream
{
    public static final int THRESHOLD_DELAY = -999;

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean blocking = false;
    private InputStream backingStream;
    private int millisPerCycle;
    private int bytesPerCycle;
    private char[] eolCharSequence;
    private long currentCycleEpoch;
    private int bytesSentThisCycle;
    private final Stack<String> unusedStack = new Stack<String>();
    private final Stack<String> usedStack = new Stack<String>();

    /**
     * Constructs a throttled input stream that serves up a maximum of 140 bytes during a cycle
     * period that is timed to last a maximum of 1 second.  This stream guarantees that during
     * any given 1 second cycle no more then 140 bytes will ever be made available to the stream reader.
     * <p/>
     * Use the overloaded constructor if it is necessary to define custom values for bytes per cycle and cycle duration.
     * <p/>
     * This constructor operates strictly on the number of bytes and doesn't recognize line breaks.  Use
     * the overloaded constructor that requires an EOL character sequence to dynamically determine the
     * number of bytes that are feed to a reader per cycle.
     *
     * @param stream Stream to throttle
     */
    public ThrottledInputStream(InputStream stream)
    {
        this(stream, 1000, 140, true);
    }

    /**
     * Constructs a throttled input stream that serves up a maximum of bytesPerCycle during a cycle
     * period that is timed to last a maximum of millisPerCycle.  This stream guarantees that during
     * any given cycle no more then bytesPerCycle will ever be made available to the stream reader.
     * <p/>
     * In the event that the cycle period expires <i>prior</i> to reaching the bytes per cycle threshold, then
     * the next cycle immediately starts and the bytes counter automatically is reset with no perceived
     * delay between sequential cycles.
     * <p/>
     * This constructor operates strictly on the number of bytes and doesn't recognize line breaks.  Use
     * the overloaded constructor that requires an EOL character sequence to dynamically determine the
     * number of bytes that are feed to a reader per cycle.
     *
     * @param stream         Stream to throttle
     * @param millisPerCycle Number of milliseconds that each cycle should last
     * @param bytesPerCycle  Maximum number of bytes to feed to a reader per cycle
     * @param blocking       When true, read() waits until the threshold period clears.  Otherwise, read() returns
     *                       THRESHOLD_DELAY when waiting for a threshold block to clear, and the caller is expected
     *                       to check back later via another read() to continue reading until a -1 value is received.
     */
    public ThrottledInputStream(@Nonnull InputStream stream, int millisPerCycle, int bytesPerCycle, boolean blocking)
    {
        if (bytesPerCycle < 1)
        {
            throw new IllegalArgumentException("bytesPerCycle must be 1 or more");
        }

        if (millisPerCycle < 100)
        {
            throw new IllegalArgumentException("Milliseconds per cycle must be 100 or greater");
        }

        this.blocking = blocking;
        this.backingStream = stream;
        this.bytesPerCycle = bytesPerCycle;
        this.millisPerCycle = millisPerCycle;
        this.eolCharSequence = null;
        currentCycleEpoch = System.currentTimeMillis();

        logger.info("Successfully created a throttled input stream based on {} bytes/cycle with a cycle period of {}", bytesPerCycle, millisPerCycle);
    }

    /**
     * Constructs a throttled input stream that serves up a variable number of bytes during a cycle
     * period that is timed to last a maximum of millisPerCycle.  This stream guarantees that during
     * any given cycle no bytes beyond the explicit end of line (EOL) character sequence will be
     * fed to the reader.  This constructor is useful if the stream should feed readers a logical "line"
     * of data at a time, where the "line" of data will have a variable number of characters in it.
     * <p/>
     * In the event that the cycle period expires <i>prior</i> to reaching the exact EOL character sequence, then
     * the next cycle immediately starts and bytes are read until the exact EOL character sequence is identified.  In
     * the rare event that the cycle period expired while the exact EOL character sequence was being read, the behavior
     * of the stream is to start the next cycle immediately and continue reading until the exact EOL character sequence
     * is identified.  As a concrete example, if the stream was configured to look for a CR/LF combination as the exact
     * EOL character sequence, and the last character read before the cycle expired was CR, then the next cycle would
     * start immediately and continue reading.  Since the very next read byte would be the LF to complete the exact
     * EOL character sequence, then that would constitute the completion of bytes read for that cycle-- a total of but
     * 1 byte, the LF character.
     * <p/>
     * This constructor operates strictly on the EOL character sequence and doesn't set a maximum byte threshold.  Use
     * the overloaded constructor that defines a maximum bytes per cycle if a fixed number of bytes is required per
     * cycle.
     *
     * @param stream          Stream to throttle
     * @param millisPerCycle  Number of milliseconds that each cycle should last
     * @param eolCharSequence Sequence of characters (<b>exact</b>) used to delineate a logical "line" of data
     */
    public ThrottledInputStream(@Nonnull InputStream stream, int millisPerCycle, @Nonnull char[] eolCharSequence)
    {
        if (eolCharSequence.length == 0)
        {
            throw new IllegalArgumentException("EOL character sequence must include at least 1 character");
        }

        if (millisPerCycle < 100)
        {
            throw new IllegalArgumentException("Milliseconds per cycle must be 100 or greater");
        }

        this.backingStream = stream;
        this.bytesPerCycle = -1;
        this.millisPerCycle = millisPerCycle;
        this.eolCharSequence = eolCharSequence;
        currentCycleEpoch = System.currentTimeMillis();

        //
        // Prime the unused stack
        for (int i = eolCharSequence.length; i >= 1; i--)
        {
            unusedStack.push(String.valueOf(eolCharSequence[i - 1]));
        }

        logger.info("Successfully created a throttled input stream based on a {} count character sequence with a cycle period of {}", eolCharSequence.length, millisPerCycle);
    }

    /**
     * Identifies the total number of bytes that are available from the backing stream.  This value
     * does not reflect how many bytes may or may not be available in the current cycle- it is a cumulative
     * value only.
     *
     * @return number of total bytes available from the backing stream.
     */
    @Override
    public int available() throws IOException
    {
        return backingStream.available();
    }

    /**
     * This method blocks until input data is available, the end of the stream is detected, or an exception is thrown.
     * If no byte is available because the end of the backing stream has been reached, the value -1 is returned.
     * However, if the backing stream has data available but the maximum number of bytes or the exact EOL character
     * sequence was encountered, this method blocks until the opening of the next cycle.
     *
     * @return current byte as an int, or -1 if the there are no more bytes available from the backing stream
     * @throws IOException
     */
    public int read() throws IOException
    {
        int value = -1;

        //
        // Does the backing stream have data available?
        if (backingStream.available() > 0)
        {
            //
            // Cycle periodicity check- ensure that the cycle period is valid for byte delivery
            long delay = (System.currentTimeMillis() - currentCycleEpoch);

            //
            // Read the next byte as appropriate (bytes per cycle or line per cycle).  Internally,
            // this method will return a -2 to indicate either the bytes per cycle threshold has
            // been reached or the exact EOL character sequence has been identified in this cycle.
            value = (bytesPerCycle > 0) ? readNextThrottledByte() : readNextEOLByte();

            //
            // If the delay is within the definition of our current cycle but we've reached the bytes threshold
            // we need to block until the next cycle period opens up.  In reality, the delay likely won't be the
            // limiting factor here- it will almost always be the -2 value.
            if (delay <= millisPerCycle && value == -2)
            {
                while (delay < millisPerCycle)
                {
                    if (!blocking)
                    {
                        value = THRESHOLD_DELAY;
                        delay = (System.currentTimeMillis() - currentCycleEpoch);
                    } else
                    {
                        //
                        // Wait until next cycle opens up
                        try
                        {
                            Thread.sleep(50);
                            delay = (System.currentTimeMillis() - currentCycleEpoch);
                        } catch (InterruptedException e)
                        {
                            logger.info("Interrupt Detected on Throttled Input Stream");
                        }

                        currentCycleEpoch = System.currentTimeMillis();
                        bytesSentThisCycle = 0;

                        while (usedStack.size() > 0)
                        {
                            unusedStack.push(usedStack.pop());
                        }

                        value = (bytesPerCycle > 0) ? readNextThrottledByte() : readNextEOLByte();
                    }
                }
            } else if (delay > millisPerCycle)
            {
                //
                // Delay is > millisPerCycle and there was still data available in the last cycle.
                // All that is required is to reset the epoch, and bytes sent count and continue
                // along with the current byte being sent counting against the "new" cycle (notice the
                // bytesSentThisCycle is set to 1, not 0 like above
                logger.trace("RESET");

                currentCycleEpoch = System.currentTimeMillis();
                bytesSentThisCycle = 1;

                //
                // NOTE:
                //
                //          There is probably < 0.01% chance that we were reading the EOL sequence
                //          when the cycle's period ran out, which would have resulted in a partially
                //          populated EOL stack.  As per the disclosed class JavaDoc documentation, the
                //          behavior is to read for the full period, and in the event the EOL sequence
                //          isn't identified during that period, the next period starts and continues
                //          reading along until such time that the EOL sequence is met.  Since we
                //          ran out of cycle time, the stack is left alone and NOT reset- it's either
                //          empty or partially populated.  In other words, if the EOL char sequence was 2
                //          characters and the 1st character was pushed as the very last character during
                //          cycle n, and the next EOL character is pushed as the first character read
                //          in cycle (n+1), then the (n+1) cycle literally will consist of a single character.
            }
        }

        return value;
    }

    @Override
    public void close() throws IOException
    {
        backingStream.close();
    }

    @Override
    public boolean markSupported()
    {
        return false;
    }

    /**
     * Unsupported operation.
     *
     * @param bytes byte array to populate
     * @return byte read
     * @throws IOException io exception
     */
    @Override
    public int read(byte[] bytes) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     *
     * @param bytes  byte array to populate
     * @param offset offset
     * @param len    length
     * @return byte read
     * @throws IOException io exception
     */
    @Override
    public int read(byte[] bytes, int offset, int len) throws IOException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void reset()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     *
     * @param n number of bytes to skip
     * @return bytes skipped
     */
    @Override
    public long skip(long n)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Identifies the number of milliseconds per cycle.
     *
     * @return Immutable number of milliseconds used per cycle
     */
    public int getMillisPerCycle()
    {
        return this.millisPerCycle;
    }

    /**
     * Identifies the number of bytes to read per cycle.
     *
     * @return Bytes per cycle
     */
    public int getBytesPerCycle()
    {
        return bytesPerCycle;
    }

    /**
     * Identifies the <b>exact</b> end of line (EOL) character sequence that this stream
     * uses to delineate data blocks for a given cycle.
     *
     * @return Immutable EOL character sequence in effect for this stream
     */
    public char[] getEolCharSequence()
    {
        return this.eolCharSequence;
    }

    private int readNextEOLByte() throws IOException
    {
        int value = -2;

        if (unusedStack.size() != 0)
        {
            value = backingStream.read();

            String topUnused = unusedStack.peek();
            if (((char) value) == topUnused.toCharArray()[0])
            {
                usedStack.push(unusedStack.pop());
            } else
            {
                //
                // False alarm- clear the used stack to reflect the fact
                // that the exact sequence of chars was not matched
                if (usedStack.size() > 0)
                {
                    while (usedStack.size() > 0)
                    {
                        unusedStack.push(usedStack.pop());
                    }
                }
            }
        }

        return value;
    }

    private int readNextThrottledByte() throws IOException
    {
        int value = -2;

        if (bytesSentThisCycle <= bytesPerCycle)
        {
            value = backingStream.read();
            bytesSentThisCycle++;
        }

        return value;
    }
}
