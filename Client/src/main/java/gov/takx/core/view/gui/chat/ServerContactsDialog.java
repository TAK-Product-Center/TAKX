package gov.takx.core.view.gui.chat;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import java.awt.Component;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Displays a list of server contacts and notifies the caller when the user selects a contact.
 *
 * @since 5.7
 */
class ServerContactsDialog extends AStandardDialog
{
    private static final ImageIcon REFRESH_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "refresh.svg");
    private static final ImageIcon FILTER_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "filter.svg");

    private static final Comparator<ClientEndpoint> CLIENT_ENDPOINT_COMPARATOR = Comparator.comparing(ClientEndpoint::callsign);

    private final Supplier<Collection<ClientEndpoint>> contactsSupplier;
    private final DefaultListModel<ClientEndpoint> listModel = new DefaultListModel<>();;
    private List<ClientEndpoint> sortedContacts;

    /**
     * @param serverName The name of the server to be used in the dialog's title
     * @param contactsSupplier The supplier that will provide the list of contacts to display
     * @param contactConsumer A consumer that will accept the selected contact
     */
    ServerContactsDialog(@Nonnull String serverName, @Nonnull Supplier<Collection<ClientEndpoint>> contactsSupplier,
                         @Nonnull Consumer<ClientEndpoint> contactConsumer)
    {
        super("TAK Server Contacts - " + serverName);

        this.contactsSupplier = contactsSupplier;

        initComponents();

        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        filterIconLabel.setIcon(FILTER_ICON);
        refreshButton.setIcon(REFRESH_ICON);

        contactsList.setModel(listModel);

        contactsList.setCellRenderer(new ClientEndpointListCellRenderer());

        contactsList.addListSelectionListener(e -> {
            contactConsumer.accept(contactsList.getSelectedValue());
            dispose();
        });

        GUIUtils.addSingleActionDocumentListener(filterTextField, e -> filter());

        refresh();
    }

    /**
     * Reloads the model with the contacts that pass the current filter.
     */
    private void filter()
    {
        listModel.clear();

        final String filterText = filterTextField.getText();

        if (StringUtils.isEmpty(filterText))
        {
            listModel.addAll(sortedContacts);
        } else
        {
            final var filterTextLowerCase = filterText.toLowerCase();
            final var filteredList = sortedContacts.stream()
                    .filter(clientEndpoint -> clientEndpoint.callsign().toLowerCase().contains(filterTextLowerCase)
                            || clientEndpoint.username().toLowerCase().contains(filterTextLowerCase))
                    .toList();
            listModel.addAll(filteredList);
        }
    }

    /**
     * Retrieves the latest contact list from the supplier and updates the model.
     */
    private void refresh()
    {
        sortedContacts = contactsSupplier.get().stream().sorted(CLIENT_ENDPOINT_COMPARATOR).toList();
        filter();
    }

    private static class ClientEndpointListCellRenderer extends DefaultListCellRenderer
    {
        private static final Border BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        private static final Icon USER_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "user.png");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            final var clientEndpoint = (ClientEndpoint) value;
            setText("<html><body><b>" + clientEndpoint.callsign() + "</b><br>" + clientEndpoint.username() + "</body></html>");
            setBorder(BORDER);
            setIcon(USER_ICON);

            return this;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var label4 = new JLabel();
        var filterPanel = new JPanel();
        filterIconLabel = new JLabel();
        filterTextField = new JTextField();
        refreshButton = new JButton();
        var contactListScrollPane = new JScrollPane();
        contactsList = new JList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), fill:default:grow"));

        //---- label4 ----
        label4.setText("Select a contact");
        label4.setHorizontalAlignment(SwingConstants.CENTER);
        label4.setName("label4");
        contentPane.add(label4, cc.xy(1, 1));

        //======== filterPanel ========
        {
            filterPanel.setName("filterPanel");
            filterPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, 24dlu",
                    "fill:default"));

            //---- filterIconLabel ----
            filterIconLabel.setName("filterIconLabel");
            filterPanel.add(filterIconLabel, cc.xy(1, 1));

            //---- filterTextField ----
            filterTextField.setToolTipText("Enter text to filter contact list");
            filterTextField.setName("filterTextField");
            filterPanel.add(filterTextField, cc.xy(3, 1));

            //---- refreshButton ----
            refreshButton.setToolTipText("Refresh the list of contacts");
            refreshButton.setName("refreshButton");
            refreshButton.addActionListener(e -> refresh());
            filterPanel.add(refreshButton, cc.xy(5, 1));
        }
        contentPane.add(filterPanel, cc.xy(1, 3));

        //======== contactListScrollPane ========
        {
            contactListScrollPane.setName("contactListScrollPane");

            //---- contactsList ----
            contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            contactsList.setName("contactsList");
            contactListScrollPane.setViewportView(contactsList);
        }
        contentPane.add(contactListScrollPane, cc.xy(1, 5));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel filterIconLabel;
    private JTextField filterTextField;
    private JButton refreshButton;
    private JList<ClientEndpoint> contactsList;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
