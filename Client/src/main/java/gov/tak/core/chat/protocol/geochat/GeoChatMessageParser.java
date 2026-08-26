package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.platform.chat.ChatReceipt;
import gov.tak.platform.contact.GroupContact;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.tak.platform.contact.IndividualContactBuilder;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.chat.ChatDetailConstants;
import gov.takx.core.chat.ChatLink;
import gov.takx.core.chat.ChatMessageBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for parsing a variety of GeoChat messages.
 *
 * @since 3.0
 */
class GeoChatMessageParser
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final GeoChatContactProvider geoChatContactProvider;

    /**
     * @param geoChatContactProvider The GeoChat contact provider through which contacts are queried and managed
     */
    GeoChatMessageParser(@Nonnull GeoChatContactProvider geoChatContactProvider)
    {
        this.geoChatContactProvider = geoChatContactProvider;
    }

    /**
     * Parses a CoT event representing a direct chat message, "All Chat Rooms" message, or "Groups" group message.
     *
     * @param event The CotEvent representing a chat message
     * @return The parsed GeoChat message, or {@code null} if the event could not be parsed
     */
    @Nullable
    public GeoChatParsedMessage parseDirectCotEvent(@Nonnull CotEvent event)
    {
        final CotDetail chatDetail = event.findDetail(DetailConstants.CHAT);
        final CotDetail linkDetail = event.findDetail(DetailConstants.LINK);
        final CotDetail remarksDetail = event.findDetail(DetailConstants.REMARKS);

        if (areDetailsInvalid(chatDetail, linkDetail, remarksDetail, event)) return null;

        final String messageContent = remarksDetail.getInnerText();
        final String messageUid = getMessageUid(event, chatDetail);
        final String cotTime = remarksDetail.getAttribute(DetailConstants.ATTR_TIME);
        final String senderName = getSenderName(event, chatDetail);
        final String chatroom = chatDetail.getAttribute(DetailConstants.ATTR_CHATROOM);
        final List<ChatLink> chatLinks = getChatLinks(event);

        final String senderUid = linkDetail.getAttribute(DetailConstants.UID);

        if (GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME.equals(chatroom))
        {
            return parseAllChatRoomsMessage(messageContent, messageUid, cotTime, senderUid, senderName);
        } else if ((GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME.equals(chatroom)
                && GeoChatRootGroupContact.GROUPS_ROOT_UID.equals(chatDetail.getAttribute(DetailConstants.ATTR_ID))) ||
                chatDetail.hasAttribute(DetailConstants.ATTR_DELETE_CHILD)) // always route group deletion messages to "Groups"
        {
            return parseRootGroupsMessage(messageContent, messageUid, cotTime, senderUid, senderName);
        }

        final String uid = linkDetail.getAttribute(DetailConstants.ATTR_UID);
        final IContact senderContact = uid != null ? geoChatContactProvider.getContact(uid) : null;

        // Update the display name we're currently tracking for the sender if it's outdated
        if (senderContact != null)
        {
            updateSenderContactName(senderContact, senderName);
        } else
        {
            logger.warn("Received chat message from unknown Contact: {}", senderName);
            return null;
        }

        return new GeoChatParsedMessage(senderContact, buildChatMessage(messageContent, messageUid, cotTime, senderUid,
                senderName, senderUid, chatLinks));
    }

    /**
     * Updates the sending contact being tracked in the {@link GeoChatContactProvider} if the contact's name is not
     * up-to-date.
     *
     * @param senderContact  The sending contact to update if necessary
     * @param senderName The sending contact's updated name
     */
    private void updateSenderContactName(IContact senderContact, String senderName)
    {
        if (!senderContact.getName().equals(senderName))
        {
            IContact updatedContact =
                    new IndividualContactBuilder()
                            .withUniqueContactId(senderContact.getUid())
                            .withDisplayName(senderName)
                            .withAttributes(senderContact.getAttributes())
                            .build();
            geoChatContactProvider.updateContact(updatedContact);
        }
    }

    /**
     * Creates an {@link GeoChatParsedMessage} from the given message and sender information.
     *
     * @param message        The received message content
     * @param messageUid     The UID of the received message
     * @param cotTime        The send-time of the received message
     * @param senderUid      The sender's UID
     * @param senderName     The name of the sender
     * @return A parsed GeoChat message containing the {@link GeoChatAllChatRoomsContact} and the newly created
     * {@link GeoChatParsedMessage}
     */
    private GeoChatParsedMessage parseAllChatRoomsMessage(String message, String messageUid, String cotTime,
                                                          String senderUid, String senderName)
    {
        return new GeoChatParsedMessage(
                geoChatContactProvider.getContact(GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME),
                new ChatMessageBuilder(senderUid, senderName, message, new CoordinatedTime(cotTime).getMilliseconds(),
                        GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME)
                        .withReceiveTime(System.currentTimeMillis())
                        .withUid(messageUid)
                        .build());
    }

    /**
     * Creates a {@link GeoChatParsedMessage} from the given message and sender information.
     *
     * @param message        The received message content
     * @param messageUid     The UID of the received message
     * @param cotTime        The send-time of the received message
     * @param senderUid      The sender's UID
     * @param senderName     The name of the sender
     * @return A parsed GeoChat message containing the {@link GeoChatRootGroupContact} and the newly created 
     * {@link GeoChatParsedMessage}
     */
    private GeoChatParsedMessage parseRootGroupsMessage(String message, String messageUid, String cotTime,
                                                        String senderUid, String senderName)
    {
        return new GeoChatParsedMessage(
                geoChatContactProvider.getContact(GeoChatRootGroupContact.GROUPS_ROOT_UID),
                new ChatMessageBuilder(senderUid, senderName, message, new CoordinatedTime(cotTime).getMilliseconds(),
                        GeoChatRootGroupContact.GROUPS_ROOT_UID)
                        .withReceiveTime(System.currentTimeMillis())
                        .withUid(messageUid)
                        .build());
    }

    /**
     * Parses a CoT event representing a chat message intended for a user-owned group.
     *
     * @param event The CotEvent representing a chat message
     * @param kicked Flag indicating the event is a message kicking the client from a group
     * @return The parsed GeoChat message, or {@code null} if the event could not be parsed
     */
    @Nullable
    public GeoChatParsedMessage parseGroupCotEvent(@Nonnull CotEvent event, @Nonnull String clientUid, boolean kicked)
    {
        final CotDetail chatDetail = event.findDetail(DetailConstants.CHAT);
        final CotDetail linkDetail = event.findDetail(DetailConstants.LINK);
        final CotDetail remarksDetail = event.findDetail(DetailConstants.REMARKS);

        if (areDetailsInvalid(chatDetail, linkDetail, remarksDetail, event)) return null;

        final CotDetail hierarchyDetail = chatDetail.getFirstChildByName(DetailConstants.HIERARCHY);

        if (hierarchyDetail == null)
        {
            logger.warn("Received group chat message with missing <hierarchy> detail, skipping message.");
            logger.warn("CotEvent: {}", event);
            return null;
        }

        final String messageContent = remarksDetail.getInnerText();
        final String messageUid = getMessageUid(event, chatDetail);
        final String cotTime = remarksDetail.getAttribute(DetailConstants.ATTR_TIME);

        final String recipientGroupUid = chatDetail.getAttribute(DetailConstants.ATTR_ID);
        final String senderName = getSenderName(event, chatDetail);

        final String senderUid = linkDetail.getAttribute(DetailConstants.UID);

        // Get the base "Groups" group detail
        final CotDetail rootUserGroup = hierarchyDetail.getFirstChildByName(DetailConstants.HIERARCHY_GROUP);
        final List<CotDetail> pathToRecipient = getPathToRecipient(rootUserGroup, recipientGroupUid);

        if (pathToRecipient == null)
        {
            logger.warn("Could not find <group> detail with UID {} in group hierarchy, group message CoT may be malformed." +
                    " Skipping message.", recipientGroupUid);
            logger.warn("CotEvent: {}", event);
            return null;
        }

        // Sometimes the UPDATE message arrives after the KICKED message. It should be ignored to prevent recreating
        // the group's hierarchy after the user has been kicked
        if (isRemovalUpdateMessage(messageContent, pathToRecipient, recipientGroupUid, clientUid))
        {
            return null;
        }

        IGroupContact groupContact = (IGroupContact) geoChatContactProvider.getContact(recipientGroupUid);

        // If there's an existing group, update it as needed; otherwise, create a new unowned group, building the full 
        // path as needed
        if (groupContact != null)
        {
            updateGroupContact(groupContact, pathToRecipient.get(pathToRecipient.size() - 1), senderUid, senderName);
        } else
        {
            IGroupContact parent = null;

            for (final CotDetail detail : pathToRecipient)
            {
                final String uid = detail.getAttribute(DetailConstants.UID);
                groupContact = (IGroupContact) geoChatContactProvider.getContact(uid);

                if (groupContact == null)
                {
                    final Set<IContact> groupMembers = getGroupMembersFromDetail(detail, parent, senderUid, senderName);

                    groupContact = buildGroupContact(uid, detail.getAttribute(DetailConstants.ATTR_NAME), parent, groupMembers);

                    // Assume that, if we don't already have this contact, then we're not the group owner.
                    groupContact.getAttributes().setAttribute(GeoChatConstants.CLIENT_OWNED_GROUP_ATTRIBUTE, false);

                    // Don't persist any groups in the hierarchy if the user is being kicked from a group
                    if (!kicked) geoChatContactProvider.addUnownedGroupContact(groupContact);
                }

                parent = groupContact;
            }
        }

        return new GeoChatParsedMessage(groupContact, buildChatMessage(messageContent, messageUid, cotTime, senderUid,
                senderName, groupContact.getUid(), getChatLinks(event)));
    }

    /**
     * @param event The team message CoT event
     * @return The {@link GeoChatParsedMessage}, or {@code null} if the event could not be parsed
     */
    @Nullable
    public GeoChatParsedMessage parseTeamCotEvent(@Nonnull CotEvent event)
    {
        final CotDetail chatDetail = event.findDetail(DetailConstants.CHAT);
        final String team = chatDetail.getAttribute(DetailConstants.ATTR_ID);

        final IGroupContact teamGroupContact =
                (IGroupContact) geoChatContactProvider.getContact(GeoChatTeamsContact.TEAMS_GROUP_UID);

        // Look for the team's group contact. Teams do not support subgroups, so no need to recurse into descendants
        final IGroupContact teamContact = team.equals(teamGroupContact.getUid())
                ? teamGroupContact
                : (IGroupContact) teamGroupContact.getGroupMembers().stream()
                        .filter(member -> member.getUid().equals(team))
                        .findFirst()
                        .orElse(null);

        if (teamContact == null)
        {
            logger.error("Could not find a group contact for team {}", team);
            return null;
        }

        // Collect the message data
        final String messageUid = getMessageUid(event, chatDetail);
        final String senderName = getSenderName(event, chatDetail);

        final CotDetail remarksDetail = event.findDetail(DetailConstants.REMARKS);
        final String messageContent = remarksDetail.getInnerText();
        final String cotTime = remarksDetail.getAttribute(DetailConstants.ATTR_TIME);

        final String senderUid = event.findDetail(DetailConstants.LINK).getAttribute(DetailConstants.UID);

        return new GeoChatParsedMessage(teamContact, buildChatMessage(messageContent, messageUid, cotTime, senderUid,
                senderName, teamContact.getUid(), getChatLinks(event)));
    }

    /**
     * @param event The role message CoT event
     * @return The {@link GeoChatParsedMessage}, or {@code null} if the event could not be parsed
     */
    @Nullable
    public GeoChatParsedMessage parseRoleCotEvent(@Nonnull CotEvent event)
    {
        final CotDetail chatDetail = event.findDetail(DetailConstants.CHAT);
        final CotDetail linkDetail = event.findDetail(DetailConstants.LINK);

        final String roleId = chatDetail.getAttribute(DetailConstants.ATTR_ID);
        final String senderName = getSenderName(event, chatDetail);
        final String senderUid = linkDetail.getAttribute(DetailConstants.UID);

        if (roleId == null || senderUid == null || senderName == null) return null;

        if (!GeoChatRolesManager.isManagedRole(roleId))
        {
            logger.error("\"{}\" is not a valid role id", roleId);
        }

        final IGroupContact roleGroupContact = (IGroupContact) geoChatContactProvider.getContact(roleId);

        if (roleGroupContact == null)
        {
            logger.error("No role group contact for role id {}", roleId);
            return null;
        }

        // Collect the message data
        final String messageUid = getMessageUid(event, chatDetail);
        final CotDetail remarksDetail = event.findDetail(DetailConstants.REMARKS);
        final String messageContent = remarksDetail.getInnerText();
        final String cotTime = remarksDetail.getAttribute(DetailConstants.ATTR_TIME);

        return new GeoChatParsedMessage(roleGroupContact, buildChatMessage(messageContent, messageUid, cotTime, senderUid,
                senderName, roleId, getChatLinks(event)));
    }

    /**
     * Checks to see if a {@link GeoChatConstants#UPDATED_CONTACTS_MESSAGE} contains an update indicating that the
     * client has been removed from a group
     *
     * @param message The message text
     * @param path The path to the target group
     * @param targetGroupUid The target group's UID
     * @param clientUid The local client's UID
     * @return {@code true} if the message is a removal update
     * @since 3.5
     */
    boolean isRemovalUpdateMessage(@Nonnull String message, @Nonnull List<CotDetail> path,
                                   @Nonnull String targetGroupUid, @Nonnull String clientUid)
    {
        if (!GeoChatConstants.UPDATED_CONTACTS_MESSAGE.equals(message)) return false;

        for (CotDetail cotDetail : path)
        {
            // Look for the detail containing the updated group
            if (cotDetail.getAttribute(DetailConstants.ATTR_UID).equals(targetGroupUid))
            {
                return cotDetail.getChildren().stream()
                        .noneMatch(detail -> detail.getAttribute(DetailConstants.ATTR_UID).equals(clientUid));
            }
        }

        return false;
    }

    /**
     * Checks if the given CoT details are invalid.
     *
     * @param chatDetail    The "__chat" detail to check
     * @param linkDetail    The "link" detail to check
     * @param remarksDetail The "remarks" detail to check
     * @param cotEvent      The original CoT event
     * @return {@code true} if any of the given details are invalid
     */
    private boolean areDetailsInvalid(CotDetail chatDetail, CotDetail linkDetail, CotDetail remarksDetail, CotEvent cotEvent)
    {
        String missingDetails = null;

        if (chatDetail == null)
        {
            missingDetails = "<__chat>";
        }

        if (linkDetail == null)
        {
            missingDetails = missingDetails == null ? "<link>" : missingDetails + ", <link>";
        }

        if (remarksDetail == null)
        {
            missingDetails = missingDetails == null ? "<remarks>" : missingDetails + ", <remarks>";
        }

        if (missingDetails != null)
        {
            logger.warn("Received chat message that is missing the following details: [{}]. Skipping message.", missingDetails);
            logger.warn("CotEvent: {}", cotEvent);
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * Update a group contact's display name and group members from a new CotDetail as needed.
     *
     * @param groupContact The contact to update
     * @param recipientGroupDetail The recipient detail
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @since 3.3
     */
    private void updateGroupContact(@Nonnull IGroupContact groupContact, @Nonnull CotDetail recipientGroupDetail,
                                    @Nonnull String senderUid, @Nonnull String senderName)
    {
        boolean updated = false;

        final String name = recipientGroupDetail.getAttribute(DetailConstants.ATTR_NAME);
        if (!groupContact.getName().equals(name))
        {
            ((GroupContact) groupContact).setName(name);
            updated = true;
        }

        final Set<IContact> groupMembers = getGroupMembersFromDetail(recipientGroupDetail,
                groupContact.getParentContact(), senderUid, senderName);

        if (!groupContact.getGroupMembers().equals(groupMembers))
        {
            groupContact.setGroupMembers(groupMembers);
            updated = true;
        }

        if (updated) geoChatContactProvider.updateContact(groupContact); // triggers database update
    }

    /**
     * Creates a list of {@link CotDetail}s representing the path to the group detail matching the given group UID.
     *
     * @param parentGroupDetail The parent "group" detail in which to search for the "group" detail 
     * @param recipientGroupUid The group UID to match against
     * @return A list containing the path to the group detail matching the given recipient group UID, or {@code null} 
     * if no such detail was found
     */
    @Nullable
    private List<CotDetail> getPathToRecipient(@Nonnull CotDetail parentGroupDetail,
                                               @Nonnull String recipientGroupUid)
    {
        final List<CotDetail> match = findMatchingGroup(parentGroupDetail, recipientGroupUid);

        if (match != null) return match;

        // This is a workaround for WinTAK allowing users to send messages to the "Groups" group even when there are no
        // subgroups. This results in a single level of <groups> elements rather than the expected root <groups> +
        // child <groups> elements. ATAK disables "Groups" group messages if there are no subgroups.
        return recipientGroupUid.equals(parentGroupDetail.getAttribute(DetailConstants.ATTR_UID))
                ? List.of(parentGroupDetail)
                : null;
    }

    /**
     * Recursively search for a group. If it's found, build the path back to the root.
     *
     * @param groupDetail The group to check
     * @param recipientGroupUid The UID to match
     * @return The matching group detail and its parent group detail
     * @since 3.4.1
     */
    @Nullable
    private List<CotDetail> findMatchingGroup(@Nonnull final CotDetail groupDetail,
                                              @Nonnull final String recipientGroupUid)
    {
        if (recipientGroupUid.equals(groupDetail.getAttribute(DetailConstants.ATTR_UID)))
        {
            final List<CotDetail> result = new ArrayList<>();
            result.add(groupDetail);
            return result;
        }

        // recursively search each child group for a match
        for (CotDetail child : groupDetail.getChildrenByName(DetailConstants.HIERARCHY_GROUP))
        {
            final List<CotDetail> match = findMatchingGroup(child, recipientGroupUid);

            if (match != null)
            {
                match.add(0, groupDetail); // insert parent at front
                return match;
            }
        }

        return null;
    }

    /**
     * Gets the group members from the given group detail, recursively building the members' members if necessary.
     *
     * @param groupDetail The "group" detail from which the group members are derived
     * @param parent The group detail's parent group. The parent will be {@code null} for recursive calls because the
     *               parent will not have been constructed yet
     * @param senderUid The message sender's UID
     * @param senderName The message sender's name
     * @return A set of contacts representing the individual group members
     */
    @Nonnull
    private Set<IContact> getGroupMembersFromDetail(@Nonnull CotDetail groupDetail, @Nullable IGroupContact parent,
                                                    @Nonnull String senderUid, @Nonnull String senderName)
    {
        final Set<IContact> groupMembers = new HashSet<>();

        for (CotDetail childGroupDetail : groupDetail.getChildren())
        {
            final String elementName = childGroupDetail.getElementName();
            final boolean individual = elementName.equals(DetailConstants.CONTACT);
            final boolean group = elementName.equals(DetailConstants.HIERARCHY_GROUP);

            if (!individual && !group) continue;

            final String uid = childGroupDetail.getAttribute(DetailConstants.ATTR_UID);
            IContact contact = geoChatContactProvider.getContact(uid);

            if (contact == null)
            {
                final String name = childGroupDetail.getAttribute(DetailConstants.ATTR_NAME);

                if (individual)
                {
                    contact = buildIndividualContact(uid, name);
                } else
                {
                    // Create the group contact. The group members' parent is null because we're building it now
                    final IGroupContact groupContact =
                            buildGroupContact(uid, name, parent,
                                    getGroupMembersFromDetail(childGroupDetail, null, senderUid, senderName));

                    // Set the parent now that it's constructed
                    if (parent == null)
                    {
                        groupContact.getGroupMembers().forEach(memberContact -> {
                            if (memberContact instanceof IGroupContact memberGroupContact)
                                memberGroupContact.setParentContact(groupContact);
                        });
                    }

                    contact = groupContact;
                }
            }

            groupMembers.add(contact);
        }

        // ATAK doesn't include the sender in subgroups
        addContactIfMissing(groupMembers, senderUid, senderName);

        return groupMembers;
    }

    /**
     * Build an {@link gov.tak.api.contact.IIndividualContact} from a UID and name. Its attributes will be set to the
     * default GeoChat attribute set.
     *
     * @param uid The contact's UID
     * @param name The contact's name
     * @return A new individual contact
     * @since 3.5 (renamed from buildContact)
     */
    private IContact buildIndividualContact(@Nonnull String uid, @Nonnull String name)
    {
        return new IndividualContactBuilder()
                .withUniqueContactId(uid)
                .withDisplayName(name)
                .withAttributes(GeoChatContactProvider.createDefaultGeoChatContactAttributeSet())
                .build();
    }

    /**
     * Build an {@link gov.tak.api.contact.IGroupContact} from a UID and name. Its attributes will be set to the
     * default GeoChat attribute set.
     *
     * @param uid The contact's UID
     * @param name The contact's name
     * @param parent The contact's parent
     * @param groupMembers The group members
     * @return A new group contact
     * @since 3.5
     */
    private IGroupContact buildGroupContact(@Nonnull String uid, @Nonnull String name, @Nullable IGroupContact parent,
                                            @Nonnull Set<IContact> groupMembers)
    {
        return new GroupContactBuilder()
                .withUniqueContactId(uid)
                .withDisplayName(name)
                .withParentContact(parent)
                .withGroupMembers(groupMembers)
                .withAttributes(GeoChatContactProvider.createDefaultGeoChatContactAttributeSet())
                .build();
    }

    /**
     * Adds the contact specified by UID and name if it's missing from the set.
     * 
     * @param contacts The set of contacts; must be mutable
     * @param uid The contact's UID
     * @param name The contact's name
     * @since 3.4.1
     */
    private void addContactIfMissing(@Nonnull Set<IContact> contacts, @Nonnull String uid, @Nonnull String name)
    {
        if (contacts.stream().noneMatch(contact -> contact.getUid().equals(uid)))
        {
            contacts.add(buildIndividualContact(uid, name));
        }
    }

    /**
     * Builds a chat message from the given chat message information.
     *
     * @param message The message text
     * @param messageUid The message UID
     * @param cotTime The send-time of the chat message
     * @param senderUid The sender's UID
     * @param senderName The name of the message sender
     * @param conversationUid The conversation UID. For direct messages, this should be the sender's UID. For group
     *                        messages, this should be the group's {@link IGroupContact} UID.
     * @return The built chat message
     */
    private IChatMessage buildChatMessage(String message, String messageUid, String cotTime, String senderUid,
                                          String senderName, String conversationUid, List<ChatLink> links)
    {
        final long messageSendTime = new CoordinatedTime(cotTime).getMilliseconds();

        return new ChatMessageBuilder(senderUid, senderName, message, messageSendTime, conversationUid)
                .withReceiveTime(System.currentTimeMillis())
                .withUid(messageUid)
                .withLinks(links)
                .build();
    }

    /**
     * Parses a CoT event representing a group deletion notification.
     *
     * @param event The CoT event to parse
     * @return The group contact to delete
     */
    @Nullable
    public IGroupContact parseGroupDeletionEvent(@Nonnull CotEvent event)
    {
        final CotDetail chatDetail = event.findDetail(DetailConstants.CHAT);

        if (chatDetail == null)
        {
            logger.warn("Received chat message with missing <__chat> detail, skipping message.");
            logger.warn("CotEvent: {}", event);
            return null;
        }

        final String deletedGroupUid = chatDetail.getAttribute(DetailConstants.ATTR_DELETE_CHILD);
        final IContact groupContact = geoChatContactProvider.getContact(deletedGroupUid);

        return groupContact != null ? (IGroupContact) groupContact : null;
    }

    /**
     * @param event The chat receipt CoT message
     * @return A {@link ChatReceipt} object or {@code null} if there is an error parsing the message
     * @since 3.4
     */
    @Nullable
    ChatReceipt parseReceipt(@Nonnull CotEvent event)
    {
        final CotDetail linkDetail = event.findDetail(DetailConstants.LINK);

        if (linkDetail == null)
        {
            logger.warn("Received chat message with missing <link> detail, skipping message.");
            logger.warn("CotEvent: {}", event);
            return null;
        }

        final IContact sender = geoChatContactProvider.getContact(linkDetail.getAttribute(DetailConstants.ATTR_UID));

        if (sender != null)
        {
            return new ChatReceipt(sender, event.getUID(), event.getType());
        } else
        {
            logger.warn("Receipt message received from unknown sender.");
            logger.warn("CotEvent: {}", event);
            return null;
        }
    }

    /**
     * TAKX, WinTAK and ATAK all store the message id in the chat detail. WebTAK does not, and we have to extract it from
     * the event id.
     *
     * @param event The CoT event whose id will be used as a fallback way to access the message id
     * @param chatDetail The chat detail that may contain the message id
     * @return The message id
     * @see 5.1
     */
    private String getMessageUid(@Nonnull CotEvent event, @Nonnull CotDetail chatDetail)
    {
        final String messageId = chatDetail.getAttribute(DetailConstants.ATTR_MESSAGE_ID);

        if (messageId != null) return messageId;

        // no message id in <__chat messageId="..."/>; fallback to parsing the message id out of the event id
        final String eventUid = event.getUID();
        return eventUid.substring(eventUid.lastIndexOf('.') + 1);
    }

    /**
     * Looks for the {@link ChatDetailConstants#CHAT_LINKS} {@link CotDetail} and parses out the {@link ChatLink ChatLinks}
     * if it exists.
     *
     * @param event the event to inspect
     * @return a list of the {@link ChatLink ChatLinks} in the event or {@code null} if the event does not include the
     * {@link ChatDetailConstants#CHAT_LINKS} {@link CotDetail}
     * @since 5.2
     */
    @Nullable
    private List<ChatLink> getChatLinks(@Nonnull CotEvent event)
    {
        final CotDetail chatLinks = event.findDetail(ChatDetailConstants.CHAT_LINKS);

        if (chatLinks == null) return null;

        return chatLinks.getChildrenByName(ChatDetailConstants.ITEM)
                .stream()
                .map(cotDetail -> new ChatLink(
                        event.getTime().getMilliseconds(),
                        cotDetail.getAttribute(DetailConstants.ATTR_UID),
                        cotDetail.getAttribute(DetailConstants.ATTR_NAME)))
                .collect(Collectors.toList());
    }

    /**
     * Returns the sender's callsign or an empty String if the callsign cannot be determined.
     * <p>
     * This method first attempts to retrieve the sender's callsign from the expected, standard location in the event's
     * chat detail. If the chat detail does not contain a callsign, it will attempt to extract the callsign from the CoT
     * event's UID. This is required for some chat simulators (and possibly some older versions of TAK products?).
     *
     * @param cotEvent the CoT event
     * @param cotDetail the CoT detail that should contain the sender's callsign
     * @return The sender's callsign
     * @since 5.5
     */
    private String getSenderName(@Nonnull CotEvent cotEvent, @Nonnull CotDetail cotDetail)
    {
        final var senderName = cotDetail.getAttribute(DetailConstants.ATTR_SENDER_CALLSIGN);

        if (!StringUtils.isEmpty(senderName)) return senderName;

        // Extract uid from sender section (why not just read senderUid? legacy?)
        final var uid = cotEvent.getUID();

        if (uid != null)
        {
            // GeoChat events should have a UID where the second element is the sender's UID, e.g.
            // <protocol>.<senderUid>.<roomUid>?.<messageUid> --> 'GeoChat.JS_WinTAK.All Chat Rooms.1747869950595'
            final var uidComponents = uid.split("\\.");
            if (uidComponents.length > 1) return uidComponents[1];
        }

        logger.debug("Could not extract callsign from CoT event:\n\n{}", cotEvent);

        return "";
    }
}
