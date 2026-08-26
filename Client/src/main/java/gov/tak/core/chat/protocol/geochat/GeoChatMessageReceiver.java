package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.CoTMessageListener;
import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.platform.chat.ChatReceipt;
import gov.tak.platform.contact.GroupContact;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.tak.platform.contact.IndividualContactBuilder;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 * Receives CoT messages through Commo as a {@link CoTMessageListener}. GeoChat messages are parsed and forwarded as
 * appropriate, while all other messages are checked only for the purpose of keeping contacts up-to-date.
 *
 * @since 3.0
 */
class GeoChatMessageReceiver implements CoTMessageListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The specific CoT types indicating a chat related message
    private static final Set<String> CHAT_MESSAGE_TYPES = Set.of(CotTypes.PREFIX_FREE_TEXT, CotTypes.FREE_TEXT_READ, CotTypes.FREE_TEXT_DELIVERED);

    private final IChatProtocolHandlerDelegate chatServiceDelegate;
    private final GeoChatMessageParser geoChatMessageParser;
    private final GeoChatContactProvider geoChatContactProvider;

    /**
     * @param chatServiceDelegate    The chat service delegate to which received messages are forwarded
     * @param geoChatContactProvider The GeoChat contact provider to manage contact updates through
     * @param geoChatMessageParser   The message parser responsible for parsing incoming CoT messages
     */
    GeoChatMessageReceiver(@Nonnull IChatProtocolHandlerDelegate chatServiceDelegate,
                           @Nonnull GeoChatContactProvider geoChatContactProvider,
                           @Nonnull GeoChatMessageParser geoChatMessageParser)
    {
        this.chatServiceDelegate = chatServiceDelegate;
        this.geoChatContactProvider = geoChatContactProvider;
        this.geoChatMessageParser = geoChatMessageParser;
    }

    @Override
    public synchronized void cotMessageReceived(String cotMessage, String rxEndpointId)
    {
        // This is an entry point from the CommonCommo library, and it seems to be swallowing exceptions that
        // occur here.  So, wrap the whole operation in a try/catch to ensure exceptions get reported.
        try
        {
            final CotEvent cotEvent = CotEvent.parse(cotMessage);

            possiblyUpdateContact(cotEvent);

            if (!isGeoChatMessage(cotEvent)) return;

            GeoChatParsedMessage parsedMessage;

            // Check delete first because WinTAK deletion messages also pass the group message test below
            if (isGroupDeletionMessage(cotEvent))
            {
                final IGroupContact deletedGroup = geoChatMessageParser.parseGroupDeletionEvent(cotEvent);
                if (deletedGroup == null)
                {
                    logger.warn("Received group deletion notification for unknown group, ignoring notification.");
                    return;
                }

                geoChatContactProvider.removeGroup(deletedGroup);

                // delete ancestors if they are no longer relevant
                cleanUpHierarchy(deletedGroup);

                parsedMessage = geoChatMessageParser.parseDirectCotEvent(cotEvent);
            } else if (isGroupMessage(cotEvent))
            {
                final String clientUid = chatServiceDelegate.getClientUid();
                final boolean kickedFromGroup = isKickedFromGroupMessage(cotEvent);

                // The "kicked" flag will prevent new groups from being created if the user is being kicked from a group
                parsedMessage = geoChatMessageParser.parseGroupCotEvent(cotEvent, clientUid, kickedFromGroup);

                if (kickedFromGroup && parsedMessage != null)
                {
                    final IGroupContact formerGroup = (IGroupContact) parsedMessage.getContact();

                    if (!contactIsMemberOfDescendantGroup(formerGroup, formerGroup.getUid(), clientUid))
                    {
                        geoChatContactProvider.removeGroup(formerGroup);
                        cleanUpHierarchy(formerGroup);
                    }
                }
            } else if (isTeamMessage(cotEvent))
            {
                parsedMessage = geoChatMessageParser.parseTeamCotEvent(cotEvent);
            } else if (isRoleMessage(cotEvent))
            {
                parsedMessage = geoChatMessageParser.parseRoleCotEvent(cotEvent);
            } else if (isReceipt(cotEvent))
            {
                ChatReceipt receipt = geoChatMessageParser.parseReceipt(cotEvent);

                if (receipt != null) chatServiceDelegate.receiveReceipt(receipt);

                parsedMessage = null;
            } else
            {
                parsedMessage = geoChatMessageParser.parseDirectCotEvent(cotEvent);
            }

            if (parsedMessage != null)
            {
                chatServiceDelegate.receiveMessage(parsedMessage.getContact(), parsedMessage.getMessage());
            }
        } catch (Exception e)
        {
            logger.error("Failed to process CoT message: {}", cotMessage, e);
        }
    }

    /**
     * Check if we need to update the name associated with the Contact UID that the message is from,
     * and do so if needed.  If this is a new UID (not a known contact), determine if the contact is "reachable",
     * meaning it has a non-empty {@code endpoint} attribute on the contact detail, and a non-empty name.
     * If it is reachable, then create a new individual contact for it.
     *
     * @param cotEvent CoT event to process
     */
    private void possiblyUpdateContact(CotEvent cotEvent)
    {
        final String type = cotEvent.getType();

        // Weed out any events that are not friendly units
        if (!type.startsWith(CotTypes.PREFIX_FRIENDLY)) return;

        // Must have a contact detail
        final CotDetail detail = cotEvent.getDetail();
        if (detail == null) return;
        final CotDetail contactDetail = detail.getFirstChildByName(DetailConstants.CONTACT);
        if (contactDetail == null) return;

        // Must have an endpoint
        final String endPoint = contactDetail.getAttribute(DetailConstants.ATTR_ENDPOINT);
        if (StringUtils.isEmpty(endPoint, true)) return;

        final String name = cotEvent.getCallsign();
        if (StringUtils.isEmpty(name, true)) return;

        final String uid = cotEvent.getUID();
        if (chatServiceDelegate.getClientUid().equals(uid)) return; // ignore team role updates about client

        final IContact contact = geoChatContactProvider.getContact(uid);
        final boolean exists = contact != null;
        boolean updateContact = exists && !contact.getName().equals(name);

        if (updateContact)
        {
            updateContactName(contact, name);
        } else if (!exists)
        {
            // Need to add a new individual contact
            logger.debug("Add new contact: {} = {}", uid, name);

            geoChatContactProvider.createIndividualContact(uid, name);
        }
    }

    /**
     * Checks if the given CoT event represents a GeoChat message.
     *
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a GeoChat message
     */
    private boolean isGeoChatMessage(CotEvent cotEvent)
    {
        return cotEvent.isValid()
                && cotEvent.getDetail() != null
                && CHAT_MESSAGE_TYPES.contains(cotEvent.getType());
    }

    /**
     * Updates a contact's call sign.
     *
     * @param contact The contact to update
     * @param name The new name
     */
    private void updateContactName(IContact contact, String name)
    {
        IContact updatedContact = null;

        if (contact instanceof IIndividualContact)
        {
            updatedContact = new IndividualContactBuilder()
                    .withUniqueContactId(contact.getUid())
                    .withDisplayName(name)
                    .withAttributes(contact.getAttributes())
                    .build();
        } else if (contact instanceof IGroupContact groupContact)
        {
            String uniqueId = contact.getUid();
            GroupContact existingGroupContact = (GroupContact) geoChatContactProvider.getContact(uniqueId);

            if (existingGroupContact != null)
            {
                existingGroupContact.setName(name);
                updatedContact = existingGroupContact;
            } else
            {
                updatedContact = new GroupContactBuilder()
                        .withUniqueContactId(uniqueId)
                        .withDisplayName(name)
                        .withAttributes(groupContact.getAttributes())
                        .withParentContact(groupContact.getParentContact())
                        .withGroupMembers(groupContact.getGroupMembers())
                        .build();
            }
        }

        if (updatedContact != null) geoChatContactProvider.updateContact(updatedContact);
    }

    /**
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a group chat message
     */
    private boolean isGroupMessage(CotEvent cotEvent)
    {
        // Must contain <Hierarchy><Group uid="UserGroups">
        final CotDetail hierarchyDetail = getFirstHierarchyGroup(cotEvent);
        return hierarchyDetail != null
                && GeoChatRootGroupContact.GROUPS_ROOT_UID.equals(hierarchyDetail.getAttribute(DetailConstants.UID));
    }

    /**
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a team chat message
     * @since 3.5
     */
    private boolean isTeamMessage(CotEvent cotEvent)
    {
        final CotDetail chatDetail = cotEvent.findDetail(DetailConstants.CHAT);

        if (chatDetail == null) return false;

        // Top-level message sent to "Teams" room
        if (GeoChatTeamsContact.TEAMS_GROUP_UID.equals(chatDetail.getAttribute(DetailConstants.ATTR_ID)))
        {
            return true;
        }

        // ATAK - Must contain <__chat parent="TeamGroups"> (ATAK doesn't include <Hierarchy>)
        if (GeoChatTeamsContact.TEAMS_GROUP_UID.equals(chatDetail.getAttribute(DetailConstants.ATTR_PARENT)))
        {
            return true;
        }

        // WinTAK - Must contain ...<__chat><Hierarchy><Group uid="TeamGroups"> (WinTAK doesn't include <__chat parent>)
        final CotDetail group = getFirstHierarchyGroup(cotEvent);
        return group != null && GeoChatTeamsContact.TEAMS_GROUP_UID.equals(group.getAttribute(DetailConstants.UID));
    }

    /**
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a role chat message
     * @since 3.5
     */
    private boolean isRoleMessage(CotEvent cotEvent)
    {
        final CotDetail chatDetail = cotEvent.findDetail(DetailConstants.CHAT);

        if (chatDetail == null) return false;

        final String chatId = chatDetail.getAttribute(DetailConstants.ATTR_ID);
        final String chatroom = chatDetail.getAttribute(DetailConstants.ATTR_CHATROOM);

        if (chatId == null || chatroom == null) return false;

        // ATAK and WinTAK role message both set the chatroom and id to the role name, 
        // e.g. <__chat chatroom="Team Lead" id="Team Lead">
        return chatId.equals(chatroom) && GeoChatRolesManager.isManagedRole(chatId);
    }

    /**
     * @param cotEvent The CoT event to search
     * @return The first Group element under the Hierarchy element, or {@code null} if it does not exist
     * @since 3.5
     */
    @Nullable
    private CotDetail getFirstHierarchyGroup(CotEvent cotEvent)
    {
        final CotDetail chatDetail = cotEvent.findDetail(DetailConstants.CHAT);
        if (chatDetail == null) return null;

        final CotDetail hierarchy = chatDetail.getFirstChildByName(DetailConstants.HIERARCHY);
        if (hierarchy == null) return null;

        return hierarchy.getFirstChildByName(DetailConstants.HIERARCHY_GROUP);
    }

    /**
     * Checks if the given CoT event is a chat receipt.
     *
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a chat receipt
     * @since 3.3
     */
    private boolean isReceipt(CotEvent cotEvent)
    {
        final String cotType = cotEvent.getType();
        return cotType.equals(CotTypes.FREE_TEXT_DELIVERED) || cotType.equals(CotTypes.FREE_TEXT_READ);
    }

    /**
     * Checks if the given CoT event is a group deletion message.
     *
     * @param cotEvent The CoT event to check
     * @return {@code true} if the CoT event represents a group deletion message
     */
    private boolean isGroupDeletionMessage(CotEvent cotEvent)
    {
        final CotDetail cotDetail = cotEvent.findDetail(DetailConstants.CHAT);
        return cotDetail != null && cotDetail.getAttribute(DetailConstants.ATTR_DELETE_CHILD) != null;
    }

    /**
     * Checks if the given CoT event is a notification that we've been kicked from a group.
     *
     * @param cotEvent The CoT event to check
     * @return {@code true} if the given message is a notification that we've been kicked from a group
     * @since 3.3
     */
    private boolean isKickedFromGroupMessage(CotEvent cotEvent)
    {
        CotDetail cotDetail = cotEvent.findDetail(DetailConstants.REMARKS);

        if (cotDetail != null)
        {
            String text = cotDetail.getInnerText();
            if (text != null) return text.trim().equals(GeoChatConstants.KICKED_FROM_GROUP_MESSAGE);
        }

        return false;
    }

    /**
     * Checks to see if the local user is a member of any ancestor group or sibling group. If not, delete all ancestors
     * up to the root.
     * 
     * @param deletedGroupContact The group that is being deleted
     * @since 3.5
     */
    @VisibleForTesting
    void cleanUpHierarchy(@Nonnull IGroupContact deletedGroupContact)
    {
        final String clientUid = chatServiceDelegate.getClientUid();
        final String deletedGroupUid = deletedGroupContact.getUid();

        IGroupContact parent = deletedGroupContact.getParentContact();

        // Climb up hierarchy to the root (null parent indicates root) and remove nodes as needed
        while (parent != null && parent.getParentContact() != null)
        {
            if (contactIsMemberOfDescendantGroup(parent, deletedGroupUid, clientUid))
            {
                logger.debug("Keeping {} because the client is a member of a descendant group.", parent);
                return;
            }

            logger.debug("Removing {} because the client is not a member of its descendant groups.", parent);

            geoChatContactProvider.removeGroup(parent);
            parent = parent.getParentContact();
        }
    }

    /**
     * Checks to see if a contact is a member of a group or its descendants.
     *
     * @param groupContact The group to check
     * @param ignoredGroupUid The UID of the group whose members should be ignored (e.g. the group being deleted)
     * @param contactUid The contact's UID
     * @return {@code true} if the contact is a member of the {@link IGroupContact}'s descendants
     * @since 3.5
     */
    @VisibleForTesting
    boolean contactIsMemberOfDescendantGroup(@Nonnull IGroupContact groupContact, @Nonnull String ignoredGroupUid,
                                             @Nonnull String contactUid)
    {
        final Set<IContact> groupMembers = groupContact.getGroupMembers();
        final boolean checkGroupMembers = !groupContact.getUid().equals(ignoredGroupUid);

        for (IContact groupMember : groupMembers)
        {
            if (checkGroupMembers && groupMember.getUid().equals(contactUid))
            {
                logger.debug("Found contact in group {} while ignoring {} and looking for contact {}", groupContact,
                        ignoredGroupUid, contactUid);
                return true;
            }

            // check subgroup
            if (groupMember instanceof IGroupContact childGroup)
            {
                if (contactIsMemberOfDescendantGroup(childGroup, ignoredGroupUid, contactUid)) return true;
            }
        }

        return false;
    }
}
