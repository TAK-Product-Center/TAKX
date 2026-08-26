package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.lists.GroupContactCheckboxCellRenderer;
import gov.takx.core.view.gui.contacts.lists.GroupContactTreeController;
import gov.takx.core.view.gui.contacts.lists.GroupContactTreeNode;
import gov.takx.core.view.gui.contacts.lists.ImprovedSelectionJList;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckBoxCellRenderer;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import gov.takx.core.view.gui.contacts.lists.IndividualContactListController;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.utils.ContactUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import java.awt.Dimension;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Abstract class for dialogs to collect Contacts and send content to them
 *
 * @since 5.3
 */
@PseudoBean
public abstract class ASendAsDialog extends AStandardDialog
{
    private static final Border EMPTY_BORDER = new EmptyBorder(5, 5, 5, 5);

    @Inject
    private GroupContactManager groupContactManager;

    @Inject
    private ContactUtils contactUtils;

    private GroupContactTreeController treeController;
    private IndividualContactListController<IndividualContactCheckListItem> listController;

    /**
     * @param title String to set as the dialog title
     */
    protected ASendAsDialog(@Nonnull String title)
    {
        super(title);
        setTitle(title);
        initComponents();

    }

    /**
     * Hides the broadcast button. For use in dialogs where broadcasting is not a supported option.
     *
     * @since 5.4
     */
    protected void hideBroadcast()
    {
        broadcastButton.setVisible(false);
    }

    @PostConstruct
    private void postConstruct()
    {
        ((JPanel) getContentPane()).setBorder(EMPTY_BORDER);
        groupsTree.setShowsRootHandles(true); // The JFormDesigner designer setting doesn't work
        // group tree controller
        treeController = new GroupContactTreeController(groupsTree, new GroupContactCheckboxCellRenderer(),
                treeNode -> {
                    toggleGroupContact(treeNode);
                    sendButton.setEnabled(isAnyRecipientSelected());
                });

        // set renderer after treeController sets model
        groupsTree.setCellRenderer(new GroupContactCheckboxCellRenderer());

        // add the groups we care about; top-level nodes should never be null at this point
        //noinspection DataFlowIssue
        treeController.contactAdded(groupContactManager.getRootGroupContact());
        treeController.contactAdded(groupContactManager.getTeamsContact());
        groupContactManager.getRoleContacts().forEach(treeController::contactAdded);
        groupContactManager.getGroupContacts().forEach(treeController::contactAdded);

        // individual list controller (adds all individual contacts to model by default)
        listController = new IndividualContactListController<>(individualList, IndividualContactCheckListItem::new,
                IndividualContactCheckListItem::getContact, entry -> {
                    entry.setSelected(!entry.isSelected());
                    sendButton.setEnabled(isAnyRecipientSelected());
                });

        // set renderer
        individualList.setCellRenderer(new IndividualContactCheckBoxCellRenderer());

        revalidate();
        repaint();
    }

    /**
     * Sends the content to the selected contacts.
     */
    protected abstract void send();

    /**
     * Broadcast the content to all connected contacts.
     */
    protected abstract void broadcast();

    /**
     * Cancels the send request.
     */
    private void cancel()
    {
        dispose();
    }

    @Override
    protected void onWindowClosing()
    {
        treeController.releaseResources();
        listController.releaseResources();
    }

    /**
     * Toggles a {@link GroupContactTreeNode}'s checkbox. If a top-level node is toggled, all of its descendants will
     * be updated to match its state. If a descendant node is deselected, its top-level node will also be deselected.
     *
     * @param node The node to toggle
     */
    private void toggleGroupContact(GroupContactTreeNode node)
    {
        final boolean selected = !node.isSelected();

        node.setSelected(selected);

        // top-level nodes toggle all of their children
        if (node.getParent() == treeController.getRootNode())
        {
            final DefaultTreeModel model = (DefaultTreeModel) groupsTree.getModel();
            final Iterator<TreeNode> children = node.breadthFirstEnumeration().asIterator();

            while (children.hasNext())
            {
                final GroupContactTreeNode child = (GroupContactTreeNode) children.next();
                child.setSelected(selected);
                model.nodeChanged(child);
            }
        }
        // make sure top-level node is not selected if a descendant is deselected
        else if (!selected)
        {
            final TreeNode rootNode = treeController.getRootNode();
            TreeNode parent = node.getParent();

            while (parent.getParent() != rootNode) parent = parent.getParent();

            ((GroupContactTreeNode) parent).setSelected(false);
        }
    }

    /**
     * Checks the {@link IGroupContact} tree and {@link IIndividualContact} list to see if any contacts are selected.
     *
     * @return boolean indicating if any nodes are currently selected
     */
    private boolean isAnyRecipientSelected()
    {
        final var nodes = treeController.getRootNode().breadthFirstEnumeration().asIterator();
        while (nodes.hasNext()) if (((GroupContactTreeNode) nodes.next()).isSelected()) return true;

        final var entries = listController.getModel().elements().asIterator();
        while (entries.hasNext()) if (entries.next().isSelected()) return true;

        return false;
    }

    /**
     * Scans the tree and returns a list of selected {@link IIndividualContact} UUIDs. If a group or team is selected,
     * the UUIDs of any {@link IIndividualContact} members will be added to the results.
     *
     * @return A list of the contacts that are selected.
     */
    @Nonnull
    protected Collection<String> getSelectedContactUids()
    {
        final Set<String> selectedContactUids = new HashSet<>(); // use a Set to prevent duplicates
        final var descendantNodes = treeController.getRootNode().breadthFirstEnumeration();

        while (descendantNodes.hasMoreElements())
        {
            final GroupContactTreeNode contactNode = (GroupContactTreeNode) descendantNodes.nextElement();

            if (contactNode.isSelected())
            {
                final IGroupContact groupContact = contactNode.getContact();

                // Only include members that are individual contacts and not the local client
                // Only the invisible root node has a null contact, so groupContact should never be null
                //noinspection DataFlowIssue
                contactUtils.getGroupMembers(groupContact, false, false)
                        .forEach(c -> selectedContactUids.add(c.getUid()));
            }
        }

        final var entries = listController.getModel().elements().asIterator();

        while (entries.hasNext())
        {
            final IndividualContactCheckListItem entry = entries.next();
            if (entry.isSelected()) selectedContactUids.add(entry.getContact().getUid());
        }

        return selectedContactUids;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var contactsScrollPane = new JScrollPane();
        var contactsPanel = new JPanel();
        groupsTree = new JTree();
        individualList = new ImprovedSelectionJList<>();
        var buttonsPanel = new JPanel();
        sendButton = new JButton();
        broadcastButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(300, 100));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:[200dlu,default]:grow, $lgap, default"));

        //======== contactsScrollPane ========
        {
            contactsScrollPane.setName("contactsScrollPane");

            //======== contactsPanel ========
            {
                contactsPanel.setName("contactsPanel");
                contactsPanel.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default)"));

                //---- groupsTree ----
                groupsTree.setRootVisible(false);
                groupsTree.setName("groupsTree");
                contactsPanel.add(groupsTree, cc.xy(1, 1));

                //---- individualList ----
                individualList.setName("individualList");
                contactsPanel.add(individualList, cc.xy(1, 2));
            }
            contactsScrollPane.setViewportView(contactsPanel);
        }
        contentPane.add(contactsScrollPane, cc.xy(1, 1));

        //======== buttonsPanel ========
        {
            buttonsPanel.setName("buttonsPanel");
            buttonsPanel.setLayout(new FormLayout(
                    "default, $lcgap, default, [1dlu,default]:grow, default",
                    "default"));
            ((FormLayout) buttonsPanel.getLayout()).setColumnGroups(new int[][]{{1, 3, 5}});

            //---- sendButton ----
            sendButton.setText("Send");
            sendButton.setEnabled(false);
            sendButton.setName("sendButton");
            sendButton.addActionListener(e -> send());
            buttonsPanel.add(sendButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- broadcastButton ----
            broadcastButton.setText("Broadcast");
            broadcastButton.setName("broadcastButton");
            broadcastButton.addActionListener(e -> broadcast());
            buttonsPanel.add(broadcastButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancel());
            buttonsPanel.add(cancelButton, cc.xy(5, 1));
        }
        contentPane.add(buttonsPanel, cc.xy(1, 3));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTree groupsTree;
    private ImprovedSelectionJList<IndividualContactCheckListItem> individualList;
    private JButton sendButton;
    private JButton broadcastButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
