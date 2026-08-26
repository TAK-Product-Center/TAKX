package gov.takx.commons.swing;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.plugin.connection.IDelegatedConnectionAuthentication;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import java.awt.BorderLayout;

/**
 * Authentication dialog used for IDelegatedConnectionAuthentication dialogs....
 *
 * @since 3.4 (moved from Client to Platform)
 */
public class AuthenticationDialog extends AStandardDialog
{
    private final IDelegatedConnectionAuthentication delegate;
    private final boolean passwordOnly;
    private boolean okButtonClicked = false;

    public AuthenticationDialog(@Nonnull IDelegatedConnectionAuthentication delegate)
    {
        super(JFrame.getFrames()[0]);
        this.delegate = delegate;
        passwordOnly = delegate.requiresPasswordOnly();

        initComponents();
        init();
    }

    public boolean isOkay()
    {
        return okButtonClicked;
    }

    private void init()
    {
        final String title = delegate.getTitle();
        if (null != title) setTitle(title);

        final String message = delegate.getMessage();
        if (null != message) labelMessage.setText(message);

        if (passwordOnly)
        {
            labelUsername.setVisible(false);
            textFieldUsername.setVisible(false);

            textFieldPassword.requestFocus();
        } else
        {
            String defaultUser = delegate.getDefaultUsername();
            if (null != defaultUser)
            {
                textFieldUsername.setText(defaultUser);
                textFieldPassword.requestFocus();
            } else
            {
                textFieldUsername.requestFocus();
            }
        }
    }

    private void okButtonActionPerformed()
    {
        okButtonClicked = true;

        if (!passwordOnly) delegate.setUsername(textFieldUsername.getText());
        delegate.setPassword(textFieldPassword.getPassword());

        String msg = delegate.validateContents();

        if (null != msg)
        {
            AlwaysOnTopJOptionPane.showMessageDialog(this, msg, "Authentication Error", JOptionPane.ERROR_MESSAGE);
            textFieldPassword.setText("");
            textFieldPassword.requestFocus();
        } else
        {
            dispose();
        }
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        labelMessage = new JLabel();
        labelUsername = new JLabel();
        textFieldUsername = new JTextField();
        var labelPassword = new JLabel();
        textFieldPassword = new JPasswordField();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Connection Credentials");
        setModal(true);
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
                        "default, $lcgap, default:grow",
                        "2*(default, $lgap), default"));

                //---- labelMessage ----
                labelMessage.setText("Please provide your credentials to establish this connection");
                labelMessage.setName("labelMessage");
                contentPanel.add(labelMessage, cc.xywh(1, 1, 3, 1));

                //---- labelUsername ----
                labelUsername.setText("Username:");
                labelUsername.setName("labelUsername");
                contentPanel.add(labelUsername, cc.xy(1, 3));

                //---- textFieldUsername ----
                textFieldUsername.setName("textFieldUsername");
                contentPanel.add(textFieldUsername, cc.xy(3, 3));

                //---- labelPassword ----
                labelPassword.setText("Password:");
                labelPassword.setName("labelPassword");
                contentPanel.add(labelPassword, cc.xy(1, 5));

                //---- textFieldPassword ----
                textFieldPassword.setName("textFieldPassword");
                contentPanel.add(textFieldPassword, cc.xy(3, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $lcgap, default",
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
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel labelMessage;
    private JLabel labelUsername;
    private JTextField textFieldUsername;
    private JPasswordField textFieldPassword;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
