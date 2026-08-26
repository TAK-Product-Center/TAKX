package gov.takx.core.media.decode;

import gov.tak.api.annotation.IncubatingApi;

/**
 * A clock that can be paused and un-paused.
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
@IncubatingApi(since = "5.8")
public class ExternalClock
{
    private volatile boolean paused;
    private double start = Clock.timeInSeconds();
    private double pts = start;

    public ExternalClock()
    {
        reset();
    }

    /**
     * Update our PTS if not paused, and return the PTS.
     *
     * @return The current presentation timestamp, updated if not paused
     */
    public double getClock()
    {
        if (!paused) pts = Clock.timeInSeconds() - start;
        return pts;
    }

    /**
     * @return {@code true} if we are paused
     */
    public boolean isPaused()
    {
        return paused;
    }

    /**
     * @param paused Pass {@code true} to pause the clock, {@code false} to un-pause and update our start time
     */
    public synchronized void setPaused(boolean paused)
    {
        this.paused = paused;
        if (!paused) start = pts - start + Clock.timeInSeconds();
    }

    /**
     * Set our PTS and start time to the current system time.
     */
    public synchronized void reset()
    {
        pts = start = Clock.timeInSeconds();
    }
}
