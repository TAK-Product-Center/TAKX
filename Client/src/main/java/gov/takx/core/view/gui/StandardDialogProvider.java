package gov.takx.core.view.gui;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.ui.ISendAsCotDialog;
import gov.takx.api.ui.IStandardDialogProvider;
import gov.takx.core.mapobject.gui.SendAsCotDialog;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.function.Supplier;

/**
 * Provider to create common dialogs shared between the core and plugins to support common UI/UX.
 *
 * @since 5.3
 */
@ApplicationScoped
public class StandardDialogProvider implements IStandardDialogProvider
{
    @Override
    public ISendAsCotDialog newSendAsCotDialog(@Nonnull String itemName, @Nonnull Supplier<List<CotEvent>> cotEventSupplier, @Nonnull Runnable nullCotEventErrorHandler)
    {
        return new SendAsCotDialog(itemName, cotEventSupplier, nullCotEventErrorHandler);
    }
}
