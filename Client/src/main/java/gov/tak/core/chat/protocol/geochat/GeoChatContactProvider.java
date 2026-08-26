package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.ContactPresenceListener;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.contact.ContactStore;
import gov.tak.platform.contact.Contacts;
import gov.tak.platform.contact.IndividualContactBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static gov.tak.core.chat.protocol.geochat.GeoChatConstants.GEOCHAT_PROTOCOL;

/**
 * Responsible for providing GeoChat contact updates to the {@link ContactStore} as well as internal tracking of their
 * ownership.
 *
 * @since 3.0
 */
public class GeoChatContactProvider implements ContactPresenceListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Commo commo;
    private final IContactService contactService;

    private final Set<String> ownedGroupIds = ConcurrentHashMap.newKeySet();

    /**
     * @param commo        The Commo instance used to retrieve the list of contacts that are currently available
     * @param contactService The contact service through which this contact provider manages contacts
     */
    GeoChatContactProvider(@Nonnull Commo commo, @Nonnull IContactService contactService)
    {
        this.commo = commo;
        this.contactService = contactService;
    }

    /**
     * Starts the GeoChat contact provider.
     *
     * @param clientUid The unique ID of the client
     * @param clientName The call sign of the client
     */
    void start(@Nonnull String clientUid, @Nonnull String clientName)
    {
        final GeoChatAllChatRoomsContact allChatRoomsContact = new GeoChatAllChatRoomsContact(clientUid, clientName);
        final GeoChatRootGroupContact rootGroupContact = new GeoChatRootGroupContact(clientUid, clientName);
        final GeoChatTeamsContact teamsContact = new GeoChatTeamsContact();

        contactService.addContact(allChatRoomsContact);
        contactService.addContact(rootGroupContact);
        contactService.addContact(teamsContact);
    }

    /**
     * Stops the GeoChat contact provider.
     */
    void stop()
    {
        contactService.removeContact(GeoChatAllChatRoomsContact.ALL_CHAT_ROOMS_GROUP_NAME);
        contactService.removeContact(GeoChatRootGroupContact.GROUPS_ROOT_UID);
        contactService.removeContact(GeoChatTeamsContact.TEAMS_GROUP_UID);
    }

    /**
     * Populates the {@link ContactStore} with all existing direct contacts (as provided by {@link Commo#getContacts()}).
     */
    void populateContactsFromGeoChat()
    {
        // Create individual contacts and add them to the contact store via the contact service
        for (Contact currentContact : commo.getContacts())
        {
            createIndividualContact(currentContact.contactUID, currentContact.contactUID);
        }
    }

    @Override
    public void contactAdded(Contact contact)
    {
        createIndividualContact(contact.contactUID, contact.contactUID);
    }

    @Override
    public void contactRemoved(Contact contact)
    {
        contactService.removeContact(contact.contactUID);
    }

    /**
     * Gets the contact with a UID matching the given UID.
     *
     * @param contactUid The UID of the contact to get
     * @return The contact that matches the given UID, or {@code null} if no such contact currently exists
     */
    @Nullable
    IContact getContact(@Nonnull String contactUid)
    {
        return contactService.getContact(contactUid);
    }

    /**
     * Adds a group contact to the {@link ContactStore}. The added group contact will be tracked as an "owned" group
     * (i.e. it was created by the client user).
     *
     * @param groupContact The group contact to add as an "owned" group
     */
    void addOwnedGroupContact(@Nonnull IGroupContact groupContact)
    {
        final String groupContactUid = groupContact.getUid();

        ownedGroupIds.add(groupContactUid);
        contactService.addContact(groupContact);
    }

    /**
     * Adds a group contact to the {@link ContactStore}. The added group contact will be tracked as an "unowned" group
     * (i.e. it was created by another TAK user on the network).
     *
     * @param groupContact The group contact to add as an "unowned" group
     */
    void addUnownedGroupContact(@Nonnull IGroupContact groupContact)
    {
        contactService.addContact(groupContact);
    }

    /**
     * Checks if a group contact UID is that of an "owned" group contact.
     *
     * @param groupContactUid The group contact UID to check
     * @return {@code true} if the group contact matching the given UID is an "owned" group contact
     */
    boolean isGroupOwnedByClient(@Nonnull String groupContactUid)
    {
        return ownedGroupIds.contains(groupContactUid);
    }

    /**
     * Updates a contact in the {@link ContactStore}.
     *
     * @param updatedContact The updated contact
     */
    void updateContact(@Nonnull IContact updatedContact)
    {
        contactService.updateContact(updatedContact);
    }

    /**
     * Removes a group from the {@link ContactStore}.
     *
     * @param deletedGroup The group to remove
     */
    void removeGroup(@Nonnull IGroupContact deletedGroup)
    {
        final String deletedGroupUid = deletedGroup.getUid();

        ownedGroupIds.remove(deletedGroupUid);
        contactService.removeContact(deletedGroupUid);
    }

    /**
     * Creates and adds a new {@link IIndividualContact} to the {@link ContactStore}.
     *
     * @param uid UID of the contact to create
     * @param name The name of the contact to create
     */
    void createIndividualContact(@Nonnull String uid, @Nonnull String name)
    {
        if (contactService.getContact(uid) != null)
        {
            logger.debug("Attempted to create individual contact that already exists in the contact store: uid={}", uid);
            return;
        }

        final IIndividualContact contact = new IndividualContactBuilder()
                .withUniqueContactId(uid)
                .withDisplayName(name)
                .withAttributes(createDefaultGeoChatContactAttributeSet()).build();

        contactService.addContact(contact);
    }

    /**
     * Creates the default set of attributes used by GeoChat contacts to identify them as such.
     *
     * @return A new attribute set instance containing the default GeoChat attributes
     */
    @Nonnull
    public static AttributeSet createDefaultGeoChatContactAttributeSet()
    {
        final AttributeSet defaultGeoChatContactAttributeSet = new AttributeSet();

        defaultGeoChatContactAttributeSet.setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, new String[]{GEOCHAT_PROTOCOL});

        return defaultGeoChatContactAttributeSet;
    }
}
