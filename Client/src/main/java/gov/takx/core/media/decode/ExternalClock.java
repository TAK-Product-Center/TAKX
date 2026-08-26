package gov.takx.core.media.decode;

/**
 * A clock that can be paused and unpaused
 *
 * @since 2.8
 */
public class ExternalClock
{
    private volatile boolean paused = false;
    private double start = Clock.timeInSeconds();
    private double pts = start;
    private boolean enabled = true;

    public ExternalClock()
    {
        reset();
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public double getClock()
    {
        if (!paused)
        {
            pts = Clock.timeInSeconds() - start;
        }
        return pts;
    }

    public boolean isPaused()
    {
        return paused;
    }

    public synchronized void setPaused(boolean paused)
    {
        this.paused = paused;
        if (!paused)
        {
            start = pts - start + Clock.timeInSeconds();
        }
    }

    public synchronized void reset()
    {
        start = Clock.timeInSeconds();
        pts = start;
    }
}
