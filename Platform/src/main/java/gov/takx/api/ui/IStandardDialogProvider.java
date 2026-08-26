package gov.takx.api.ui;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Provides access to common dialogs used by the core that are also of use to plugins, to support common UI/UX.
 * <p/>
 *
 * @apiNote The factory methods here all return a distinct interface unique to the dialog of interest.  However, since
 * Swing {@link javax.swing.JDialog JDialog} is not an interface, these returned interfaces can't be used as a dialog
 * directly.  To provide access to the actual dialog, all of these returned interfaces extend {@link IStandardDialog},
 * which provides a {@link IStandardDialog#getDialog()} method.  From this, you can then perform operations on the
 * dialog itself, as needed.  Example usage:
 * <pre>{@code
 *
 * var sendAsCot = standardDialogProvider.newSendAsCotDialog("name",
 *                     () -> generateEvents(),
 *                     () -> displayError());
 * sendAsCot.getDialog().setVisible(true); // show the dialog
 * }</pre>
 *
 * @since 5.3
 */
@InjectableService
@NoPluginImplementations
public interface IStandardDialogProvider
{
    /**
     * Create a dialog that presents the user with options to broadcast  {@link CotEvent CoT Events} or send it to
     * specific groups or users.
     *
     * @param itemName The name of the item(s) to be sent (used in the dialog title)
     * @param cotEventSupplier Supplies the collection of CoT events to be sent after the user chooses recipients,
     *                         return {@code null} if CoT event(s) could not be generated
     * @param nullCotEventErrorHandler Called if the {@code cotEventSupplier} returns {@code null}
     * @return New dialog instance
     */
    ISendAsCotDialog newSendAsCotDialog(@Nonnull String itemName, @Nonnull Supplier<List<CotEvent>> cotEventSupplier,
                                        @Nonnull Runnable nullCotEventErrorHandler);
}
