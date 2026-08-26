package gov.takx.api.ui;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.function.Supplier;

/**
 * There is no single UI for TAKX. Nor is there a single way of interacting with the user.
 * <p>
 * So, we need a mechanism to allow various parts of the core to interact with the "user," regardless of the current
 * UI expression (if there even is a UI).
 * <p>
 * For now, I am creating methods to perform very specific interrogations of the user, so the implementing code
 * can perform operations unique to that operation.  If we find a way to make this more generic over time, then
 * great, but we need to start somewhere.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IUserProxy
{
    /**
     * Query the user whether they want to quit.  If there are background tasks that haven't been completed,
     * the user will be shown those pending operations to inform the decision on what to do.
     *
     * @param quitting {@code true} if we are asking what to do when quitting takx
     * @return {@code true} if the user agreed to quit/close
     */
    @Internal
    boolean confirmQuit(boolean quitting);

    /**
     * Ask the user to confirm that they want to upgrade the database on the project being opened.
     *
     * @param msg Message to display
     * @return {@code true} if yes, {@code false} if no
     */
    @Internal
    boolean confirmDatabaseUpgrade(String msg);

    /**
     * Ask the user to confirm some option.  This differs from {@link #reportError} and friends in that it requires
     * user input and causes the calling thread to block.
     *
     * @param title Dialog title
     * @param message Dialog message
     * @since 5.0
     */
    void notify(String title, String message);

    /**
     * Perform a user interaction, unlocking the main app frame temporarily to ensure the user can properly
     * interact with any UI element presented (typically dialogs).
     * 
     * @param operation The operation to perform
     * @return The return value of {@code operation}
     * @param <T> inferred return type
     * @since 5.2
     */
    default <T> T doWithoutApplicationLock(Supplier<T> operation)
    {
        return null; // Ignored by default
    }

    /**
     * Report an error that occurred writing to preferences to the user.
     *
     * @param cause Exception causing the error to be reported, can be {@code null}
     * @since 2.35
     */
    void reportPreferencesError(Throwable cause);

    /**
     * Report an error to the user.
     *
     * @param title Title for a display component (like a dialog), if used
     * @param msg   Message to display
     * @param cause Exception causing the error to be reported, can be {@code null}
     */
    void reportError(String title, String msg, Throwable cause);

    /**
     * Report a warning message to the user.
     *
     * @param title Title for a display component (like a dialog), if used
     * @param msg   Message to display
     * @since 2.30
     */
    void reportWarning(String title, String msg);

    /**
     * Report a message to the user.
     *
     * @param title Title for a display component (like a dialog), if used
     * @param msg   Message to display
     * @since 2.20
     */
    void reportMessage(String title, String msg);
}
