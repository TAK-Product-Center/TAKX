package gov.takx.core.timer;

import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.timer.ITimerService;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implementation of the timer service for the TAKX core.
 *
 * @see ITimerService
 */
@ApplicationScoped
public class TimerService implements ITimerService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_TIMER_NAME = "Default TAKX Timer Service";

    /**
     * Our default timer instance, always available.
     */
    private static Timer DEFAULT_TIMER = new Timer(DEFAULT_TIMER_NAME, true);

    /**
     * The map from timer name to timer instance.
     */
    private final Map<String, Timer> timerMap = new HashMap<>();

    @Override
    public Timer getTimer(@Nonnull String timerName)
    {
        return getOrCreateTimer(timerName);
    }

    @Override
    public void destroyTimer(@Nonnull String timerName)
    {
        Timer timer = timerMap.remove(timerName);
        if (timer != null)
        {
            timer.cancel();
            timer.purge();
        }
    }

    @Override
    public TimerTask createTask(@Nonnull Runnable runnable, long delay)
    {
        TimerTask task = new TargettableTimerTask(runnable);
        DEFAULT_TIMER.schedule(task, delay);
        return task;
    }

    @Override
    public TimerTask createTask(@Nonnull String timerName, @Nonnull Runnable runnable, long delay)
    {
        Timer timer = getOrCreateTimer(timerName);
        TimerTask task = new TargettableTimerTask(runnable);
        timer.schedule(task, delay);
        return task;
    }

    @Override
    public void schedule(@Nonnull TimerTask task, long delay)
    {
        DEFAULT_TIMER.schedule(task, delay);
    }

    @Override
    public void schedule(@Nonnull String timerName, @Nonnull TimerTask task, long delay)
    {
        getOrCreateTimer(timerName).schedule(task, delay);
    }

    @Override
    public void scheduleAtFixedRate(@Nonnull TimerTask task, long delay, long period)
    {
        DEFAULT_TIMER.scheduleAtFixedRate(task, delay, period);
    }

    @Override
    public void scheduleAtFixedRate(@Nonnull String timerName, @Nonnull TimerTask task, long delay, long period)
    {
        getOrCreateTimer(timerName).scheduleAtFixedRate(task, delay, period);
    }

    /**
     * Get or create the named timer.
     *
     * @param timerName The name of the timer on which to schedule the task, must not be empty
     * @return Timer instance (added to the map)
     */
    private synchronized Timer getOrCreateTimer(@Nonnull String timerName)
    {
        timerName = timerName.trim();
        if (timerName.isEmpty()) throw new IllegalArgumentException("timer name must not be empty");

        Timer timer = timerMap.get(timerName);

        if (timer == null)
        {
            // No timer by this name, create it
            timer = new Timer(timerName, true);
            timerMap.put(timerName, timer);
        }

        return timer;
    }

    /**
     * Respond to a project closing event by canceling and recreating our default timer and canceling all other
     * timers created during the life of the current project.
     */
    private void projectClosing(@SuppressWarnings("UnusedParameters") @Observes ProjectClosing event)
    {
        // Need to cancel all the timers
        DEFAULT_TIMER.cancel();
        DEFAULT_TIMER = new Timer(DEFAULT_TIMER_NAME, true); // Recreate default timer

        timerMap.values().forEach(Timer::cancel);
        timerMap.clear();
    }

    /**
     * Trivial class to provide a TimerTask that runs the specified runnable and catches/reports exceptions to
     * prevent the timer thread from exiting.
     */
    private static class TargettableTimerTask extends TimerTask
    {

        private Runnable targetRunnable;

        /**
         * Constructor.
         *
         * @param targetRunnable Target runnable to execute
         */
        TargettableTimerTask(Runnable targetRunnable)
        {
            this.targetRunnable = targetRunnable;
        }

        @Override
        public void run()
        {
            // Avoid letting the runnable throw an exception, which causes the timer to exit and become unusable.

            try
            {
                targetRunnable.run();
            } catch (Throwable e)
            {
                logger.error("Timer task failed with exception", e);
            }
        }
    }
}
