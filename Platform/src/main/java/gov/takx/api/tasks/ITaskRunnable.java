package gov.takx.api.tasks;

import jakarta.annotation.Nonnull;

/**
 * Interface to implement for tasks that can be submitted by a delegate. Tasks are submitted to the TAKX
 * core and run on the executor service with a progress and status showing up in the TAKX status bar at the bottom
 * of the frame. It is up to the task to update the progress monitor that is injected in the method
 * {@link #setProgressMonitor(ITaskProgressMonitor)}. It is also very important to call the method
 * {@link ITaskProgressMonitor#setComplete()} after the task is finished.
 * <p>
 * Plugin developers are encouraged to extend {@link ATaskRunnable} which implements most of the methods with
 * common default values.
 *
 * @since 2.14
 */
public interface ITaskRunnable extends Runnable
{
    /**
     * @return Task name.  Must not be {@code null}.
     */
    @Nonnull
    String getName();

    /**
     * Does the task support canceling an operation that has started but not finished? Return true if the user
     * should have the option to cancel the operation, or false if cancellation is not supported.
     *
     * @return {@code true} if the task supports cancellation, or {@code false} if not.
     * @see #doCancel()
     */
    boolean allowCancel();

    /**
     * Perform the actual cancel operation.
     */
    void doCancel();

    /**
     * Called to determine if this task needs to present a message to the user to verify before exit.
     *
     * @return {@code true} if exit should be allowed without querying the user, {@code false} if this task
     * is in a state where the user should be queried exiting takx.
     */
    boolean allowExit();

    /**
     * @return The message to display to the user if {@link #allowExit()} returns {@code false}
     */
    String getExitMessage();

    /**
     * Injects the progress monitor associated with this task.  Plugins which submit tasks should make sure to call
     * the {@link ITaskProgressMonitor#setComplete()} when the task is finished.
     *
     * @param monitor progress monitor
     */
    void setProgressMonitor(ITaskProgressMonitor monitor);
}
