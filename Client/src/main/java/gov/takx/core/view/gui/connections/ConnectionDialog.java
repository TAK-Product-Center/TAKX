package gov.takx.core.view.gui.connections;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.plugin.connection.IConnectionPanel;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ConnectionDialog extends AStandardDialog implements IUserInputValidator
{
    @Inject
    private IConnectionManager connectionManager;

    private final IComposedConnectionController controller;
    private final IConnectionPanel connectionPanel;

    public ConnectionDialog(IComposedConnectionController controller)
    {
        this(controller, new HashMap<>());
    }

    public ConnectionDialog(IComposedConnectionController controller, Map<String, Object> configuration)
    {
        initComponents();

        setModal(true);

        textAreaErrorMessages.setOpaque(false);
        scrollPaneErrorMessages.setVisible(false);

        CellConstraints cc = new CellConstraints();
        this.controller = controller;

        JPanel panel = null;

        connectionPanel = controller.getConnectionPanel();
        if (connectionPanel != null)
        {
            connectionPanel.setConfiguration(configuration);
            panel = connectionPanel.getPanel();
        }

        if (panel != null)
        {
            contentPanel.add(panel, cc.xy(1, 1));
            validateUserInput();
            scrollPaneErrorMessages.setVisible(false); // Don't show errors initially, just disable the connect button.
            setTitle("Connect via " + controller.getConnectionName());

            GenericListener.attachTo(panel, this);
        } else
        {
            contentPanel.add(errorPanel, cc.xy(1, 1));
            connectButton.setEnabled(false);
            setTitle(controller.getConnectionName() + " Connection Error");
        }

        ComponentUtils.centerComponentOnScreen(this);
    }

    @Override
    public boolean validateUserInput()
    {
        String errorMessage = connectionPanel.validateContents();
        boolean valid = errorMessage == null;

        connectButton.setEnabled(valid);
        textAreaErrorMessages.setText(errorMessage);
        scrollPaneErrorMessages.setVisible(!valid);

        pack();
        return valid;
    }

    private void cancelButtonActionPerformed()
    {
        connectionManager.removeConnection(controller.getUniqueKey(), false);
        dispose();
    }

    @Override
    protected void onClosedByXButton()
    {
        cancelButtonActionPerformed();
    }

    @Override
    protected void onEscape()
    {
        cancelButtonActionPerformed();
    }

    private void connectButtonActionPerformed()
    {
        if (validateUserInput())
        {
            connectionManager.addActiveConnection(controller);
            dispose();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPaneErrorMessages = new JScrollPane();
        textAreaErrorMessages = new JTextArea();
        var buttonBar = new JPanel();
        connectButton = new JButton();
        var cancelButton = new JButton();
        errorPanel = new JPanel();
        var errorLabel = new JLabel();
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
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- connectButton ----
                connectButton.setText("Connect");
                connectButton.setEnabled(false);
                connectButton.setName("connectButton");
                connectButton.addActionListener(e -> connectButtonActionPerformed());
                buttonBar.add(connectButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //======== errorPanel ========
        {
            errorPanel.setName("errorPanel");
            errorPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default:grow"));

            //---- errorLabel ----
            errorLabel.setText("Error: Connection plugin has no connection panel.");
            errorLabel.setName("errorLabel");
            errorPanel.add(errorLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    private JScrollPane scrollPaneErrorMessages;
    private JTextArea textAreaErrorMessages;
    private JButton connectButton;
    private JPanel errorPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
