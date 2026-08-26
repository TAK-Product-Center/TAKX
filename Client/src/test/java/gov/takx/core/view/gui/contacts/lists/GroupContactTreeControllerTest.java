package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.chat.protocol.geochat.GeoChatConstants;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.platform.contact.Contacts;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * Test the {@link GroupContactTreeController}
 *
 * @since 3.5
 */
public class GroupContactTreeControllerTest extends AIntegrationTest
{
    @Mocked
    private TreeCellRenderer editorRenderer;
    @Mocked
    private IIndividualContact individualContact;
    @Mocked
    private IContactSelectionListener<GroupContactTreeNode> listener;

    private JTree tree;
    private IGroupContact rootContact;
    private GroupContactTreeController controller;

    @BeforeEach
    public void beforeEach()
    {
        tree = new JTree();
        rootContact = new GeoChatRootGroupContact("clientUid", "clientName");
        controller = new GroupContactTreeController(tree, editorRenderer, listener);

        controller.contactAdded(rootContact);
        SwingTestUtils.waitForEdtEvents();
    }

    @AfterEach
    public void afterEach()
    {
        controller.releaseResources();
    }

    @Test
    public void treeInitializedCorrectly()
    {
        final GroupContactTreeNode root = controller.getRootNode();
        final TreeModel treeModel = tree.getModel();

        assertThat(root).isNotNull();
        assertThat(root.getContact()).isNull();
        assertThat(controller.getModel()).isEqualTo(treeModel);
        assertThat(controller.getRootNode()).isEqualTo(treeModel.getRoot());
    }

    @Test
    public void contactAdded_contactNodesAdded()
    {
        final IGroupContact groupContact1 = createGroupContact("1", "1", rootContact);
        final IGroupContact groupContact2 = createGroupContact("2", "2", rootContact);

        controller.contactAdded(groupContact1);
        controller.contactAdded(groupContact2);

        SwingTestUtils.waitForEdtEvents();

        GroupContactTreeNode groupContactTreeNode = controller.searchTreeForContact(groupContact1.getUid());
        assertThat(groupContactTreeNode).isNotNull();
        IGroupContact treeNodeContact = groupContactTreeNode.getContact();
        assertThat(treeNodeContact).isNotNull();
        assertThat(treeNodeContact).isEqualTo(groupContact1);

        groupContactTreeNode = controller.searchTreeForContact(groupContact2.getUid());
        assertThat(groupContactTreeNode).isNotNull();
        treeNodeContact = groupContactTreeNode.getContact();
        assertThat(treeNodeContact).isNotNull();
        assertThat(treeNodeContact).isEqualTo(groupContact2);
    }

    @Test
    public void contactAdded_whenContactExists_contactNodesNotAdded()
    {
        final IGroupContact groupContact1 = createGroupContact("1", "1", rootContact);
        final IGroupContact groupContact1dup = createGroupContact("1", "1", rootContact);

        final TreeNode parent = controller.searchTreeForContact(rootContact.getUid());
        final DefaultTreeModel model = controller.getModel();

        controller.contactAdded(groupContact1);
        SwingTestUtils.waitForEdtEvents();

        // The count should not change after we attempt to add a duplicate below
        final int expectedSize = model.getChildCount(parent);

        controller.contactAdded(groupContact1dup);
        SwingTestUtils.waitForEdtEvents();

        assertThat(model.getChildCount(parent)).isEqualTo(expectedSize);
    }

    @Test
    public void contactUpdated_contactNodeUpdated()
    {
        IGroupContact groupContact = createGroupContact("contact", "contact name 1", rootContact);
        controller.contactAdded(groupContact);

        groupContact = createGroupContact(groupContact.getUid(), "contact name 2", rootContact);
        controller.contactUpdated(groupContact);

        SwingTestUtils.waitForEdtEvents();

        final GroupContactTreeNode groupContactTreeNode = controller.searchTreeForContact(groupContact.getUid());
        final IGroupContact nodeGroupContact = groupContactTreeNode.getContact();
        assertThat(nodeGroupContact).isNotNull();
        assertThat(nodeGroupContact.getName()).isEqualTo(groupContact.getName());
    }

    @Test
    public void contactRemoved_contactNodesRemoved()
    {
        final IGroupContact groupContact1 = createGroupContact("1", "1", rootContact);
        final IGroupContact groupContact2 = createGroupContact("2", "2", rootContact);
        final IGroupContact groupContact3 = createGroupContact("3", "3", rootContact);

        controller.contactAdded(groupContact1);
        controller.contactAdded(groupContact2);
        controller.contactAdded(groupContact3);

        controller.contactRemoved(groupContact2);

        SwingTestUtils.waitForEdtEvents();

        GroupContactTreeNode groupContactTreeNode = controller.searchTreeForContact(groupContact1.getUid());
        assertThat(groupContactTreeNode).isNotNull();
        groupContactTreeNode = controller.searchTreeForContact(groupContact2.getUid());
        assertThat(groupContactTreeNode).isNull();
        groupContactTreeNode = controller.searchTreeForContact(groupContact3.getUid());
        assertThat(groupContactTreeNode).isNotNull();
    }

    @Test
    public void contactSelected_listenerNotified()
    {
        final IGroupContact groupContact1 = createGroupContact("1", "1", rootContact);
        final IGroupContact groupContact2 = createGroupContact("2", "2", rootContact);
        final IGroupContact groupContact3 = createGroupContact("3", "3", rootContact);

        controller.contactAdded(groupContact1);
        controller.contactAdded(groupContact2);
        controller.contactAdded(groupContact3);

        SwingTestUtils.waitForEdtEvents();

        verifySelectionListenerCalled(groupContact1);
        verifySelectionListenerCalled(groupContact2);
        verifySelectionListenerCalled(groupContact3);
    }

    @Test
    public void contactAdded_whenUsingSubtree_ignoresUnrelatedContacts(@Mocked TreeCellRenderer editorRenderer)
    {
        final IGroupContact subtreeRootContact = createGroupContact("0", "0", rootContact);
        final IGroupContact subtreeDescendant1 = createGroupContact("1", "1", subtreeRootContact);
        final IGroupContact subtreeDescendant2 = createGroupContact("2", "2", subtreeDescendant1);
        final IGroupContact otherContact = createGroupContact("other", "other", rootContact);

        final JTree tree = new JTree();
        final GroupContactTreeController subtreeController = new GroupContactTreeController(tree, editorRenderer, listener, subtreeRootContact);

        subtreeController.contactAdded(subtreeDescendant1);
        subtreeController.contactAdded(subtreeDescendant2);
        subtreeController.contactAdded(otherContact);

        SwingTestUtils.waitForEdtEvents();

        GroupContactTreeNode groupContactTreeNode = subtreeController.searchTreeForContact(subtreeDescendant1.getUid());
        assertThat(groupContactTreeNode).isNotNull();
        IGroupContact treeNodeContact = groupContactTreeNode.getContact();
        assertThat(treeNodeContact).isNotNull();
        assertThat(treeNodeContact).isEqualTo(subtreeDescendant1);

        groupContactTreeNode = subtreeController.searchTreeForContact(subtreeDescendant2.getUid());
        assertThat(groupContactTreeNode).isNotNull();
        treeNodeContact = groupContactTreeNode.getContact();
        assertThat(treeNodeContact).isNotNull();
        assertThat(treeNodeContact).isEqualTo(subtreeDescendant2);

        groupContactTreeNode = subtreeController.searchTreeForContact(otherContact.getUid());
        assertThat(groupContactTreeNode).isNull();
    }

    /**
     * Finds the specified contact, selects it, clears the selection and then verifies the
     * {@link IContactSelectionListener} was called.
     *
     * @param contact The contact to select
     */
    private void verifySelectionListenerCalled(@Nonnull IContact contact)
    {
        //noinspection unchecked
        clearInvocations(listener);

        final TreeSelectionModel selectionModel = tree.getSelectionModel();

        final GroupContactTreeNode node = controller.searchTreeForContact(contact.getUid());
        selectionModel.setSelectionPath(new TreePath(node.getPath()));
        selectionModel.clearSelection();

        verify(listener).onSelect(node);
    }

    private IGroupContact createGroupContact(String uid, String name, IGroupContact parent)
    {
        final IGroupContact contact = new GroupContactBuilder()
                .withUniqueContactId(uid)
                .withDisplayName(name)
                .withParentContact(parent)
                .withGroupMembers(Set.of(individualContact))
                .build();

        contact.getAttributes()
                .setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, new String[]{GeoChatConstants.GEOCHAT_PROTOCOL});

        return contact;
    }
}