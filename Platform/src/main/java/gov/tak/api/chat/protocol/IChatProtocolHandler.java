package gov.tak.api.chat.protocol;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatService;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Contract for a handler responsible for sending messages and creating group contacts for the underlying chat protocol
 * implementation (e.g. XMPP). It's assumed that each chat protocol handler will be responsible for interacting with an
 * underlying chat protocol.
 * <p>
 * Handlers are registered with the {@link IChatService} to be notified of requests to send messages and manage group
 * management.
 *
 * @see IChatService#registerProtocolHandler(IChatProtocolHandler)
 * @see IChatService#unregisterProtocolHandler(IChatProtocolHandler)
 * @since 3.0 (renamed from IChatServiceProvider in 3.4)
 */
@IncubatingApi(since = "3.0")
public interface IChatProtocolHandler
{
    /**
     * Notify the handler that it should start.
     *
     * @param delegate The delegate used to access chat services.
     * @since 3.4
     */
    void start(@Nonnull IChatProtocolHandlerDelegate delegate);

    /**
     * Notify the handler that it should stop.
     *
     * @since 3.4
     */
    void stop();

    /**
     * @return The protocol that a given handler supports (e.g. GeoChat, XMPP, etc.)
     */
    @Nonnull
    String getProtocol();

    /**
     * @return the default group contact.
     * @since 3.4
     */
    @Nonnull
    IGroupContact getDefaultGroupContact();

    /**
     * Attempts to send a chat message to the given target contact.
     *
     * @param targetContact The contact to whom the chat message should be sent
     * @param message The message to send
     * @return The result of the request
     * @since 3.2
     */
    @Nonnull
    ChatOperationResult sendMessage(@Nonnull IContact targetContact, @Nonnull IChatMessage message);

    /**
     * Sends a confirmation receipt to the sender of a direct message.
     *
     * @param contact The sender
     * @param messageUid The id of the message being confirmed
     * @param read {@code true} if confirming that the message has been read
     * @return The result of the request
     * @since 3.4
     */
    @Nonnull
    ChatOperationResult sendReceipt(@Nonnull IIndividualContact contact, @Nonnull String messageUid, boolean read);

    /**
     * Requests that a new group be created.
     *
     * @param groupName The name of the group to create (i.e. the display name of the IGroupContact that will be created)
     * @param groupMembers The group members that should be affiliated with the created group contact
     * @param parentGroup The parent group of the contact to create
     * @return The result of the request
     * @since 3.2
     */
    @Nonnull
    ChatOperationResult createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers, @Nullable IGroupContact parentGroup);

    /**
     * Requests that a group be deleted.
     *
     * @param groupContact The group contact to delete
     * @return The result of the request
     * @since 3.2
     */
    @Nonnull
    ChatOperationResult deleteGroup(@Nonnull IGroupContact groupContact);

    /**
     * Requests that a group be updated.
     *
     * @param groupContact The group identifier
     * @param groupMembers The updated set of group members
     * @return The result of the request
     * @since 3.3 new signature
     */
    @Nonnull
    ChatOperationResult updateGroup(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> groupMembers);
}
