package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contour.ContourProgressListener;
import gov.takx.api.annotation.InvokeOnEDT;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Progress display for in-progress contour generation.
 *
 * @since 5.8
 */
public class ContourProgressPanel extends JPanel implements ContourProgressListener
{
    private final AtomicBoolean cancellation;

    /**
     * @param cancellation Contour generation cancellation handle.
     */
    public ContourProgressPanel(@Nonnull AtomicBoolean cancellation)
    {
        this.cancellation = cancellation;
        initComponents();
    }

    @Override
    @InvokeOnEDT
    public void onProgress(int percent)
    {
        if (progressBar.isIndeterminate()) progressBar.setIndeterminate(false);

        progressBar.setValue(percent);

        // The progress bar is initially indeterminate until the user finishes drawing the area they want.
        // So once its actually reporting progress and the process has started we'll let the user cancel.
        cancelButton.setEnabled(!progressBar.isIndeterminate());
    }

    @Override
    @InvokeOnEDT
    public void onStatusMessage(String message)
    {
        // Wrap with HTML to prevent this panel from force-expanding the parent.
        // Setting a max/pref size doesn't work out of the box with our layout sadly.
        statusLabel.setText("<html>" + message + "</html>");
    }

    private void onCancel()
    {
        cancelButton.setEnabled(false);
        cancellation.set(true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        progressBar = new JProgressBar();
        statusLabel = new JLabel();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, default",
                "default, $lgap, default"));

        //---- progressBar ----
        progressBar.setIndeterminate(true);
        progressBar.setName("progressBar");
        add(progressBar, cc.xywh(1, 1, 3, 1));

        //---- statusLabel ----
        statusLabel.setText("Drawing in progress...");
        statusLabel.setName("statusLabel");
        add(statusLabel, cc.xy(1, 3));

        //---- cancelButton ----
        cancelButton.setText("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(e -> onCancel());
        add(cancelButton, cc.xy(3, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
