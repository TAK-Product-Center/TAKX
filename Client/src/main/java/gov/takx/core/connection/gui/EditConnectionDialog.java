package gov.takx.core.connection.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.connection.IConnectionManagerListener;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionPanel;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.ui.IConnectionSwingUIConsumer;
import gov.takx.api.ui.IConnectionSwingUIProvider;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.connections.GenericListener;
import gov.takx.core.view.gui.connections.IUserInputValidator;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.lang.invoke.MethodHandles;

/**
 * The dialog for editing connections.  Embeds the custom UI contributed by an annotated UI provider.
 * Besides the Connect / Disconnect options, also provides the ability to edit an existing connection,
 * should it be supported by the plugin.
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection"})
@PseudoBean(singletonObserver = true)
public class EditConnectionDialog extends AStandardDialog implements IConnectionManagerListener, IUserInputValidator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CONNECT_BUTTON_TEXT = "Connect";
    private static final String DISCONNECT_BUTTON_TEXT = "Disconnect";
    private static final String CONNECTING_BUTTON_TEXT = "Abort Connection";

    private final IComposedConnectionController controller;
    private final IConnectionPluginController pluginController;
    private IConnectionPanel connectionPanel;

    private boolean editable = false;

    @Inject
    private SwingUIBinder swingUIBinder;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    @Inject
    private IConnectionManager connectionManager;

    public EditConnectionDialog(IComposedConnectionController controller)
    {
        this.controller = controller;
        pluginController = controller.getPluginConnectionController();
    }

    @PostConstruct
    private void postConstruct()
    {
        IConnectionSwingUIProvider uiProvider = swingUIBinder.forConnection(controller);

        // Ensure that the panel has been set on the connection controller. This might not have happened if the
        // connection was created from a pre-configured connection definition and it indicated to NOT show the
        // connection UI.

        connectionPanel = uiProvider.getConnectionPanel();

        if (pluginController instanceof IConnectionSwingUIConsumer uiConsumer) uiConsumer.setConnectionPanel(connectionPanel);

        // It is possible the UI provider did not return a connection panel, so make sure we check for that case so a
        // useful error can be logged. Note: this will also occur if the UI provider is a stub (noop proxy),
        // but this case should be handled in ConnectionManager before this class is ever instantiated.
        if (connectionPanel == null)
        {
            logger.error("{} did not provide a connection panel.", uiProvider.getClass().getSimpleName());
            return;
        }

        initComponents();

        final String title = connectionPanel.getTitle();
        setTitle(title != null ? title : "Connection");

        final JPanel actualPanel = connectionPanel.getPanel();
        contentPanel.add(actualPanel, new CellConstraints(1, 1));

        // Watch for any changes on the panel components
        GenericListener.attachTo(actualPanel, this);

        connectionPluginRegistry.getPluginDefinition(pluginController.getFamily(), pluginController.getType())
                .ifPresent(iConnectionPluginDefinition -> editable = iConnectionPluginDefinition.isEditSupported());

        updateButton.setVisible(editable);

        final ConnectionState state = controller.getState();
        updateButton.setEnabled(state == ConnectionState.Connected);  // this accounts for restored connections not yet connected
        if (state == ConnectionState.Disconnected) validateUserInput();
    }

    /**
     * Override JDialog setVisible to ensure listeners are active and button text up to date when opened
     *
     * @param visible true to set the dialog to visible and false to close it
     * @since 2.36
     */
    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            addListeners();
            setButtonText();
            pack();
        }

        super.setVisible(visible);
    }

    /**
     * Add the listeners when the dialog is built or reopened.
     *
     * @since 2.36
     */
    public void addListeners()
    {
        controller.addConnectionStateListener(this);
    }

    /**
     * Cleanup when the project is closing.
     *
     * @since 2.37
     */
    private void onProjectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        dispose();
    }

    /**
     * Listener for when a connection disconnects
     *
     * @param connectionController Controller for RO that had a connection state change.
     * @param connectionState      New connection state of the RO
     * @param message              Message associated with that state change
     */
    @Override
    public void onConnectionStateChanged(IRaptorObject connectionController, ConnectionState connectionState, String message)
    {
        setButtonText();
    }

    /**
     * Set the button to show connect or disconnect based on the current state from the controller
     *
     * @since 2.36
     */
    public void setButtonText()
    {
        ConnectionState connectionState = controller.getState();

        if (connectionState == ConnectionState.Disconnected)
        {
            updateButton.setEnabled(false);
            okButton.setText(CONNECT_BUTTON_TEXT);
        } else if (connectionState == ConnectionState.Connected)
        {
            okButton.setText(DISCONNECT_BUTTON_TEXT);
        } else if (connectionState == ConnectionState.Connecting)
        {
            okButton.setText(CONNECTING_BUTTON_TEXT);
        }
    }

    @Override
    public void onConnectionAdded(IRaptorObject connectionController, String displayName, ConnectionState connectionState)
    {
        setButtonText();
    }

    @Override
    public void onConnectionAdded(String key, String displayName, String iconPath, ConnectionState connectionState, Action action)
    {
        setButtonText();
    }

    @Override
    public void onConnectionRemoved(IRaptorObject connectionController)
    {
        setButtonText();
    }

    @Override
    public void onConnectionRemoved(String key)
    {
        setButtonText();
    }

    @Override
    public void onConnectionNameChanged(IRaptorObject connectionController, String displayName)
    {
        setButtonText();
    }

    public void shutdown()
    {
        dispose();
    }

    /**
     * Operator closed this dialog without changing anything regarding this connection.
     */
    private void closeButtonActionPerformed()
    {
        dispose();
    }

    @Override
    public boolean validateUserInput()
    {
        String errorMessage = connectionPanel.validateContents();
        boolean valid = errorMessage == null;

        okButton.setEnabled(valid);
        textAreaErrorMessages.setText(errorMessage);
        scrollPaneErrorMessages.setVisible(!valid);

        pack();
        return valid;
    }

    /**
     * Operator selected to either "Connect" or "Disconnect" depending on the current state of the connection.  The
     * opening of the connection follows the same basic logic as defined in the {@code IConnectionManager}
     * after the {@code IComposedConnectionController} has been constructed.  In this case, we already have
     * that object, so simply calls the {@code IConnectionManager} method with that controller.
     */
    private void okButtonActionPerformed()
    {
        if (okButton.getText().equals(CONNECT_BUTTON_TEXT))
        {
            // Toggle from DISCONNECTED to CONNECTED state
            logger.trace("toggle connection from DISCONNECTED to CONNECTED state");

            connectionManager.openConnection(controller);

            if (editable) updateButton.setEnabled(true);
        } else
        {
            // Toggle from CONNECT to DISCONNECT state
            String displayName = controller.getConnectionDisplayName();

            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to disconnect from the " + displayName + " connection?",
                    "Disconnect",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION)
            {
                // Notify the plugin controller to disconnect the connection
                controller.getPluginConnectionController().onCloseConnection();

                if (editable) updateButton.setEnabled(false);
            }
        }

        setButtonText();
        dispose();
    }

    /**
     * Operator has edited the (editable) connection parameters and clicked on the "Update" button, make the callback.
     */
    private void updateButtonActionPerformed()
    {
        // Notify the plugin controller to reconfigure the connection
        controller.getPluginConnectionController().onUpdateConnection();

        dispose();
    }

    /**
     * Callback to complete custom close actions when the dialog is disposed.
     *
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        connectionManager.editDialogClosed(controller.getUniqueKey());
        controller.removeConnectionStateListener(this);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPaneErrorMessages = new JScrollPane();
        textAreaErrorMessages = new JTextArea();
        var buttonBar = new JPanel();
        updateButton = new JButton();
        okButton = new JButton();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

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
                        "2*(default, $lgap), default"));

                //======== scrollPaneErrorMessages ========
                {
                    scrollPaneErrorMessages.setVisible(false);
                    scrollPaneErrorMessages.setName("scrollPaneErrorMessages");

                    //---- textAreaErrorMessages ----
                    textAreaErrorMessages.setEnabled(false);
                    textAreaErrorMessages.setEditable(false);
                    textAreaErrorMessages.setLineWrap(true);
                    textAreaErrorMessages.setName("textAreaErrorMessages");
                    scrollPaneErrorMessages.setViewportView(textAreaErrorMessages);
                }
                contentPanel.add(scrollPaneErrorMessages, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, 2*([80px,pref], $lcgap), [80px,pref]",
                        "fill:pref"));

                //---- updateButton ----
                updateButton.setText("Update");
                updateButton.setName("updateButton");
                updateButton.addActionListener(e -> updateButtonActionPerformed());
                buttonBar.add(updateButton, cc.xy(2, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(4, 1));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    private JScrollPane scrollPaneErrorMessages;
    private JTextArea textAreaErrorMessages;
    private JButton updateButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
