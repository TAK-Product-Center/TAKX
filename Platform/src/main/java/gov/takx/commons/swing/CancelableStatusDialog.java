package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;

/**
 */
public class CancelableStatusDialog extends StatusDialog
{
    private final ICancelableTask cancelableTask;

    /**
     * Constructor.
     *
     * @param owner Dialog parent instance
     * @param task The cancellable task monitored by the dialog
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     */
    @SuppressWarnings("unused") // used by plugins
    public CancelableStatusDialog(Window owner, ICancelableTask task, boolean indeterminate)
    {
        super(owner, indeterminate);

        cancelableTask = task;
        init();
    }

    /**
     * Constructor.
     *
     * @param task The cancellable task monitored by the dialog
     * @param indeterminate true for indeterminate progress bar, false for determinate (percentage) bar
     * @since 3.3
     */
    public CancelableStatusDialog(ICancelableTask task, boolean indeterminate)
    {
        super(indeterminate);

        cancelableTask = task;
        init();
    }

    /**
     * Perform initialization common across the constructors.
     */
    private void init()
    {
        JPanel cancelButtonPanel = new JPanel();
        JButton cancelButton = new JButton("Cancel");
        Container contentPane = getContentPane();
        CellConstraints cc = new CellConstraints();

        cancelButtonPanel.setLayout(new FormLayout(
                "default, 187dlu, default",
                "$lgap, 16dlu, $lgap"));

        cancelButtonPanel.add(cancelButton, cc.xywh(2, 2, 1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        contentPane.add(cancelButtonPanel, BorderLayout.SOUTH);
        pack();

        cancelButton.addActionListener(e -> {
            if (cancelableTask != null) cancelableTask.onCancel();
            dispose();
        });
    }

    public static void main(String[] args)
    {
        CancelableStatusDialog dlg = new CancelableStatusDialog(null, true);
        dlg.setStatusText("This is status!");
        dlg.setProgressText("This is progress!");
        dlg.setVisible(true);
    }
}
