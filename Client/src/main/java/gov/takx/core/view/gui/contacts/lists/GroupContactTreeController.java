package gov.takx.core.view.gui.contacts.lists;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.core.chat.protocol.geochat.GeoChatRolesManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.AlphaNumericComparator;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.platform.utils.ContactUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Set;

/**
 * Controller for trees that display {@link IGroupContact}s. The controller listens for {@link GroupContactManager}
 * notifications and updates the tree model when contacts are added, updated and removed.
 * <p>
 * Note: It's up to the creator of this controller to set an appropriate renderer on the JTree.
 *
 * @since 3.5
 */
@PseudoBean
public class GroupContactTreeController implements IGroupContactManagerListener, IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Client property used to identify the Component that displays the contact's icon
     */
    public static final String ICON_LABEL_PROPERTY = "iconLabel";

    private static final AlphaNumericComparator<IGroupContact> CONTACT_COMPARATOR =
            new AlphaNumericComparator<>(IGroupContact::getName);

    private final GroupContactTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final boolean subtree;
    private final TreeModelListener treeModelListener;
    private final GroupTreeSelectionListener treeSelectionListener;

    @Inject
    private GroupContactManager groupContactManager;
    @Inject
    private IndividualContactManager individualContactManager;
    @Inject
    private GeoChatRolesManager rolesManager;
    @Inject
    private ContactUtils contactUtils;

    /**
     * Creates a new controller using a new {@link GroupContactTreeNode} as the tree root.
     *
     * @param tree The tree that will display the {@link GroupContactTreeNode}s
     * @param editorCellRenderer The renderer to use when in editing mode, typically the separate instance of the
     *                           same renderer used when not in editing mode
     * @param selectionListener Listener that will be notified when an entry is selected
     */
    public GroupContactTreeController(@Nonnull JTree tree,
                                      @Nonnull TreeCellRenderer editorCellRenderer,
                                      @Nonnull IContactSelectionListener<GroupContactTreeNode> selectionListener)
    {
        this(tree, editorCellRenderer, selectionListener, null);
    }

    /**
     * Creates a new controller using the specified {@link GroupContactTreeNode} as the tree root.
     *
     * @param tree The tree that will display the {@link GroupContactTreeNode}s
     * @param editorCellRenderer The renderer to use when in editing mode, typically the separate instance of the
     *      *                    same renderer used when not in editing mode
     * @param selectionListener Listener that will be notified when an entry is selected
     * @param rootContact Optional root contact to be used when creating a controller for a subtree. If specified, any
     *                    {@link IGroupContact}s that are not descendants of the root {@link IGroupContact} will be
     *                    ignored. If not specified, a {@link GroupContactTreeNode} root will be created and all
     *                    {@link IGroupContact}s arriving via the {@link IGroupContactManagerListener }will be processed.
     */
    public GroupContactTreeController(@Nonnull JTree tree,
                                      @Nonnull TreeCellRenderer editorCellRenderer,
                                      @Nonnull IContactSelectionListener<GroupContactTreeNode> selectionListener,
                                      @Nullable IGroupContact rootContact)
    {
        this.tree = tree;

        subtree = rootContact != null;
        rootNode = new GroupContactTreeNode(rootContact);
        treeModel = new DefaultTreeModel(rootNode);

        treeModelListener = new GroupTreeModelListener();
        treeModel.addTreeModelListener(treeModelListener);

        treeSelectionListener = new GroupTreeSelectionListener(selectionListener);
        tree.addTreeSelectionListener(treeSelectionListener);

        tree.setShowsRootHandles(true);
        tree.setModel(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.putClientProperty("JTree.lineStyle", "None");

        // Editing isn't allowed, but this will allow us to access individual cell components and detect icon clicks
        final TreeCellEditor editor = new GroupContactCellEditor(tree, editorCellRenderer);
        tree.setCellEditor(editor);
        tree.setEditable(true);
    }

    @PostConstruct
    private void postConstruct()
    {
        groupContactManager.addListener(this);
    }

    @Override
    public void releaseResources()
    {
        groupContactManager.removeListener(this);
        tree.removeTreeSelectionListener(treeSelectionListener);
        treeModel.removeTreeModelListener(treeModelListener);
    }

    /**
     * @return The tree's root node
     */
    public GroupContactTreeNode getRootNode()
    {
        return rootNode;
    }

    /**
     * @return The tree's model
     */
    public DefaultTreeModel getModel()
    {
        return treeModel;
    }

    @InvokeOnEDT
    @Override
    public void contactAdded(@Nonnull IGroupContact contact)
    {
        // Note: this needs to be on the EDT with the call to add the node to the tree so that when the next
        // node is added, the child count is correct and the parent exists. Otherwise, a node could be added
        // before the EDT runs for the previous node and the new node could be added in the wrong place or the
        // parent might not be found.

        // Look for the parent node
        final IGroupContact parentContact = contact.getParentContact();
        final String parentId = parentContact != null ? parentContact.getUid() : null;
        final GroupContactTreeNode parentNode = parentContact == null ? rootNode : searchTreeForContact(parentId);

        if (parentNode == null)
        {
            if (!subtree) logger.warn("No parent group found for id '{}' for contact {}", parentId, contact);
            return;
        }

        // Find the right spot among the node's siblings
        final int childCount = parentNode.getChildCount();
        int index = childCount;
        for (int i = 0; i < childCount; i++)
        {
            final IGroupContact childContact = ((GroupContactTreeNode) parentNode.getChildAt(i)).getContact();

            // Prevent duplicates that can occur during initialization
            if (childContact != null && childContact.getUid().equals(contact.getUid())) return;

            if (CONTACT_COMPARATOR.compare(contact, childContact) < 0)
            {
                index = i;
                break;
            }
        }

        treeModel.insertNodeInto(new GroupContactTreeNode(contact), parentNode, index);

        logger.debug("Added group contact '{}' to group panel.", contact);
    }

    @InvokeOnEDT
    @Override
    public void contactRemoved(@Nonnull IGroupContact contact)
    {
        final GroupContactTreeNode contactNode = searchTreeForContact(contact.getUid());

        // Node will be null if the parent node has already been removed or if it's not part of a subtree
        if (contactNode == null) return;

        treeModel.removeNodeFromParent(contactNode);

        logger.debug("Removed group contact \"{}\" and its children from group panel.", contact);
    }

    @InvokeOnEDT
    @Override
    public void contactUpdated(@Nonnull IGroupContact contact)
    {
        final GroupContactTreeNode contactNode = searchTreeForContact(contact.getUid());

        if (contactNode == null)
        {
            if (!subtree) logger.warn("No existing node found for contact {}", contact);
            return;
        }

        contactNode.setUserObject(contact);
        treeModel.nodeChanged(contactNode);
        logger.debug("Updated group contact '{}' in group panel.", contact);
    }

    /**
     * Searches the tree for a contact matching the provided id.
     *
     * @param contactId The id of the contact to find
     * @return The found node or {@code null} if one doesn't exist.
     */
    @VisibleForTesting
    GroupContactTreeNode searchTreeForContact(@Nonnull String contactId)
    {
        for (Enumeration<TreeNode> enumeration = rootNode.breadthFirstEnumeration(); enumeration.hasMoreElements();)
        {
            final GroupContactTreeNode currentNode = (GroupContactTreeNode) enumeration.nextElement();
            final IGroupContact groupContact = currentNode.getContact();

            if (groupContact != null && groupContact.getUid().equals(contactId)) return currentNode;
        }

        return null;
    }

    /**
     * Cell editor for {@link #tree} cells.
     * <p>
     * {@link #tree} cells are not editable, but temporarily putting a clicked cell into editing mode allows us to
     * access the individual Components (rather than just the JTree) and detect icon clicks.
     */
    private static class GroupContactCellEditor extends DefaultTreeCellEditor
    {
        private final TreeCellRenderer treeCellRenderer;

        public GroupContactCellEditor(@Nonnull JTree tree, @Nonnull TreeCellRenderer treeCellRenderer)
        {
            super(tree, new DefaultTreeCellRenderer());
            this.treeCellRenderer = treeCellRenderer;
        }

        @Override
        public boolean isCellEditable(EventObject event)
        {
            // Only respond to mouse clicks
            return event instanceof MouseEvent;
        }

        @Override
        public boolean shouldSelectCell(EventObject event)
        {
            // We need to trigger the TreeSelectionListener
            return true;
        }

        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                    boolean leaf, int row)
        {
            // We don't need to do any editing so just delegate to the renderer
            return treeCellRenderer.getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
        }
    }

    /**
     * The menu item used to display a contact in the team members list popup.
     */
    private static class ContactMenuItem extends JMenuItem
    {
        public ContactMenuItem(@Nonnull String name, @Nullable Icon icon)
        {
            super(name, icon);
            setEnabled(false); // prevents highlighting
            setDisabledIcon(getIcon()); // prevents grayed-out icon
        }
    }

    /**
     * Simple listener that expands the tree to show nodes when they are inserted
     */
    private class GroupTreeModelListener implements TreeModelListener
    {
        @Override
        public void treeNodesChanged(TreeModelEvent e)
        {
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e)
        {
            tree.expandPath(e.getTreePath());
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e)
        {
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e)
        {
        }
    }

    /**
     * Listens for {@link GroupContactTreeNode} selections and displays a list of the {@link IGroupContact}'s 
     * members if the icon was clicked. If any other part of the node was clicked, the {@link IContactSelectionListener}
     * will be notified.
     */
    private class GroupTreeSelectionListener implements TreeSelectionListener
    {
        private final IContactSelectionListener<GroupContactTreeNode> contactSelectionListener;

        private GroupTreeSelectionListener(IContactSelectionListener<GroupContactTreeNode> contactSelectionListener)
        {
            this.contactSelectionListener = contactSelectionListener;
        }

        @Override
        public void valueChanged(TreeSelectionEvent e)
        {
            final TreePath treePath = e.getNewLeadSelectionPath();
            if (treePath != null)
            {
                final GroupContactTreeNode treeNode = (GroupContactTreeNode) treePath.getLastPathComponent();
                final IGroupContact groupContact = treeNode.getContact();
                final Component iconComponent = findIconComponent();

                tree.getSelectionModel().clearSelection();

                // User clicked the group icon; display the group members
                if (iconComponent != null && groupContact != null)
                {
                    // Ignore icon clicks for "All Chat Rooms", "Groups" and "Teams" that have null parent contacts
                    if (rolesManager.isManaged(groupContact) || groupContact.getParentContact() != null)
                    {
                        // invokeLater prevents the JPopup from immediately closing when the tree state changes
                        SwingUtilities.invokeLater(() -> showMemberList(groupContact, iconComponent));
                    }
                } else
                {
                    contactSelectionListener.onSelect(treeNode);
                }

                // This selection listener gets called during BasicTreeUI.startEditing(). Startup effectively gets
                // paused while the listener does its thing, so calling cancelEditing() will do nothing. So we queue the
                // call so that it gets called after this method is complete and editing has resumed.
                SwingUtilities.invokeLater(tree::cancelEditing);
            }
        }

        /**
         * Checks to see if the mouse is over a cell's icon Component. If so, the Component is returned.
         *
         * @return The cell's icon Component if the mouse is currently over it, {@code null} otherwise
         */
        private Component findIconComponent()
        {
            if (GraphicsEnvironment.isHeadless()) return null; // allows us to unit test

            final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(mousePoint, tree);
            final Component c = SwingUtilities.getDeepestComponentAt(tree, mousePoint.x, mousePoint.y);

            return c instanceof JComponent component
                    && component.getClientProperty(ICON_LABEL_PROPERTY) != null
                            ? c
                            : null;
        }

        /**
         * Displays a set of group members in a popup
         *
         * @param groupContact The group's contact
         * @param iconComponent The Component that was clicked
         */
        private void showMemberList(@Nonnull IGroupContact groupContact, @Nonnull Component iconComponent)
        {
            final JPopupMenu popupMenu = new JPopupMenu();
            final Set<IContact> members = contactUtils.getGroupMembers(groupContact, false, false);

            if (members.isEmpty())
            {
                popupMenu.add(new JMenuItem("(empty)"));
            } else
            {
                members.stream()
                        // make sure we have our IndividualContact objects that provide state and icons
                        .map(contact -> individualContactManager.getContact(contact.getUid()))
                        // remove any contacts in the "gone" state
                        .filter(contact -> contact != null && IndividualContactUtils.getState(contact) != IndividualContactState.gone)
                        .sorted(Comparator.comparing(IContact::getName))
                        .forEach(contact -> popupMenu.add(new ContactMenuItem(contact.getName(),
                                individualContactManager.getContactIconWithStatus(contact.getUid()))));

                if (popupMenu.getComponentCount() == 0) popupMenu.add(new JMenuItem("(no available members)"));
            }

            // set the popup location to the bottom right corner of the group's icon
            popupMenu.show(iconComponent, (int) (iconComponent.getWidth() * 0.75), (int) (iconComponent.getHeight() * 0.75));
        }
    }
}