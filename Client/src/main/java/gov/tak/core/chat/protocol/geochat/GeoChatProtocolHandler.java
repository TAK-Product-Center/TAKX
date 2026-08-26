package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.protocol.IChatProtocolHandler;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.platform.contact.GroupContactBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.tak.core.chat.protocol.geochat.GeoChatConstants.CLIENT_OWNED_GROUP_ATTRIBUTE;
import static gov.tak.core.chat.protocol.geochat.GeoChatConstants.GEOCHAT_PROTOCOL;

/**
 * Default {@link IChatProtocolHandler} responsible for managing the sending and receiving of messages using the GeoChat
 * chat protocol. In most cases, this protocol handler will be communicating solely with other TAK-based users available
 * on the network.
 *
 * @since 3.0
 */
public class GeoChatProtocolHandler implements IChatProtocolHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Commo commo;
    private GeoChatContactProvider geoChatContactProvider;
    private GeoChatMessageSender geoChatMessageSender;
    private GeoChatMessageReceiver geoChatMessageReceiver;
    private GeoChatFailureListener geoChatFailureListener;

    private final String clientUid;
    private final String clientName;
    private final IContactService contactService;

    /**
     * Constructs the GeoChat protocol handler using the given Commo instance, client UID, and client name.
     *
     * @param commo The Commo instance to be used by the GeoChat service. In the {@link IChatProtocolHandler} context,
     *              Commo is primarily responsible for lower-level network operations such as sending and receiving data
     *              through a network socket.
     * @param clientUid The client UID to be used by the GeoChat service when identifying the client node
     * @param clientName The client name to be used by the GeoChat service when representing the client node
     * @param contactService The service used to manage {@link IContact}s.
     */
    public GeoChatProtocolHandler(@Nonnull Commo commo, @Nonnull String clientUid, @Nonnull String clientName,
                                  @Nonnull IContactService contactService)
    {
        this.commo = commo;
        this.clientUid = clientUid;
        this.clientName = clientName;
        this.contactService = contactService;
    }

    @Override
    public void start(@Nonnull IChatProtocolHandlerDelegate delegate)
    {
        geoChatContactProvider = new GeoChatContactProvider(commo, contactService);
        geoChatMessageSender = new GeoChatMessageSender(commo, delegate, geoChatContactProvider,
                new GeoChatMessageAssembler(delegate));
        geoChatMessageReceiver = new GeoChatMessageReceiver(delegate, geoChatContactProvider,
                new GeoChatMessageParser(geoChatContactProvider));
        geoChatFailureListener = new GeoChatFailureListener();

        geoChatContactProvider.start(clientUid, clientName);
        geoChatContactProvider.populateContactsFromGeoChat();
        commo.addContactPresenceListener(geoChatContactProvider);
        commo.addCoTMessageListener(geoChatMessageReceiver);
        commo.addCoTSendFailureListener(geoChatFailureListener);
    }

    @Override
    public void stop()
    {
        commo.removeContactPresenceListener(geoChatContactProvider);
        commo.removeCoTMessageListener(geoChatMessageReceiver);
        commo.removeCoTSendFailureListener(geoChatFailureListener);

        if (geoChatContactProvider != null) geoChatContactProvider.stop();

        geoChatContactProvider = null;
        geoChatMessageSender = null;
        geoChatMessageReceiver = null;
        geoChatFailureListener = null;
    }

    @Nonnull
    @Override
    public String getProtocol()
    {
        return GEOCHAT_PROTOCOL;
    }

    @Nonnull
    @Override
    public IGroupContact getDefaultGroupContact()
    {
        return (IGroupContact) geoChatContactProvider.getContact(GeoChatRootGroupContact.GROUPS_ROOT_UID);
    }

    @Nonnull
    @Override
    public ChatOperationResult sendMessage(@Nonnull IContact targetContact, @Nonnull IChatMessage message)
    {
        ChatOperationResult.Status status = ChatOperationResult.Status.SUCCESS;
        String resultMessage = null;

        try
        {
            if (targetContact instanceof GeoChatAllChatRoomsContact)
            {
                geoChatMessageSender.broadcastToAllChatRooms(message);
            } else if (targetContact instanceof IIndividualContact individualContact)
            {
                geoChatMessageSender.sendMessageToIndividualContact(individualContact, message);
            } else if (targetContact instanceof IGroupContact groupContact)
            {
                // Groups, Teams and all roles
                if (groupContact.getParentContact() == null)
                {
                    geoChatMessageSender.sendMessageToTopLevelGroup((IGroupContact) targetContact, message);
                } else
                {
                    geoChatMessageSender.sendMessageToGroupContact(groupContact, message);
                }

            } else
            {
                status = ChatOperationResult.Status.FAILURE;
                resultMessage = "Received request to send message to unsupported contact type: "
                        + targetContact.getClass().getName();
            }
        } catch (CommoException | CommoContactMissingException e)
        {
            logger.error("Exception occurred while attempting to send chat message.", e);
            status = ChatOperationResult.Status.FAILURE;
            resultMessage = e.getMessage();
        }

        return new ChatOperationResult(ChatOperationResult.Operation.SEND_MESSAGE, status, resultMessage, targetContact);
    }

    @Nonnull
    @Override
    public ChatOperationResult sendReceipt(@Nonnull IIndividualContact contact, @Nonnull String messageUid, boolean read)
    {
        ChatOperationResult.Status status = ChatOperationResult.Status.SUCCESS;
        String resultMessage = null;

        try
        {
            geoChatMessageSender.sendReceipt(contact, messageUid, read);
        } catch (CommoException | CommoContactMissingException e)
        {
            logger.error("Error sending chat receipt for message {} to contact {}", messageUid, contact, e);
            status = ChatOperationResult.Status.FAILURE;
            resultMessage = e.getMessage();
        }

        return new ChatOperationResult(ChatOperationResult.Operation.SEND_RECEIPT, status, resultMessage, contact);
    }

    @Nonnull
    @Override
    public ChatOperationResult createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers,
                                           @Nullable IGroupContact parentGroup)
    {
        IGroupContact groupContact = new GroupContactBuilder()
                .withDisplayName(groupName)
                .withAttributes(GeoChatContactProvider.createDefaultGeoChatContactAttributeSet())
                .withGroupMembers(groupMembers)
                .withParentContact(parentGroup).build();

        groupContact.getAttributes().setAttribute(CLIENT_OWNED_GROUP_ATTRIBUTE, true);

        ChatOperationResult.Status status = ChatOperationResult.Status.SUCCESS;
        String resultMessage = null;

        try
        {
            geoChatMessageSender.sendGroupCreationNotifications(groupContact);
            geoChatContactProvider.addOwnedGroupContact(groupContact);
        } catch (CommoException e)
        {
            logger.error("Exception occurred while attempting to create GeoChat group contact.", e);

            status = ChatOperationResult.Status.FAILURE;
            resultMessage = e.getMessage();
            groupContact = null;
        }

        return new ChatOperationResult(ChatOperationResult.Operation.CREATE_GROUP, status, resultMessage, groupContact);
    }

    @Nonnull
    @Override
    public ChatOperationResult deleteGroup(@Nonnull IGroupContact groupContact)
    {
        ChatOperationResult.Status status = ChatOperationResult.Status.SUCCESS;
        String resultMessage = null;

        try
        {
            geoChatMessageSender.sendGroupDeletionNotifications(groupContact);
        } catch (CommoException e)
        {
            logger.error("Exception occurred while attempting to delete GeoChat group contact.", e);
            status = ChatOperationResult.Status.FAILURE;
            resultMessage = e.getMessage();
        }

        geoChatContactProvider.removeGroup(groupContact);

        return new ChatOperationResult(ChatOperationResult.Operation.DELETE_GROUP, status, resultMessage, groupContact);
    }

    @Nonnull
    @Override
    public ChatOperationResult updateGroup(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> groupMembers)
    {
        final Set<IContact> currentGroupMembers = groupContact.getGroupMembers();

        final var addedMembers = groupMembers.stream()
                .filter(contact -> !currentGroupMembers.contains(contact))
                .collect(Collectors.toSet());

        final var removedMembers = currentGroupMembers.stream()
                .filter(contact -> !groupMembers.contains(contact))
                .collect(Collectors.toSet());

        groupContact.setGroupMembers(groupMembers);

        var status = ChatOperationResult.Status.SUCCESS;
        String resultMessage = null;

        try
        {
            geoChatMessageSender.sendGroupUpdateNotifications(groupContact, addedMembers, removedMembers);
            geoChatMessageSender.sendGroupEvictionNotifications(groupContact, removedMembers);
            geoChatContactProvider.updateContact(groupContact); // triggers database update
        } catch (CommoException e)
        {
            logger.error("Exception occurred while attempting to update GeoChat group contact.", e);
            status = ChatOperationResult.Status.FAILURE;
            resultMessage = e.getMessage();
        }

        return new ChatOperationResult(ChatOperationResult.Operation.UPDATE_GROUP, status, resultMessage, groupContact);
    }
}
