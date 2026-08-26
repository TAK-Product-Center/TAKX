package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;

import java.util.concurrent.TimeUnit;

/**
 * Clock to manage real-time decoding.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class Clock
{
    private static final double NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);

    double pts; // presentation timestamp, measured in fractional seconds from the start of the media stream
    double ptsOffset; // Offset from system time to the last recorded pts

    public Clock()
    {
        setClock(0);
    }

    /**
     * @return Current time adjusted by our pts offset
     */
    public synchronized double getClock()
    {
        return ptsOffset + timeInSeconds();
    }

    /**
     * Set the presentation timestamp, updating our internal offset according to the current system time.
     */
    public synchronized void setClock(double pts)
    {
        double time = timeInSeconds();
        this.pts = pts;
        ptsOffset = pts - time;
    }

    /**
     * @return Current system time in fractional seconds
     */
    public static double timeInSeconds()
    {
        return System.nanoTime() / NANOS_PER_SECOND;
    }
}
