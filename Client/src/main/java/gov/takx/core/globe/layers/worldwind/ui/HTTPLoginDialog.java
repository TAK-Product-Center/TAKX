package gov.takx.core.globe.layers.worldwind.ui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.layers.worldwind.HTTPPasswordManager;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @since 1.43
 */
public class HTTPLoginDialog extends AStandardDialog
{
    private boolean canceled = false;

    public HTTPLoginDialog(JDialog owner)
    {
        super(owner != null ? owner : JFrame.getFrames()[0]);
        initComponents();

        // Listen for ENTER-key typed; apply changes and close dialog.
        KeyAdapter keyAdapter = new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) applyAndClose();
            }
        };

        urlField.addKeyListener(keyAdapter);
        userField.addKeyListener(keyAdapter);
        passField.addKeyListener(keyAdapter);
        this.addKeyListener(keyAdapter);

        setSiteEditable(false);
    }

    public void update(String url)
    {
        setText(urlField, url);
    }

    public void setSiteEditable(boolean editable)
    {
        urlField.setEditable(editable);
        urlField.setFocusable(editable);
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible)
        {
            if (urlField.isEditable())
            {
                urlField.requestFocusInWindow();
            } else
            {
                userField.requestFocusInWindow();
            }
        }
        super.setVisible(visible);
    }

    public String getUsername()
    {
        return userField.getText();
    }

    public String getPassword()
    {
        return new String(passField.getPassword());
    }

    public String getURL()
    {
        return urlField.getText();
    }

    public boolean isCanceled()
    {
        return canceled;
    }

    public boolean isDoNotAskAgain()
    {
        return notAgainCheckBox.isSelected();
    }

    private void setText(JTextField textField, String text)
    {
        if (text == null || text.isEmpty())
        {
            text = "N/A";
        }
        textField.setText(text);
    }

    private void okButtonActionPerformed()
    {
        applyAndClose();
    }

    private void applyAndClose()
    {
        String url = urlField.getText();

        if (url.isEmpty())
        {
            AlwaysOnTopJOptionPane.showErrorDialog(this, "<html>Please provide a site URL.</html>", "Empty URL");
            return;
        }

        // Add new authorization info to the password manager
        HTTPPasswordManager.getInstance().setAuthentication(url, getUsername(), getPassword());

        // If the user doesn't want to be prompted again for this site's login information, add it to the set of
        // validated sites.
        if (isDoNotAskAgain())
        {
            HTTPPasswordManager.getInstance().setSiteValidated(url, true);
        }

        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        userField.setText("");
        passField.setText("");
        canceled = true;
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var panel1 = new JPanel();
        var label1 = new JLabel();
        var panel2 = new JPanel();
        var urlLabel = new JLabel();
        urlField = new JTextField();
        var separator1 = new JSeparator();
        var userLabel = new JLabel();
        userField = new JTextField();
        var passLabel = new JLabel();
        passField = new JPasswordField();
        var buttonBar = new JPanel();
        notAgainCheckBox = new JCheckBox();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("HTTP Login");
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
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
                        "default:grow"));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default"));

                    //---- label1 ----
                    label1.setText("<html>Please provide login information for the following site:</html>");
                    label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.BOLD));
                    label1.setName("label1");
                    panel1.add(label1, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                    //======== panel2 ========
                    {
                        panel2.setName("panel2");
                        panel2.setLayout(new FormLayout(
                                "default, $lcgap, default:grow",
                                "3*(default, $lgap), default"));

                        //---- urlLabel ----
                        urlLabel.setText("URL:");
                        urlLabel.setName("urlLabel");
                        panel2.add(urlLabel, cc.xy(1, 1));

                        //---- urlField ----
                        urlField.setName("urlField");
                        panel2.add(urlField, cc.xy(3, 1));

                        //---- separator1 ----
                        separator1.setName("separator1");
                        panel2.add(separator1, cc.xywh(1, 3, 3, 1));

                        //---- userLabel ----
                        userLabel.setText("Username: ");
                        userLabel.setName("userLabel");
                        panel2.add(userLabel, cc.xy(1, 5));

                        //---- userField ----
                        userField.setName("userField");
                        panel2.add(userField, cc.xy(3, 5));

                        //---- passLabel ----
                        passLabel.setText("Password: ");
                        passLabel.setName("passLabel");
                        panel2.add(passLabel, cc.xy(1, 7));

                        //---- passField ----
                        passField.setName("passField");
                        panel2.add(passField, cc.xy(3, 7));
                    }
                    panel1.add(panel2, cc.xy(1, 3));
                }
                contentPanel.add(panel1, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.TOP));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default, $glue, $button, $rgap, $button",
                        "pref"));

                //---- notAgainCheckBox ----
                notAgainCheckBox.setText("Do Not Ask Again");
                notAgainCheckBox.setName("notAgainCheckBox");
                buttonBar.add(notAgainCheckBox, cc.xy(1, 1));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(3, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(5, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField urlField;
    private JTextField userField;
    private JPasswordField passField;
    private JCheckBox notAgainCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
