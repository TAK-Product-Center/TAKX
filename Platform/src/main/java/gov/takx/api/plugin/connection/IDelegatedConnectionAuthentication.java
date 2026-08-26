package gov.takx.api.plugin.connection;

import gov.takx.commons.swing.IValidateAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * IPluginConnectionControllers may choose to delegate their username and password user interface interactions to the
 * TAKX core.  This service is especially useful for connections that require a username and password in order to
 * re-establish the connection on project restore.
 * <p/>
 * It is the responsibility of the IPluginConnectionController to manage its state (e.g. the last username) and to
 * forward along the TAKX captured username/password appropriately within the connection framework.
 * <p/>
 * If the IPluginConnectionController implements this interface, the credentials will be solicited only prior to the
 * invocation of the establishConnection() method on the controller.  Future calls to openConnection() will not re-
 * solicit the user for credentials.
 *
 * @since 1.3
 */
public interface IDelegatedConnectionAuthentication extends IValidateAction
{
    /**
     * @return Default username to pre-populate the UI with, or {@code null} if no default should be provided
     */
    default String getDefaultUsername()
    {
        return null;
    }

    /**
     * @return Optional message that will be displayed to the user above the authentication text field(s).
     * If {@code null}, a default message should be used.
     */
    @Nullable
    default String getMessage()
    {
        return null;
    }

    /**
     * @return Optional title for the authentication dialog.  If {@code null}, a default title will be used.
     */
    @Nullable
    default String getTitle()
    {
        return null;
    }

    /**
     * Indicates if the TAKX core should prompt for just the password.  If {@code true},
     * then no username field will be visible to the user.
     *
     * @return {@code true}, if only a password should be solicited from the user
     */
    default boolean requiresPasswordOnly()
    {
        return false;
    }

    /**
     * Sets the username, if it was supposed to be solicited, as entered by the user.
     * @see #requiresPasswordOnly()
     */
    void setUsername(@Nonnull String username);

    /**
     * Sets the password, as entered by the user.
     */
    void setPassword(@Nonnull char[] password);
}
