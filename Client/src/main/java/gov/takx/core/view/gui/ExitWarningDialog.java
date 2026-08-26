package gov.takx.core.view.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.platform.ui.AStandardDialog;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.util.List;
import java.util.Map;

/**
 * Dialog to warn the user that background operations are in progress, and ask if the user still wants to exit.
 */
public class ExitWarningDialog extends AStandardDialog
{
    private boolean confirmExit;

    public ExitWarningDialog(Map<TaskGroup, List<String>> msgMap)
    {
        initComponents();
        init(msgMap);
    }

    private void init(Map<TaskGroup, List<String>> msgMap)
    {
        // Loop through and create a sub-panel for each task group
        msgMap.forEach((taskGroup, messages) -> makeMessagePanel(taskGroup.getDisplayName(), messages));
    }

    /**
     * Make a sub-panel for the given task group showing all the messages for that group.
     *
     * @param title    Title for panel border
     * @param messages Messages to show
     */
    private void makeMessagePanel(String title, List<String> messages)
    {
        final JPanel panel = new JPanel();
        panel.setBorder(new CompoundBorder(new TitledBorder(title), new EmptyBorder(2, 0, 2, 0)));
        panel.setLayout(new VerticalLayout(3));

        for (String message : messages)
        {
            panel.add(new JLabel(message));
        }

        pnlMessages.add(panel);
    }

    private void exitButtonActionPerformed()
    {
        confirmExit = true;
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        confirmExit = false;
        dispose();
    }

    public boolean getConfirmExit()
    {
        return confirmExit;
    }

    /**
     * Callback for when the user closed the dialog using the X button.
     * 
     * @since 3.3
     */
    @Override
    protected void onClosedByXButton()
    {
        cancelButtonActionPerformed();
    }

    /**
     * Hitting the escape button with this dialog in focus is effectively hitting the cancel button.
     */
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
        var label11 = new JLabel();
        pnlMessages = new JPanel();
        var label10 = new JLabel();
        var buttonBar = new JPanel();
        var exitButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Confirm Exit");
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
                        "default, $lgap, default:grow, $lgap, default"));

                //---- label11 ----
                label11.setText("Background operations are still in progress. Are you sure you want to exit?");
                label11.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
                label11.setName("label11");
                contentPanel.add(label11, cc.xy(1, 1));

                //======== pnlMessages ========
                {
                    pnlMessages.setName("pnlMessages");
                    pnlMessages.setLayout(new VerticalLayout(5));
                }
                contentPanel.add(pnlMessages, cc.xy(1, 3));

                //---- label10 ----
                label10.setText("Really exit?");
                label10.setFont(label10.getFont().deriveFont(label10.getFont().getStyle() | Font.BOLD));
                label10.setName("label10");
                contentPanel.add(label10, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- exitButton ----
                exitButton.setText("Exit");
                exitButton.setName("exitButton");
                exitButton.addActionListener(e -> exitButtonActionPerformed());
                buttonBar.add(exitButton, cc.xy(2, 1));

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
    private JPanel pnlMessages;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
