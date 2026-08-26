package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link GeoChatTeamsManager} tests
 *
 * @since 3.5
 */
public class GeoChatTeamsManagerTest extends AIntegrationTest
{
    private static final String CLIENT_TEAM = "Red";
    private static final GeoChatTeamsContact teamsContact = new GeoChatTeamsContact();
    private static final GeoChatRootGroupContact userGroupsContact = new GeoChatRootGroupContact("uid", "name");

    @Mocked
    private GroupContactManager groupContactManager;

    @Mocked
    private IndividualContactManager individualContactManager;

    @Mocked
    private IContactService contactService;

    @Mocked
    private IGroupContactPersist groupContactPersist;

    private GeoChatTeamsManager teamsManager;

    @BeforeEach
    void beforeEach()
    {
        when(individualContactManager.getClientContactTeam()).thenReturn(CLIENT_TEAM);
        when(groupContactManager.getTeamsContact()).thenReturn(teamsContact);
        when(groupContactPersist.findGroupContact(anyString())).thenReturn(null); // we don't want a mock

        clearInvocations(groupContactManager);
        clearInvocations(contactService);

        teamsManager = new GeoChatTeamsManager(groupContactManager, contactService, groupContactPersist, individualContactManager);
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenIsTeamContact_correctResultReturned(@Mocked(allowCaching = false) IGroupContact team,
                                                        @Mocked(allowCaching = false) IGroupContact userTeam)
    {
        configureGroupContact(team, teamsContact, "uid1");
        configureGroupContact(userTeam, userGroupsContact, "uid2");

        assertThat(teamsManager.isManaged(team)).isTrue();
        assertThat(teamsManager.isManaged(userTeam)).isFalse();
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenIsOtherTeam_correctResultReturned(@Mocked(allowCaching = false) IGroupContact otherTeam,
                                                      @Mocked(allowCaching = false) IGroupContact clientTeam,
                                                      @Mocked(allowCaching = false) IGroupContact userGroup)
    {
        configureGroupContact(otherTeam, teamsContact, "Blue");
        configureGroupContact(clientTeam, teamsContact, CLIENT_TEAM);
        configureGroupContact(userGroup, userGroupsContact, "uid");

        assertThat(teamsManager.isManagedAndClientNotMember(otherTeam)).isTrue();
        assertThat(teamsManager.isManagedAndClientNotMember(clientTeam)).isFalse();
        assertThat(teamsManager.isManagedAndClientNotMember(userGroup)).isFalse();
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactAdded_managersUpdated(@Mocked(allowCaching = false) IIndividualContact teamMember1,
                                                 @Mocked(allowCaching = false) IIndividualContact teamMember2,
                                                 @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String team = "Blue";

        configureIndividualContact(teamMember1, attributeSet, team);
        configureIndividualContact(teamMember2, attributeSet, team);

        teamsManager.contactAdded(teamMember1);
        teamsManager.contactAdded(teamMember2);

        // Team contact created (real contactService would trigger groupContactManager.contactAdded)
        verify(contactService, times(1)).addContact(any(IContact.class));

        // Team contact updated
        verify(groupContactManager, times(1)).contactUpdated(any(IGroupContact.class));

        // Teams added to manager
        assertThat(teamsManager.findContactGroup(teamMember1)).isEqualTo(team);
        assertThat(teamsManager.findContactGroup(teamMember2)).isEqualTo(team);
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactUpdated_teamChangeApplied(@Mocked(allowCaching = false) IIndividualContact contact,
                                                     @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String firstTeam = "Red";
        final String secondTeam = "Blue";

        // Add contact with first team
        configureIndividualContact(contact, attributeSet, firstTeam);
        teamsManager.contactAdded(contact);
        assertThat(teamsManager.findContactGroup(contact)).isEqualTo(firstTeam);

        // Change teams and update
        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.TEAM, "")).thenReturn(secondTeam);
        teamsManager.contactUpdated(contact);
        assertThat(teamsManager.findContactGroup(contact)).isEqualTo(secondTeam);

        verify(contactService, times(1)).removeContact(anyString());
        verify(contactService, times(2)).addContact(any(IContact.class));
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactsDeleted_managerAndPersistenceUpdated(@Mocked(allowCaching = false) IIndividualContact contact1,
                                                                 @Mocked(allowCaching = false) IIndividualContact contact2,
                                                                 @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String team = "Red";

        configureIndividualContact(contact1, attributeSet, team);
        configureIndividualContact(contact2, attributeSet, team);

        // add contacts1 & contact2
        teamsManager.contactAdded(contact1);
        teamsManager.contactAdded(contact2);

        // remove contact1
        teamsManager.contactRemoved(contact1);

        // confirm that contact1 is gone
        assertThat(teamsManager.findContactGroup(contact1)).isNull();
        assertThat(teamsManager.findContactGroup(contact2)).isEqualTo(team);

        teamsManager.contactRemoved(contact2);

        assertThat(teamsManager.findContactGroup(contact2)).isNull();

        // team group contact added, persist skipped due to mock
        verify(contactService, times(1)).addContact(any(IContact.class));

        // team group contact members updated when contact2 added, contact1 removed
        verify(groupContactManager, times(2)).contactUpdated(any(IContact.class));

        // team group contact removed when last member, contact2, removed, persist skipped due to mock
        verify(contactService, times(1)).removeContact(anyString());
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenGetIcon_expectedValueReturned(@Mocked(allowCaching = false) IGroupContact groupContact,
                                                  @Mocked(allowCaching = false) IGroupContact teamContact)
    {
        // Icon should not be returned for non-team groups
        when(groupContact.getParentContact()).thenReturn(userGroupsContact);
        when(groupContact.getUid()).thenReturn("Red");
        assertThat(teamsManager.getIcon(groupContact)).isNull();

        // Icon should be returned for team groups
        when(teamContact.getParentContact()).thenReturn(teamsContact);
        when(teamContact.getUid()).thenReturn("Red");
        assertThat(teamsManager.getIcon(teamContact)).isNotNull();
    }

    private void configureGroupContact(@Nonnull IGroupContact contact, @Nonnull IGroupContact parent, @Nonnull String uid)
    {
        when(contact.getParentContact()).thenReturn(parent);
        when(contact.getUid()).thenReturn(uid);
    }

    private void configureIndividualContact(@Nonnull IIndividualContact contact, @Nonnull AttributeSet attributeSet, @Nonnull String team)
    {
        when(contact.getAttributes()).thenReturn(attributeSet);
        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.TEAM, "")).thenReturn(team);
        when(contact.getUid()).thenReturn(UUID.randomUUID().toString());
    }
}