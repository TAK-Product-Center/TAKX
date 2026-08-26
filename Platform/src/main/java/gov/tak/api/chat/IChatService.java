package gov.tak.api.chat;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.protocol.IChatProtocolHandler;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for the chat service. Plugins can issue chat-related requests and {@link IChatProtocolHandler}s can
 * register to handle chat protocols.
 *
 * @since 3.0
 */
@IncubatingApi(since = "3.0")
@NoPluginImplementations
public interface IChatService
{
    /**
     * @return The default chat protocol supported by this host
     * @since 3.4
     */
    @Nonnull
    String getDefaultProtocol();

    /**
     * Registers a chat service provider capable of handling the sending/receiving of messages through an underlying
     * transport protocol.
     *
     * @param handler The protocol handler to register
     * @throws IllegalArgumentException If the given handler handles the same protocol as our default handler,
     *                                  or when another handler with the same capabilities already is registered
     * @throws IllegalStateException If the service has not been started yet
     * @see #getDefaultProtocol()
     */
    void registerProtocolHandler(@Nonnull IChatProtocolHandler handler) throws IllegalArgumentException, IllegalStateException;

    /**
     * Unregisters a protocol handler that was previously registered, or does nothing in the event that the given
     * handler is not registered.
     *
     * @param handler The protocol handler to unregister
     * @throws IllegalArgumentException If the given handler handles the same protocol as our default handler
     * @see #getDefaultProtocol()
     */
    void unregisterProtocolHandler(@Nonnull IChatProtocolHandler handler) throws IllegalArgumentException;

    /**
     * @param listener The listener to add.
     * @since 3.4
     */
    void addListener(@Nonnull IChatServiceListener listener);

    /**
     * @param listener The listener to remove.
     * @since 3.4
     */
    void removeListener(@Nonnull IChatServiceListener listener);

    /**
     * Attempts to send a chat message to the given target contact.
     *
     * @param message The message to send
     * @param callback Optional callback that is notified with the message send request result
     * @throws IllegalArgumentException if the given contact does not have any supported protocols declared as an attribute
     * @since 3.4
     */
    void sendMessage(@Nonnull IChatMessage message, @Nullable IChatOperationCallback callback)
            throws IllegalArgumentException;

    /**
     * Returns a client-specific builder that creates a new outgoing {@link IChatMessage}.
     *
     * @param text The message text
     * @param sendTime The send time
     * @param conversationUid The conversation UID. For direct conversations, this is expected to be the UID of the other
     *                        user. For group conversations, it should be the UID of the
     *                        {@link gov.tak.api.contact.IGroupContact} that represents the group conversation.
     * @return a new {@link gov.tak.api.chat.IChatMessage} builder instance.
     * @since 3.4
     */
    @Nonnull
    IChatMessageBuilder getChatMessageBuilder(@Nonnull String text, long sendTime, @Nonnull String conversationUid);

    /**
     * @return an {@link IChatMessage} finder service.
     * @since 3.4
     */
    @Nonnull
    IChatMessageFinder getChatMessageFinder();
}
