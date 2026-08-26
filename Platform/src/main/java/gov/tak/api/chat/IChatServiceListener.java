package gov.tak.api.chat;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

/**
 * The contract for a listener that is notified of chat events.
 *
 * @since 3.4
 * @see IChatService#addListener(IChatServiceListener)
 */
@IncubatingApi(since = "3.4")
@SuppressWarnings("unused")
public interface IChatServiceListener
{
    /**
     * Called when a message is received.
     *
     * @param sender The sender
     * @param message The message that was received
     */
    default void messageReceived(@Nonnull IContact sender, @Nonnull IChatMessage message)
    {
    }

    /**
     * Called when a message is sent.
     *
     * @param recipient The target recipient
     * @param message The message that was sent to the recipient
     */
    default void messageSent(@Nonnull IContact recipient, @Nonnull IChatMessage message)
    {
    }
}
