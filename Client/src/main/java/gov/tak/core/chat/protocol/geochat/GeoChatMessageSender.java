package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Responsible for sending CoT-based GeoChat messages to other TAK users available through LAN and/or TAK Server.
 * <p>
 * When communicating with other TAK users, the usage of LAN and/or TAK Server is dependent upon the configuration of
 * the {@link Commo} instance provided when the GeoChat service is started. By default, if a given TAK user is available
 * through both LAN and TAK Server, the LAN pathway will be preferred.
 *
 * @since 3.2
 */
class GeoChatMessageSender
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Commo commo;
    private final IChatProtocolHandlerDelegate chatServiceDelegate;
    private final GeoChatContactProvider geoChatContactProvider;
    private final GeoChatMessageAssembler geoChatMessageAssembler;

    /**
     * @param commo                   The Commo instance used to send/broadcast CoT messages to other TAK users
     * @param chatServiceDelegate     The chat service delegate used to query the client's UID
     * @param geoChatContactProvider  The GeoChat contact provider used to check group ownership as well as contact existence
     * @param geoChatMessageAssembler The message assembler responsible for assembling CoT messages
     */
    GeoChatMessageSender(@Nonnull Commo commo, @Nonnull IChatProtocolHandlerDelegate chatServiceDelegate,
                         @Nonnull GeoChatContactProvider geoChatContactProvider, @Nonnull GeoChatMessageAssembler geoChatMessageAssembler)
    {
        this.commo = commo;
        this.chatServiceDelegate = chatServiceDelegate;
        this.geoChatContactProvider = geoChatContactProvider;
        this.geoChatMessageAssembler = geoChatMessageAssembler;
    }

    /**
     * Broadcasts a chat message to all TAK users currently available on the network in the "All Chat Rooms" group.
     * Typically uses UDP over multicast in order to broadcast said message.
     *
     * @param message The chat message to broadcast
     * @throws CommoException if an error occurs in the Commo library while attempting to broadcast the chat message
     */
    void broadcastToAllChatRooms(@Nonnull IChatMessage message) throws CommoException
    {
        final String cotMessage =
                geoChatMessageAssembler.assemblePeerToPeerChatMessage(GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME,
                        GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME, message);

        commo.broadcastCoT(cotMessage);
    }

    /**
     * Sends top-level group (e.g. "Groups", "Teams") direct messages to all contacts
     *
     * @param group The top-level room's group contact
     * @param message The chat message to send
     * @throws CommoException if an error occurs in the Commo library while attempting to send the chat message
     * @since 3.5
     */
    void sendMessageToTopLevelGroup(@Nonnull IGroupContact group, @Nonnull IChatMessage message) throws CommoException
    {
        final String uid = group.getUid();
        final String name = group.getName();

        for (Contact contact : commo.getContacts())
        {
            final String cotMessage =
                    geoChatMessageAssembler.assembleTopLevelGroupChatMessage(uid, name, contact.contactUID, message);
            commo.sendCoT(contact, cotMessage);
        }
    }

    /**
     * Sends a chat message to a specific TAK user on the network.
     *
     * @param recipient The TAK user that the message will be sent to
     * @param message   The chat message to send
     * @throws CommoException               if an error occurs in the Commo library while attempting to send the chat message
     * @throws CommoContactMissingException if a corresponding recipient contact cannot be found in Commo
     */
    void sendMessageToIndividualContact(@Nonnull IIndividualContact recipient, @Nonnull IChatMessage message)
            throws CommoException, CommoContactMissingException
    {
        sendMessage(
                recipient,
                requireCommoContact(recipient),
                () -> geoChatMessageAssembler.assemblePeerToPeerChatMessage(
                        recipient.getUid(),
                        recipient.getName(),
                        message),
                "Sending direct chat message");

    }

    /**
     * Sends a chat message to all members of a group contact.
     *
     * @param groupContact The group contact from which the message recipients are derived
     * @param message The chat message to send
     * @throws CommoException if an error occurs in the Commo library while attempting to send the chat message
     */
    void sendMessageToGroupContact(@Nonnull IGroupContact groupContact, @Nonnull IChatMessage message)
            throws CommoException
    {
        final String clientUid = chatServiceDelegate.getClientUid();

        for (IContact groupMember : groupContact.getGroupMembers())
        {
            if (groupMember instanceof IGroupContact || groupMember.getUid().equals(clientUid)) continue;

            sendMessage(
                    groupMember,
                    getCommoContact(groupMember),
                    () -> geoChatMessageAssembler.assembleGroupChatMessage(
                            groupMember.getUid(),
                            groupContact,
                            message,
                            geoChatContactProvider.isGroupOwnedByClient(groupContact.getUid())),
                    "Sending group chat message");
        }
    }

    /**
     * Sends a group-creation notification message to each member of a newly created group contact to inform them of the
     * new group and its membership.
     *
     * @param groupContact The group contact for which group-creation notifications are sent
     * @throws CommoException               if an error occurs in the Commo library while attempting to send the chat message
     */
    void sendGroupCreationNotifications(@Nonnull IGroupContact groupContact) throws CommoException
    {
        final String clientUid = chatServiceDelegate.getClientUid();

        for (IContact groupMember : groupContact.getGroupMembers())
        {
            if (groupMember.getUid().equals(clientUid)) continue;

            if (groupMember instanceof IGroupContact)
            {
                logger.warn("Received request to send group-creation message to a nested group member. " +
                        "This should never happen.");
                continue;
            }

            sendMessage(
                    groupMember,
                    getCommoContact(groupMember),
                    () -> geoChatMessageAssembler.assembleGroupCreationMessage(
                            groupMember.getUid(),
                            groupContact,
                            UUID.randomUUID().toString()),
                    "Sending group-creation notification");

        }
    }

    /**
     * Sends a group-deletion notification message to each member of a group contact to inform them of the group removal.
     *
     * @param groupContact The group contact for which group-deletion notifications are sent
     * @throws CommoException               if an error occurs in the Commo library while attempting to send a message
     */
    void sendGroupDeletionNotifications(@Nonnull IGroupContact groupContact) throws CommoException
    {
        final String clientUid = chatServiceDelegate.getClientUid();
        final Set<IContact> groupMembers = groupContact.getGroupMembers();

        for (IContact groupMember : groupMembers)
        {
            if (groupMember instanceof IGroupContact || groupMember.getUid().equals(clientUid)) continue;

            sendMessage(
                    groupMember,
                    getCommoContact(groupMember),
                    () -> geoChatMessageAssembler.assembleGroupDeletionMessage(
                            groupMember.getUid(),
                            groupContact,
                            UUID.randomUUID().toString()),
                    "Sending group-deletion notification");
        }
    }

    /**
     * Sends group-update notification messages to existing members of a group contact to inform them of the group
     * membership update.
     *
     * @param groupContact The group contact for which group-update notifications will be sent
     * @param added The contacts that were added to the group; may be empty
     * @param removed The contacts that were added to the group; may be empty
     * @throws CommoException               if an error occurs in the Commo library while attempting to send a message
     */
    void sendGroupUpdateNotifications(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> added, @Nonnull Set<IContact> removed)
            throws CommoException
    {
        final String clientUid = chatServiceDelegate.getClientUid();
        final String clientName = chatServiceDelegate.getClientName();

        // Create a detailed update message that includes the name of the editor and the contacts that were added/removed
        final var addedNames = added.stream().map(IContact::getName).collect(Collectors.joining(", "));
        final var removedNames = removed.stream().map(IContact::getName).collect(Collectors.joining(", "));
        final var detailedUpdateMessage = clientName + (!addedNames.isEmpty() ? " added " + addedNames : "")
                + (!removedNames.isEmpty() ? " removed " + removedNames : "");

        // Notify existing and updated group members of the group membership change
        for (IContact groupMember : groupContact.getGroupMembers())
        {
            final String groupMemberUid = groupMember.getUid();

            if (groupMember instanceof IGroupContact || groupMemberUid.equals(clientUid)) continue;

            final var commoContact = getCommoContact(groupMember);

            sendMessage(
                    groupMember,
                    commoContact,
                    () -> geoChatMessageAssembler.assembleGroupCreationMessage(
                            groupMemberUid,
                            groupContact,
                            UUID.randomUUID().toString()),
                    "Sending group-update notification");

            // Send additional message with detailed info about the editor and edits
            sendMessage(
                    groupMember,
                    commoContact,
                    () -> geoChatMessageAssembler.assembleDetailedGroupUpdatedMessage(
                            groupMemberUid,
                            groupContact,
                            UUID.randomUUID().toString(),
                            detailedUpdateMessage),
                    "Sending detailed group-update notification");
        }
    }

    /**
     * Sends group-eviction notification messages to kicked members of a group contact to inform them of their removal
     * from the group.
     *
     * @param groupContact The group contact for which group-eviction notifications will be sent
     * @throws CommoException if an error occurs in the Commo library while attempting to send a message
     */
    void sendGroupEvictionNotifications(@Nonnull IGroupContact groupContact, Set<IContact> removedContacts)
            throws CommoException
    {
        for (IContact removedGroupMember : removedContacts)
        {
            sendMessage(
                    removedGroupMember,
                    getCommoContact(removedGroupMember),
                    () -> geoChatMessageAssembler.assembleGroupMemberEvictionMessage(
                            removedGroupMember.getUid(),
                            groupContact,
                            UUID.randomUUID().toString()),
                    "Sending group chat message");
        }
    }

    /**
     * Sends a chat receipt to the sender of a message.
     *
     * @param sender The sender
     * @param messageUid The UID of the message being confirmed
     * @param read {@code true} if confirming that the message has been read
     * @throws CommoException if an error occurs in the Commo library while attempting to send a message
     * @throws CommoContactMissingException if a corresponding recipient contact cannot be found in Commo
     * @since 3.4
     */
    void sendReceipt(@Nonnull IIndividualContact sender, @Nonnull String messageUid, boolean read)
            throws CommoException, CommoContactMissingException
    {
        sendMessage(
                sender,
                requireCommoContact(sender),
                () -> geoChatMessageAssembler.assembleReceiptMessage(
                        sender.getUid(),
                        sender.getName(),
                        messageUid,
                        read),
                "Sending receipt");
    }

    /**
     * Returns the Commo {@link Contact} associated with the TAKX {@link IContact}
     * 
     * @param contact The TAKX {@link IContact}
     * @return The Commo {@link Contact}
     * @throws CommoContactMissingException if a Commo {@link Contact} does not exist for the {@code targetContact}
     * @since 3.4
     */
    @Nonnull
    private Contact requireCommoContact(@Nonnull IContact contact)
            throws CommoContactMissingException
    {
        final Contact commoContact = getCommoContact(contact);

        if (commoContact == null) throw new CommoContactMissingException(contact);

        return commoContact;
    }

    /**
     * Returns the Commo {@link Contact} associated with the TAKX {@link IContact}
     *
     * @param contact The TAKX {@link IContact}
     * @return The Commo {@link Contact} or {@code null} if there is no Commo {@link Contact} associated with the TAKX
     * {@code targetContact}
     * @since 3.4
     */
    @Nullable
    private Contact getCommoContact(@Nonnull IContact contact)
    {
        final String targetContactUid = contact.getUid();

        return Stream.of(commo.getContacts())
                .filter(commoContact -> commoContact.contactUID.equals(targetContactUid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Sends a message to the Commo {@link Contact} or logs warning if it's {@code null} 
     *
     * @param contact The TAKX {@link IContact} 
     * @param commoContact The Commo {@link Contact}
     * @param cotMessageSupplier The CoT message supplier
     * @param debugMessagePrefix The debug message prefix that describes the message being sent
     * @throws CommoException if there are errors sending the message through the Commo service
     * @since 3.4
     */
    private void sendMessage(@Nonnull IContact contact,
                             @Nullable Contact commoContact,
                             @Nonnull Supplier<String> cotMessageSupplier,
                             @Nonnull String debugMessagePrefix)
            throws CommoException
    {
        if (commoContact == null)
        {
            logger.warn("Unable to find commo contact for contact named {} with UID {}",
                    contact.getName(), contact.getUid());
            return;
        }

        final String cotMessage = cotMessageSupplier.get();
        logger.debug("{} to {}: {}", debugMessagePrefix, commoContact.contactUID, cotMessage);
        commo.sendCoT(commoContact, cotMessage);
    }
}
