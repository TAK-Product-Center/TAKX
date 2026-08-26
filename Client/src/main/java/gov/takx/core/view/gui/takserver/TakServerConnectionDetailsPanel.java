package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.tak.api.comms.security.ISslCertificateManager;
import gov.tak.core.comms.takserver.TakServerConnection;
import gov.tak.core.comms.takserver.TakServerConnectionManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.commons.swing.ProtectedPasswordField;
import gov.takx.commons.swing.SwingFileFilter;
import gov.takx.commons.utils.StringUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.security.CertUtils;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.UUID;

/**
 * This panel offers controls to specify the details of a TAK Server connection.
 *
 * @since 3.0 (converted to a panel and renamed in 5.4)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TakServerConnectionDetailsPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_PORT = 8089;
    private static final String P12_SUFFIX = "p12";
    private static final FileFilter P12_FILE_FILTER = new SwingFileFilter(P12_SUFFIX);

    // Default to home directory to make life easier
    private final AlwaysOnTopFileChooser certFileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this, SystemUtils.getUserHome());
    private final TakServerConnection connection;
    private final Runnable doneCallback;

    private boolean showErrors;

    @Inject
    private TakServerConnectionManager manager;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private ISslCertificateManager certificateManager;

    /**
     * Constructor for creating the panel for defining a new connection.
     * 
     * @param doneCallback Callback to invoke when the user is done with the panel (either ok or cancel button press)
     */
    TakServerConnectionDetailsPanel(Runnable doneCallback)
    {
        this(null, doneCallback);
    }

    /**
     * Constructor for creating the panel to edit existing connection.
     *
     * @param connection Connection to edit, or {@code null} for defining a new connection
     * @param doneCallback Callback to invoke when the user is done with the panel (either ok or cancel button press)
     */
    TakServerConnectionDetailsPanel(@Nullable TakServerConnection connection, Runnable doneCallback)
    {
        this.connection = connection;
        this.doneCallback = doneCallback;

        initComponents();

        // Don't show comma in port number
        portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));
        portSpinner.setValue(DEFAULT_PORT);

        certFileChooser.setAcceptAllFileFilterUsed(false);
        certFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        certFileChooser.setMultiSelectionEnabled(false);
        certFileChooser.addChoosableFileFilter(P12_FILE_FILTER);

        // Avoid a double separator when editing a connection
        if (connection != null) bottomSeparator.setVisible(false);
    }

    @PostConstruct
    private void postConstruct()
    {
        // This logic utilizes the certificate manager, so it must be done in the post construct
        if (connection != null)
        {
            nameTextField.setText(connection.getDisplayName());
            final URI uri = connection.getUri();
            protocolComboBox.setSelectedItem(uri.getScheme().toUpperCase());
            addressTextField.setText(uri.getHost());
            portSpinner.setValue(uri.getPort());

            final StreamingConnectionConfiguration connectionConfig = connection.getConnectionConfig();
            setCertFileTextField(connectionConfig.getCertificateAuthorityFileName(), certAuthorityTextField);
            setCertFileTextField(connectionConfig.getCertificateFileName(), clientCertTextField);

            GUIUtils.setTextIfNonNull(passwordCertAuthority, connectionConfig.getCertificateAuthorityPassword());
            GUIUtils.setTextIfNonNull(passwordClientCert, connectionConfig.getCertificatePassword());
            GUIUtils.setTextIfNonNull(usernameTextField, connectionConfig.getUsername());
            GUIUtils.setTextIfNonNull(passwordLogin, connectionConfig.getPassword());
            useAuthCheckBox.setSelected(connectionConfig.isUseAuthentication());
            enrollForClientCertCheckBox.setSelected(connectionConfig.isEnrollForClientCert());

            updateUiComponentVisibility();

            // Set to true since we know we are starting with a valid connection so the user will be notified
            // immediately of any new errors that are introduced.
            showErrors = true;
        }
    }

    /**
     * Sets the text field with the absolute path to the file (if it exists in the store).
     *
     * @param certFileName  The name of the cert file, relative to the VFS certificate store
     * @param certTextField The text field to update with the file path
     */
    private void setCertFileTextField(String certFileName, JTextField certTextField)
    {
        if (!StringUtils.isEmpty(certFileName) && certificateManager.certExists(certFileName))
        {
            // Since we use the relative path in the config, update it to the absolute path so we will be able to
            // navigate to the file in the file chooser
            certFileName = certificateManager.getCertFile(certFileName).getAbsolutePath();
        }
        certTextField.setText(certFileName);
    }

    private void okButtonActionPerformed()
    {
        // Now that the user has attempted to submit the connection, that means they are done filling out fields and we
        // can start showing them errors. We don't want to show errors while they are first editing since that could be
        // annoying if they haven't made it through the form yet.
        showErrors = true;

        final boolean errorDisplayed = displayErrorIfNecessary();
        if (errorDisplayed) return; // An error is being displayed; don't try to connect and leave the dialog open

        final String name = nameTextField.getText();
        final String protocol = (String) protocolComboBox.getSelectedItem();
        final String address = addressTextField.getText().trim();
        final int portNumber = (int) portSpinner.getValue();
        String certificateAuthorityFile = certAuthorityTextField.getText();
        final String certificateAuthorityPassword = String.valueOf(passwordCertAuthority.getPassword());
        String certificateFile = clientCertTextField.getText();
        final String certificatePassword = String.valueOf(passwordClientCert.getPassword());
        final String username = usernameTextField.getText().trim();
        final String password = String.valueOf(passwordLogin.getPassword());
        final boolean useAuthentication = useAuthCheckBox.isSelected();
        final boolean enrollForClientCert = enrollForClientCertCheckBox.isSelected();

        final StreamingConnectionConfiguration connectionConfig;
        final UUID newNodeUuid = connection == null ? UUIDUtils.generateType1UUID() : null;

        try
        {
            connectionConfig = new StreamingConnectionConfiguration(connection == null ? newNodeUuid : connection.getId());
            connectionConfig.setDisplayName(name);

            //noinspection ConstantConditions
            connectionConfig.setUri(protocol, address, portNumber);

            // Setting the cert on the config will add it to the VFS certificate store if it isn't present
            connectionConfig.setCertificate(certificateFile, certificatePassword);
            connectionConfig.setCertificateAuthority(certificateAuthorityFile, certificateAuthorityPassword);

            connectionConfig.setUsername(username);
            connectionConfig.setPassword(password);

            connectionConfig.setUseAuthentication(useAuthentication);
            connectionConfig.setEnrollForClientCert(enrollForClientCert);

            connectionConfig.setEnabled(connection == null || connection.isEnabled());
        } catch (Exception e)
        {
            logger.error("Failed to create connection configuration", e);

            // If we were trying to create a new connection, remove the bogus preference node.
            if (newNodeUuid != null)
            {
                PreferenceEntry.COMMON_COMMO_TAK_SERVERS_NODE.node().removeChildNode(newNodeUuid.toString());
            }

            errorMessageLabel.setText("Configuration error: " + e.getMessage());
            errorMessageLabel.setVisible(true);

            revalidate();
            repaint();
            return;
        }

        // Use executor service to get the connection logic off the UI thread
        executorService.execute(() -> {
            Thread.currentThread().setName("Add TAK Server connection");

            if (connection == null)
            {
                manager.addTakServerConnection(connectionConfig);
            } else
            {
                manager.updateTakServerConnection(connection.getId(), connectionConfig);
            }
        });

        setVisible(false);
        doneCallback.run();
    }

    /**
     * Determines if the connection parameters are valid, and displays an error message if not.
     *
     * @return True if an error message is being displayed.
     */
    private boolean displayErrorIfNecessary()
    {
        String errorMessage = null;
        String warningMessage = null;
        final boolean enrollForClientCert = enrollForClientCertCheckBox.isSelected();
        final boolean useAuthentication = useAuthCheckBox.isSelected();
        boolean sslProtocol = isSslProtocol();

        if (nameTextField.getText().trim().isEmpty())
        {
            errorMessage = "A valid connection name must be provided.";
        } else if (addressTextField.getText().trim().isEmpty())
        {
            errorMessage = "A valid address must be provided.";
        } else if (sslProtocol)
        {
            if (!enrollForClientCert)
            {
                // If we aren't enrolling for certs, they must be provided when SSL is selected
                if (certAuthorityTextField.getText().trim().isEmpty())
                {
                    errorMessage = "Provide a valid certificate authority file or enroll for client certificate";
                } else if (String.valueOf(passwordCertAuthority.getPassword()).isEmpty())
                {
                    errorMessage = "Provide a valid certificate authority file password or enroll for client certificate";
                } else if (clientCertTextField.getText().trim().isEmpty())
                {
                    errorMessage = "Provide a valid client certificate file or enroll for client certificate";
                } else if (String.valueOf(passwordClientCert.getPassword()).isEmpty())
                {
                    errorMessage = "Provide a valid client certificate password or enroll for client certificate";
                } else
                {
                    // Validate certificate authority trust-store and client certificate trust-store
                    File certPath = new File(certAuthorityTextField.getText());
                    CertUtils.CertValidation validation = CertUtils.validateCert("Certificate authority cert", certPath, passwordCertAuthority.getPassword());
                    warningMessage = validation.validationWarning();
                    if (validation.isValid())
                    {
                        certPath = new File(clientCertTextField.getText());
                        validation = CertUtils.validateCert("Client cert", certPath, passwordClientCert.getPassword());
                        if (warningMessage == null)
                        {
                            warningMessage = validation.validationWarning();
                        }
                    }
                    errorMessage = validation.validationErrorExplanation();
                }
            }

            if (enrollForClientCert || useAuthentication)
            {
                String suffix = useAuthentication ? "using authentication" : "enrolling for client certificate";
                if (usernameTextField.getText().trim().isEmpty())
                {
                    errorMessage = "A valid username must be provided when " + suffix;
                } else if (String.valueOf(passwordLogin.getPassword()).isEmpty())
                {
                    errorMessage = "A valid password must be provided when " + suffix;
                }
            }
        }

        errorMessageLabel.setText(errorMessage);
        errorMessageLabel.setVisible(errorMessage != null);
        warningMessageLabel.setText(warningMessage);
        warningMessageLabel.setVisible(warningMessage != null);
        revalidate();
        repaint();

        return errorMessage != null;
    }

    private void cancelButtonActionPerformed()
    {
        setVisible(false);
        doneCallback.run();
    }

    private void selectCertAuthorityButtonActionPerformed()
    {
        certAuthorityTextField.setText(getCertificateFile("Select Trust Store File", certAuthorityTextField.getText()));
    }

    private void selectClientCertButtonActionPerformed()
    {
        clientCertTextField.setText(getCertificateFile("Select Client Certificate File", clientCertTextField.getText()));
    }

    /**
     * Displays a file chooser for the user to select a certificate file.
     *
     * @param title                 The title to display for the file chooser
     * @param currentlySelectedFile The currently selected cert file (may be empty or {@code null} if none selected)
     * @return The path of the selected file or the previously selected file if the user cancelled
     */
    private String getCertificateFile(String title, String currentlySelectedFile)
    {
        // If a file was already selected, open the file chooser to the file. This is important for usability when
        // editing an existing connection.
        if (!StringUtils.isEmpty(currentlySelectedFile))
        {
            final File certFile = new File(currentlySelectedFile);
            if (certFile.exists()) certFileChooser.setSelectedFile(certFile);
        }

        certFileChooser.setDialogTitle(title);
        int returnVal = certFileChooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = certFileChooser.getSelectedFile();

            if (file != null && FileUtils.getExtension(file).equalsIgnoreCase(P12_SUFFIX))
            {
                return file.getAbsolutePath();
            }
        }

        return currentlySelectedFile;
    }

    private void useAuthCheckBoxActionPerformed()
    {
        updateUiComponentVisibility();
        if (showErrors) displayErrorIfNecessary();
    }

    private void enrollForClientCertCheckBoxActionPerformed()
    {
        updateUiComponentVisibility();
        if (showErrors) displayErrorIfNecessary();
    }

    private void protocolComboBoxActionPerformed()
    {
        updateUiComponentVisibility();
        if (showErrors) displayErrorIfNecessary();
    }

    /**
     * Determines which UI components should be visible based on the user's selections.
     */
    private void updateUiComponentVisibility()
    {
        final boolean enrollForClientCert = enrollForClientCertCheckBox.isSelected();
        final boolean useAuthentication = useAuthCheckBox.isSelected();
        boolean sslProtocol = isSslProtocol();

        // Credentials are used for authentication or enrolling for a client cert
        credentialsPanel.setVisible(useAuthentication || enrollForClientCert);

        // Client certs are used if the protocol is set to SSL, as long as the user isn't requesting to enroll for certs
        clientCertPanel.setVisible(sslProtocol && !enrollForClientCert);

        // Trust stores are used if SSL. They are also used on classified networks that don't use Let's Encrypt even
        // when enroll for certs is selected
        trustStorePanel.setVisible(sslProtocol);

        // Add an info label to make sure the user isn't confused by the presence of these fields when they aren't required
        trustStoreOptionalPanel.setVisible(sslProtocol && enrollForClientCert);

        certsAndCredsWrapperPanel.setVisible(sslProtocol);

        revalidate();
        repaint();
    }

    /**
     * @return True if the SSL protocol is selected in the combo box.
     */
    private boolean isSslProtocol()
    {
        return protocolComboBox.getSelectedIndex() == 0;
    }

    /**
     * Custom document listener to validate the form on any change in a critical test field.
     */
    private final DocumentListener listener = new DocumentListener()
    {
        @Override
        public void insertUpdate(DocumentEvent event)
        {
            if (showErrors) displayErrorIfNecessary();
        }

        @Override
        public void removeUpdate(DocumentEvent event)
        {
            if (showErrors) displayErrorIfNecessary();
        }

        @Override
        public void changedUpdate(DocumentEvent event)
        {
            if (showErrors) displayErrorIfNecessary();
        }
    };

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var connectionDetailsPanel = new JPanel();
        var nameLabel = new JLabel();
        nameTextField = new PlaceholderTextField();
        var addressLabel = new JLabel();
        addressTextField = new PlaceholderTextField();
        var protocolLabel = new JLabel();
        protocolComboBox = new JComboBox<>();
        var portLabel = new JLabel();
        portSpinner = new JSpinner();
        certsAndCredsWrapperPanel = new JPanel();
        useAuthCheckBox = new JCheckBox();
        enrollForClientCertCheckBox = new JCheckBox();
        credentialsPanel = new JPanel();
        var usernameLabel = new JLabel();
        usernameTextField = new JTextField();
        var passwordLabel = new JLabel();
        passwordLogin = new ProtectedPasswordField();
        trustStorePanel = new JPanel();
        var certAuthorityFileLabel = new JLabel();
        certAuthorityTextField = new JTextField();
        var selectCertAuthorityButton = new JButton();
        var certAuthorityPasswordLabel = new JLabel();
        passwordCertAuthority = new ProtectedPasswordField();
        trustStoreOptionalPanel = new JPanel();
        var trustStoreOptionalLabel = new JLabel();
        clientCertPanel = new JPanel();
        var clientCertFileLabel = new JLabel();
        clientCertTextField = new JTextField();
        var selectClientCertButton = new JButton();
        var clientCertPasswordLabel = new JLabel();
        passwordClientCert = new ProtectedPasswordField();
        errorMessageLabel = new JLabel();
        warningMessageLabel = new JLabel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        bottomSeparator = new JSeparator();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow, default"));

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $rgap, 2*(default, $lgap), default"));

                //======== connectionDetailsPanel ========
                {
                    connectionDetailsPanel.setBorder(new TitledBorder("Connection Details"));
                    connectionDetailsPanel.setName("connectionDetailsPanel");
                    connectionDetailsPanel.setLayout(new FormLayout(
                            "default, $lcgap, left:default:grow, $lcgap, default, $lcgap, right:default",
                            "2*(default, $lgap), default"));

                    //---- nameLabel ----
                    nameLabel.setText("Name:");
                    nameLabel.setToolTipText("The user defined name for the connection");
                    nameLabel.setName("nameLabel");
                    connectionDetailsPanel.add(nameLabel, cc.xy(1, 1));

                    //---- nameTextField ----
                    nameTextField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                    nameTextField.setPlaceholderText("<Descriptive Server Name>");
                    nameTextField.setName("nameTextField");
                    nameTextField.getDocument().addDocumentListener(listener);
                    connectionDetailsPanel.add(nameTextField, cc.xywh(3, 1, 5, 1));

                    //---- addressLabel ----
                    addressLabel.setText("Address:");
                    addressLabel.setName("addressLabel");
                    connectionDetailsPanel.add(addressLabel, cc.xy(1, 3));

                    //---- addressTextField ----
                    addressTextField.setPlaceholderText("<Remote TAK Server IP or Hostname>");
                    addressTextField.setName("addressTextField");
                    addressTextField.getDocument().addDocumentListener(listener);
                    connectionDetailsPanel.add(addressTextField, cc.xywh(3, 3, 5, 1));

                    //---- protocolLabel ----
                    protocolLabel.setText("Protocol:");
                    protocolLabel.setName("protocolLabel");
                    connectionDetailsPanel.add(protocolLabel, cc.xy(1, 5));

                    //---- protocolComboBox ----
                    protocolComboBox.setModel(new DefaultComboBoxModel<>(new String[]{
                            "SSL",
                            "TCP"
                    }));
                    protocolComboBox.setName("protocolComboBox");
                    protocolComboBox.addActionListener(e -> protocolComboBoxActionPerformed());
                    connectionDetailsPanel.add(protocolComboBox, cc.xy(3, 5));

                    //---- portLabel ----
                    portLabel.setText("Port:");
                    portLabel.setName("portLabel");
                    connectionDetailsPanel.add(portLabel, cc.xy(5, 5));

                    //---- portSpinner ----
                    portSpinner.setModel(new SpinnerNumberModel(10000, 1, 65535, 1));
                    portSpinner.setName("portSpinner");
                    connectionDetailsPanel.add(portSpinner, cc.xy(7, 5));
                }
                contentPanel.add(connectionDetailsPanel, cc.xy(1, 1));

                //======== certsAndCredsWrapperPanel ========
                {
                    certsAndCredsWrapperPanel.setBorder(new TitledBorder("Certificates & Credentials"));
                    certsAndCredsWrapperPanel.setName("certsAndCredsWrapperPanel");
                    certsAndCredsWrapperPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default:grow",
                            "5*(default)"));

                    //---- useAuthCheckBox ----
                    useAuthCheckBox.setText("Use Authentication");
                    useAuthCheckBox.setName("useAuthCheckBox");
                    useAuthCheckBox.addActionListener(e -> useAuthCheckBoxActionPerformed());
                    certsAndCredsWrapperPanel.add(useAuthCheckBox, cc.xy(1, 1));

                    //---- enrollForClientCertCheckBox ----
                    enrollForClientCertCheckBox.setText("Enroll for Client Certificate");
                    enrollForClientCertCheckBox.setName("enrollForClientCertCheckBox");
                    enrollForClientCertCheckBox.addActionListener(e -> enrollForClientCertCheckBoxActionPerformed());
                    certsAndCredsWrapperPanel.add(enrollForClientCertCheckBox, cc.xy(3, 1));

                    //======== credentialsPanel ========
                    {
                        credentialsPanel.setVisible(false);
                        credentialsPanel.setName("credentialsPanel");
                        credentialsPanel.setLayout(new FormLayout(
                                "default, $lcgap, default:grow, $lcgap, default",
                                "$ugap, default, $lgap, fill:default"));

                        //---- usernameLabel ----
                        usernameLabel.setText("Username:");
                        usernameLabel.setToolTipText("The user defined name for the connection");
                        usernameLabel.setName("usernameLabel");
                        credentialsPanel.add(usernameLabel, cc.xy(1, 2));

                        //---- usernameTextField ----
                        usernameTextField.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                        usernameTextField.setName("usernameTextField");
                        usernameTextField.getDocument().addDocumentListener(listener);
                        credentialsPanel.add(usernameTextField, cc.xywh(3, 2, 3, 1));

                        //---- passwordLabel ----
                        passwordLabel.setText("Password:");
                        passwordLabel.setToolTipText("The user defined name for the connection");
                        passwordLabel.setName("passwordLabel");
                        credentialsPanel.add(passwordLabel, cc.xy(1, 4));

                        //---- passwordLogin ----
                        passwordLogin.setToolTipText("<Login Password>");
                        passwordLogin.setName("passwordLogin");
                        passwordLogin.getDocument().addDocumentListener(listener);
                        credentialsPanel.add(passwordLogin, cc.xywh(3, 4, 3, 1));
                    }
                    certsAndCredsWrapperPanel.add(credentialsPanel, cc.xywh(1, 2, 3, 1));

                    //======== trustStorePanel ========
                    {
                        trustStorePanel.setName("trustStorePanel");
                        trustStorePanel.setLayout(new FormLayout(
                                "default, $lcgap, default:grow, $lcgap, default",
                                "$ugap, default, $lgap, fill:default, default"));

                        //---- certAuthorityFileLabel ----
                        certAuthorityFileLabel.setText("CA File:");
                        certAuthorityFileLabel.setName("certAuthorityFileLabel");
                        trustStorePanel.add(certAuthorityFileLabel, cc.xy(1, 2));

                        //---- certAuthorityTextField ----
                        certAuthorityTextField.setToolTipText("Certificate Authority file location");
                        certAuthorityTextField.setName("certAuthorityTextField");
                        certAuthorityTextField.getDocument().addDocumentListener(listener);
                        trustStorePanel.add(certAuthorityTextField, cc.xy(3, 2));

                        //---- selectCertAuthorityButton ----
                        selectCertAuthorityButton.setText("...");
                        selectCertAuthorityButton.setName("selectCertAuthorityButton");
                        selectCertAuthorityButton.addActionListener(e -> selectCertAuthorityButtonActionPerformed());
                        trustStorePanel.add(selectCertAuthorityButton, cc.xy(5, 2));

                        //---- certAuthorityPasswordLabel ----
                        certAuthorityPasswordLabel.setText("CA Password:");
                        certAuthorityPasswordLabel.setName("certAuthorityPasswordLabel");
                        trustStorePanel.add(certAuthorityPasswordLabel, cc.xy(1, 4));

                        //---- passwordCertAuthority ----
                        passwordCertAuthority.setToolTipText("<Password for Certificate Authority>");
                        passwordCertAuthority.setName("passwordCertAuthority");
                        passwordCertAuthority.getDocument().addDocumentListener(listener);
                        trustStorePanel.add(passwordCertAuthority, cc.xywh(3, 4, 3, 1));
                    }
                    certsAndCredsWrapperPanel.add(trustStorePanel, cc.xywh(1, 3, 3, 1));

                    //======== trustStoreOptionalPanel ========
                    {
                        trustStoreOptionalPanel.setVisible(false);
                        trustStoreOptionalPanel.setName("trustStoreOptionalPanel");
                        trustStoreOptionalPanel.setLayout(new FormLayout(
                                "default:grow",
                                "$lgap, default"));

                        //---- trustStoreOptionalLabel ----
                        trustStoreOptionalLabel.setText("Client certificate is likely not required unless this is a restricted network.");
                        trustStoreOptionalLabel.setFont(trustStoreOptionalLabel.getFont().deriveFont(trustStoreOptionalLabel.getFont().getStyle() | Font.ITALIC));
                        trustStoreOptionalLabel.setName("trustStoreOptionalLabel");
                        trustStoreOptionalPanel.add(trustStoreOptionalLabel, cc.xy(1, 2));
                    }
                    certsAndCredsWrapperPanel.add(trustStoreOptionalPanel, cc.xywh(1, 4, 3, 1));

                    //======== clientCertPanel ========
                    {
                        clientCertPanel.setName("clientCertPanel");
                        clientCertPanel.setLayout(new FormLayout(
                                "default, $lcgap, default:grow, $lcgap, default",
                                "$ugap, default, $lgap, fill:default"));

                        //---- clientCertFileLabel ----
                        clientCertFileLabel.setText("Client Cert File:");
                        clientCertFileLabel.setName("clientCertFileLabel");
                        clientCertPanel.add(clientCertFileLabel, cc.xy(1, 2));

                        //---- clientCertTextField ----
                        clientCertTextField.setToolTipText("Client certificate file location");
                        clientCertTextField.setName("clientCertTextField");
                        clientCertTextField.getDocument().addDocumentListener(listener);
                        clientCertPanel.add(clientCertTextField, cc.xy(3, 2));

                        //---- selectClientCertButton ----
                        selectClientCertButton.setText("...");
                        selectClientCertButton.setName("selectClientCertButton");
                        selectClientCertButton.addActionListener(e -> selectClientCertButtonActionPerformed());
                        clientCertPanel.add(selectClientCertButton, cc.xy(5, 2));

                        //---- clientCertPasswordLabel ----
                        clientCertPasswordLabel.setText("Client Password:");
                        clientCertPasswordLabel.setName("clientCertPasswordLabel");
                        clientCertPanel.add(clientCertPasswordLabel, cc.xy(1, 4));

                        //---- passwordClientCert ----
                        passwordClientCert.setToolTipText("<Password for Certificate>");
                        passwordClientCert.setName("passwordClientCert");
                        passwordClientCert.getDocument().addDocumentListener(listener);
                        clientCertPanel.add(passwordClientCert, cc.xywh(3, 4, 3, 1));
                    }
                    certsAndCredsWrapperPanel.add(clientCertPanel, cc.xywh(1, 5, 3, 1));
                }
                contentPanel.add(certsAndCredsWrapperPanel, cc.xy(1, 3));

                //---- errorMessageLabel ----
                errorMessageLabel.setText("Error message");
                errorMessageLabel.setForeground(Color.red);
                errorMessageLabel.setVisible(false);
                errorMessageLabel.setName("errorMessageLabel");
                contentPanel.add(errorMessageLabel, cc.xy(1, 5));

                //---- warningMessageLabel ----
                warningMessageLabel.setVisible(false);
                warningMessageLabel.setForeground(Color.orange);
                warningMessageLabel.setName("warningMessageLabel");
                contentPanel.add(warningMessageLabel, cc.xy(1, 7));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        add(dialogPane, cc.xy(1, 1));

        //---- bottomSeparator ----
        bottomSeparator.setName("bottomSeparator");
        add(bottomSeparator, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private PlaceholderTextField nameTextField;
    private PlaceholderTextField addressTextField;
    private JComboBox<String> protocolComboBox;
    private JSpinner portSpinner;
    private JPanel certsAndCredsWrapperPanel;
    private JCheckBox useAuthCheckBox;
    private JCheckBox enrollForClientCertCheckBox;
    private JPanel credentialsPanel;
    private JTextField usernameTextField;
    private ProtectedPasswordField passwordLogin;
    private JPanel trustStorePanel;
    private JTextField certAuthorityTextField;
    private ProtectedPasswordField passwordCertAuthority;
    private JPanel trustStoreOptionalPanel;
    private JPanel clientCertPanel;
    private JTextField clientCertTextField;
    private ProtectedPasswordField passwordClientCert;
    private JLabel errorMessageLabel;
    private JLabel warningMessageLabel;
    private JSeparator bottomSeparator;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
