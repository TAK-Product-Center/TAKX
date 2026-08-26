package gov.takx.core.view.gui.preferences.network;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.core.view.gui.GUIUtils;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

/**
 * Displays controls to edit network input or output values.
 *
 * @since 5.0
 */
public class NetworkInputOutputEditPanel extends JPanel
{
    private static final String LEGAL_DESCRIPTION_CHARS = "_-";
    private static final Predicate<Character> DESCRIPTION_PREDICATE =
            character -> Character.isLetterOrDigit(character) || Character.isSpaceChar(character)
                    || LEGAL_DESCRIPTION_CHARS.indexOf(character) >= 0;
    private static final Predicate<Character> PORT_PREDICATE = Character::isDigit;
    private static final int PORT_MIN = 1;
    private static final int PORT_MAX = 65_535;

    private Runnable okayListener;
    private Runnable cancelListener;
    private Function<String, Boolean> duplicateDescriptionChecker;

    public NetworkInputOutputEditPanel()
    {
        initComponents();

        descriptionTextField.setDocument(new LimitedDocument(Preferences.MAX_KEY_LENGTH, DESCRIPTION_PREDICATE));
        portTextField.setDocument(new LimitedDocument(5, PORT_PREDICATE));
    }

    /**
     * Removes TCP option if only UDP is supported.
     *
     * @since 5.4
     */
    public void setUdpOnly()
    {
        protocolComboBox.removeItem("TCP");
    }

    /**
     * @param okayListener Called when the user clicks "Okay"
     * @param cancelListener Called when the user clicks "Cancel"
     * @param duplicateDescriptionChecker Function that takes a description String and returns {@code true} if the
     *                                    description is already in use
     */
    public void setListeners(@Nonnull Runnable okayListener, @Nonnull Runnable cancelListener,
                             @Nonnull Function<String, Boolean> duplicateDescriptionChecker)
    {
        this.okayListener = okayListener;
        this.cancelListener = cancelListener;
        this.duplicateDescriptionChecker = duplicateDescriptionChecker;

        GUIUtils.addSingleActionDocumentListener(descriptionTextField, e -> validateFields());
        GUIUtils.addSingleActionDocumentListener(hostTextField, e -> validateFields());
        GUIUtils.addSingleActionDocumentListener(portTextField, e -> validateFields());
    }

    /**
     * Update our controls with the values from the given {@code configuration}.
     */
    public void setFromConfiguration(@Nonnull ConnectionConfiguration configuration)
    {
        setDescription(configuration.getDisplayName());

        final var netConnectString = configuration.getNetConnectString();
        setProtocol(netConnectString.getProto());
        setHost(netConnectString.getHost());
        setPort(String.valueOf(netConnectString.getPort()));
    }

    /**
     * @return The network description
     */
    @Nonnull
    public String getDescription()
    {
        return descriptionTextField.getText().trim();
    }

    /**
     * @param description The network description
     */
    public void setDescription(@Nonnull String description)
    {
        descriptionTextField.setText(description);
    }

    /**
     * @return The network protocol
     */
    @Nonnull
    public String getProtocol()
    {
        return protocolComboBox.getItemAt(protocolComboBox.getSelectedIndex()).toLowerCase();
    }

    /**
     * @param protocol The network protocol
     */
    public void setProtocol(@Nonnull String protocol)
    {
        protocolComboBox.setSelectedItem(protocol.toUpperCase());
    }

    /**
     * @return The network host
     */
    @Nonnull
    public String getHost()
    {
        return hostTextField.getText().trim();
    }

    /**
     * @param host The network host
     */
    public void setHost(@Nonnull String host)
    {
        hostTextField.setText(host);
    }

    /**
     * @return The network port
     */
    @Nonnull
    public String getPort()
    {
        return portTextField.getText().trim();
    }

    /**
     * @param port The network port
     */
    public void setPort(@Nonnull String port)
    {
        portTextField.setText(port);
    }

    /**
     * @return The network URI in the form {@code [protocol]://[host]:[port]}
     */
    @Nonnull
    public String getUri()
    {
        return getProtocol() + "://" + getHost() + ':' + getPort();
    }

    /**
     * Clears all the fields and disables the "Okay" button
     */
    public void clear()
    {
        descriptionTextField.setText("");
        protocolComboBox.setSelectedItem(0);
        hostTextField.setText("");
        portTextField.setText("");
        okayButton.setEnabled(false);
        errorLabel.setText("");
    }

    /**
     * Responds to "Okay" button clicks
     */
    private void okay()
    {
        okayListener.run();
    }

    /**
     * Responds to "Cancel" button clicks
     */
    private void cancel()
    {
        cancelListener.run();
    }

    /**
     * Validates the fields by ensuring they are not empty and that the description is not already in use by another
     * network IO definition. If any errors are detected, a descriptive error message will be displayed.
     */
    private void validateFields()
    {
        final List<String> errors = new ArrayList<>();

        final String descriptionText = descriptionTextField.getText().trim();
        final boolean descriptionEmpty = descriptionText.isEmpty();
        final String portText = portTextField.getText();
        final boolean portEmpty = portText.trim().isEmpty();

        final List<String> missingFields = new ArrayList<>();
        if (descriptionEmpty) missingFields.add("description");
        if (hostTextField.getText().trim().isEmpty()) missingFields.add("host");
        if (portEmpty) missingFields.add("port");
        if (!missingFields.isEmpty()) errors.add(StringUtils.join(missingFields, ", ") + " req'd");

        if (!portEmpty)
        {
            final int port = Integer.parseInt(portText);
            if (port < PORT_MIN || port > PORT_MAX) errors.add("invalid port");
        }

        if (!descriptionEmpty && duplicateDescriptionChecker.apply(descriptionText)) errors.add("duplicate description");

        if (!errors.isEmpty())
        {
            final String errorsString = StringUtils.capitalize(StringUtils.join(errors, "; "));
            errorLabel.setText(errorsString);
            okayButton.setEnabled(false);
        } else
        {
            errorLabel.setText("");
            okayButton.setEnabled(true);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var editDescriptionLabel = new JLabel();
        descriptionTextField = new JTextField();
        var editProtocolLabel = new JLabel();
        var editHostAddressLabel = new JLabel();
        var editPortLabel = new JLabel();
        protocolComboBox = new JComboBox<>();
        hostTextField = new JTextField();
        portTextField = new JTextField();
        var buttonPanel = new JPanel();
        errorLabel = new JLabel();
        okayButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default:grow(0.3)",
                "2*(default), $lgap, 2*(default), $rgap, default"));
        ((FormLayout) getLayout()).setColumnGroups(new int[][]{{3, 5}});

        //---- editDescriptionLabel ----
        editDescriptionLabel.setText("Description");
        editDescriptionLabel.setFont(editDescriptionLabel.getFont().deriveFont(editDescriptionLabel.getFont().getSize() - 3f));
        editDescriptionLabel.setName("editDescriptionLabel");
        add(editDescriptionLabel, cc.xy(1, 1));

        //---- descriptionTextField ----
        descriptionTextField.setName("descriptionTextField");
        add(descriptionTextField, cc.xywh(1, 2, 5, 1));

        //---- editProtocolLabel ----
        editProtocolLabel.setText("Protocol");
        editProtocolLabel.setFont(editProtocolLabel.getFont().deriveFont(editProtocolLabel.getFont().getSize() - 3f));
        editProtocolLabel.setName("editProtocolLabel");
        add(editProtocolLabel, cc.xy(1, 4));

        //---- editHostAddressLabel ----
        editHostAddressLabel.setText("Host Address");
        editHostAddressLabel.setFont(editHostAddressLabel.getFont().deriveFont(editHostAddressLabel.getFont().getSize() - 3f));
        editHostAddressLabel.setName("editHostAddressLabel");
        add(editHostAddressLabel, cc.xy(3, 4));

        //---- editPortLabel ----
        editPortLabel.setText("Port");
        editPortLabel.setFont(editPortLabel.getFont().deriveFont(editPortLabel.getFont().getSize() - 3f));
        editPortLabel.setName("editPortLabel");
        add(editPortLabel, cc.xy(5, 4));

        //---- protocolComboBox ----
        protocolComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                "TCP",
                "UDP"
        }));
        protocolComboBox.setName("protocolComboBox");
        add(protocolComboBox, cc.xy(1, 5));

        //---- hostTextField ----
        hostTextField.setName("hostTextField");
        add(hostTextField, cc.xy(3, 5));

        //---- portTextField ----
        portTextField.setName("portTextField");
        add(portTextField, cc.xy(5, 5));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, default)",
                    "default"));
            ((FormLayout) buttonPanel.getLayout()).setColumnGroups(new int[][]{{3, 5}});

            //---- errorLabel ----
            errorLabel.setForeground(Color.yellow);
            errorLabel.setName("errorLabel");
            buttonPanel.add(errorLabel, cc.xy(1, 1));

            //---- okayButton ----
            okayButton.setText("OK");
            okayButton.setName("okayButton");
            okayButton.addActionListener(e -> okay());
            buttonPanel.add(okayButton, cc.xy(3, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancel());
            buttonPanel.add(cancelButton, cc.xy(5, 1));
        }
        add(buttonPanel, cc.xywh(1, 7, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField descriptionTextField;
    private JComboBox<String> protocolComboBox;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JLabel errorLabel;
    private JButton okayButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
