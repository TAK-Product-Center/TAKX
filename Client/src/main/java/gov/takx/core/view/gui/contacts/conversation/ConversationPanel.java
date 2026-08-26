package gov.takx.core.view.gui.contacts.conversation;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.chat.IChatMessage;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.core.chat.ChatMessageBuilder;
import gov.takx.core.chat.ChatMessageUtils;
import gov.takx.core.chat.Conversation;
import gov.takx.core.view.gui.contacts.util.SmartScroller;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Shows a conversation and all of its messages to the user.
 *
 * @since 3.0
 */
@PseudoBean
public class ConversationPanel extends JPanel
{
    private static final long TWO_MINUTES_IN_MS = TimeUnit.MINUTES.toMillis(2);
    private static final ImageIcon BACK_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "arrow_left.png");
    private static final ImageIcon ADD_GROUP_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "group_add.png");
    private static final ImageIcon ATTACH_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "attach.png");
    private static final ImageIcon REMOVE_GROUP_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "group_remove.png");
    private static final long DELAY_BETWEEN_NOTIFICATIONS = TimeUnit.SECONDS.toMillis(2);
    private final List<MessageAndPanel> chatMessagesAndPanels = new ArrayList<>();
    private final SmartScroller smartScroller;
    private long lastNotificationTime; // last time a notification was displayed, to prevent spamming the user

    // Constraints used for new rows
    private final GridBagConstraints messageRowConstraints =
            new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    @SuppressWarnings("unused")
    @Inject
    private ConversationPanelController conversationPanelController;

    @SuppressWarnings("unused")
    @Inject
    @NodeUUID
    private UUID clientUid;

    @Inject
    private INotificationService notificationService;

    public ConversationPanel()
    {
        initComponents();
        backButton.setIcon(BACK_ICON);
        editGroupButton.setIcon(UIResources.EDIT_ACTION_BUTTON_ICON);
        addSubgroupButton.setIcon(ADD_GROUP_ICON);
        dataPackageButton.setIcon(ATTACH_ICON);
        deleteSubgroupButton.setIcon(REMOVE_GROUP_ICON);

        // Remap the Enter key to call the send button action method
        Action enterKeyAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sendButtonActionPerformed();
            }
        };

        messageTextArea.getActionMap()
                .put(messageTextArea.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
                        enterKeyAction);

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                resetComponentSizes();
            }
        });

        // Increase mouse scrolling speed (default is 1)
        messagesScrollPane.getVerticalScrollBar().setUnitIncrement(5);

        smartScroller = new SmartScroller(messagesScrollPane);
    }

    /**
     * @param enable {@code true} if the remove subgroups button should be enabled, {@code false} otherwise
     * @since 3.5
     */
    void enableRemoveSubgroups(boolean enable)
    {
        deleteSubgroupButton.setEnabled(enable);
    }

    /**
     * Notified on a change to the time preference so the conversation times can be updated.
     */
    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onTimePreferenceChange(@Observes @UserPreferenceType(UserPreferenceEntry.DISPLAY_LOCAL_TIME) PreferenceEntryChange ignored)
    {
        chatMessagesAndPanels.forEach(messageAndPanel -> messageAndPanel.panel.updateTimeLabel());
        repaint();
    }

    /**
     * Adds a message to the conversation currently shown on the panel.
     *
     * @param message The message to be added
     */
    @InvokeOnEDT
    void addMessage(@Nonnull final IChatMessage message)
    {
        final String messageUid = message.getUid();

        // For messages broadcast to the all chat groups convo, it's possible to receive them twice.
        if (chatMessagesAndPanels.stream().anyMatch(messageAndPanel -> messageAndPanel.message.getUid().equals(messageUid))) return;

        // Check to see if the last message was from the same user within the last 2 minutes; if so, combine them
        if (!chatMessagesAndPanels.isEmpty())
        {
            final int lastIndex = chatMessagesAndPanels.size() - 1;
            final IChatMessage prevMessage = chatMessagesAndPanels.get(lastIndex).message;

            // Note: this used to compare the UID to determine if the message was from the same user, but that doesn't
            // work for group chats because the UID is the ID of the room, not the sending user. We could have it check
            // UID for direct chats and callsign for group chats, but it's probably good enough to just always check
            // sender name; the worst case scenario is a user sends a messages, changes their name, and sends another
            // message within 2 minutes. In that case, it's probably better not to combine the messages anyway so the
            // new name can be displayed for the new messages.
            if (StringUtils.equals(prevMessage.getSenderName(), message.getSenderName()) &&
                    message.getSendTime() - prevMessage.getSendTime() < TWO_MINUTES_IN_MS
                    // Also don't combine if the previous message has links and a "Details" button
                    && !ChatMessageUtils.hasChatLinks(prevMessage))
            {
                final String newText = prevMessage.getText() + "\n" + message.getText();

                final IChatMessage compoundMessage = new ChatMessageBuilder(prevMessage)
                        .withText(newText)
                        .withUid(messageUid)
                        .withLinks(ChatMessageUtils.getChatLinks(message))
                        .build();

                final AChatMessagePanel panel = createMessagePanel(compoundMessage);
                chatMessagesAndPanels.set(lastIndex, new MessageAndPanel(compoundMessage, panel));
                replaceLastMessagePanel(panel);
                refresh(message);

                return;
            }
        }

        // Create a new message and add it
        final AChatMessagePanel panel = createMessagePanel(message);
        chatMessagesAndPanels.add(new MessageAndPanel(message, panel));
        addMessagePanel(panel);
        refresh(message);
    }

    /**
     * Refreshes the conversation panel after changes are applied.
     *
     * @param message The message that was added
     * @since 5.2
     */
    private void refresh(@Nonnull IChatMessage message)
    {
        revalidate();
        repaint();
        checkScrollToBottom(message);
    }

    /**
     * Overrides the {@link SmartScroller} and forces the scroll pane to scroll to the bottom when the local contact
     * sends a message.
     *
     * @since 5.2
     */
    private void checkScrollToBottom(@Nonnull IChatMessage chatMessage)
    {
        if (chatMessage.sentByLocalContact())
        {
            smartScroller.scrollToBottom();
        } else // todo replace temp notification with persistent alert TAKX-3012
        {
            final long time = System.currentTimeMillis();

            if (isShowing() && !smartScroller.isAtBottom() && (time - lastNotificationTime > DELAY_BETWEEN_NOTIFICATIONS))
            {
                notificationService.show(Notification.builder("New message(s) received. Scroll down to view.").build(),
                        DELAY_BETWEEN_NOTIFICATIONS);
                lastNotificationTime = time;
            }
        }
    }

    /**
     * Clears the status icon from the previous message panel and sets the status icon on the most recent message panel.
     *
     * @param activeConversation The active conversation
     * @since 5.2
     */
    void updateSentMessageStatus(@Nonnull Conversation activeConversation)
    {
        final String lastSentMessageUid = activeConversation.getLastSentMessageUid();
        final Icon icon = activeConversation.getStatus().getIcon();

        chatMessagesAndPanels.forEach(
                messageAndPanel -> messageAndPanel.panel.getTimeLabel()
                        .setIcon(messageAndPanel.message.getUid().equals(lastSentMessageUid) ? icon : null));
    }

    /**
     * Adds an {@link AChatMessagePanel} to the panel grid.
     *
     * @param panel The panel to add
     * @since 5.2
     */
    private void addMessagePanel(@Nonnull AChatMessagePanel panel)
    {
        messageRowConstraints.gridy++;
        messageRowsPanel.add(panel, messageRowConstraints);
    }

    /**
     * Replaces the last message {@link AChatMessagePanel} in the grid with a new instance.
     *
     * @param panel The new panel that will replace the last panel
     * @since 5.2
     */
    private void replaceLastMessagePanel(@Nonnull AChatMessagePanel panel)
    {
        messageRowsPanel.remove(messageRowsPanel.getComponentCount() - 1);
        messageRowsPanel.add(panel, messageRowConstraints);
    }

    /**
     * Resets the size of the {@link AChatMessagePanel} JTextAreas. Without this, they will grow when the panel is
     * widened but will not shrink when the panel is narrowed.
     *
     * @since 5.2
     */
    private void resetComponentSizes()
    {
        chatMessagesAndPanels.forEach(chatMessageAndPanel -> chatMessageAndPanel.panel.resetSize());
    }

    /**
     * @param message The chat message
     * @return A new {@link ReceivedMessagePanel} or {@link SentMessagePanel} initialized with the message info
     * @since 5.2
     */
    @Nonnull
    private AChatMessagePanel createMessagePanel(@Nonnull IChatMessage message)
    {
        final boolean sentMessage = clientUid.toString().equals(message.getSenderUid());
        return sentMessage
                ? new SentMessagePanel(message, e -> conversationPanelController.showDataPackageDetails(message))
                : new ReceivedMessagePanel(message, e -> conversationPanelController.showDataPackageDetails(message));
    }

    /**
     * Prepares the conversation panel for a new conversation to be displayed by clearing all existing content and setting
     * the conversation name and optionally showing the edit button
     *
     * @param conversationName The conversation name
     * @param owned {@code true} if the conversation is a group conversation and is owned by the local user
     * @param supportsDataPackages {@code true} if the files can be sent via data package
     * @param unavailableMessage The unavailable message or {@code null} if conversation is available
     */
    void prepareConversationForDisplay(@Nonnull String conversationName, boolean owned, boolean supportsDataPackages,
                                       @Nullable String unavailableMessage)
    {
        chatMessagesAndPanels.clear();
        messageRowsPanel.removeAll();
        messageRowConstraints.gridy = 1;

        conversationLabel.setText(conversationName);
        editGroupButton.setVisible(owned);
        addSubgroupButton.setVisible(owned);
        deleteSubgroupButton.setVisible(owned);
        dataPackageButton.setVisible(supportsDataPackages);

        setConversationEnabled(unavailableMessage == null, unavailableMessage);
    }

    /**
     * @param name The conversation name
     * @since 3.4.1
     */
    void setConversationName(@Nonnull String name)
    {
        conversationLabel.setText(name);
    }

    /**
     * @param icon The contact icon
     * @since 3.4.1
     */
    void setContactIcon(@Nullable Icon icon)
    {
        conversationLabel.setIcon(icon);
    }

    /**
     * Enables or disables the client's ability to send messages. The conversation should be disabled when the active
     * conversation cannot be continued. For group conversations, this happens when the client has been removed from
     * the chat group or the chat group has been deleted. For direct conversations, this happens when an individual
     * contact has been flagged as gone.
     *
     * @param enabled {@code true} if the conversation should be enabled, {@code false} if it should be disabled
     * @param message The message to display if the conversation is being disabled. May be {@code null}
     */
    void setConversationEnabled(boolean enabled, @Nullable String message)
    {
        conversationDisabledPanel.setVisible(!enabled);
        conversationDisabledLabel.setText(message);

        messageTextArea.setVisible(enabled);
        buttonPanel.setVisible(enabled);
    }

    /**
     * Called when the user presses ENTER or clicks the "Send" button. Attempts to send the message that's currently
     * in the message text field to the contact associated with the active conversation.
     */
    private void sendButtonActionPerformed()
    {
        String message = messageTextArea.getText();

        if (!message.isEmpty())
        {
            SwingUtilities.invokeLater(() -> conversationPanelController.sendMessage(message));
            messageTextArea.setText("");
        }
    }

    /**
     * Informs the controller that the user clicked the "Back" button to return to the previously shown panel.
     */
    private void backButtonClicked()
    {
        conversationPanelController.backButtonClicked();
    }

    /**
     * Informs the controller that the user clicked the "Edit group" button and thus the group editing panel should be
     * shown.
     */
    private void editGroupButtonClicked()
    {
        conversationPanelController.editGroupButtonClicked();
    }

    /**
     * @since 3.4.1
     */
    private void addGroupActionPerformed()
    {
        conversationPanelController.addSubgroupButtonClicked();
    }

    /**
     * @since 3.4.1
     */
    private void removeGroupActionPerformed()
    {
        conversationPanelController.removeSubgroupButtonClicked();
    }

    /**
     * @since 5.2
     */
    private void dataPackageButtonActionPerformed()
    {
        conversationPanelController.sendDataPackage();
    }

    /**
     * @param message the chat message
     * @param panel the chat message panel
     * @since 5.2
     */
    private record MessageAndPanel(IChatMessage message, AChatMessagePanel panel)
    {
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var headerPanel = new JPanel();
        backButton = new JButton();
        conversationLabel = new JLabel();
        editGroupButton = new JButton();
        addSubgroupButton = new JButton();
        deleteSubgroupButton = new JButton();
        messagesScrollPane = new JScrollPane();
        messageRowsPanel = new JPanel();
        var textPanel = new JPanel();
        messageTextArea = new JTextArea();
        buttonPanel = new JPanel();
        dataPackageButton = new JButton();
        var sendButton = new JButton();
        conversationDisabledPanel = new JPanel();
        conversationDisabledLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("conversationPanel");
        setMinimumSize(new Dimension(228, 80));
        setLayout(new BorderLayout());

        //======== headerPanel ========
        {
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "center:default, $rgap, 3*(center:default, $lcgap), center:default",
                    "default"));

            //---- backButton ----
            backButton.setName("backButton");
            backButton.addActionListener(e -> backButtonClicked());
            headerPanel.add(backButton, cc.xy(1, 1));

            //---- conversationLabel ----
            conversationLabel.setText("Conversation Name");
            conversationLabel.putClientProperty("FlatLaf.styleClass", "large");
            conversationLabel.setName("conversationLabel");
            headerPanel.add(conversationLabel, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

            //---- editGroupButton ----
            editGroupButton.setToolTipText("Edit Group");
            editGroupButton.setName("editGroupButton");
            editGroupButton.addActionListener(e -> editGroupButtonClicked());
            headerPanel.add(editGroupButton, cc.xy(5, 1));

            //---- addSubgroupButton ----
            addSubgroupButton.setToolTipText("New Subgroup");
            addSubgroupButton.setName("addSubgroupButton");
            addSubgroupButton.addActionListener(e -> addGroupActionPerformed());
            headerPanel.add(addSubgroupButton, cc.xy(7, 1));

            //---- deleteSubgroupButton ----
            deleteSubgroupButton.setToolTipText("Delete Subgroup");
            deleteSubgroupButton.setName("deleteSubgroupButton");
            deleteSubgroupButton.addActionListener(e -> removeGroupActionPerformed());
            headerPanel.add(deleteSubgroupButton, cc.xy(9, 1));
        }
        add(headerPanel, BorderLayout.PAGE_START);

        //======== messagesScrollPane ========
        {
            messagesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            messagesScrollPane.setName("messagesScrollPane");

            //======== messageRowsPanel ========
            {
                messageRowsPanel.setBorder(Borders.DLU2_BORDER);
                messageRowsPanel.setName("messageRowsPanel");
                messageRowsPanel.setLayout(new GridBagLayout());
                ((GridBagLayout) messageRowsPanel.getLayout()).columnWidths = new int[]{0, 0};
                ((GridBagLayout) messageRowsPanel.getLayout()).rowHeights = new int[]{0, 0};
                ((GridBagLayout) messageRowsPanel.getLayout()).columnWeights = new double[]{1.0, 1.0E-4};
                ((GridBagLayout) messageRowsPanel.getLayout()).rowWeights = new double[]{0.0, 1.0E-4};
            }
            messagesScrollPane.setViewportView(messageRowsPanel);
        }
        add(messagesScrollPane, BorderLayout.CENTER);

        //======== textPanel ========
        {
            textPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
            textPanel.setName("textPanel");
            textPanel.setLayout(new BorderLayout());

            //---- messageTextArea ----
            messageTextArea.setLineWrap(true);
            messageTextArea.setWrapStyleWord(true);
            messageTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            messageTextArea.setName("messageTextArea");
            textPanel.add(messageTextArea, BorderLayout.CENTER);

            //======== buttonPanel ========
            {
                buttonPanel.setName("buttonPanel");
                buttonPanel.setLayout(new FormLayout(
                        "2*(2dlu, default)",
                        "bottom:default:grow"));

                //---- dataPackageButton ----
                dataPackageButton.setName("dataPackageButton");
                dataPackageButton.addActionListener(e -> dataPackageButtonActionPerformed());
                buttonPanel.add(dataPackageButton, cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

                //---- sendButton ----
                sendButton.setText("Send");
                sendButton.setFont(sendButton.getFont().deriveFont(sendButton.getFont().getStyle() | Font.BOLD));
                sendButton.setName("sendButton");
                sendButton.addActionListener(e -> sendButtonActionPerformed());
                buttonPanel.add(sendButton, cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));
            }
            textPanel.add(buttonPanel, BorderLayout.EAST);

            //======== conversationDisabledPanel ========
            {
                conversationDisabledPanel.setName("conversationDisabledPanel");
                conversationDisabledPanel.setLayout(new FormLayout(
                        "default:grow",
                        "20dlu"));

                //---- conversationDisabledLabel ----
                conversationDisabledLabel.setText("Conversation not accessible");
                conversationDisabledLabel.setHorizontalAlignment(SwingConstants.CENTER);
                conversationDisabledLabel.setForeground(Color.gray);
                conversationDisabledLabel.setFont(conversationDisabledLabel.getFont().deriveFont(conversationDisabledLabel.getFont().getSize() + 1f));
                conversationDisabledLabel.setName("conversationDisabledLabel");
                conversationDisabledPanel.add(conversationDisabledLabel, cc.xy(1, 1));
            }
            textPanel.add(conversationDisabledPanel, BorderLayout.NORTH);
        }
        add(textPanel, BorderLayout.PAGE_END);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton backButton;
    private JLabel conversationLabel;
    private JButton editGroupButton;
    private JButton addSubgroupButton;
    private JButton deleteSubgroupButton;
    private JScrollPane messagesScrollPane;
    private JPanel messageRowsPanel;
    private JTextArea messageTextArea;
    private JPanel buttonPanel;
    private JButton dataPackageButton;
    private JPanel conversationDisabledPanel;
    private JLabel conversationDisabledLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
