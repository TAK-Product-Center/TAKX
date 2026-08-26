package gov.takx.core.view.gui.chat;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.chat.IChatMessage;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.chat.Conversation;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.contacts.conversation.AChatMessagePanel;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Displays the most recent message from each conversation and opens the full conversation history when clicked.
 *
 * @since 5.2
 */
@PseudoBean
public class ChatsPanel extends ATitledPanel implements IResourceCleaner
{
    private static final Icon SEARCH_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "search.png");
    private static final Icon CONTACT_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "user.png");
    private static final Icon GROUP_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "contacts.svg");
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");
    private final CellConstraints cellConstraints = new CellConstraints();
    private final Consumer<Conversation> openConversationCallback;
    private final Function<Conversation, Icon> conversationToIconFunction;
    private final BiConsumer<JLabel, Integer> unreadIconUpdater;

    @VisibleForTesting
    final Map<Conversation, ChatEntryPanel> conversationAndPanels = new ConcurrentHashMap<>();

    @Inject
    AllContactsDialogController allContactsDialogController;

    /**
     * @param openConversationConsumer Consumer that accepts a conversation UID and opens the corresponding conversation
     * @param conversationToIconFunction Function that accepts a conversation and returns the conversation's icon
     * @param unreadIconUpdater Function that updates the provided label with an unread message count icon
     */
    ChatsPanel(@Nonnull Consumer<Conversation> openConversationConsumer,
               @Nonnull Function<Conversation, Icon> conversationToIconFunction,
               @Nonnull BiConsumer<JLabel, Integer> unreadIconUpdater)
    {
        super(ButtonType.CHATS.getDisplayText());

        this.openConversationCallback = openConversationConsumer;
        this.conversationToIconFunction = conversationToIconFunction;
        this.unreadIconUpdater = unreadIconUpdater;

        initComponents();

        searchIconLabel.setIcon(SEARCH_ICON);
        newChatButton.setIcon(CONTACT_ICON);
        groupsButton.setIcon(GROUP_ICON);

        // Resizes the JTextFields when the panel width changes
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                resetComponentSizes();
            }
        });

        GUIUtils.addSingleActionDocumentListener(filterTextField, e -> conversationAndPanels.forEach(this::updatePanelVisibility));
    }

    @Override
    public void releaseResources()
    {
        removeAllPanels();
        conversationAndPanels.clear();
        filterTextField.setText(null);
    }

    /**
     * Updates or creates the conversation's {@link ChatEntryPanel} with the latest message and message time and
     * refreshes the {@link ChatEntryPanel} list.
     *
     * @param conversation The conversation with a new message
     */
    @InvokeOnEDT
    void onNewMessage(@Nonnull Conversation conversation)
    {
        addMessage(conversation);
        refreshList();
    }

    /**
     * Updates or creates the conversation's {@link ChatEntryPanel} with the latest message and message time.
     *
     * @param conversation The conversation with a new message
     */
    void addMessage(@Nonnull Conversation conversation)
    {
        final IChatMessage message = getLastMessage(conversation);
        final ChatEntryPanel panel = conversationAndPanels.computeIfAbsent(conversation, this::createChatEntryPanel);

        panel.setMessage(message.getText(), message.getSendTime());
        updatePanelVisibility(conversation, panel);
    }

    /**
     * @param uid The conversation UID
     * @param name The new conversation name
     */
    @InvokeOnEDT
    void updateConversation(@Nonnull String uid, @Nonnull String name)
    {
        conversationAndPanels.entrySet().stream()
                .filter(e -> e.getKey().getUid().equals(uid))
                .findFirst()
                .ifPresent(entry -> {
                    final Conversation conversation = entry.getKey();
                    final ChatEntryPanel panel = entry.getValue();
                    panel.setConversationDetails(name, conversationToIconFunction.apply(conversation));
                    updatePanelVisibility(conversation, panel);

                    repaint();
                });
    }

    /**
     * Refreshes all {@link ChatEntryPanel} time labels.
     */
    void refreshTimeLabels()
    {
        conversationAndPanels.values().forEach(ChatEntryPanel::updateTimeLabel);
    }

    /**
     * Refreshes all the unread message indicators.
     *
     * @since 5.2
     */
    @InvokeOnEDT
    void refreshUnreadMessageCounts()
    {
        conversationAndPanels.forEach((key, value) -> unreadIconUpdater.accept(value.getUnreadCountLabel(), key.getUnreadMessageCount()));
        repaint();
    }

    /**
     * Rebuild the panel to reflect the model and ensure proper sizing and sorting.
     */
    @InvokeOnEDT
    void refreshList()
    {
        removeAllPanels();

        conversationAndPanels.values().stream()
                .sorted(Comparator.comparingLong(ChatEntryPanel::getTime).reversed())
                .forEach(this::addPanel);

        revalidate();
        repaint();
    }

    /**
     * @param panel The panel to add to the bottom of the {@link #chatEntriesPanel}
     */
    private void addPanel(@Nonnull ChatEntryPanel panel)
    {
        final FormLayout formLayout = (FormLayout) chatEntriesPanel.getLayout();
        formLayout.appendRow(ROW_SPEC);
        chatEntriesPanel.add(panel, cellConstraints.xy(1, formLayout.getRowCount()));
    }

    /**
     * Removes all panels from the {@link #chatEntriesPanel} and all rows from its FormLayout
     */
    private void removeAllPanels()
    {
        chatEntriesPanel.removeAll();

        final FormLayout formLayout = (FormLayout) chatEntriesPanel.getLayout();
        int rowCount;
        while ((rowCount = formLayout.getRowCount()) > 0) formLayout.removeRow(rowCount);
    }

    /**
     * @param conversation The conversation
     * @return A new panel to display the conversation's last message
     */
    @Nonnull
    private ChatEntryPanel createChatEntryPanel(@Nonnull Conversation conversation)
    {
        final ChatEntryPanel chatEntryPanel = new ChatEntryPanel();
        chatEntryPanel.setConversationDetails(conversation.getName(), conversationToIconFunction.apply(conversation));

        chatEntryPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                openConversationCallback.accept(conversation);

            }
        });

        return chatEntryPanel;
    }

    /**
     * @param conversation The conversation (must have one or more messages)
     * @return The last message in the conversation
     */
    @Nonnull
    private IChatMessage getLastMessage(@Nonnull Conversation conversation)
    {
        return conversation.getMessages().last();
    }

    /**
     * Resets the size of the {@link AChatMessagePanel} JTextAreas. Without this, they will grow when the panel is
     * widened but will not shrink when the panel is narrowed.
     */
    private void resetComponentSizes()
    {
        conversationAndPanels.values().forEach(ChatEntryPanel::resetSize);
    }

    /**
     * Responds to new chat button clicks by delegating to the provided runnable.
     */
    private void newChat()
    {
        allContactsDialogController.showSelectionDialog();
    }

    private void groups()
    {
        // todo: TAKX-4283
    }

    /**
     * Updates a panel's visibility based on the text in the filter field.
     *
     * @param conversation The conversation
     * @param panel The conversation's panel
     */
    private void updatePanelVisibility(@Nonnull Conversation conversation, @Nonnull ChatEntryPanel panel)
    {
        final String filterText = filterTextField.getText();

        if (StringUtils.isEmpty(filterText))
        {
            panel.setVisible(true);
            return;
        }

        final String filterTextLowerCase = filterText.toLowerCase();
        final boolean match = conversation.getName().toLowerCase().contains(filterTextLowerCase)
                || panel.getMessage().toLowerCase().contains(filterTextLowerCase);
        panel.setVisible(match);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var headerPanel = new JPanel();
        var searchPanel = new JPanel();
        searchIconLabel = new JLabel();
        filterTextField = new JTextField();
        newChatButton = new JButton();
        groupsButton = new JButton();
        var conversationListScrollPane = new JScrollPane();
        chatEntriesPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new BorderLayout());

        //======== headerPanel ========
        {
            headerPanel.setBorder(Borders.DLU2_BORDER);
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "default:grow, 10dlu, 24dlu, $lcgap, 24dlu",
                    "fill:default"));

            //======== searchPanel ========
            {
                searchPanel.setName("searchPanel");
                searchPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "fill:default"));

                //---- searchIconLabel ----
                searchIconLabel.setName("searchIconLabel");
                searchPanel.add(searchIconLabel, cc.xy(1, 1));

                //---- filterTextField ----
                filterTextField.setName("filterTextField");
                searchPanel.add(filterTextField, cc.xy(3, 1));
            }
            headerPanel.add(searchPanel, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

            //---- newChatButton ----
            newChatButton.setName("newChatButton");
            newChatButton.addActionListener(e -> newChat());
            headerPanel.add(newChatButton, cc.xy(3, 1));

            //---- groupsButton ----
            groupsButton.setEnabled(false);
            groupsButton.setToolTipText("Not yet supported");
            groupsButton.setName("groupsButton");
            groupsButton.addActionListener(e -> groups());
            headerPanel.add(groupsButton, cc.xy(5, 1));
        }
        add(headerPanel, BorderLayout.NORTH);

        //======== conversationListScrollPane ========
        {
            conversationListScrollPane.setName("chatsScrollPanel");
            conversationListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            //======== chatEntriesPanel ========
            {
                chatEntriesPanel.setName("chatEntriesPanel");
                chatEntriesPanel.setLayout(new FormLayout(
                        "default:grow",
                        ""));
            }
            conversationListScrollPane.setViewportView(chatEntriesPanel);
        }
        add(conversationListScrollPane, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel searchIconLabel;
    private JTextField filterTextField;
    private JButton newChatButton;
    private JButton groupsButton;
    private JPanel chatEntriesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
