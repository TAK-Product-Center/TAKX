package gov.tak.platform.chat.client;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import gov.tak.api.chat.IChatMessageFinder;
import gov.tak.api.chat.IChatService;
import gov.tak.api.chat.IChatServiceListener;
import gov.tak.api.chat.protocol.IChatProtocolHandler;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.chat.ChatReceipt;
import gov.tak.platform.chat.ChatService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Consumer of the {@link IChatService}, capable of receiving messages to display to the end user. Note that, because
 * a chat service consumer effectively represents the end user, only one consumer can be registered with the chat
 * service at any given time.
 * <p>
 * The client registered with the {@link IChatService} via its
 * {@link ChatService#start(IChatProtocolHandler, IChatServiceClient, IContactService)} method.
 *
 * @see IChatService
 * @since 3.0 (moved from gov.tak.api in 3.4)
 */
@NoPluginImplementations
@IncubatingApi(since = "3.0")
public interface IChatServiceClient extends IChatServiceListener
{
    /**
     * @return The unique ID of the consumer
     */
    @Nonnull
    String getUid();

    /**
     * @return The display name of the consumer (e.g. a callsign), shown to all users communicating with this consumer
     */
    @Nonnull
    String getName();

    /**
     * @return The current location of the consumer, represented as a GeoPoint
     */
    @Nonnull
    GeoPoint getLocation();

    /**
     * @param receipt The message receipt to process
     * @since 3.4
     */
    void receiveReceipt(@Nonnull ChatReceipt receipt);

    /**
     * Set the chat service that will be used to send messages and issue group requests.
     *
     * @param chatService The chat service used to access general chat features
     * @param delegate The delegate used to access client-specific chat features.
     * @since 3.4
     */
    void setChatService(@Nonnull ChatService chatService, @Nonnull IChatServiceClientDelegate delegate);

    /**
     * Returns a builder that creates a new outgoing {@link IChatMessage}. The sender UID and name will automatically
     * be set to the client's values.
     *
     * @param text The message text
     * @param sendTime The send time
     * @param conversationUid The conversation UID. For direct conversations, this is expected to be the UID of the other
     *                        user. For group conversations, it should be the UID of the {@link IGroupContact} that
     *                        represents the group conversation.
     * @return a new {@link gov.tak.api.chat.IChatMessage} builder instance.
     * @since 3.4
     */
    @Nonnull
    IChatMessageBuilder getChatMessageBuilder(@Nonnull String text, long sendTime, @Nonnull String conversationUid);

    /**
     * Returns a builder that creates a new {@link IChatMessage}.
     *
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @param text The message text
     * @param sendTime The send time
     * @param conversationUid The conversation UID. For direct conversations, this is expected to be the UID of the other
     *                        user. For group conversations, it should be the UID of the {@link IGroupContact} that
     *                        represents the group conversation.
     * @return a new {@link gov.tak.api.chat.IChatMessage} builder instance.
     * @since 3.5
     */
    @Nonnull
    IChatMessageBuilder getChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName,
                                              @Nonnull String text, long sendTime, @Nonnull String conversationUid);

    /**
     * @return an {@link IChatMessage} finder service.
     * @since 3.4
     */
    @Nonnull
    IChatMessageFinder getChatMessageFinder();
}
