package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.CotConstants;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.core.chat.ChatDetailConstants;
import gov.takx.core.chat.ChatLink;
import gov.takx.core.chat.ChatMessageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Responsible for assembling a variety of CoT-based GeoChat messages to be sent to other TAK users on the network.
 *
 * @since 3.0
 */
class GeoChatMessageAssembler
{
    private final IChatProtocolHandlerDelegate chatServiceProviderDelegate;

    /**
     * @param chatServiceProviderDelegate The chat service delegate through which client information, such as UID or
     *                                    display name, is queried
     */
    GeoChatMessageAssembler(@Nonnull IChatProtocolHandlerDelegate chatServiceProviderDelegate)
    {
        this.chatServiceProviderDelegate = chatServiceProviderDelegate;
    }

    /**
     * Assembles a peer-to-peer GeoChat message (i.e. a direct message).
     *
     * @param recipientUid The UID of the recipient contact
     * @param recipientName The name of the recipient contact
     * @param message The chat message to include
     * @return The peer-to-peer GeoChat message as a CoT string
     */
    @Nonnull
    String assemblePeerToPeerChatMessage(@Nonnull String recipientUid, @Nonnull String recipientName,
                                         @Nonnull IChatMessage message)
    {
        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, recipientUid, recipientName,
                message.getUid(), DetailConstants.CHAT_ROOT_CONTACT_GROUP, false);

        return assembleEventXmlWithRemarks(recipientUid, recipientName, message.getUid(),
                new CoordinatedTime(message.getSendTime()), message.getText(), chatDetail,
                ChatMessageUtils.getChatLinks(message));
    }

    /**
     * Assembles a GeoChat message intended for a member of a top-level chat group.
     *
     * @param groupId The top-level group id ("UserGroups", "TeamGroups")
     * @param groupName The top-level group name ("Groups", "Teams")
     * @param recipientUid The UID of the recipient contact
     * @param message      The chat message to include
     * @return The "Groups" GeoChat message as a CoT string
     * @since 3.5 (renamed from assembleRootGroupChatMessage)
     */
    @Nonnull
    String assembleTopLevelGroupChatMessage(@Nonnull String groupId, @Nonnull String groupName,
                                            @Nonnull String recipientUid, @Nonnull IChatMessage message)
    {
        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid, groupId);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, groupId, groupName, message.getUid(),
                DetailConstants.CHAT_ROOT_CONTACT_GROUP, false);

        return assembleEventXmlWithRemarks(recipientUid, GeoChatRootGroupContact.GROUPS_ROOT_UID, message.getUid(),
                new CoordinatedTime(message.getSendTime()), message.getText(), chatDetail,
                ChatMessageUtils.getChatLinks(message));
    }

    /**
     * Assembles a GeoChat message intended for a member of the given group contact.
     *
     * @param recipientUid       The UID of the recipient contact
     * @param groupContact       The group contact for which the chat message is being assembled
     * @param message            The chat message to include
     * @param isClientGroupOwner {@code true} if the client is the owner of the group contact
     * @return The group GeoChat message as a CoT string
     */
    @Nonnull
    String assembleGroupChatMessage(@Nonnull String recipientUid, @Nonnull IGroupContact groupContact,
                                    @Nonnull IChatMessage message, boolean isClientGroupOwner)
    {
        final IGroupContact parentGroup = groupContact.getParentContact();
        final String recipientGroupUid = groupContact.getUid();
        final String recipientGroupDisplayName = groupContact.getName();

        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid, recipientGroupUid);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, recipientGroupUid, recipientGroupDisplayName,
                message.getUid(), parentGroup.getUid(), isClientGroupOwner);

        addHierarchyDetailToChatDetail(groupContact, chatDetail);

        return assembleEventXmlWithRemarks(recipientUid, recipientGroupUid, message.getUid(), new CoordinatedTime(message.getSendTime()),
                message.getText(), chatDetail, ChatMessageUtils.getChatLinks(message));
    }

    /**
     * Assembles a group-creation GeoChat message intended for a member of a newly-created GeoChat group.
     *
     * @param recipientUid The UID of the recipient contact
     * @param createdGroup The newly-created group
     * @param messageUid   The message UID to use when assembling the message
     * @return The group-creation GeoChat message as a CoT string
     */
    @Nonnull
    String assembleGroupCreationMessage(@Nonnull String recipientUid, @Nonnull IGroupContact createdGroup, @Nonnull String messageUid)
    {
        final String createdGroupUid = createdGroup.getUid();
        final String createdGroupDisplayName = createdGroup.getName();
        final IGroupContact parentGroup = createdGroup.getParentContact();

        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid, createdGroupUid);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, createdGroupUid, createdGroupDisplayName,
                messageUid, parentGroup.getUid(), true);

        addHierarchyDetailToChatDetail(createdGroup, chatDetail);

        return assembleEventXmlWithRemarks(recipientUid, createdGroupUid, messageUid, new CoordinatedTime(System.currentTimeMillis()),
                GeoChatConstants.UPDATED_CONTACTS_MESSAGE, chatDetail, null);
    }

    /**
     * Assembles a group-updated GeoChat message intended for a member of an updated GeoChat group.
     *
     * @param recipientUid The UID of the recipient contact
     * @param updatedGroup The updated group
     * @param messageUid The message UID to use when assembling the message
     * @param message The detailed update message
     * @return The group-creation GeoChat message as a CoT string
     * @since 5.8
     */
    @Nonnull
    String assembleDetailedGroupUpdatedMessage(@Nonnull String recipientUid, @Nonnull IGroupContact updatedGroup,
                                               @Nonnull String messageUid, @Nonnull String message)
    {
        final String createdGroupUid = updatedGroup.getUid();

        final var chatGroupDetail = makeChatGroupDetail(recipientUid, createdGroupUid);
        final var chatDetail = makeChatDetail(chatGroupDetail, createdGroupUid, updatedGroup.getName(), messageUid,
                updatedGroup.getParentContact().getUid(), true);

        addHierarchyDetailToChatDetail(updatedGroup, chatDetail);

        return assembleEventXmlWithRemarks(recipientUid, createdGroupUid, messageUid,
                // + 1ms ensures message is displayed after the standard update message
                new CoordinatedTime(System.currentTimeMillis()).addMilliseconds(1),
                message, chatDetail, null);
    }

    /**
     * Assembles a group-deletion GeoChat message intended for a member of a recently-deleted GeoChat group.
     *
     * @param recipientUid The UID of the recipient contact
     * @param deletedGroup The group contact that was deleted
     * @param messageUid   The message UID to use when assembling the message
     * @return The group-deletion GeoChat message as a CoT string
     */
    @Nonnull
    String assembleGroupDeletionMessage(@Nonnull String recipientUid, @Nonnull IGroupContact deletedGroup, @Nonnull String messageUid)
    {
        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid, GeoChatRootGroupContact.GROUPS_ROOT_UID);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, GeoChatRootGroupContact.GROUPS_ROOT_UID,
                GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME, messageUid, DetailConstants.CHAT_ROOT_CONTACT_GROUP, false);

        chatDetail.setAttribute(DetailConstants.ATTR_DELETE_CHILD, deletedGroup.getUid());

        return assembleEventXmlWithRemarks(recipientUid, GeoChatRootGroupContact.GROUPS_ROOT_UID, messageUid, new CoordinatedTime(System.currentTimeMillis()),
                "Deleted group: " + deletedGroup.getName(), chatDetail, null);
    }

    /**
     * Assembles a GeoChat message intended for a group member that was evicted (kicked) from a GeoChat group.
     *
     * @param recipientUid The UID of the recipient contact
     * @param groupContact The group contact from which the recipient is being kicked
     * @param messageUid   The message UID to use when assembling the message
     * @return The group member eviction GeoChat message as a CoT string
     */
    @Nonnull
    String assembleGroupMemberEvictionMessage(@Nonnull String recipientUid, @Nonnull IGroupContact groupContact, @Nonnull String messageUid)
    {
        final String recipientGroupUid = groupContact.getUid();
        final String recipientGroupDisplayName = groupContact.getName();

        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid, recipientGroupUid);
        final CotDetail chatDetail = makeChatDetail(chatGroupDetail, recipientGroupUid, recipientGroupDisplayName,
                messageUid, GeoChatRootGroupContact.GROUPS_ROOT_UID, true);

        chatGroupDetail.removeAttribute(DetailConstants.ATTR_UID_1);

        addHierarchyDetailToChatDetail(groupContact, chatDetail);

        return assembleEventXmlWithRemarks(recipientUid, recipientGroupUid, messageUid, new CoordinatedTime(System.currentTimeMillis()),
                GeoChatConstants.KICKED_FROM_GROUP_MESSAGE, chatDetail, null);
    }

    /**
     * Assembles a peer-to-peer GeoChat message (i.e. a direct message) receipt.
     *
     * @param recipientUid The UID of the recipient contact
     * @param recipientName The name of the recipient contact
     * @param uid The message UID to use when assembling the chat detail
     * @param read {@code true} if confirming that the message has been read
     * @return The receipt message as a CoT string
     * @since 3.4
     */
    @Nonnull
    String assembleReceiptMessage(@Nonnull String recipientUid, @Nonnull String recipientName, @Nonnull String uid,
                                  boolean read)
    {
        final String clientDisplayName = chatServiceProviderDelegate.getClientName();

        // __chatreceipt element and its attributes
        final CotDetail chatReceiptDetail = new CotDetail(DetailConstants.CHAT_RECEIPT);
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_PARENT, DetailConstants.CHAT_ROOT_CONTACT_GROUP);
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_GROUP_OWNER, "false");
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_MESSAGE_ID, uid);
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_CHATROOM, recipientName);
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_ID, recipientUid);
        chatReceiptDetail.setAttribute(DetailConstants.ATTR_SENDER_CALLSIGN, clientDisplayName);

        // chatgrp element and its attributes
        final CotDetail chatGroupDetail = makeChatGroupDetail(recipientUid);

        chatReceiptDetail.addChild(chatGroupDetail);

        final String clientUid = chatServiceProviderDelegate.getClientUid();
        final String cotType = read ? CotTypes.FREE_TEXT_READ : CotTypes.FREE_TEXT_DELIVERED;
        final CoordinatedTime sendTime = new CoordinatedTime(System.currentTimeMillis());
        final CotDetail linkDetail = makeLinkDetail(clientUid);

        return assembleEventXml(uid, cotType, sendTime, CotConstants.HOW_MACHINE_GENERATED_PREFIX, chatReceiptDetail,
                linkDetail);
    }

    /**
     * Creates a "chatgrp" detail using the given recipient UID as the value for both the "id" and "uid1" attributes.
     * The client UID is used as the value of the "uid0" attribute.
     * <p>
     * This method is specific to direct messages at the moment.
     *
     * @param recipientUid The UID of the recipient contact
     * @return A "chatgrp" detail
     */
    private CotDetail makeChatGroupDetail(String recipientUid)
    {
        return makeChatGroupDetail(recipientUid, recipientUid);
    }

    /**
     * Creates a "chatgrp" detail using the given group UID as the value of the "id" attribute and the recipient UID as
     * the value of the "uid1" attribute. The client UID is used as the value of the "uid0" attribute.
     *
     * @param recipientUid The UID of the recipient contact
     * @param groupUid     The UID of the group
     * @return A "chatgrp" detail
     */
    private CotDetail makeChatGroupDetail(String recipientUid, String groupUid)
    {
        final CotDetail chatGroupDetail = new CotDetail(DetailConstants.CHAT_GROUP);

        chatGroupDetail.setAttribute(DetailConstants.ATTR_ID, groupUid);
        chatGroupDetail.setAttribute(DetailConstants.ATTR_UID_0, chatServiceProviderDelegate.getClientUid());
        chatGroupDetail.setAttribute(DetailConstants.ATTR_UID_1, recipientUid);

        return chatGroupDetail;
    }

    /**
     * Creates a "__chat" detail, adding the given chat group detail as a child.
     *
     * @param chatGroupDetail The "chatgrp" detail to add as a child
     * @param recipientUid The UID of the recipient contact
     * @param recipientName The name of the recipient contact
     * @param messageUid The message UID to use when assembling the chat detail
     * @param parentUid The UID of the parent group (if the chat detail is to be used in a group-based message)
     * @param isGroupOwner {@code true} if the client is the owner of the group (if the chat detail is to be used
     *                          in a group-based message)
     * @return A "__chat" detail
     */
    private CotDetail makeChatDetail(CotDetail chatGroupDetail, String recipientUid, String recipientName,
                                     String messageUid, String parentUid, boolean isGroupOwner)
    {
        final CotDetail chatDetail = new CotDetail(DetailConstants.CHAT);
        final String senderName = chatServiceProviderDelegate.getClientName();

        chatDetail.setAttribute(DetailConstants.ATTR_ID, recipientUid);
        chatDetail.setAttribute(DetailConstants.ATTR_MESSAGE_ID, messageUid);
        chatDetail.setAttribute(DetailConstants.ATTR_SENDER_CALLSIGN, senderName);
        chatDetail.setAttribute(DetailConstants.ATTR_CHATROOM, recipientName);
        chatDetail.setAttribute(DetailConstants.ATTR_PARENT, parentUid);
        chatDetail.setAttribute(DetailConstants.ATTR_GROUP_OWNER, Boolean.toString(isGroupOwner));
        chatDetail.addChild(chatGroupDetail);

        return chatDetail;
    }

    /**
     * Creates and adds a "hierarchy detail" to the given chat detail.
     *
     * @param groupContact The group contact to use when creating the hierarchy detail and its children
     * @param chatDetail   The chat detail to which the hierarchy detail is added
     */
    private void addHierarchyDetailToChatDetail(IGroupContact groupContact, CotDetail chatDetail)
    {
        final CotDetail hierarchyDetail = new CotDetail(DetailConstants.HIERARCHY);

        hierarchyDetail.addChild(buildGroupHierarchy(groupContact, null, null));
        chatDetail.addChild(hierarchyDetail);
    }

    /**
     * Builds a group hierarchy, starting with the new group and working up the tree. As the tree is climbed, each
     * ancestor's other children must also be added.
     * 
     * @param groupContact The next group contact to add
     * @param childDetail The child detail created in the previous iteration
     * @param childUid The UID of the child created in the previous iteration
     * @return The group's detail element, including all of its descendants
     * @since 3.4.1
     */
    @Nonnull
    private CotDetail buildGroupHierarchy(@Nonnull IGroupContact groupContact, @Nullable CotDetail childDetail,
                                          @Nullable String childUid)
    {
        while (true)
        {
            // create a group detail for the contact
            final CotDetail groupDetail = createDetailWithUidAndName(groupContact);

            // add the child detail from previous loop
            if (childDetail != null) groupDetail.addChild(childDetail);

            final String groupUid = groupContact.getUid();

            // if not at the root, add any other group members (children) and climb tree
            if (!GeoChatRootGroupContact.GROUPS_ROOT_UID.equals(groupUid)
                    && !GeoChatTeamsContact.TEAMS_GROUP_UID.equals(groupUid))
            {
                // add all group members except the child already added above
                addGroupMemberDetails(groupDetail, groupContact.getGroupMembers(), childUid);

                // climb tree
                childDetail = groupDetail;
                childUid = groupUid;
                groupContact = groupContact.getParentContact();
            } else
            {
                return groupDetail;
            }
        }
    }

    /**
     * Method that adds individual group member details. Note that the client is assumed to be a group member and is
     * added here as well. This works for both owned and non-owned groups, as in either event the client should be
     * listed as a contact.
     *
     * @param groupDetail The "group" detail to which individual group member details are added
     * @param groupMembers The group members for which details are added to the "group" detail
     * @param ignoreUid The UID of a group member that should be ignored (e.g. because it's already been added)
     */
    private void addGroupMemberDetails(@Nonnull CotDetail groupDetail, @Nonnull Set<IContact> groupMembers,
                                       @Nullable String ignoreUid)
    {
        for (IContact groupMember : groupMembers)
        {
            if (groupMember.getUid().equals(ignoreUid)) continue;

            final CotDetail contactDetail = createDetailWithUidAndName(groupMember);

            // If the group member is another group, add all of its children
            if (groupMember instanceof IGroupContact groupContact)
            {
                // The ignored child will always be at the top level, so we can pass null here
                addGroupMemberDetails(contactDetail, groupContact.getGroupMembers(), null);
            }

            groupDetail.addChild(contactDetail);
        }
    }

    /**
     * Creates a new contact detail element and sets the UID and name
     * @param contact The contact
     * @return A new contact detail element initialized with the contact's UID and name
     * @since 3.4.1
     */
    private CotDetail createDetailWithUidAndName(@Nonnull IContact contact)
    {
        final CotDetail detail = new CotDetail(contact instanceof IGroupContact
                ? DetailConstants.HIERARCHY_GROUP
                : DetailConstants.CONTACT);
        detail.setAttribute(DetailConstants.ATTR_UID, contact.getUid());
        detail.setAttribute(DetailConstants.ATTR_NAME, contact.getName());

        return detail;
    }

    /**
     * Assembles the XML of a CoT chat event that should include a "remarks" element with inner text. The "link",
     * "remarks" and "details" elements will be included by default. The CoT <i>type</i> will be set to
     * {@link CotTypes#PREFIX_FREE_TEXT} and the CoT <i>how</i> will be set to
     * {@link CotConstants#HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT}.
     *
     * @param recipientUid      The UID of the recipient contact
     * @param recipientName The name of the recipient contact
     * @param messageUid        The message UID to use when assembling the event
     * @param sendTime          The send-time to use for the event's timestamp
     * @param innerText         The inner-text to include in the "remarks" detail
     * @param chatDetail        The chat detail to include as a child to the "detail" detail
     * @return The CoT event as an XML string
     * @since 3.4 (renamed from assembleEventXml; new signature)
     */
    private String assembleEventXmlWithRemarks(String recipientUid, String recipientName, String messageUid,
                                               CoordinatedTime sendTime, String innerText, CotDetail chatDetail,
                                               @Nullable List<ChatLink> chatLinks)
    {
        final String clientUid = chatServiceProviderDelegate.getClientUid();
        final CotDetail remarksDetail = makeRemarksDetail(clientUid, recipientUid, sendTime.toString(), innerText);
        final String uid = String.format("GeoChat.%s.%s.%s", clientUid, recipientName, messageUid);
        final CotDetail linkDetail = makeLinkDetail(clientUid);
        final CotDetail chatLinksDetail = chatLinks == null || chatLinks.isEmpty() ? null : makeChatLinksDetail(chatLinks);

        return assembleEventXml(uid, CotTypes.PREFIX_FREE_TEXT, sendTime, CotConstants.HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT,
                chatDetail, linkDetail, remarksDetail, chatLinksDetail);
    }

    /**
     * Assembles the XML of a generic CoT event. Only the "detail" element will be included by default. Children of the
     * "detail" element should be provided via the {@code detailChildren} parameter in the desired order.
     *
     * @param uid The event UID
     * @param cotType The event type
     * @param sendTime The event send time
     * @param how The event's how attribute
     * @param detailChildren The children of the "detail" event
     * @return The CoT event as an XML string
     * @since 3.4
     */
    private String assembleEventXml(String uid, String cotType, CoordinatedTime sendTime, String how,
                                    CotDetail... detailChildren)
    {
        final GeoPoint location = chatServiceProviderDelegate.getClientLocation();
        final CotDetail detail = new CotDetail();

        for (CotDetail child : detailChildren) if (child != null) detail.addChild(child);

        final CotEvent cotEvent = new CotEvent(uid, cotType, CotEvent.VERSION_2_0, location, sendTime, sendTime,
                sendTime.addDays(1), how, detail, null, null, null);

        final StringBuilder cotXmlBuilder = new StringBuilder();
        cotEvent.buildXml(cotXmlBuilder);

        return cotXmlBuilder.toString();
    }

    /**
     * Creates a "link" detail using the given client UID.
     *
     * @param clientUid The client UID to include in the link detail
     * @return A "link" detail
     */
    private CotDetail makeLinkDetail(String clientUid)
    {
        final CotDetail linkDetail = new CotDetail(DetailConstants.LINK);

        linkDetail.setAttribute(DetailConstants.ATTR_UID, clientUid);
        linkDetail.setAttribute(DetailConstants.ATTR_TYPE, CotTypes.FRIENDLY_GROUND_COMBAT_UNIT);
        linkDetail.setAttribute(DetailConstants.ATTR_RELATION, DetailConstants.LINK_RELATION_PARENT_PRODUCER);

        return linkDetail;
    }

    /**
     * Creates a {@link CotDetail} for the chat links element and its items.
     *
     * @param chatLinks The chat link items to add
     * @return The chat links CoT detail
     * @since 5.2
     */
    @Nonnull
    private CotDetail makeChatLinksDetail(@Nonnull List<ChatLink> chatLinks)
    {
        final CotDetail chatLinksDetail = new CotDetail(ChatDetailConstants.CHAT_LINKS);

        chatLinks.forEach(chatLink -> {
            final CotDetail itemDetail = new CotDetail(ChatDetailConstants.ITEM);
            itemDetail.setAttribute(DetailConstants.ATTR_NAME, chatLink.name());
            itemDetail.setAttribute(DetailConstants.ATTR_UID, chatLink.uid());
            chatLinksDetail.addChild(itemDetail);
        });

        return chatLinksDetail;
    }

    /**
     * Creates a "remarks" detail.
     *
     * @param clientUid      The client UID to include in the remarks detail
     * @param recipientUid   The UID of the recipient contact
     * @param sendTimeString The string representing the send-time
     * @param innerText      The string to use as the inner text of the "remarks" tag
     * @return A "remarks" detail
     */
    private CotDetail makeRemarksDetail(String clientUid, String recipientUid, String sendTimeString, String innerText)
    {
        final CotDetail remarksDetail = new CotDetail(DetailConstants.REMARKS);

        if (recipientUid != null) remarksDetail.setAttribute(DetailConstants.ATTR_TO, recipientUid);

        remarksDetail.setAttribute(DetailConstants.ATTR_SOURCE, clientUid);
        remarksDetail.setAttribute(DetailConstants.ATTR_TIME, sendTimeString);
        remarksDetail.setInnerText(innerText);

        return remarksDetail;
    }
}
