package gov.takx.core.mapobject.gui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Dialog that allows the user to either select contacts or request a broadcast. 
 *
 * @since 5.4
 */
public class SelectContactsDialog extends ASendAsDialog
{
    private final Consumer<Collection<String>> selectedContactsHandler;
    private final Runnable broadcastHandler;

    /**
     * @param title The dialog title
     * @param selectedContactsHandler Accepts the list of contact UIDs selected by the user
     * @param broadcastHandler Responds to a broadcast request. May be {@code null} if braodcasting is not supported.
     */
    public SelectContactsDialog(@Nonnull String title, @Nonnull Consumer<Collection<String>> selectedContactsHandler,
                                @Nullable Runnable broadcastHandler)
    {
        super(title);

        this.selectedContactsHandler = selectedContactsHandler;
        this.broadcastHandler = broadcastHandler;

        if (broadcastHandler == null) hideBroadcast();
    }

    @Override
    protected void send()
    {
        final var uids = getSelectedContactUids();
        if (!uids.isEmpty()) selectedContactsHandler.accept(uids);

        dispose();
    }

    @Override
    protected void broadcast()
    {
        dispose();

        if (broadcastHandler != null)
        {
            broadcastHandler.run();
        } else
        {
            throw new UnsupportedOperationException("Broadcast not supported");
        }
    }
}