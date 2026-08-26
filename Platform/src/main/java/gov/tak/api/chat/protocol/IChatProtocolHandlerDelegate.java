package gov.tak.api.chat.protocol;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import gov.tak.api.contact.IContact;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.chat.ChatReceipt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Delegate used by {@link IChatProtocolHandler}s to interact with the chat service.
 *
 * @since 3.0
 */
@IncubatingApi(since = "3.0")
public interface IChatProtocolHandlerDelegate
{
    /**
     * @return The unique ID of the client, or {@code null} if no client is currently registered with the chat service API
     */
    @Nullable
    String getClientUid();

    /**
     * @return The name of the client (e.g. a callsign), or {@code null} if no client is currently registered with the
     * chat service API. The display name is expected to be shown to other users available through the chat service.
     */
    @Nullable
    String getClientName();

    /**
     * @return The location of the client as a GeoPoint, or {@code null} if no client is currently registered with the chat
     * service API
     */
    @Nullable
    GeoPoint getClientLocation();

    /**
     * Returns a builder capable of building persistable {@link IChatMessage}s. For outgoing messages, the sender's name 
     * and UID should be set to the local client's name and UID that can be obtained with the {@link #getClientName()}
     * and {@link #getClientUid()} methods.
     * 
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @param text The message text
     * @param sendTime The time the message was sent
     * @param conversationUid The conversation UID. For direct messages, use the sender's 
     * {@link gov.tak.api.contact.IIndividualContact} UID. For group messages, use the group's 
     * {@link gov.tak.api.contact.IGroupContact} UID.
     * @return A new chat message builder.
     * @since 3.5
     */
    @Nonnull
    IChatMessageBuilder getChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName,
                                              @Nonnull String text, long sendTime, @Nonnull String conversationUid);

    /**
     * Notifies the chat service that a message has been received and needs to be processed.
     *
     * @param contact The sender
     * @param message The message being received
     */
    void receiveMessage(@Nonnull IContact contact, @Nonnull IChatMessage message);

    /**
     * @param receipt The message receipt to process
     * @since 3.4
     */
    void receiveReceipt(@Nonnull ChatReceipt receipt);
}
