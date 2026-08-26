package gov.takx.core.mapobject.gui;

import gov.tak.api.comms.commo.ICotEventSender;
import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.ISendAsCotDialog;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Presents the user with options to broadcast a {@link CotEvent} or send it to specific groups or users.
 *
 * @since 3.5
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SendAsCotDialog extends ASendAsDialog implements ISendAsCotDialog
{
    private final Supplier<List<CotEvent>> cotEventSupplier;
    private final Runnable nullCotEventErrorHandler;

    @Inject
    private ICotEventSender cotEventSender;

    /**
     * @param itemName The name of the item to be sent
     * @param cotEventSupplier Supplies the collection of CoT events to be sent after the user chooses recipients
     * @param nullCotEventErrorHandler Called if the cotEventSupplier returns a null {@link CotEvent}
     */
    public SendAsCotDialog(@Nonnull String itemName, @Nonnull Supplier<List<CotEvent>> cotEventSupplier,
                           @Nonnull Runnable nullCotEventErrorHandler)
    {
        super("Send As Cot: " + itemName);
        this.cotEventSupplier = cotEventSupplier;
        this.nullCotEventErrorHandler = nullCotEventErrorHandler;
    }

    /**
     * Sends the CoT event to selected contacts.
     */
    @Override
    protected void send()
    {
        processRequest(cotEvent -> {
            final Collection<String> selectedContactUids = getSelectedContactUids();
            if (!selectedContactUids.isEmpty()) cotEventSender.sendEvent(cotEvent, selectedContactUids);
        });
    }

    /**
     * Broadcasts the event to all users.
     */
    @Override
    protected void broadcast()
    {
        processRequest(cotEvent -> cotEventSender.broadcastEvent(cotEvent));
    }

    /**
     * Tries to retrieve the {@link CotEvent} and pass it to the Consumer. If the event cannot be retrieved, the 
     * {@link #nullCotEventErrorHandler} is called.
     * 
     * @param consumer The consumer that will process the {@link CotEvent} if it can be retrieved.
     */
    private void processRequest(@Nonnull Consumer<CotEvent> consumer)
    {
        final List<CotEvent> cotEventList = cotEventSupplier.get();

        if (cotEventList != null && !cotEventList.isEmpty())
        {
            for (CotEvent e : cotEventList)
            {
                if (e != null) consumer.accept(e);
            }
        } else
        {
            nullCotEventErrorHandler.run();
        }

        dispose();
    }
}
