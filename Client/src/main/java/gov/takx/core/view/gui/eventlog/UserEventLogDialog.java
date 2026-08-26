package gov.takx.core.view.gui.eventlog;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;

/**
 */
public class UserEventLogDialog extends AStandardDialog
{
    private boolean logRequested = false;

    public UserEventLogDialog()
    {
        initComponents();
        textFieldMessage.setDocument(new LimitedDocument(255));
        textPaneDetails.setDocument(new LimitedDocument(255));
    }

    public String getMessage()
    {
        return textFieldMessage.getText();
    }

    public String getDetails()
    {
        return textPaneDetails.getText();
    }

    public boolean wasLogRequested()
    {
        return logRequested;
    }

    private void cancelButtonActionPerformed()
    {
        logRequested = false;
        dispose();
    }

    private void okButtonActionPerformed()
    {
        logRequested = true;
        dispose();
    }

    @Override
    protected void onEscape()
    {
        cancelButtonActionPerformed();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        textFieldMessage = new JTextField();
        var label2 = new JLabel();
        var scrollPane1 = new JScrollPane();
        textPaneDetails = new JTextPane();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Add Event");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("dialogUserEventLog");
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
                        "default, $lcgap, default",
                        "2*(default, $lgap), default:grow"));

                //---- label1 ----
                label1.setText("Message:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- textFieldMessage ----
                textFieldMessage.setName("textFieldMessage");
                contentPanel.add(textFieldMessage, cc.xy(3, 1));

                //---- label2 ----
                label2.setText("Details:");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 3));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- textPaneDetails ----
                    textPaneDetails.setName("textPaneDetails");
                    scrollPane1.setViewportView(textPaneDetails);
                }
                contentPanel.add(scrollPane1, cc.xywh(3, 3, 1, 3));
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
                okButton.setText("Log");
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
    private JTextField textFieldMessage;
    private JTextPane textPaneDetails;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
