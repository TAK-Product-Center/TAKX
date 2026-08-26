package gov.takx.core.view.gui.contacts;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.ChatServiceStarted;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.contacts.conversation.ConversationPanel;
import gov.takx.core.view.gui.contacts.conversation.ConversationPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupCreationPanel;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupCreationPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupDeletionPanel;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupDeletionPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupEditPanel;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupEditPanelController;
import gov.takx.core.view.gui.contacts.individualmanagement.IndividualDeletionPanel;
import gov.takx.core.view.gui.contacts.individualmanagement.IndividualDeletionPanelController;
import gov.takx.core.view.gui.contacts.lists.ContactsPanelGroupContactTreeController;
import gov.takx.core.view.gui.contacts.lists.ContactsPanelIndividualListController;
import gov.takx.core.view.gui.contacts.lists.ImprovedSelectionJList;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.CardLayout;
import java.awt.Dimension;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * The primary Contacts panel shown to the user to manage and chat with other TAK contacts.
 *
 * @since 3.0
 */
@PseudoBean(singletonObserver = true)
public class ContactsPanel extends ATitledPanel
{
    private static final ImageIcon USER_REMOVE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "user_remove.png");
    private static final ImageIcon GROUP_ADD_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "group_add.png");
    private static final ImageIcon GROUP_REMOVE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "group_remove.png");
    private static final ImageIcon USER_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "user.png");

    private ConversationPanel conversationPanel;
    private IndividualDeletionPanel individualDeletionPanel;
    private GroupCreationPanel groupCreationPanel;
    private GroupDeletionPanel groupDeletionPanel;
    private GroupEditPanel groupEditPanel;

    @Inject
    @CallSign
    private String userName;

    @Inject
    private ContactsPanelController contactsPanelController;

    @Inject
    private ConversationPanelController conversationPanelController;

    @Inject
    private ContactsPanelGroupContactTreeController contactsPanelGroupsTreeController;

    @Inject
    private IndividualDeletionPanelController individualDeletionPanelController;
    @Inject
    private GroupCreationPanelController groupCreationPanelController;

    @Inject
    private GroupDeletionPanelController groupDeletionPanelController;

    @Inject
    private GroupEditPanelController groupEditPanelController;

    @Inject
    private ContactsPanelIndividualListController contactsPanelIndividualListController;

    public ContactsPanel()
    {
        super(ButtonType.CONTACTS.getDisplayText());

        initComponents();

        // Make sure they're empty until lateInit
        groupContactTree.setModel(null);
        individualContactList.setModel(new DefaultListModel<>());

        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
    }

    /** @since 5.2 */
    @PostConstruct
    private void postConstruct()
    {
        // This has to happen in an @PC instead of in the constructor to avoid a race condition where the EDT
        // picks up the event *before* the injection processing happens at the end of the constructor.
        SwingUtilities.invokeLater(this::lateInit);
    }

    private void lateInit()
    {
        conversationPanel = new ConversationPanel();
        individualDeletionPanel = new IndividualDeletionPanel();
        groupCreationPanel = new GroupCreationPanel();
        groupDeletionPanel = new GroupDeletionPanel();
        groupEditPanel = new GroupEditPanel();

        userLabel.setIcon(USER_ICON);
        removeIndividualContactButton.setIcon(USER_REMOVE_ICON);
        addGroupButton.setIcon(GROUP_ADD_ICON);
        removeGroupButton.setIcon(GROUP_REMOVE_ICON);

        removeIndividualContactButton.setVisible(PreferenceEntry.INDIVIDUAL_CONTACT_DELETION_ENABLED.getBooleanValue());
    }

    /**
     * @param enable {@code true} if the remove groups button should be enabled, {@code false} otherwise
     * @since 3.4.1
     */
    public void enableRemoveGroups(boolean enable)
    {
        removeGroupButton.setEnabled(enable);
    }

    /**
     * @param userName The new name to set on this panel
     * @since 3.1
     */
    public void setUserName(@Nonnull String userName)
    {
        this.userName = userName;

        SwingUtilities.invokeLater(() -> userLabel.setText(userName));
    }

    /**
     * Finalize our components once the Chat service has started.
     * @since 5.2
     */
    @InvokeOnEDT
    private void handleChatServiceStarted(@Observes ChatServiceStarted ignored)
    {
        initializeCardLayout();

        contactsPanelGroupsTreeController.setComponent(groupContactTree);
        contactsPanelIndividualListController.setComponent(individualContactList);

        conversationPanelController.setPanel(conversationPanel);
        individualDeletionPanelController.setPanel(individualDeletionPanel);
        groupCreationPanelController.setPanel(groupCreationPanel);
        groupDeletionPanelController.setPanel(groupDeletionPanel);
        groupEditPanelController.setPanel(groupEditPanel);

        contactsPanelController.setPanel(this);
        contactsPanelController.start();

        SwingUtilities.invokeLater(() -> userLabel.setText(userName));
    }

    /**
     * Initializes the CardLayout manager responsible for swapping other panels in and out of the card layout.
     */
    private void initializeCardLayout()
    {
        cardContainer.add(conversationPanel, conversationPanel.getName());
        cardContainer.add(individualDeletionPanel, individualDeletionPanel.getName());
        cardContainer.add(groupCreationPanel, groupCreationPanel.getName());
        cardContainer.add(groupDeletionPanel, groupDeletionPanel.getName());
        cardContainer.add(groupEditPanel, groupEditPanel.getName());

        showPrimaryContactsPanel();
    }

    /**
     * Shows this panel to the user.
     */
    void showPrimaryContactsPanel()
    {
        showPanel(chatPanel);
    }

    /**
     * Shows the conversation panel to the user.
     */
    void showConversationPanel()
    {
        showPanel(conversationPanel);
    }

    /**
     * Shows the group creation panel to the user.
     */
    void showGroupCreationPanel()
    {
        groupCreationPanel.initGroupName();
        showPanel(groupCreationPanel);
    }

    /**
     * Shows the group creation panel to the user.
     */
    void showGroupDeletionPanel()
    {
        showPanel(groupDeletionPanel);
    }

    /**
     * Shows the group editing panel to the user.
     */
    public void showGroupEditPanel()
    {
        showPanel(groupEditPanel);
    }

    /**
     * Helper method to show the provided panel.
     *
     * @param panel The panel to show
     */
    @InvokeOnEDT
    private void showPanel(JPanel panel)
    {
        ((CardLayout) cardContainer.getLayout()).show(cardContainer, panel.getName());
    }

    private void addGroupButtonActionPerformed()
    {
        contactsPanelController.addGroupRequested(null);
    }

    private void removeGroupButtonActionPerformed()
    {
        contactsPanelController.deleteGroupRequested(null);
    }

    /**
     * @since 5.1
     */
    private void removeIndividualContactButtonActionPerformed()
    {
        individualDeletionPanelController.prepare();
        showPanel(individualDeletionPanel);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        cardContainer = new JPanel();
        chatPanel = new JPanel();
        userLabel = new JLabel();
        removeIndividualContactButton = new JButton();
        addGroupButton = new JButton();
        removeGroupButton = new JButton();
        scrollPane = new JScrollPane();
        var groupAndIndividualPanel = new JPanel();
        groupContactTree = new JTree();
        individualContactList = new ImprovedSelectionJList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setPreferredSize(new Dimension(300, 400));
        setName("this");
        setLayout(new FormLayout(
                "pref:grow",
                "fill:pref:grow"));

        //======== cardContainer ========
        {
            cardContainer.setPreferredSize(new Dimension(106, 300));
            cardContainer.setName("cardContainer");
            cardContainer.setLayout(new CardLayout());

            //======== chatPanel ========
            {
                chatPanel.setName("chatPanel");
                chatPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default, 2*($lcgap, center:default)",
                        "default, $lgap, fill:default:grow(0.5)"));

                //---- userLabel ----
                userLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
                userLabel.setOpaque(true);
                userLabel.setName("userLabel");
                chatPanel.add(userLabel, cc.xy(1, 1));

                //---- removeIndividualContactButton ----
                removeIndividualContactButton.setToolTipText("Remove user");
                removeIndividualContactButton.setVisible(false);
                removeIndividualContactButton.setName("removeIndividualContactButton");
                removeIndividualContactButton.addActionListener(e -> removeIndividualContactButtonActionPerformed());
                chatPanel.add(removeIndividualContactButton, cc.xy(3, 1));

                //---- addGroupButton ----
                addGroupButton.setToolTipText("Add group");
                addGroupButton.setName("addGroupButton");
                addGroupButton.addActionListener(e -> addGroupButtonActionPerformed());
                chatPanel.add(addGroupButton, cc.xy(5, 1));

                //---- removeGroupButton ----
                removeGroupButton.setEnabled(false);
                removeGroupButton.setToolTipText("Remove group");
                removeGroupButton.setName("removeGroupButton");
                removeGroupButton.addActionListener(e -> removeGroupButtonActionPerformed());
                chatPanel.add(removeGroupButton, cc.xy(7, 1));

                //======== scrollPane ========
                {
                    scrollPane.setName("scrollPane");

                    //======== groupAndIndividualPanel ========
                    {
                        groupAndIndividualPanel.setName("groupAndIndividualPanel");
                        groupAndIndividualPanel.setLayout(new FormLayout(
                                "default:grow",
                                "top:default, fill:default:grow"));

                        //---- groupContactTree ----
                        groupContactTree.setRootVisible(false);
                        groupContactTree.setName("groupContactTree");
                        groupAndIndividualPanel.add(groupContactTree, cc.xy(1, 1));

                        //---- individualContactList ----
                        individualContactList.setName("individualContactList");
                        groupAndIndividualPanel.add(individualContactList, cc.xy(1, 2));
                    }
                    scrollPane.setViewportView(groupAndIndividualPanel);
                }
                chatPanel.add(scrollPane, cc.xywh(1, 3, 7, 1));
            }
            cardContainer.add(chatPanel, "chatPanel");
        }
        add(cardContainer, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel cardContainer;
    private JPanel chatPanel;
    private JLabel userLabel;
    private JButton removeIndividualContactButton;
    private JButton addGroupButton;
    private JButton removeGroupButton;
    private JScrollPane scrollPane;
    private JTree groupContactTree;
    private ImprovedSelectionJList<IIndividualContact> individualContactList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
