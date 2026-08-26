package gov.takx.core.view.gui.contacts.lists;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.contact.IGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatRolesManager;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.timer.ITimerService;
import gov.takx.core.chat.Conversation;
import gov.takx.core.chat.ConversationStore;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;

/**
 * Adds additional functionality to the {@link GroupContactTreeController} to support the tree that is displayed in the
 * {@link gov.takx.core.view.gui.contacts.ContactsPanel}.
 *
 * @since 3.5
 */
@ApplicationScoped
public class ContactsPanelGroupContactTreeController extends AContactsPanelComponentController<JTree>
{
    // Popup icon displayed when the user tries to open a conversation that is not accessible
    private static final Icon LOCK_ICON = UIResources.getSizedIcon(ResourceDirectory.PLATFORM_ICONS, "lock.png",
            new Dimension(15, 15), false);

    private final ITimerService timerService;
    private final GroupContactManager groupContactManager;
    private final GeoChatTeamsManager teamsManager;
    private final GeoChatRolesManager rolesManager;

    private GroupContactTreeController treeController;

    @Inject
    ContactsPanelGroupContactTreeController(ContactsPanelController contactsPanelController, ITimerService timerService,
                                            GroupContactManager groupContactManager, GeoChatTeamsManager teamsManager,
                                            GeoChatRolesManager rolesManager)
    {
        super(contactsPanelController);

        this.timerService = timerService;
        this.groupContactManager = groupContactManager;
        this.teamsManager = teamsManager;
        this.rolesManager = rolesManager;
    }

    @Override
    public void onComponentSet(@Nonnull JTree component)
    {
        treeController = new GroupContactTreeController(component, new GroupContactTreeCellRenderer(),
                treeNode -> {
                    final IGroupContact contact = treeNode.getContact();

                    // Only open a team or role chat if it's the client's team
                    //noinspection DataFlowIssue Only the root node has a null contact and it cannot be selected
                    if (teamsManager.isManagedAndClientNotMember(contact) || rolesManager.isManagedAndClientNotMember(contact))
                    {
                        showMembersOnlyPopup();
                    } else
                    {
                        select(contact);
                    }
                });

        // set renderer after treeController sets model
        component.setCellRenderer(new GroupContactTreeCellRenderer());

        // Initialize the tree with all groups; the top-level groups should not be null by the time this is called
        treeController.contactAdded(groupContactManager.getAllChatRoomsContact());
        //noinspection DataFlowIssue
        treeController.contactAdded(groupContactManager.getRootGroupContact());
        treeController.contactAdded(groupContactManager.getTeamsContact());
        groupContactManager.getRoleContacts().forEach(treeController::contactAdded);
        groupContactManager.getGroupContacts().forEach(treeController::contactAdded);
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onProjectClosing(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosing event)
    {
        treeController.releaseResources();
    }

    /**
     * This is required to update the width of the JTree node when the unread count icon is added or removed. JTrees
     * cache node widths and will not respond to updated Component dimensions until
     * {@link DefaultTreeModel#nodeChanged(TreeNode)} is called.
     *
     * @param groupContactUid The contact whose node had changed
     * @since 5.2 (signature change)
     */
    @InvokeOnEDT
    public void unreadStatusChanged(@Nonnull String groupContactUid)
    {
        treeController.getModel().nodeChanged(treeController.searchTreeForContact(groupContactUid));
    }

    /**
     * Displays a timed popup that informs the user that they cannot open a team chat because they are not a team member
     *
     * @since 3.5
     */
    private void showMembersOnlyPopup()
    {
        final Point mousePoint = MouseInfo.getPointerInfo().getLocation();
        final Popup popup = PopupFactory.getSharedInstance()
                .getPopup(component, new JLabel("Members only", LOCK_ICON, SwingConstants.LEADING),
                        mousePoint.x + 10, mousePoint.y + 10);
        popup.show();
        timerService.createTask(popup::hide, 750);
    }

    /**
     * Renderer for contacts in the tree.
     */
    @VisibleForTesting
    @PseudoBean
    static class GroupContactTreeCellRenderer extends DefaultTreeCellRenderer
    {
        private final ContactListCellRendererPanel contactRendererPanel = new ContactListCellRendererPanel();

        @Inject
        private ConversationStore conversationStore;

        @Inject
        GroupContactManager groupContactManager;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            final IGroupContact groupContact = ((GroupContactTreeNode) value).getContact();

            // Object may be null for the root node
            if (groupContact == null)
            {
                setText("");
                return this;
            }

            final Conversation conversation = conversationStore.getConversationForContact(groupContact);

            // Check for null because we may be in the process of deleting the conversation
            if (conversation != null) contactRendererPanel.setConversation(conversation);

            contactRendererPanel.setIcon(groupContactManager.getIcon(groupContact));

            return contactRendererPanel;
        }
    }
}
