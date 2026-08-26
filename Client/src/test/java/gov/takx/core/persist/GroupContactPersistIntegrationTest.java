package gov.takx.core.persist;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IPersistableGroupContact;
import gov.tak.api.util.AttributeSet;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsContact;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.tak.platform.contact.IndividualContactBuilder;
import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static gov.takx.core.persist.GroupContactPersist.GROUP_CONTACT_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

// TODO KMB: Should be merged with GroupContactPersistTest

/**
 * Persistence tests for {@link IGroupContactPersist}. Stands up a real DB instance to test persistence of
 * {@link IGroupContact}s.
 *
 * @since 3.2
 */
@PseudoBean
public class GroupContactPersistIntegrationTest extends AContainerTest
{
    private PersistTestSetup test;

    @Inject
    @NodeUUID
    private UUID nodeUuid;

    @Inject
    @CallSign
    private String callSign;

    @Inject
    private GroupContactManager groupContactManager;

    @Inject
    private IGroupContactPersist groupContactPersist;

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.init();

        groupContactManager.contactAdded(new GeoChatRootGroupContact(nodeUuid.toString(), callSign));
        groupContactManager.contactAdded(new GeoChatTeamsContact());
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    @AfterEach
    public void clearPersistedContacts()
    {
        final IPersistService persistService = test.getService();

        // HSQL throws a warning when doing a "DELETE FROM" on an empty table, so ensure it's not empty first
        if (persistService.countSQL(GROUP_CONTACT_TABLE, "") != 0)
        {
            persistService.executeNativeUpdateSQL("DELETE FROM " + GROUP_CONTACT_TABLE);
        }

        // Since we just nuked the table out from under Hibernate, close the session to remove all data between tests
        persistService.cleanupAndClose();
    }

    @Test
    public void persistGroupContact_PersistsGroupUid()
    {
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        final IGroupContact queriedGroupContact = queryGroupContact(groupContact.getUid());
        assertThat(queriedGroupContact.getUid()).isEqualTo(groupContact.getUid());
    }

    @Test
    public void persistGroupContact_PersistsGroupDisplayName()
    {
        final String testDisplayName = "Test group 1357";
        final IGroupContact groupContact = new GroupContactBuilder()
                .withDisplayName(testDisplayName)
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        final IGroupContact queriedGroupContact = queryGroupContact(groupContact.getUid());
        assertThat(queriedGroupContact.getName()).isEqualTo(groupContact.getName());
    }

    @Test
    public void persistGroupContact_PersistsGroupMembers_WhenOneGroupMemberExists()
    {
        final IContact groupMember = new IndividualContactBuilder().build();
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(groupMember))
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        assertThat(queryGroupContact(groupContact.getUid()).getGroupMembers()).containsOnly(groupMember);
    }

    @Test
    @SuppressWarnings("DistinctVarargsChecker")
    public void persistGroupContact_PersistsGroupMembers_WhenMultipleGroupMembersExist()
    {
        final Set<IContact> groupMembers = Set.of(
                new IndividualContactBuilder().build(),
                new IndividualContactBuilder().build(),
                new IndividualContactBuilder().build());
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(groupMembers)
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        assertThat(queryGroupContact(groupContact.getUid()).getGroupMembers())
                .containsOnly(groupMembers.toArray(new IContact[0]));
    }

    @Test
    public void persistGroupContact_PersistsAttributeSet()
    {
        final String intAttributeName = "int";
        final String stringAttributeName = "string";
        final String blobAttributeName = "blob";
        final int anInt = 15;
        final String aString = "I'm a string";
        final byte[] aBlob = {1, 3, 5, 7};
        final AttributeSet attributeSet = new AttributeSet();

        attributeSet.setAttribute(intAttributeName, anInt);
        attributeSet.setAttribute(stringAttributeName, aString);
        attributeSet.setAttribute(blobAttributeName, aBlob);

        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .withAttributes(attributeSet)
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        final AttributeSet queriedAttributeSet = queryGroupContact(groupContact.getUid()).getAttributes();
        assertThat(queriedAttributeSet.getIntAttribute(intAttributeName)).isEqualTo(anInt);
        assertThat(queriedAttributeSet.getStringAttribute(stringAttributeName)).isEqualTo(aString);
        assertThat(queriedAttributeSet.getBinaryAttribute(blobAttributeName)).isEqualTo(aBlob);
    }

    @Test
    public void persistGroupContact_PersistsMultipleGroupContacts()
    {
        final IGroupContact groupContactOne = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactTwo = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactThree = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContactOne);
        groupContactPersist.persistGroupContact(groupContactTwo);
        groupContactPersist.persistGroupContact(groupContactThree);

        final IGroupContact queriedGroupContactOne = queryGroupContact(groupContactOne.getUid());
        final IGroupContact queriedGroupContactTwo = queryGroupContact(groupContactTwo.getUid());
        final IGroupContact queriedGroupContactThree = queryGroupContact(groupContactThree.getUid());

        assertThat(queriedGroupContactOne).isEqualTo(groupContactOne);
        assertThat(queriedGroupContactTwo).isEqualTo(groupContactTwo);
        assertThat(queriedGroupContactThree).isEqualTo(groupContactThree);
    }

    @Test
    public void queryAll_GetsGroupContact_WhenOneGroupContactHasBeenPersisted()
    {
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        assertThat(groupContactPersist.findAllGroupContacts()).containsOnly(groupContact);
    }

    @Test
    public void queryAll_GetsAllGroupContacts_WhenMultipleGroupContactsHaveBeenPersisted()
    {
        final IGroupContact groupContactOne = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactTwo = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactThree = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContactOne);
        groupContactPersist.persistGroupContact(groupContactTwo);
        groupContactPersist.persistGroupContact(groupContactThree);

        assertThat(groupContactPersist.findAllGroupContacts()).containsOnly(groupContactOne, groupContactTwo, groupContactThree);
    }

    @Test
    public void queryAll_ReturnsEmptySet_WhenNoGroupContactsHaveBeenPersisted()
    {
        assertThat(groupContactPersist.findAllGroupContacts()).isEmpty();
    }

    @Test
    public void queryAll_ReturnsGroupContactWithParentContact_WhenParentContactIsRootGroupContact()
    {
        final IGroupContact rootGroupContact = new GeoChatRootGroupContact(nodeUuid.toString(), callSign);
        final IGroupContact groupContact = new GroupContactBuilder()
                .withParentContact(rootGroupContact)
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);

        final var queriedGroupContacts = groupContactPersist.findAllGroupContacts();

        assertThat(queriedGroupContacts).hasSize(1).contains(groupContact);

        final IGroupContact queriedGroupContactParent = queriedGroupContacts.get(0).getParentContact();
        assertThat(queriedGroupContactParent).isInstanceOf(GeoChatRootGroupContact.class);

        final Set<IContact> rootGroupContactMembers = queriedGroupContactParent.getGroupMembers();
        assertThat(rootGroupContactMembers).hasSize(1);

        final IContact rootGroupContactMember = rootGroupContactMembers.iterator().next();
        assertThat(rootGroupContactMember.getUid()).isEqualTo(nodeUuid.toString());
        assertThat(rootGroupContactMember.getName()).isEqualTo(callSign);
    }

    @Test
    public void queryAll_ReturnsGroupContactWithGroupMembers_WhenAllGroupMembersAreIndividualContacts()
    {
        final String intAttributeName = "int";
        final String stringAttributeName = "string";
        final String blobAttributeName = "blob";
        final int anInt = 21;
        final String aString = "I'm a String!";
        final byte[] aBlob = {2, 4, 6, 8};
        final AttributeSet attributesOne = new AttributeSet();
        final AttributeSet attributesTwo = new AttributeSet();
        final AttributeSet attributesThree = new AttributeSet();
        final IIndividualContact groupMemberOne = new IndividualContactBuilder()
                .withDisplayName("displayNameOne")
                .withAttributes(attributesOne)
                .build();
        final IIndividualContact groupMemberTwo = new IndividualContactBuilder()
                .withDisplayName("displayNameTwo")
                .withAttributes(attributesTwo)
                .build();
        final IIndividualContact groupMemberThree = new IndividualContactBuilder()
                .withDisplayName("displayNameThree")
                .withAttributes(attributesThree)
                .build();
        final Set<IContact> groupMembers = Set.of(groupMemberOne, groupMemberTwo, groupMemberThree);
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(groupMembers)
                .build();

        attributesOne.setAttribute(intAttributeName, anInt);
        attributesTwo.setAttribute(stringAttributeName, aString);
        attributesThree.setAttribute(blobAttributeName, aBlob);

        groupContactPersist.persistGroupContact(groupContact);

        assertThat(groupContactPersist.findAllGroupContacts().get(0).getGroupMembers())
                .containsOnly(groupMembers.toArray(new IContact[0]));
    }

    @Test
    public void updateGroupContact_UpdatesGroupDisplayName()
    {
        final String testDisplayName = "Test group 1357";
        final IPersistableGroupContact groupContact = new GroupContactBuilder()
                .withDisplayName("initial value")
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);
        groupContact.setName(testDisplayName);
        groupContactPersist.persistGroupContact(groupContact);

        final IGroupContact queriedGroupContactTwo = queryGroupContact(groupContact.getUid());
        assertThat(queriedGroupContactTwo.getName()).isEqualTo(testDisplayName);
    }

    @Test
    public void updateGroupContact_UpdatesGroupMembers_WhenOneGroupMemberExists()
    {
        final IIndividualContact groupMemberOne = new IndividualContactBuilder().build();
        final IIndividualContact groupMemberTwo = new IndividualContactBuilder().build();

        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(groupMemberOne))
                .build();

        groupContactPersist.persistGroupContact(groupContact);
        groupContact.setGroupMembers(Set.of(groupMemberTwo));
        groupContactPersist.persistGroupContact(groupContact);

        final Set<IContact> queriedGroupContactMembers = queryGroupContact(groupContact.getUid()).getGroupMembers();
        assertThat(queriedGroupContactMembers).containsOnly(groupMemberTwo);
    }

    @Test
    public void updateGroupContact_UpdatesGroupMemberUids_WhenMultipleGroupMembersExist()
    {
        final IIndividualContact groupMemberOne = new IndividualContactBuilder().build();
        final IIndividualContact groupMemberTwo = new IndividualContactBuilder().build();
        final IIndividualContact groupMemberThree = new IndividualContactBuilder().build();
        final IIndividualContact groupMemberFour = new IndividualContactBuilder().build();
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(groupMemberOne, groupMemberTwo, groupMemberThree))
                .build();

        groupContactPersist.persistGroupContact(groupContact);
        groupContact.setGroupMembers(Set.of(groupMemberOne, groupMemberTwo, groupMemberFour));
        groupContactPersist.persistGroupContact(groupContact);

        final Set<IContact> queriedGroupContactMembers = queryGroupContact(groupContact.getUid()).getGroupMembers();
        assertThat(queriedGroupContactMembers).containsOnly(groupMemberOne, groupMemberTwo, groupMemberFour);
    }

    @Test
    public void updateGroupContact_UpdatesAttributeSet()
    {
        final String intAttributeName = "int";
        final String stringAttributeName = "string";
        final String blobAttributeName = "blob";
        final int anInt = 21;
        final String aString = "I'm a String!";
        final byte[] aBlob = {2, 4, 6, 8};
        final AttributeSet attributeSet = new AttributeSet();
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .withAttributes(attributeSet)
                .build();
        final String uniqueId = groupContact.getUid();

        attributeSet.setAttribute(intAttributeName, anInt);
        attributeSet.setAttribute(stringAttributeName, aString);

        groupContactPersist.persistGroupContact(groupContact);

        final IGroupContact gc1 = queryGroupContact(uniqueId);
        final AttributeSet queriedAttributeSetOne = gc1.getAttributes();
        assertThat(queriedAttributeSetOne.containsAttribute(intAttributeName)).isTrue();
        assertThat(queriedAttributeSetOne.containsAttribute(stringAttributeName)).isTrue();
        assertThat(queriedAttributeSetOne.containsAttribute(blobAttributeName)).isFalse();

        queriedAttributeSetOne.removeAttribute(stringAttributeName);
        queriedAttributeSetOne.setAttribute(blobAttributeName, aBlob);

        groupContactPersist.persistGroupContact(gc1);

        final IGroupContact gc2 = queryGroupContact(uniqueId);
        final AttributeSet queriedAttributeSetTwo = gc2.getAttributes();
        assertThat(queriedAttributeSetTwo.containsAttribute(intAttributeName)).isTrue();
        assertThat(queriedAttributeSetTwo.containsAttribute(stringAttributeName)).isFalse();
        assertThat(queriedAttributeSetTwo.containsAttribute(blobAttributeName)).isTrue();
    }

    /**
     * Queries the database for the group contact with the given unique ID.
     *
     * @param uniqueId The UID of the contact to query
     * @return The queried group contact
     */
    private IGroupContact queryGroupContact(String uniqueId)
    {
        final IGroupContact groupContact = groupContactPersist.findGroupContact(uniqueId);
        assertThat(groupContact).isNotNull();
        return groupContact;
    }

    @Test
    public void deleteGroupContact_DeletesContact_WhenOneGroupContactHasBeenPersisted()
    {
        final IGroupContact groupContact = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContact);
        groupContactPersist.deleteGroupContact(groupContact.getUid());

        assertThat(groupContactPersist.groupContactExists(groupContact.getUid())).isFalse();
    }

    @Test
    public void deleteGroupContact_DeletesContact_WhenMultipleContactsHaveBeenPersisted()
    {
        final IGroupContact groupContactOne = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactTwo = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();
        final IGroupContact groupContactThree = new GroupContactBuilder()
                .withGroupMembers(Set.of(new IndividualContactBuilder().build()))
                .build();

        groupContactPersist.persistGroupContact(groupContactOne);
        groupContactPersist.persistGroupContact(groupContactTwo);
        groupContactPersist.persistGroupContact(groupContactThree);

        groupContactPersist.deleteGroupContact(groupContactOne.getUid());
        groupContactPersist.deleteGroupContact(groupContactTwo.getUid());

        final List<IGroupContact> allGroupContacts = groupContactPersist.findAllGroupContacts();
        assertThat(allGroupContacts).hasSize(1);
        assertThat(allGroupContacts.get(0)).isEqualTo(groupContactThree);
    }
}
