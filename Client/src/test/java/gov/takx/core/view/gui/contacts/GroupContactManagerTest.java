package gov.takx.core.view.gui.contacts;

import gov.tak.api.chat.IChatOperationCallback;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.chat.protocol.geochat.GeoChatConstants;
import gov.tak.core.chat.protocol.geochat.GeoChatRolesManager;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsManager;
import gov.tak.platform.chat.ChatService;
import gov.tak.platform.contact.Contacts;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.chat.ConversationStore;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the {@link gov.takx.core.view.gui.contacts.GroupContactManager}
 *
 * @since 3.5
 */
public class GroupContactManagerTest extends AIntegrationTest
{
    private static final String OWNED_GROUP_CONTACT_UID = "UID1";
    private static final String UNOWNED_GROUP_CONTACT_UID = "UID2";

    private GroupContactManager groupContactManager;

    @Mocked(allowCaching = false)
    private IGroupContact ownedGroupContact;

    @Mocked(allowCaching = false)
    private IGroupContact unownedGroupContact;

    @Mocked
    private IGroupContactPersist groupContactPersist;

    @Mocked
    private IContactService contactService;

    @Mocked
    private ConversationStore conversationStore;

    @Mocked
    private ChatService chatService;

    @Mocked
    private GeoChatTeamsManager teamsManager;

    @Mocked
    private GeoChatRolesManager rolesManager;

    @Mocked
    private GeoChatRootGroupContact rootGroupContact;

    @BeforeEach
    void beforeEach()
    {
        when(chatService.getDefaultProtocol()).thenReturn(GeoChatConstants.GEOCHAT_PROTOCOL);
        when(rootGroupContact.getUid()).thenReturn(GeoChatRootGroupContact.GROUPS_ROOT_UID);
        when(rootGroupContact.getParentContact()).thenReturn(null);

        configureContact(ownedGroupContact, OWNED_GROUP_CONTACT_UID, true);
        configureContact(unownedGroupContact, UNOWNED_GROUP_CONTACT_UID, false);

        groupContactManager = new GroupContactManager(groupContactPersist, getBean(ExecutorService.class),
                contactService, conversationStore, chatService, teamsManager, rolesManager);
        groupContactManager.contactAdded(rootGroupContact);
    }

    @Test
    public void whenCreateGroupCalled_chatServiceCreateGroupCalled()
    {
        final String groupName = "New Group";
        final Set<IContact> members = Set.of(ownedGroupContact, unownedGroupContact);

        groupContactManager.createGroup(groupName, members, null);

        verify(chatService)
                .createGroup(eq(groupName), eq(members), eq(null), eq(GeoChatConstants.GEOCHAT_PROTOCOL),
                        any(IChatOperationCallback.class));
    }

    @Test
    public void whenUpdateGroupCalled_chatServiceUpdateGroupCalled()
    {
        final Set<IContact> members = Set.of(ownedGroupContact, unownedGroupContact);

        groupContactManager.updateGroup(ownedGroupContact, members, null);

        verify(chatService).updateGroup(eq(ownedGroupContact), eq(members), isNull());
    }

    @Test
    public void whenDeleteGroupCalled_withOwnedContact_chatServiceDeleteGroupCalled()
    {
        // Get the contact in the owned groups set
        groupContactManager.contactAdded(ownedGroupContact);

        groupContactManager.deleteGroup(ownedGroupContact, null);

        verify(chatService).deleteGroup(eq(ownedGroupContact), isNull());
    }

    @Test
    public void whenDeleteGroupCalled_withUnownedContact_contactServiceRemoveContactCalled()
    {
        groupContactManager.deleteGroup(unownedGroupContact, null);

        verify(contactService).removeContact(unownedGroupContact.getUid());
    }

    @Test
    public void whenContactAddedCalled_contactPersistedAndListenersNotified(@Mocked IGroupContactManagerListener listener)
    {
        clearInvocations(groupContactPersist);

        groupContactManager.addListener(listener);
        groupContactManager.contactAdded(ownedGroupContact);

        verify(groupContactPersist).persistGroupContact(ownedGroupContact);
        verify(listener).contactAdded(ownedGroupContact);
    }

    @Test
    public void whenContactAddedCalled_ownedGroupsUpdated()
    {
        assertThat(groupContactManager.hasUserGroups()).isFalse();

        groupContactManager.contactAdded(ownedGroupContact);
        groupContactManager.contactAdded(unownedGroupContact);

        assertThat(groupContactManager.ownsGroup(ownedGroupContact)).isTrue();
        assertThat(groupContactManager.ownsGroup(unownedGroupContact)).isFalse();
        assertThat(groupContactManager.hasUserGroups()).isTrue();
    }

    @Test
    public void whenContactUpdatedCalled_contactPersistedAndListenersNotified(@Mocked IGroupContactManagerListener listener)
    {
        groupContactManager.contactAdded(ownedGroupContact);
        clearInvocations(groupContactPersist);

        groupContactManager.addListener(listener);
        groupContactManager.contactUpdated(ownedGroupContact);

        verify(conversationStore).updateConversationForContact(ownedGroupContact);
        verify(groupContactPersist).persistGroupContact(ownedGroupContact);
        verify(listener).contactUpdated(ownedGroupContact);
    }

    @Test
    public void whenContactRemovedCalled_contactPersistedAndListenersNotified(@Mocked IGroupContactManagerListener listener)
    {
        groupContactManager.contactAdded(ownedGroupContact);
        clearInvocations(groupContactPersist);

        groupContactManager.addListener(listener);
        groupContactManager.contactRemoved(ownedGroupContact);

        verify(groupContactPersist).deleteGroupContact(ownedGroupContact.getUid());
        verify(listener).contactRemoved(ownedGroupContact);
    }

    @Test
    public void whenGetRootGroupContactCalled_rootGroupContactReturned()
    {
        assertThat(groupContactManager.getRootGroupContact()).isEqualTo(rootGroupContact);
    }

    @Test
    public void whenContactRemoved_hierarchyIsCleaned(@Mocked(allowCaching = false) IGroupContact child,
                                                      @Mocked(allowCaching = false) IGroupContact grandchild,
                                                      @Mocked(allowCaching = false) IGroupContact greatGrandchild,
                                                      @Mocked IGroupContactManagerListener listener)
    {
        configureContact(child, "1", false);
        configureContact(grandchild, "1B", false);
        configureContact(greatGrandchild, "1C", false);

        when(child.getParentContact()).thenReturn(rootGroupContact);
        when(grandchild.getParentContact()).thenReturn(child);
        when(greatGrandchild.getParentContact()).thenReturn(grandchild);

        when(child.getGroupMembers()).thenReturn(Set.of(grandchild));
        when(grandchild.getGroupMembers()).thenReturn(Set.of(greatGrandchild));

        groupContactManager.addListener(listener);

        groupContactManager.contactAdded(child);
        groupContactManager.contactAdded(grandchild);
        groupContactManager.contactAdded(greatGrandchild);

        groupContactManager.contactRemoved(child);

        assertThat(groupContactManager.getGroupContacts()).hasSize(0);

        verify(groupContactPersist).deleteGroupContact(child.getUid());
        verify(groupContactPersist).deleteGroupContact(grandchild.getUid());
        verify(groupContactPersist).deleteGroupContact(greatGrandchild.getUid());

        verify(listener).contactRemoved(child);
        verify(listener).contactRemoved(grandchild);
        verify(listener).contactRemoved(greatGrandchild);

    }

    private void configureContact(@Nonnull IGroupContact mockedGroupContact, @Nonnull String uid, boolean owned)
    {
        when(mockedGroupContact.getAttributes()).thenReturn(getAttributeSet(owned));
        when(mockedGroupContact.getUid()).thenReturn(uid);
        when(mockedGroupContact.getParentContact()).thenReturn(rootGroupContact);
    }

    private AttributeSet getAttributeSet(boolean owned)
    {
        final AttributeSet attributeSet = new AttributeSet();
        attributeSet.setAttribute(GeoChatConstants.CLIENT_OWNED_GROUP_ATTRIBUTE, owned);
        attributeSet.setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, new String[]{GeoChatConstants.GEOCHAT_PROTOCOL});
        return attributeSet;
    }
}