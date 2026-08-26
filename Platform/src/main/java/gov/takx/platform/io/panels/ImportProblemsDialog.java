package gov.takx.platform.io.panels;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Frame;

/**
 * A dialog to inform the user about problems in an import
 * operation. The dialog displays a message and asks if the user wants
 * to abort the import.
 *
 */
public class ImportProblemsDialog extends AStandardDialog
{
    private boolean abort = false;

    /**
     * Create the dialog.
     *
     * @param owner   Component that owns the dialog.
     * @param message Message to display.
     */
    public ImportProblemsDialog(Frame owner, String message)
    {
        super(owner);
        setModal(true);
        initComponents(message);
    }

    /**
     * Should the import continue?
     */
    public boolean continueImport()
    {
        return !abort;
    }

    private void yesButtonActionPerformed()
    {
        abort = false;
        dispose();
    }

    private void noButtonActionPerformed()
    {
        abort = true;
        dispose();
    }

    private void initComponents(String message)
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        var scrollPane1 = new JScrollPane();
        var errorTextArea = new JTextArea();
        var label2 = new JLabel();
        var buttonBar = new JPanel();
        var yesButton = new JButton();
        var noButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Problem encountered");
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
                        "default, $lgap, fill:default:grow, $lgap, default"));

                //---- label1 ----
                label1.setText("Problems were encountered validating file:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- errorTextArea ----
                    errorTextArea.setRows(10);
                    errorTextArea.setEditable(false);
                    errorTextArea.setWrapStyleWord(true);
                    errorTextArea.setLineWrap(true);
                    errorTextArea.setName("errorTextArea");
                    errorTextArea.setText(message);
                    scrollPane1.setViewportView(errorTextArea);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 3));

                //---- label2 ----
                label2.setText("Do you want to ignore these points, and import the rest of the data?");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- yesButton ----
                yesButton.setText("Yes");
                yesButton.setName("yesButton");
                yesButton.addActionListener(e -> yesButtonActionPerformed());
                buttonBar.add(yesButton, cc.xy(2, 1));

                //---- noButton ----
                noButton.setText("No");
                noButton.setName("noButton");
                noButton.addActionListener(e -> noButtonActionPerformed());
                buttonBar.add(noButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
