package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;

/**
 */
public class PackageExtractionLocationDialog extends AStandardDialog
{
    private static final String DESCRIPTION_TEXT = "<html>Please select the directory where you would like to " +
            "extract the package's project data.</html>";

    public PackageExtractionLocationDialog(Frame owner, String defaultPath)
    {
        super(owner);
        initComponents();
        dialogHeaderPanel.setDescription(DESCRIPTION_TEXT);
        dialogHeaderPanel.setIcon("folder_open.png");
        fileChooser.setCurrentDirectory(new File(defaultPath));
        ComponentUtils.removeMinMaxClose(this);
    }

    public String showDialog()
    {
        setVisible(true);
        return fileChooser.getCurrentDirectory().getAbsolutePath();
    }

    private void okButtonActionPerformed()
    {
        setVisible(false);
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        var contentPanel = new JPanel();
        fileChooser = new JFileChooser();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Project Extraction Location");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //---- fileChooser ----
                fileChooser.setControlButtonsAreShown(false);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setName("fileChooser");
                contentPanel.add(fileChooser, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button",
                        "default"));

                //---- okButton ----
                okButton.setText("Select Directory");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private DialogHeaderPanel dialogHeaderPanel;
    private JFileChooser fileChooser;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
