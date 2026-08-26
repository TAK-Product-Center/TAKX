package gov.takx.core.view.gui;

import gov.takx.api.platform.IProgressMonitor;
import jakarta.annotation.Nonnull;

import java.awt.Component;
import java.util.function.Supplier;

/**
 * Interface to represent the app frame (so it can be stubbed out when headless).
 *
 * @since 3.3
 */
public interface IAppFrame
{
    /**
     * This unlocks or locks all the ui components in the app frame by setting the root pane visible or not. This
     * is used to disable any interaction with the app frame while the project is loading or is being shut down. It is also
     * used to enable interaction once the product is loaded.
     *
     * @param isLocked Pass {@code true} to lock the UI, {@code false} to unlock it
     */
    void setAppFrameLocked(boolean isLocked);

    /**
     * Perform a user interaction, unlocking the main app frame temporarily to ensure the user can properly
     * interact with any UI element presented (typically dialogs).
     *
     * @param operation The operation to perform
     * @return The return value of {@code operation}
     * @param <T> inferred return type
     * @since 5.2
     */
    <T> T doWithoutLock(Supplier<T> operation);

    /**
     * Make the progress dialog visible with an indeterminate status and the given message.
     *
     * @param status Status message to display
     */
    void showProgressDialog(String status);

    /**
     * @return A progress dialog for displaying status.
     * @since 3.3
     */
    IProgressMonitor getProgressMonitor();

    /**
     * Close and dispose of the progress dialog.
     */
    void disposeProgressDialog();

    /**
     * Sets whether this frame is resizable by the user.
     * @param    resizable   {@code true} if this frame is resizable;
     *                       {@code false} otherwise.
     * @see      java.awt.Frame#isResizable
     */
    void setResizable(boolean resizable);

    /**
     * Add the given component to the "persistent" area of the main app frame.  These components are always visible,
     * and the user has no mechanism to control that. It is up to the plugin to make any user accommodations.
     *
     * @param component Component to add
     * @see gov.takx.api.ui.IUISpaceManager#addPersistentComponent(Component)
     * @since 5.4
     */
    void addPersistentComponent(@Nonnull Component component);

    /**
     * Remove the given component to the "persistent" area of the main app frame.
     *
     * @param component Component to remove
     * @see gov.takx.api.ui.IUISpaceManager#removePersistentComponent(Component)
     * @since 5.4
     */
    void removePersistentComponent(@Nonnull Component component);
}
