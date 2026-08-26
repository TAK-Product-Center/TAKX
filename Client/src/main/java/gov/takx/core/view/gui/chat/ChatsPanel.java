package gov.takx.core.view.gui.chat;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.core.comms.takserver.TakServerConnection;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
    private static final Icon ADD_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "add.png");
    private static final Icon CONTACTS_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "contacts.svg");
    private static final Icon TAK_SERVER_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "tak_server.svg");
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");
    private final CellConstraints cellConstraints = new CellConstraints();
    private final Runnable openContactsRunnable;
    private final Consumer<Conversation> openConversationCallback;
    private final Function<Conversation, Icon> conversationToIconFunction;
    private final BiConsumer<JLabel, Integer> unreadIconUpdater;

    @VisibleForTesting
    final Map<Conversation, ChatEntryPanel> conversationAndPanels = new ConcurrentHashMap<>();

    @Inject
    ITakServerConnectionManager takServerConnectionManager;

    @Inject
    ServerContactsDialogController serverContactsDialogController;

    /**
     * @param openContactsRunnable Runnable that responds when the user clicks the new chat button
     * @param openConversationConsumer Consumer that accepts a conversation UID and opens the corresponding conversation
     * @param conversationToIconFunction Function that accepts a conversation and returns the conversation's icon
     */
    ChatsPanel(@Nonnull Runnable openContactsRunnable, @Nonnull Consumer<Conversation> openConversationConsumer,
               @Nonnull Function<Conversation, Icon> conversationToIconFunction,
               @Nonnull BiConsumer<JLabel, Integer> unreadIconUpdater)
    {
        super(ButtonType.CHATS.getDisplayText());

        this.openContactsRunnable = openContactsRunnable;
        this.openConversationCallback = openConversationConsumer;
        this.conversationToIconFunction = conversationToIconFunction;
        this.unreadIconUpdater = unreadIconUpdater;

        initComponents();

        searchIconLabel.setIcon(SEARCH_ICON);
        newChatButton.setIcon(ADD_ICON);

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
        // Find all the TAK servers that support commo
        final var takServers = takServerConnectionManager.getTakServerConnections(true).stream()
                .filter(TakServerConnection.class::isInstance)
                .map(TakServerConnection.class::cast)
                .filter(c -> c.getStreamingNetInterface() != null)
                .toList();

        // If there are no eligible servers, just open the Contacts panel
        if (takServers.isEmpty())
        {
            openContactsRunnable.run();
        } else
        {
            final var menu = new JPopupMenu("Select contact source");

            final var contactsItem = new JMenuItem("Contacts");
            contactsItem.setIcon(CONTACTS_ICON);
            contactsItem.addActionListener(e -> openContactsRunnable.run());
            menu.add(contactsItem);

            menu.addSeparator();

            takServers.forEach(connection -> {
                final var serverItem = new JMenuItem(connection.getDisplayName());
                serverItem.setIcon(TAK_SERVER_ICON);
                serverItem.addActionListener(e -> serverContactsDialogController.showSelectionDialog(connection));
                menu.add(serverItem);
            });

            menu.show(newChatButton, 0, newChatButton.getHeight());
        }
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
                    "default:grow, 10dlu, default",
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
            headerPanel.add(searchPanel, cc.xy(1, 1));

            //---- newChatButton ----
            newChatButton.setName("newChatButton");
            newChatButton.addActionListener(e -> newChat());
            headerPanel.add(newChatButton, cc.xy(3, 1));
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
    private JPanel chatEntriesPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
