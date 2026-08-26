package gov.takx.api.tasks;

/**
 * Contract for managed tasks that are cancelabel.
 *
 * @since 1.42
 */
public interface ICancelableManagedTask extends IManagedTask
{
    /**
     * Does the task support canceling an operation that has started but not finished? Return true if the user
     * should have the option to cancel the operation, or false if cancellation is not supported.
     *
     * @return {@code true} if the task supports cancelation, or {@code false} if not.
     * @see #cancel()
     */
    public boolean allowCancel();

    /**
     * Called if the user chooses to cancel the operation. This method will never
     * be called if {@link #allowCancel()} returns {@code false}.  Do not block in this method waiting for the
     * cancel to complete or you may lock up the task manager.  Just mark the deletion and return promptly.
     *
     * @see #allowCancel()
     */
    public void cancel();

    /**
     * @return {@code true} if the task has been canceled, {@code false} otherwise
     */
    public boolean isCanceled();
}
