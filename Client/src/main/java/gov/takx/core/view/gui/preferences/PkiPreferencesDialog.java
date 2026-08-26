package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dialog;

/**
 * Dialog for allowing the user to choose a specific PKI cert.
 *
 * @see PkiPreferencesPanel
 */
public class PkiPreferencesDialog extends AStandardDialog
{
    private final PkiPreferencesPanel pkiPreferencesPanel = new PkiPreferencesPanel(null);
    private boolean okay = false;

    /**
     * Constructor.  Add the PkiPreferencesPanel to a containing panel.
     */
    public PkiPreferencesDialog()
    {
        initComponents();

        CellConstraints cc = new CellConstraints();
        pkiPanel.add(pkiPreferencesPanel, cc.xy(1, 1));
    }

    public boolean isOkay()
    {
        return okay;
    }

    public String getSelectedKeyEntry()
    {
        return pkiPreferencesPanel.getSelectedKeyEntry();
    }

    private void okButtonActionPerformed()
    {
        okay = true;
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
        var label1 = new JLabel();
        pkiPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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
                        "default, $rgap, fill:default:grow"));

                //---- label1 ----
                label1.setText("Select PKI Key, if needed:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== pkiPanel ========
                {
                    pkiPanel.setName("pkiPanel");
                    pkiPanel.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow"));
                }
                contentPanel.add(pkiPanel, cc.xy(1, 3));
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
    private JPanel pkiPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
