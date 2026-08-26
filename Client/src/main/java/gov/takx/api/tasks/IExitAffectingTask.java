package gov.takx.api.tasks;

/**
 * Tasks that can/should affect the exit process.  Exit affecting tasks that are executing at the time the user
 * chooses to exit can provide a message to be shown to the user as part of a dialog to ensure they want to exit
 * and terminate said tasks.
 *
 * @since 1.42
 */
public interface IExitAffectingTask extends ICancelableManagedTask
{
    /**
     * Called to determine if this task needs to present a message to the user to verify before exit.
     *
     * @return {@code true} if exit should be allowed without querying the user, {@code false} if this task
     * is in a state where the user should be queried exiting takx.
     */
    public boolean allowExit();

    /**
     * @return The message to display to the user if {@link #allowExit()} returns {@code false}
     */
    public String getExitMessage();
}
