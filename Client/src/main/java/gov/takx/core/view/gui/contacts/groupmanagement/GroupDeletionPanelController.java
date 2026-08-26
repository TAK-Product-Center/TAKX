package gov.takx.core.view.gui.contacts.groupmanagement;

import gov.tak.api.contact.IGroupContact;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.lists.GroupContactCheckboxCellRenderer;
import gov.takx.core.view.gui.contacts.lists.GroupContactTreeController;
import gov.takx.core.view.gui.contacts.lists.GroupContactTreeNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;

import java.util.Iterator;
import java.util.List;

/**
 * Controller for the group deletion panel.
 *
 * @since 3.0
 */
@ApplicationScoped
public class GroupDeletionPanelController implements IResourceCleaner
{
    private final ContactsPanelController contactsPanelController;
    private final GroupContactManager groupContactManager;

    private GroupContactTreeController treeController;
    @Nullable
    private IGroupContact parent;
    private GroupDeletionPanel groupPanel;

    @Inject
    public GroupDeletionPanelController(ContactsPanelController contactsPanelController,
                                        GroupContactManager groupContactManager)
    {
        this.contactsPanelController = contactsPanelController;
        this.groupContactManager = groupContactManager;
    }

    /**
     * @param groupPanel The group panel to set on this controller
     */
    public void setPanel(@Nonnull GroupDeletionPanel groupPanel)
    {
        this.groupPanel = groupPanel;

        final JTree tree = groupPanel.getTree();
        tree.setModel(null); // prevents rendering errors until real model is set
        tree.setCellRenderer(new GroupContactCheckboxCellRenderer());
    }

    /**
     * @param parent The parent {@link IGroupContact} if deleting subgroups, or {@code null} if deleting top-level groups
     * @since 3.5
     */
    public void prepare(@Nullable IGroupContact parent)
    {
        this.parent = parent;

        // If the controller was used previously, shut down the previous tree controller
        if (treeController != null) treeController.releaseResources();

        // Create a new tree controller
        final JTree tree = groupPanel.getTree();
        treeController = new GroupContactTreeController(tree, new GroupContactCheckboxCellRenderer(),
                this::selectItem, parent != null ? parent : groupContactManager.getRootGroupContact());

        // Add the current group contacts
        groupContactManager.getGroupContacts().forEach(treeController::contactAdded);
    }

    @Override
    public void releaseResources()
    {
        treeController.releaseResources();
    }

    /**
     * Called when the user clicks "Cancel" on the group panel, informing the contacts panel controller that the main
     * contacts panel should be shown.
     */
    void cancel()
    {
        showPreviousPanel();
    }

    /**
     * Informs the {@link GroupContactManager} that group contacts should be deleted and displays the primary contacts 
     * panel.
     *
     * @param groupContacts The group contacts to delete
     */
    void deleteGroups(@Nonnull List<IGroupContact> groupContacts)
    {
        groupContacts.forEach(groupContact -> groupContactManager.deleteGroup(groupContact, null));
        showPreviousPanel();
    }

    /**
     * Called when a group in the group tree is selected by the user. Sets the checkbox associated with the clicked
     * contact to "selected".
     *
     * @since 3.5
     */
    private void selectItem(@Nonnull GroupContactTreeNode node)
    {
        groupPanel.getTree().clearSelection();

        final boolean selected = !node.isSelected();
        node.setSelected(selected);

        // If selecting a parent, all descendants must also be deleted, so select them
        if (selected)
        {
            node.breadthFirstEnumeration().asIterator()
                    .forEachRemaining(child -> ((GroupContactTreeNode) child).setSelected(true));
        }
        // If deselecting a child, any ancestors cannot be deleted, so deselect them
        else
        {
            GroupContactTreeNode parent = (GroupContactTreeNode) node.getParent();

            while (parent != null)
            {
                parent.setSelected(false);
                parent = (GroupContactTreeNode) parent.getParent();
            }
        }

        groupPanel.setOkayButtonEnabled(isGroupSelected());
        groupPanel.repaint();
    }

    /**
     * @return boolean indicating if any nodes are currently selected
     */
    private boolean isGroupSelected()
    {
        final Iterator<TreeNode> nodes = treeController.getRootNode().breadthFirstEnumeration().asIterator();
        while (nodes.hasNext()) if (((GroupContactTreeNode) nodes.next()).isSelected()) return true;

        return false;
    }

    /**
     * Display the previous panel.
     *
     * @since 3.5
     */
    private void showPreviousPanel()
    {
        if (parent == null)
        {
            contactsPanelController.showPrimaryContactsPanel();
        } else
        {
            contactsPanelController.onSelect(parent);
        }
    }
}
