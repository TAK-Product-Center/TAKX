package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IGroupContact;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.chat.protocol.geochat.GeoChatAllChatRoomsContact;
import gov.tak.core.chat.protocol.geochat.GeoChatRolesManager;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsManager;
import gov.takx.api.timer.ITimerService;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import java.util.ArrayList;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ContactsPanelGroupContactTreeController}
 * 
 * @since 3.4.1
 */
public class ContactsPanelGroupContactTreeControllerTest extends AIntegrationTest
{
    @Mocked
    private ContactsPanelController contactsPanelController;
    @Mocked
    private GroupContactManager groupContactManager;
    @Mocked
    private GeoChatTeamsManager teamsManager;
    @Mocked
    private GeoChatRolesManager rolesManager;
    @Mocked
    private ITimerService timerService;

    private static final GeoChatAllChatRoomsContact allChatRoomsContact = new GeoChatAllChatRoomsContact("clientUid", "clientName");
    private static final GeoChatRootGroupContact rootGroupContact = new GeoChatRootGroupContact("clientUid", "clientName");
    private static final GeoChatTeamsContact teamsContact = new GeoChatTeamsContact();

    private ContactsPanelGroupContactTreeController controller;

    @BeforeEach
    public void beforeEach()
    {
        when(groupContactManager.getAllChatRoomsContact()).thenReturn(allChatRoomsContact);
        when(groupContactManager.getRootGroupContact()).thenReturn(rootGroupContact);
        when(groupContactManager.getTeamsContact()).thenReturn(teamsContact);
        when(groupContactManager.getGroupContacts()).thenReturn(new ArrayList<>());

        controller = new ContactsPanelGroupContactTreeController(
                contactsPanelController, timerService, groupContactManager, teamsManager, rolesManager);
    }

    @Test
    public void treeModelInitializedCorrectly()
    {
        final JTree tree = new JTree();

        controller.onComponentSet(tree);

        SwingTestUtils.waitForEdtEvents();

        assertThat(tree.getCellRenderer())
                .isInstanceOf(ContactsPanelGroupContactTreeController.GroupContactTreeCellRenderer.class);

        assertThat(findContactNode(tree, allChatRoomsContact)).isNotNull();
        assertThat(findContactNode(tree, rootGroupContact)).isNotNull();
        assertThat(findContactNode(tree, teamsContact)).isNotNull();

        verify(groupContactManager).getGroupContacts();
    }

    private GroupContactTreeNode findContactNode(@Nonnull JTree tree, @Nonnull IGroupContact contact)
    {
        final GroupContactTreeNode root = (GroupContactTreeNode) tree.getModel().getRoot();
        final Iterator<TreeNode> iterator = root.breadthFirstEnumeration().asIterator();

        while (iterator.hasNext())
        {
            final GroupContactTreeNode node = (GroupContactTreeNode) iterator.next();
            final IGroupContact nodeContact = node.getContact();

            if (nodeContact != null && nodeContact.getUid().equals(contact.getUid())) return node;
        }

        return null;
    }
}