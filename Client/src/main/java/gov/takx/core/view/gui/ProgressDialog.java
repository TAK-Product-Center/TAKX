package gov.takx.core.view.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;

/**
 */
public class ProgressDialog extends AStandardDialog implements IProgressMonitor
{
    private Runnable cancelAction;

    public ProgressDialog(Window owner)
    {
        super(owner);
        initComponents();

        cancelButton.setVisible(false);
        ComponentUtils.removeMinMaxClose(this); // Removes the close button from the frame.
    }

    /**
     * Show a "cancel" button at the bottom of the dialog.
     * 
     * @param cancelAction The action to run when the cancel button is clicked
     * @since 5.8
     */
    public void showCancelButton(@Nonnull Runnable cancelAction)
    {
        this.cancelAction = cancelAction;
        cancelButton.setVisible(true);
    }

    @Override
    @InvokeOnEDT
    public void setSubstatusMaximum(final int maximum)
    {
        if (maximum == INDETERMINATE)
        {
            progressBarSubtask.setVisible(true);
            progressBarSubtask.setIndeterminate(true);
        } else if (maximum == INVISIBLE)
        {
            progressBarSubtask.setVisible(false);
        } else
        {
            progressBarSubtask.setVisible(true);
            progressBarSubtask.setIndeterminate(false);
            progressBarSubtask.setValue(0);
            progressBarSubtask.setMaximum(maximum);
            progressBarSubtask.invalidate();
        }

        pack();
    }

    @Override
    @InvokeOnEDT
    public void incrementSubstatus()
    {
        progressBarSubtask.setValue(progressBarSubtask.getValue() + 1);
        repaint();
    }

    @Override
    @InvokeOnEDT
    public void setMaximum(final int maximum)
    {
        if (maximum < 0)
        {
            primaryProgressBar.setIndeterminate(true);
        } else
        {
            primaryProgressBar.setIndeterminate(false);
            primaryProgressBar.setValue(0);
            primaryProgressBar.setMaximum(maximum);
            primaryProgressBar.invalidate();
        }
    }

    @Override
    @InvokeAndWaitOnEDT
    public void increment()
    {
        primaryProgressBar.setValue(primaryProgressBar.getValue() + 1);
        repaint();
    }

    @Override
    @InvokeOnEDT
    public void setVisible(final boolean b)
    {
        boolean wasVisible = isVisible();

        super.setVisible(b);

        if (b) // If dialog was disposed, and then set visible again we need to remove the "X" again.
        {
            ComponentUtils.removeMinMaxClose(this);

            if (!wasVisible)
            {
                setLocationRelativeTo(getParent());
            }
        } else
        {
            // Always revert the dialog title when hiding.
            setTitle("Session Manager");
        }
    }

    @Override
    @InvokeOnEDT
    public void dispose()
    {
        super.dispose();
    }

    @Override
    @InvokeOnEDT
    public void setText(final String text)
    {
        statusLabel.setText(text);
        substatusLabel.setText("");
        progressBarSubtask.setVisible(false);
        pack();
    }

    @Override
    @InvokeOnEDT
    public void setSubstatus(final String text)
    {
        substatusLabel.setText(text);
        repaint();
    }

    @InvokeAndWaitOnEDT
    public void setValue(final int v)
    {
        primaryProgressBar.setIndeterminate(false);
        primaryProgressBar.setValue(v);
        repaint();
    }

    private void cancel()
    {
        cancelAction.run();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        primaryProgressBar = new JProgressBar();
        statusLabel = new JLabel();
        progressBarSubtask = new JProgressBar();
        substatusLabel = new JLabel();
        var panel1 = new JPanel();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModal(true);
        setTitle("Status");
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
                        "default:grow, $lcgap, default:grow",
                        "2*(default, $lgap), default, $glue, default"));

                //---- primaryProgressBar ----
                primaryProgressBar.setName("primaryProgressBar");
                contentPanel.add(primaryProgressBar, cc.xywh(1, 1, 3, 1));

                //---- statusLabel ----
                statusLabel.setText("text");
                statusLabel.setFont(statusLabel.getFont().deriveFont(statusLabel.getFont().getStyle() | Font.BOLD));
                statusLabel.setName("statusLabel");
                contentPanel.add(statusLabel, cc.xy(1, 3));

                //---- progressBarSubtask ----
                progressBarSubtask.setVisible(false);
                progressBarSubtask.setName("progressBarSubtask");
                contentPanel.add(progressBarSubtask, cc.xy(3, 3));

                //---- substatusLabel ----
                substatusLabel.setText("yahoo");
                substatusLabel.setName("substatusLabel");
                contentPanel.add(substatusLabel, cc.xywh(1, 5, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== panel1 ========
            {
                panel1.setName("panel1");
                panel1.setLayout(new FormLayout(
                        "$glue, right:pref",
                        "default"));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
                panel1.add(cancelButton, cc.xy(2, 1));
            }
            dialogPane.add(panel1, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JProgressBar primaryProgressBar;
    private JLabel statusLabel;
    private JProgressBar progressBarSubtask;
    private JLabel substatusLabel;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
