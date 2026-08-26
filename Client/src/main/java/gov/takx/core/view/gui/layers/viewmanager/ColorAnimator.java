package gov.takx.core.view.gui.layers.viewmanager;

import gov.nasa.worldwind.util.WWMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.util.Timer;
import java.util.TimerTask;

/**
 */
public class ColorAnimator implements IAnimator<Color>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Component c;
    private long startTime;
    private long duration;
    private long halfPeriod;
    private boolean active;
    private Color startColor;
    private Color endColor;
    private Timer timer;
    private boolean started;

    /**
     * @param c          Owning component to be repainted (animated).
     * @param startColor color at start of animation (and beginning of each period).
     * @param endColor   color at the midpoint of the animation period.
     * @param halfPeriod duration (in ms) of half the animation period.  For example, if 500, the animation will
     *                   start at startColor, then gradually fade to endColor for 500 ms, then fade back to
     *                   startColor for another 500 ms, for a full period of 1000 ms.
     * @param duration   duration (in ms) of the entire animation.  If it is greater than the full period, then the
     *                   animation will loop until the duration is elapsed.
     */
    public ColorAnimator(Component c, Color startColor, Color endColor, long halfPeriod, long duration)
    {
        this.c = c;
        startTime = -1;
        this.duration = duration;
        active = false;
        this.startColor = startColor;
        this.endColor = endColor;
        this.halfPeriod = halfPeriod;
        started = false;
        timer = new Timer();
    }

    /**
     * Start the color animation.  The start time is captured to calculate the color at a given moment.
     */
    @Override
    public synchronized void start()
    {
        if (!started)
        {
            startTime = System.currentTimeMillis();
            started = true;
            active = true;

            if (timer != null)
            {
                // Have timer kill itself after the duration has elapsed.
                timer.schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        stop();
                    }
                }, duration + 2000);
            }
        }
    }

    /**
     * Stop the animation.
     */
    @Override
    public synchronized void stop()
    {
        active = false;
    }

    /**
     * Get the next color in the animation.
     */
    @Override
    public synchronized Color next()
    {
        if (!hasNext())
        {
            return endColor;
        }
        double elapsed = getElapsed();

        // The idea here is to trigger another paint IN THE FUTURE, after the color returned from this call has been
        // used.  Assuming that next() is called in a paint method, this should result in the desired behavior.
        // Repaints should be called as long as the animator hasn't elapsed its duration.
        if (c != null && timer != null)
        {
            timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    c.repaint();
                }
            }, 33);
        }

        if (elapsed <= 0)
        {
            return startColor;
        } else if (elapsed >= duration)
        {
            return endColor;
        }

        int periodsElapsed = (int) (elapsed / halfPeriod);
        elapsed -= (halfPeriod * periodsElapsed);
        double fraction = elapsed / halfPeriod;
        boolean reverse = periodsElapsed % 2 == 1;
        if (reverse)
        {
            fraction = 1 - fraction;
        }
        int r = clampChannel(WWMath.mixSmooth(fraction, startColor.getRed(), endColor.getRed()));
        int g = clampChannel(WWMath.mixSmooth(fraction, startColor.getGreen(), endColor.getGreen()));
        int b = clampChannel(WWMath.mixSmooth(fraction, startColor.getBlue(), endColor.getBlue()));
        if (logger.isTraceEnabled())
        {
            logger.trace("r: {}, g: {}, b: {}, elapsed: {} ms", r, g, b, elapsed);
        }
        return new Color(r, g, b);
    }

    /**
     * Determine whether the animation is active.  If the animation duration has elapsed, then end the animation.
     */
    @Override
    public synchronized boolean hasNext()
    {
        if (!active)
        {
            return false;
        }
        long elapsed = getElapsed();
        if (elapsed > duration)
        {
            stop();
            if (timer != null)
            {
                timer.cancel();
                timer = null;
            }
        }
        return active;
    }

    /**
     * Determine whether the animation has started.
     */
    @Override
    public synchronized boolean hasStarted()
    {
        return started;
    }

    private long getElapsed()
    {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Clamp an RGB color channel to 0-255 range.
     */
    private int clampChannel(double channel)
    {
        int c = (int) channel;
        if (c < 0)
        {
            return 0;
        } else if (c > 255)
        {
            return 255;
        }
        return c;
    }
}
