package gov.takx.core.view.gui.data.kml;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;

import java.awt.BorderLayout;

/**
 * Simple dialog in which to display a tree view of all things exportable on the globe.  Tree is of course
 * passed in the constructor.
 *
 */
public class KmlExportDialog extends AStandardDialog
{
    private boolean okay = false;

    protected KmlExportDialog(JTree theTree)
    {
        initComponents();

        CellConstraints cc = new CellConstraints();
        JScrollPane scrollPanel = new JScrollPane();
        scrollPanel.setViewportView(theTree);
        contentPanel.add(scrollPanel, cc.xy(1, 3));
        enableExport(false);    // note, if we change to selecting all to start with, remove this.
    }

    public boolean isOkay()
    {
        return okay;
    }

    public void enableExport(boolean export)
    {
        okButton.setEnabled(export);
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
        contentPanel = new JPanel();
        var label1 = new JLabel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModal(true);
        setTitle("Export KML/KMZ Data");
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
                        "default, $lgap, default"));

                //---- label1 ----
                label1.setText("Select Objects to Export to KML/KMZ");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));
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
                okButton.setText("Export");
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
    private JPanel contentPanel;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
