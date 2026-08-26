package gov.takx.commons.swing;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.file.IFileProgress;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.BorderLayout;
import java.awt.Window;

/**
 * A simple status dialog that presents a progress bar with labels above and below.  The
 * label above is typically used to present the overall task description and the label
 * below the progress bar is typically for progress related information (like a sub-task,
 * estimated time to completion, etc.).
 * <p/>
 * This dialog implements the {@link IFileProgress} interface
 * so it can be used with operations that generate events using that interface.
 *
 */
public class StatusDialog extends AStandardDialog implements IFileProgress
{
    /**
     * Create modal status dialog from frame owner.
     *
     * @param owner         Frame parent instance
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     * @param modal         true for a modal dialog
     */
    @SuppressWarnings("unused") // used by plugins
    public StatusDialog(Window owner, boolean indeterminate, boolean modal)
    {
        this(owner, indeterminate);
        setModal(modal);
    }

    /**
     * Create modal status dialog from frame owner.
     *
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     * @param modal         true for a modal dialog
     * @since 3.3
     */
    public StatusDialog(boolean indeterminate, boolean modal)
    {
        initComponents();

        setModal(modal);

        progressBar.setIndeterminate(indeterminate);
    }

    /**
     * Create status dialog from frame owner.
     *
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     * @since 3.3
     */
    public StatusDialog(boolean indeterminate)
    {
        this(indeterminate, false);
    }

    /**
     * Create status dialog from dialog owner.
     *
     * @param owner         Dialog parent instance
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     */
    public StatusDialog(Window owner, boolean indeterminate)
    {
        super(owner);
        initComponents();

        progressBar.setIndeterminate(indeterminate);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Set the text above the progress bar to display description of task
     *
     * @param text String value to display above progress bar
     */
    @InvokeOnEDT
    public void setStatusText(final String text)
    {
        statusText.setText(text);
    }

    /**
     * Set the text below the progress bar to show progress (ie. percent done, time remaining, etc)
     *
     * @param text String value to display below progress bar
     */
    @InvokeOnEDT
    public void setProgressText(final String text)
    {
        progressText.setText(text);
    }

    /**
     * Set the dialog title, default is "Progress"
     *
     * @param title Dialog title
     */
    @InvokeOnEDT
    public void setDialogTitle(final String title)
    {
        super.setTitle(title);
    }

    /**
     * Set the bounds (min/max) of the progress bar.
     *
     * @param min Min value for progress bar
     * @param max Max value for progress bar
     */
    @InvokeOnEDT
    public void setProgressBounds(final int min, final int max)
    {
        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
    }

    /**
     * Set the value of the progress bar.
     *
     * @param value New value (must be between min and max values set previously)
     */
    @InvokeAndWaitOnEDT // TODO KMB: Not sure what it was calling invoke and wait in the boilerplate, so kept it for now
    public void setProgressValue(final int value)
    {
        progressBar.setValue(value);
    }

    @InvokeOnEDT
    @Override
    public void setFileMaximum(final int maximum)
    {
        progressBar.setMaximum(maximum);
    }

    @InvokeOnEDT
    @Override
    public void setFileMinimum(final int minimum)
    {
        progressBar.setMinimum(minimum);
    }

    @Override
    public void setFileProgress(final int progress)
    {
        setProgressValue(progress);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        statusText = new JLabel();
        progressBar = new JProgressBar();
        progressText = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Progress");
        setModal(false);
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
                        "2*(default, $lgap), default"));

                //---- statusText ----
                statusText.setText("Loading...");
                statusText.setName("statusText");
                contentPanel.add(statusText, cc.xy(1, 1));

                //---- progressBar ----
                progressBar.setIndeterminate(true);
                progressBar.setName("progressBar");
                contentPanel.add(progressBar, cc.xy(1, 3));

                //---- progressText ----
                progressText.setName("progressText");
                contentPanel.add(progressText, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel statusText;
    private JProgressBar progressBar;
    private JLabel progressText;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
