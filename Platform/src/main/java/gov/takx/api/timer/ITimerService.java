package gov.takx.api.timer;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The timer service offers a simple set of functionality for registering timed <em>events</em>.  It is layered on
 * top of the {@link java.util.Timer} class, and offers simplified mechanisms for scheduling tasks, sharing timer
 * threads, creating timers, etc.
 * <p>
 * Each timer has a name, and each named timer runs as its own thread (more concretely, each named timer is an
 * instance of {@link java.util.Timer}, which manages a thread to execute the tasks on that timer).
 * Each thread will stay in existence as long as it has tasks to execute and
 * there is an active reference to it.  It is the responsibility of the programmer to release timer instances
 * they create (if they are no longer needed) by calling {@link #destroyTimer(String)}.
 * There is a "default" unnamed timer that is available at all times.  All timer instances will be automatically
 * destroyed when the current project is closed.
 * <p>
 * As a consequence of the way that persistence services are related to threads, every task allocated to a given
 * timer instance will share a persist service instance.
 * <p>
 * In general, if you just need to register a task to execute at some point in the future, there is no need to
 * create a new, named timer instance.  Just register the task with the default timer.  There are a few cases
 * where a new, named timer instance would be reasonable:
 * <ul>
 * <li>If the task you're scheduling can take a long time to run (you don't want to affect other tasks)</li>
 * <li>If the timing of your task is fairly critical (you don't want other tasks to affect your task</li>
 * </ul>
 *
 */
@InjectableService
@NoPluginImplementations
public interface ITimerService
{
    /**
     * Get or create a timer with the given name.  The caller is responsible for releasing the timer created
     * by this method when it is no longer needed by calling {@link #destroyTimer(String)}.
     *
     * @param timerName The name of the timer, must not be {@code null} or empty.
     * @return A timer instance
     * @throws IllegalArgumentException If the timerName is empty
     * @see #destroyTimer(String)
     */
    Timer getTimer(@Nonnull String timerName);

    /**
     * Cancel any pending tasks on the given timer and release our reference to the timer (so it may be GC'ed).
     * If the named timer doesn't exist, then no action is taken.
     *
     * @param timerName The timer to destroy
     * @throws IllegalArgumentException If the timerName is empty
     */
    void destroyTimer(@Nonnull String timerName);

    /**
     * Create and register a {@link TimerTask} using the given runnable and the specified delay.  A new TimerTask
     * instance will be created that will execute the given runnable.  It will be scheduled for execution on
     * the default timer using the specified delay.
     *
     * @param runnable The runnable to execute
     * @param delay    delay in milliseconds before the task is to be executed
     * @return The created timer task, which can be used to cancel or otherwise manipulate the task as needed
     * @see java.util.TimerTask#cancel()
     */
    TimerTask createTask(@Nonnull Runnable runnable, long delay);

    /**
     * Create and register a {@link TimerTask} using the given runnable and the specified delay.  A new TimerTask
     * instance will be created that will execute the given runnable.  It will be scheduled for execution on
     * the timer with the given name using the specified delay.  The named timer will be created if needed.
     * The caller is responsible for releasing the timer created by this method when it is no longer needed
     * by calling {@link #destroyTimer(String)}.
     *
     * @param timerName The name of the timer on which to schedule the task, must not be {@code null} or empty
     * @param runnable  The runnable to execute
     * @param delay     delay in milliseconds before the task is to be executed
     * @return The created timer task, which can be used to cancel or otherwise manipulate the task as needed
     * @throws IllegalArgumentException If the timerName is empty
     * @throws IllegalStateException    if timer was cancelled or timer thread terminated.
     * @see #getTimer(String)
     */
    TimerTask createTask(@Nonnull String timerName, @Nonnull Runnable runnable, long delay);

    /**
     * Schedule the given {@link java.util.TimerTask} on the default timer with the given delay.
     *
     * @param task  The task to schedule, must not be {@code null}
     * @param delay delay in milliseconds before the task is to be executed
     * @throws IllegalArgumentException if <tt>delay</tt> is negative, or
     *                                  <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException    if task was already scheduled or cancelled.
     */
    void schedule(@Nonnull TimerTask task, long delay);

    /**
     * Schedule the given {@link java.util.TimerTask} on the named timer with the given delay.
     * The named timer will be created if needed.  The caller is responsible for releasing the timer created
     * by this method when it is no longer needed by calling {@link #destroyTimer(String)}.
     *
     * @param timerName The name of the timer on which to schedule the task, must not be {@code null} or empty
     * @param task      The task to schedule, must not be {@code null}
     * @param delay     delay in milliseconds before the task is to be executed
     * @throws IllegalArgumentException If the timerName is {@code null} or empty,
     *                                  if <tt>delay</tt> is negative, or
     *                                  <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException    if task was already scheduled or
     *                                  cancelled, timer was cancelled, or timer thread terminated.
     * @see #destroyTimer(String)
     */
    void schedule(@Nonnull String timerName, @Nonnull TimerTask task, long delay);

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution</i>,
     * beginning after the specified delay.  Subsequent executions take place
     * at approximately regular intervals, separated by the specified period.
     * The task is scheduled on the default timer.
     *
     * @param task   task to be scheduled.
     * @param delay  delay in milliseconds before task is to be executed.
     * @param period time in milliseconds between successive task executions.
     * @throws IllegalArgumentException if <tt>delay</tt> is negative, or
     *                                  <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException    if task was already scheduled or
     *                                  cancelled, timer was cancelled, or timer thread terminated.
     * @see Timer#scheduleAtFixedRate(java.util.TimerTask, long, long)
     */
    void scheduleAtFixedRate(@Nonnull TimerTask task, long delay, long period);

    /**
     * Schedules the specified task for repeated <i>fixed-rate execution</i>,
     * beginning after the specified delay on the named timer.  Subsequent executions take place
     * at approximately regular intervals, separated by the specified period.
     * The named timer will be created if needed.  The caller is responsible for releasing the timer created
     * by this method when it is no longer needed by calling {@link #destroyTimer(String)}.
     *
     * @param timerName The name of the timer on which to schedule the task, must not be {@code null} or empty
     * @param task      task to be scheduled.
     * @param delay     delay in milliseconds before task is to be executed.
     * @param period    time in milliseconds between successive task executions.
     * @throws IllegalArgumentException If the timerName is empty,
     *                                  if <tt>delay</tt> is negative, or
     *                                  <tt>delay + System.currentTimeMillis()</tt> is negative.
     * @throws IllegalStateException    if task was already scheduled or
     *                                  cancelled, timer was cancelled, or timer thread terminated.
     * @see Timer#scheduleAtFixedRate(java.util.TimerTask, long, long)
     * @see #destroyTimer(String)
     */
    void scheduleAtFixedRate(@Nonnull String timerName, @Nonnull TimerTask task, long delay, long period);
}
