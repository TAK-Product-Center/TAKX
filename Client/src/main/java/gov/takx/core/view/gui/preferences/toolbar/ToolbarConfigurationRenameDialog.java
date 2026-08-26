package gov.takx.core.view.gui.preferences.toolbar;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Dialog;

/**
 * Dialog to rename an element in a toolbar configuration.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationRenameDialog extends AStandardDialog
{
    private final IToolbarConfigurationRenameListener listener;

    public ToolbarConfigurationRenameDialog(@Nullable Dialog owner, @Nonnull String name, @Nonnull IToolbarConfigurationRenameListener listener)
    {
        super(owner);
        this.listener = listener;
        initComponents();
        nameTextField.setText(name);
        nameTextField.setSelectionStart(0);
        nameTextField.setSelectionEnd(name.length());
    }

    private void okButtonActionPerformed()
    {
        listener.updateName(nameTextField.getText());
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var displayNameLabel = new JLabel();
        nameTextField = new JTextField();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Rename");
        setAlwaysOnTop(true);
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
                        "fill:default"));

                //---- displayNameLabel ----
                displayNameLabel.setText("Display name:");
                displayNameLabel.setName("displayNameLabel");
                contentPanel.add(displayNameLabel, cc.xy(1, 1));

                //---- nameTextField ----
                nameTextField.setName("nameTextField");
                contentPanel.add(nameTextField, cc.xy(3, 1));
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
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField nameTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
