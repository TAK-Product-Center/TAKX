package gov.tak.core.chat.protocol.geochat;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Base class for GeoChat group managers
 *
 * @since 3.5
 */
abstract class AGeoChatGroupManager implements IIndividualContactManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Maps a group identifier to its corresponding IGroupContact
    protected final Map<String, IGroupContact> groupUidToGroupContactMap = new ConcurrentHashMap<>();

    // Cache of each group's icon. Icons are reusable and can be kept across projects
    protected final Map<String, Icon> iconCache = new HashMap<>(); // always accessed on the EDT

    protected final GroupContactManager groupContactManager;
    protected final IContactService contactService;
    protected final IGroupContactPersist groupContactPersist;
    protected final IndividualContactManager individualContactManager;

    // Takes a contact and returns its relevant group identifier
    private final Function<IIndividualContact, String> contactToGroupUidFunction;

    // Provides the client's relevant group identifier
    private final Supplier<String> clientGroupUidSupplier;

    /**
     * @param contactToGroupUidFunction Takes a contact and returns its group identifier
     * @param clientGroupUidSupplier Provides the client's group identifier
     */
    AGeoChatGroupManager(GroupContactManager groupContactManager, IContactService contactService,
                         IGroupContactPersist groupContactPersist, IndividualContactManager individualContactManager,
                         Function<IIndividualContact, String> contactToGroupUidFunction,
                         Supplier<String> clientGroupUidSupplier)
    {

        this.groupContactManager = groupContactManager;
        this.contactService = contactService;
        this.groupContactPersist = groupContactPersist;
        this.individualContactManager = individualContactManager;
        this.contactToGroupUidFunction = contactToGroupUidFunction;
        this.clientGroupUidSupplier = clientGroupUidSupplier;
    }

    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event,
                              @PlatformUnderTest boolean underTest)
    {
        if (!underTest) individualContactManager.addListener(this);

        // Add existing groups
        groupUidToGroupContactMap.putAll(
                groupContactManager.getGroupContacts().stream()
                        .filter(this::isManaged)
                        .collect(Collectors.toMap(IContact::getUid, Function.identity())));
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event,
                             @PlatformUnderTest boolean underTest)
    {
        if (!underTest) individualContactManager.removeListener(this);

        groupUidToGroupContactMap.clear();
    }

    @Override
    public void contactAdded(@Nonnull IIndividualContact contact)
    {
        // The group may be null when first restored from the database
        if (contactToGroupUidFunction.apply(contact) != null) addMember(contact);
    }

    @Override
    public void contactRemoved(@Nonnull IIndividualContact contact)
    {
        final String groupUid = contactToGroupUidFunction.apply(contact);

        if (groupUid != null) removeMember(groupUid, contact);
    }

    @Override
    public void contactUpdated(@Nonnull IIndividualContact contact)
    {
        final String currentGroupUid = findContactGroup(contact);
        final String newGroupUid = contactToGroupUidFunction.apply(contact);

        if (StringUtils.equals(newGroupUid, currentGroupUid)) return;

        // remove from old group
        if (currentGroupUid != null) removeMember(currentGroupUid, contact);

        // add to new group
        if (newGroupUid != null) addMember(contact);
    }

    @Override
    public void contactStateChanged(@Nonnull IIndividualContact contact)
    {
        switch (IndividualContactUtils.getState(contact))
        {
            case active ->
            {
                if (findContactGroup(contact) == null) addMember(contact);
            }
            case inactive ->
            {
                // ignore
            }
            case gone ->
            {
                contactRemoved(contact);
            }
        }
    }

    /**
     * @param groupContact The group's group contact
     * @return The group's icon or {@code null} if the group is not managed by this manager
     */
    @Nullable
    public Icon getIcon(@Nonnull IGroupContact groupContact)
    {
        if (!isManaged(groupContact)) return null;

        return iconCache.computeIfAbsent(groupContact.getUid(), this::createIcon);
    }

    /**
     * Subclasses can override this method to customize {@link GroupContactBuilder}s before contacts are built
     *
     * @param builder The builder
     */
    void customizeGroupContactBuilder(@Nonnull GroupContactBuilder builder)
    {
    }

    /**
     * @param groupContact The {@link IGroupContact} to check
     * @return {@code true} if the group is managed by this manager but the local client is not a member of the group
     */
    public boolean isManagedAndClientNotMember(@Nonnull IGroupContact groupContact)
    {
        return isManaged(groupContact) && !groupContact.getUid().equalsIgnoreCase(clientGroupUidSupplier.get());
    }

    /**
     * @param groupContact The {@link IGroupContact} to check
     * @return {@code true} if the {@link IGroupContact} represents a group managed by this manager
     */
    abstract boolean isManaged(@Nonnull IGroupContact groupContact);

    /**
     * @param groupUid The group UID
     * @return A new instance of the group's icon
     */
    @Nonnull
    abstract Icon createIcon(@Nonnull String groupUid);

    /**
     * Add an {@link IIndividualContact} to its group, creating the group's {@link IGroupContact} if necessary.
     * Synchronized to prevent multiple threads from creating and persisting the same contact.
     *
     * @param contact The contact to add to its group
     */
    private synchronized void addMember(@Nonnull IIndividualContact contact)
    {
        final String groupUid = contactToGroupUidFunction.apply(contact);

        if (groupUid == null) return;

        final IGroupContact groupContact = groupUidToGroupContactMap.get(groupUid);

        if (groupContact == null)
        {
            // Restore or create the group contact with its first member
            restoreOrCreateGroupContact(groupUid, contact);
        } else
        {
            // Update the group's group members and persist
            addMemberToGroup(groupContact, contact);
        }
    }

    /**
     * @param groupContact The group contact to update
     * @param member The member to add to the group contact
     */
    private void addMemberToGroup(@Nonnull IGroupContact groupContact, @Nonnull IContact member)
    {
        groupContact.setGroupMembers(
                Stream.concat(groupContact.getGroupMembers().stream(), Stream.of(member)).collect(Collectors.toSet()));

        groupContactManager.contactUpdated(groupContact);

        logger.debug("Added contact {} to group {}", member, groupContact);
    }

    /**
     * @param groupUid The UID of the group to restore or create
     * @param contact The contact to add to the group contact
     */
    private void restoreOrCreateGroupContact(@Nonnull String groupUid, @Nonnull IIndividualContact contact)
    {
        IGroupContact groupContact = groupContactPersist.findGroupContact(groupUid);

        if (groupContact != null)
        {
            // Always start from scratch when a group contact is restored
            groupContact.setGroupMembers(Set.of(contact));
        } else
        {
            final GroupContactBuilder builder = new GroupContactBuilder()
                    .withAttributes(GeoChatContactProvider.createDefaultGeoChatContactAttributeSet())
                    .withUniqueContactId(groupUid)
                    .withDisplayName(groupUid)
                    .withGroupMembers(Set.of(contact));

            customizeGroupContactBuilder(builder);

            groupContact = builder.build();

            groupContactPersist.persistGroupContact(groupContact);
        }

        groupUidToGroupContactMap.put(groupUid, groupContact);

        // This will notify the GroupContactManager
        contactService.addContact(groupContact);

        logger.debug("Added group {} with contact {}", groupUid, contact);
    }

    /**
     * Removes an {@link IIndividualContact} from a group
     *
     * @param groupUid The group UID
     * @param contact The contact to remove
     */
    private void removeMember(@Nonnull String groupUid, @Nonnull IIndividualContact contact)
    {
        final IGroupContact groupContact = groupUidToGroupContactMap.get(groupUid);

        // The member's group may have been deleted
        if (groupContact == null)
        {
            logger.debug("The group with uid {} has already been removed. Ignoring request to remove member {}",
                    groupUid, contact.getName());
            return;
        }

        final String memberUid = contact.getUid();
        final Set<IContact> groupMembers = new HashSet<>(groupContact.getGroupMembers());

        // If the group didn't change, the contact has already been removed
        if (!groupMembers.removeIf(c -> c.getUid().equals(memberUid))) return;

        logger.debug("Removed contact {} from group {}", contact, groupUid);

        // If the group is now empty, remove the group
        if (groupMembers.isEmpty())
        {
            groupUidToGroupContactMap.remove(groupUid);

            // This will notify the GroupContactManager
            contactService.removeContact(groupContact.getUid());

            logger.debug("Removed empty group {}", groupUid);
        }
        // Otherwise, update the group's members
        else
        {
            groupContact.setGroupMembers(groupMembers);
            groupContactManager.contactUpdated(groupContact);
        }
    }

    @VisibleForTesting
    @Nullable
    public String findContactGroup(@Nonnull IContact contact)
    {
        final String contactUid = contact.getUid();

        return groupUidToGroupContactMap.entrySet().stream()
                .filter(entry -> entry.getValue().getGroupMembers().stream().anyMatch(c -> c.getUid().equals(contactUid)))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}