package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.contact.IndividualContactState;
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
 * Tests the {@link GeoChatRolesManager}
 *
 * @since 3.5
 */
public class GeoChatRolesManagerTest extends AIntegrationTest
{
    private static final String CLIENT_ROLE = "K9";

    @Mocked
    private GroupContactManager groupContactManager;

    @Mocked
    private IndividualContactManager individualContactManager;

    @Mocked
    private IContactService contactService;

    @Mocked
    private IGroupContactPersist groupContactPersist;

    private GeoChatRolesManager rolesManager;

    @BeforeEach
    void beforeEach()
    {
        when(individualContactManager.getClientContactRole()).thenReturn(CLIENT_ROLE);
        when(groupContactPersist.findGroupContact(anyString())).thenReturn(null); // we don't want a mock

        clearInvocations(groupContactManager);
        clearInvocations(contactService);

        rolesManager = new GeoChatRolesManager(groupContactManager, contactService, groupContactPersist, individualContactManager);
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactAdded_managersUpdated(@Mocked(allowCaching = false) IIndividualContact roleMember1,
                                                 @Mocked(allowCaching = false) IIndividualContact roleMember2,
                                                 @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String role = "K9";

        configureIndividualContact(roleMember1, attributeSet, role);
        configureIndividualContact(roleMember2, attributeSet, role);

        rolesManager.contactAdded(roleMember1);
        rolesManager.contactAdded(roleMember2);

        // Role contact created (real contactService would trigger groupContactManager.contactAdded)
        verify(contactService, times(1)).addContact(any(IContact.class));

        // Role contact updated
        verify(groupContactManager, times(1)).contactUpdated(any(IGroupContact.class));

        // Role added to manager
        assertThat(rolesManager.findContactGroup(roleMember1)).isEqualTo(role);
        assertThat(rolesManager.findContactGroup(roleMember2)).isEqualTo(role);
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactUpdated_teamChangeApplied(@Mocked IIndividualContact contact,
                                                     @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String firstRole = "K9";
        final String secondRole = "HQ";

        // Add contact with first team
        configureIndividualContact(contact, attributeSet, firstRole);
        rolesManager.contactAdded(contact);
        assertThat(rolesManager.findContactGroup(contact)).isEqualTo(firstRole);

        // Change teams and update
        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.ROLE, "")).thenReturn(secondRole);
        rolesManager.contactUpdated(contact);
        assertThat(rolesManager.findContactGroup(contact)).isEqualTo(secondRole);

        verify(contactService, times(1)).removeContact(anyString());
        verify(contactService, times(2)).addContact(any(IContact.class));
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactsDeleted_managerAndPersistenceUpdated(@Mocked(allowCaching = false) IIndividualContact contact1,
                                                                 @Mocked(allowCaching = false) IIndividualContact contact2,
                                                                 @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String role = "Medic";

        configureIndividualContact(contact1, attributeSet, role);
        configureIndividualContact(contact2, attributeSet, role);

        // add contacts1 & contact2
        rolesManager.contactAdded(contact1);
        rolesManager.contactAdded(contact2);

        // remove contact1
        rolesManager.contactRemoved(contact1);

        // confirm that contact1 is gone
        assertThat(rolesManager.findContactGroup(contact1)).isNull();
        assertThat(rolesManager.findContactGroup(contact2)).isEqualTo(role);

        rolesManager.contactRemoved(contact2);

        assertThat(rolesManager.findContactGroup(contact2)).isNull();

        // role group contact added, persist skipped due to mock
        verify(contactService, times(1)).addContact(any(IContact.class));

        // role group contact members updated when contact2 added, contact1 removed
        verify(groupContactManager, times(2)).contactUpdated(any(IContact.class));

        // role group contact removed when last member, contact2, removed, persist skipped due to mock
        verify(contactService, times(1)).removeContact(anyString());
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenGetIcon_expectedValueReturned(@Mocked(allowCaching = false) IGroupContact groupContact,
                                                  @Mocked(allowCaching = false) IGroupContact roleContact)
    {
        // Icon should not be returned for non-role groups
        when(groupContact.getParentContact()).thenReturn(null);
        when(groupContact.getName()).thenReturn("name");
        when(groupContact.getUid()).thenReturn("uid");
        assertThat(rolesManager.getIcon(groupContact)).isNull();

        final String role = "K9";

        // Icon should be returned for role groups
        when(roleContact.getParentContact()).thenReturn(null);
        when(roleContact.getName()).thenReturn(role);
        when(roleContact.getUid()).thenReturn(role);
        assertThat(rolesManager.getIcon(roleContact)).isNotNull();
    }

    @Test
    public void whenIsManagedGroupType_correctResultReturned()
    {
        assertThat(GeoChatRolesManager.isManagedRole("Team Member")).isFalse();
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenIsManagedGroupContact_correctResultReturned(@Mocked(allowCaching = false) IGroupContact groupContact,
                                                                @Mocked(allowCaching = false) IGroupContact roleContact)
    {
        when(groupContact.getParentContact()).thenReturn(null);
        when(groupContact.getName()).thenReturn("name");
        when(groupContact.getUid()).thenReturn("uid");
        assertThat(rolesManager.isManaged(groupContact)).isFalse();

        final String role =  "K9";

        when(roleContact.getParentContact()).thenReturn(null);
        when(roleContact.getName()).thenReturn(role);
        when(roleContact.getUid()).thenReturn(role);
        assertThat(rolesManager.isManaged(roleContact)).isTrue();
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenIsClientExcluded_correctResultReturned(@Mocked(allowCaching = false) IGroupContact roleContact1,
                                                           @Mocked(allowCaching = false) IGroupContact roleContact2,
                                                           @Mocked(allowCaching = false) IGroupContact groupContact)
    {
        final String clientRole = "HQ";
        final String otherRole = "K9";

        when(individualContactManager.getClientContactRole()).thenReturn(clientRole);

        when(roleContact1.getParentContact()).thenReturn(null);
        when(roleContact1.getName()).thenReturn(otherRole);
        when(roleContact1.getUid()).thenReturn(otherRole);

        when(roleContact2.getParentContact()).thenReturn(null);
        when(roleContact2.getName()).thenReturn(clientRole);
        when(roleContact2.getUid()).thenReturn(clientRole);

        assertThat(rolesManager.isManagedAndClientNotMember(roleContact1)).isTrue();
        assertThat(rolesManager.isManagedAndClientNotMember(roleContact2)).isFalse();
        assertThat(rolesManager.isManagedAndClientNotMember(groupContact)).isFalse();
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // parameters okay
    @Test
    public void whenContactStateChanged_contactRemovedAndAdded(@Mocked(allowCaching = false) IIndividualContact contact,
                                                               @Mocked(allowCaching = false) AttributeSet attributeSet)
    {
        final String role = "K9";

        configureIndividualContact(contact, attributeSet, role);
        when(contact.getAttributes()).thenReturn(attributeSet);

        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.STATE, "")).thenReturn(IndividualContactState.active.name());
        rolesManager.contactAdded(contact);
        assertThat(rolesManager.findContactGroup(contact)).isEqualTo(role);
        // role group contact added, persist skipped due to mock
        verify(contactService, times(1)).addContact(any(IContact.class));

        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.STATE, "")).thenReturn(IndividualContactState.gone.name());
        rolesManager.contactStateChanged(contact);
        assertThat(rolesManager.findContactGroup(contact)).isNull();
        // role group contact added, persist skipped due to mock
        verify(contactService, times(1)).removeContact(anyString());

        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.STATE, "")).thenReturn(IndividualContactState.active.name());
        rolesManager.contactStateChanged(contact);
        assertThat(rolesManager.findContactGroup(contact)).isEqualTo(role);
        verify(contactService, times(2)).addContact(any(IContact.class));
    }

    private void configureIndividualContact(@Nonnull IIndividualContact contact, @Nonnull AttributeSet attributeSet,
                                            @Nonnull String role)
    {
        when(attributeSet.getStringAttribute(IndividualContactAttributeNames.ROLE, "")).thenReturn(role);
        when(contact.getAttributes()).thenReturn(attributeSet);
        when(contact.getUid()).thenReturn(UUID.randomUUID().toString());
    }
}