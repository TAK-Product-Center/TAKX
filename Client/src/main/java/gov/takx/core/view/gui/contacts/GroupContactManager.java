package gov.takx.core.view.gui.contacts;

import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.IChatOperationCallback;
import gov.tak.api.chat.IChatService;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManager;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.core.chat.protocol.geochat.GeoChatAllChatRoomsContact;
import gov.tak.core.chat.protocol.geochat.GeoChatConstants;
import gov.tak.core.chat.protocol.geochat.GeoChatRolesManager;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsManager;
import gov.tak.platform.chat.ChatService;
import gov.takx.api.annotation.Internal;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.chat.ConversationStore;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Manages all {@link IGroupContact}s.
 *
 * @since 3.5
 */
@ApplicationScoped
public class GroupContactManager implements IGroupContactManager, IContactListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final ImageIcon GROUP_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "group.png");

    private final Set<IGroupContactManagerListener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, IGroupContact> groupContactMap = new ConcurrentHashMap<>();
    private final Set<String> ownedGroupsByUid = ConcurrentHashMap.newKeySet();
    private final IGroupContactPersist groupContactPersist;
    private final ExecutorService executorService;
    private final IContactService contactService;
    private final ConversationStore conversationStore;
    private final ChatService chatService;
    private final GeoChatTeamsManager teamsManager;
    private final GeoChatRolesManager rolesManager;

    private IGroupContact allChatRoomsContact;
    private IGroupContact rootGroupContact;
    private IGroupContact teamsContact;

    // startService isn't always called when tests are run, so we need to track it
    private boolean started;

    @Inject
    public GroupContactManager(IGroupContactPersist groupContactPersist, ExecutorService executorService,
                               IContactService contactService, ConversationStore conversationStore,
                               IChatService chatService, GeoChatTeamsManager teamsManager, GeoChatRolesManager rolesManager)
    {
        this.groupContactPersist = groupContactPersist;
        this.executorService = executorService;
        this.contactService = contactService;
        this.conversationStore = conversationStore;
        this.chatService = (ChatService) chatService;
        this.teamsManager = teamsManager;
        this.rolesManager = rolesManager;
    }

    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event)
    {
        contactService.registerContactListener(this);
        started = true;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        if (started)
        {
            contactService.unregisterContactListener(this);

            ownedGroupsByUid.clear();
            groupContactMap.clear();
            listeners.clear();

            started = false;
        }
    }

    /**
     * @param groupContact The group contact
     * @return The group contact's icon
     */
    @Nonnull
    public Icon getIcon(@Nonnull IGroupContact groupContact)
    {
        final Icon icon = teamsManager.isManaged(groupContact) ? teamsManager.getIcon(groupContact)
                : rolesManager.isManaged(groupContact) ? rolesManager.getIcon(groupContact)
                : null;

        return icon != null ? icon : GROUP_ICON;
    }

    /**
     * Requests that a new group contact be created for the given chat service protocol (e.g. GeoChat, XMPP, etc.). All
     * requested group members must support the given chat service protocol, and group members cannot be empty.
     *
     * @param groupName The name of the group to create (i.e. the display name of the IGroupContact that will be created)
     * @param groupMembers The group members that should be affiliated with the created group contact
     * @param parentGroup The parent group contact, or {@code null} if the group being created is not a subgroup
     * @throws IllegalArgumentException if group members is empty or a given group member does not support the requested protocol
     */
    public void createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers,
                            @Nullable IGroupContact parentGroup)
    {
        createGroup(groupName, groupMembers, parentGroup, chatService.getDefaultProtocol(), this::logChatOperationResult);
    }

    @Override
    public void createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers,
                            @Nullable IGroupContact parentGroup, @Nonnull String protocol,
                            @Nullable IChatOperationCallback callback)
    {
        chatService.createGroup(groupName, groupMembers, parentGroup, protocol, callback);
    }

    @Override
    public void updateGroup(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> groupMembers,
                            @Nullable IChatOperationCallback callback)
    {
        chatService.updateGroup(groupContact, groupMembers, callback);
    }

    @Override
    public void deleteGroup(@Nonnull IGroupContact groupContact, @Nullable IChatOperationCallback callback)
    {
        // If the group was created by this user, notify the group members via the chat service. The chat service will
        // notify the IContactService
        if (ownsGroup(groupContact))
        {
            chatService.deleteGroup(groupContact, callback);
        }
        // If the group was created by another user, just delete it locally
        else
        {
            contactService.removeContact(groupContact.getUid());
        }
    }

    @Nullable
    @Override
    public IGroupContact getGroupContact(@Nonnull String uid)
    {
        return groupContactMap.get(uid);
    }

    @Nonnull
    @Override
    public List<IGroupContact> getGroupContacts()
    {
        return groupContactMap.values().stream()
                .filter(groupContact -> !rolesManager.isManaged(groupContact))
                .sorted(Comparator.comparing(this::getDepth))
                .collect(Collectors.toList());
    }

    /**
     * Returns the group contact associated with a UID. Differs from {@link #getGroupContact(String)} in that it will
     * also search the group roots (e.g. Groups, All Chat Rooms, etc.). For internal use only.
     *
     * @param uid The group contact UID
     * @return The {@link IGroupContact} with the specified UID, or {@code null} if not found
     */
    @Internal
    @Nullable
    public IGroupContact getGroupContactIncludeRoots(@Nonnull String uid)
    {
        if (uid.equals(rootGroupContact.getUid())) return rootGroupContact;
        if (uid.equals(allChatRoomsContact.getUid())) return allChatRoomsContact;
        if (uid.equals(teamsContact.getUid())) return teamsContact;

        return groupContactMap.get(uid);
    }

    /**
     * @return The root group contact. This may be {@code null} during startup if the root contact has not yet been
     * added.
     */
    @Nullable
    @Override
    public IGroupContact getRootGroupContact()
    {
        return rootGroupContact;
    }

    /**
     * @return The all chat rooms group contact. This may be {@code null} during startup if the root contact has not yet 
     * been added.
     */
    public IGroupContact getAllChatRoomsContact()
    {
        return allChatRoomsContact;
    }

    /**
     * @return The teams group contact. This may be {@code null} during startup if the root contact has not yet been 
     * added.
     */
    public IGroupContact getTeamsContact()
    {
        return teamsContact;
    }

    /**
     * @return The role {@link IGroupContact}s.
     */
    @Nonnull
    public List<IGroupContact> getRoleContacts()
    {
        return groupContactMap.values().stream().filter(rolesManager::isManaged).collect(Collectors.toList());
    }

    @Override
    public void addListener(@Nonnull IGroupContactManagerListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull IGroupContactManagerListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Checks to see if the local user owns the group represented by a {@link IGroupContact}.
     *
     * @param groupContact The contact to check
     * @return {@code true} if the local user owns the group represented by the contact, {@code false} if not.
     */
    public boolean ownsGroup(@Nonnull IGroupContact groupContact)
    {
        return ownedGroupsByUid.contains(groupContact.getUid());
    }

    /**
     * Returns a boolean indicating if there are any user-created groups. These groups do not include the standard
     * top-level groups ("All Contacts", "Groups", "Teams") or any groups automatically generated to represent teams.
     *
     * @return {@code true} if there are user groups
     */
    public boolean hasUserGroups()
    {
        return groupContactMap.values().stream()
                .anyMatch(contact -> {
                    final IGroupContact parent = contact.getParentContact();
                    // Teams have a null parent and are not user managed groups
                    return parent != null && !GeoChatTeamsContact.TEAMS_GROUP_UID.equals(parent.getUid());
                });
    }

    @Override
    public void contactAdded(@Nonnull IContact contact)
    {
        if (contact instanceof final IGroupContact groupContact)
        {
            logger.debug("Adding contact {}", groupContact);

            final boolean isRootGroupContact = groupContact instanceof GeoChatRootGroupContact;
            final boolean isAllChatRoomsContact = groupContact instanceof GeoChatAllChatRoomsContact;
            final boolean isTeamsContact = groupContact instanceof GeoChatTeamsContact;

            if (isRootGroupContact) rootGroupContact = groupContact;
            if (isAllChatRoomsContact) allChatRoomsContact = groupContact;
            if (isTeamsContact) teamsContact = groupContact;

            if (!isRootGroupContact && !isAllChatRoomsContact && !isTeamsContact)
            {
                final String uid = groupContact.getUid();

                // IContactService sometimes calls contactUpdated before contactAdded
                if (groupContactMap.containsKey(uid))
                {
                    logger.debug("Ignoring contact that already exists: {}", contact);
                    return;
                }

                groupContactMap.put(uid, groupContact);
                groupContactPersist.persistGroupContact(groupContact);
                trackGroupIfOwned(groupContact);

                final IGroupContact parentContact = groupContact.getParentContact();

                // Roles don't have parent contacts
                if (parentContact != null)
                {
                    // Add to parent's group members
                    parentContact.setGroupMembers(
                            Stream.concat(parentContact.getGroupMembers().stream(), Stream.of(groupContact))
                                    .collect(Collectors.toSet()));

                    // We update but don't persist the top-level group contacts (Groups, Teams)
                    if (parentContact.getParentContact() != null)
                        groupContactPersist.persistGroupContact(parentContact);
                }
            }

            listeners.forEach(listener -> listener.contactAdded(groupContact));

            logger.debug("Added group contact '{}'", groupContact);

            if ((isRootGroupContact || isTeamsContact) && rootGroupContact != null && teamsContact != null)
            {
                loadGroupContacts();
            }
        }
    }

    @Override
    public void contactRemoved(@Nonnull IContact contact)
    {
        if (contact instanceof final IGroupContact groupContact)
        {
            final String groupContactUid = groupContact.getUid();
            final IGroupContact parentContact = groupContact.getParentContact();

            if (parentContact != null)
            {
                final String parentUid = parentContact.getUid();

                // Call remove just in case it's owned
                ownedGroupsByUid.remove(groupContactUid);

                // The group will have been removed from persistence if the parent has already been removed
                if (groupContactMap.remove(groupContactUid) == null || // stop if already removed
                // stop if parent already removed (Groups and Teams contacts not tracked, but always exist)
                        !groupContactMap.containsKey(parentUid)
                                && !parentUid.equals(rootGroupContact.getUid())
                                && !parentUid.equals(teamsContact.getUid()))
                {
                    return;
                }
            }

            // Remove the group and its children from persistence, delete conversation (if needed) and notify listeners
            deleteGroupAndDescendants(groupContact);

            // Clean up and persist parent
            if (parentContact != null)
            {
                // Remove group from parent contact
                parentContact.setGroupMembers(
                        parentContact.getGroupMembers().stream()
                                .filter(child -> !child.getUid().equals(groupContactUid))
                                .collect(Collectors.toSet()));
                groupContactPersist.persistGroupContact(parentContact);
                listeners.forEach(listener -> listener.contactUpdated(parentContact));
            }

            logger.debug("Removed group contact \"{}\" and its children", groupContact);
        }
    }

    @Override
    public void contactUpdated(@Nonnull IContact contact)
    {
        if (contact instanceof final IGroupContact groupContact)
        {
            logger.debug("Updating contact {}", groupContact);

            if (!groupContactMap.containsKey(groupContact.getUid()))
            {
                // IContactService sometimes calls contactUpdated before contactAdded
                logger.debug("Trying to update contact that doesn't exist: {}. Contact will be added", contact);
                contactAdded(contact);
                return;
            }

            conversationStore.updateConversationForContact(groupContact);

            groupContactPersist.persistGroupContact(groupContact);

            listeners.forEach(listener -> listener.contactUpdated(groupContact));

            logger.debug("Updated group contact '{}'", groupContact);
        }
    }

    /**
     * Queries all group contacts currently persisted, and adds them to the contact store. This is done in the 
     * background.
     */
    private void loadGroupContacts()
    {
        executorService.submit(() -> {
            Thread.currentThread().setName("Group Contact Query Thread");

            groupContactPersist.findAllGroupContacts().stream()
                    // sort the group contacts by depth so parents are added before their children
                    .sorted(Comparator.comparingInt(this::getDepth))
                    // teams and roles will be restored by the managers as they become available
                    .filter(groupContact -> !teamsManager.isManaged(groupContact) && !rolesManager.isManaged(groupContact))
                    .forEach(groupContact -> {
                        if (contactService.getContact(groupContact.getUid()) == null)
                        {
                            contactService.addContact(groupContact);
                            trackGroupIfOwned(groupContact);
                        }
                    });
        });
    }

    /**
     * Adds an {@link IGroupContact} to the set of owned groups if it is owned.
     * 
     * @param groupContact The contact to check
     */
    private void trackGroupIfOwned(@Nonnull IGroupContact groupContact)
    {
        if (groupContact.getAttributes()
                .getBooleanAttribute(GeoChatConstants.CLIENT_OWNED_GROUP_ATTRIBUTE, false))
        {
            ownedGroupsByUid.add(groupContact.getUid());
        }
    }

    /**
     * Calculates the depth of a potentially nested group contact, where the root is at depth 0
     *
     * @param groupContact The group contact
     * @return The group contact's depth
     */
    private int getDepth(@Nonnull IGroupContact groupContact)
    {
        int depth = 0;

        while (groupContact.getParentContact() != null)
        {
            depth++;
            groupContact = groupContact.getParentContact();
        }

        return depth;
    }

    /**
     * Recursively deletes a {@link IGroupContact} and all of its descendants, descendants first.
     * 
     * @param groupContact The contact to delete
     */
    private void deleteGroupAndDescendants(@Nonnull IGroupContact groupContact)
    {
        for (IContact child : groupContact.getGroupMembers())
        {
            if (child instanceof IGroupContact childGroupContact)
            {
                deleteGroupAndDescendants(childGroupContact);
            }
        }

        groupContactMap.remove(groupContact.getUid());
        groupContactPersist.deleteGroupContact(groupContact.getUid());

        // Group conversations should not be deleted, just cleared, so they can still be accessed via Chats
        conversationStore.getConversationForContact(groupContact).clearUnreadMessages();

        listeners.forEach(listener -> listener.contactRemoved(groupContact));
    }

    /**
     * Logs the result of a chat operation.
     *
     * @param chatOperationResult The result of the attempted chat operation
     */
    private void logChatOperationResult(ChatOperationResult chatOperationResult)
    {
        if (chatOperationResult.getStatus() == ChatOperationResult.Status.SUCCESS)
        {
            if (logger.isDebugEnabled()) logger.debug("{} request successful", chatOperationResult.getOperation());
        } else
        {
            logger.warn(chatOperationResult.getMessage());
        }
    }
}