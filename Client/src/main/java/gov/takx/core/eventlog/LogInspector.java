package gov.takx.core.eventlog;

import gov.takx.core.eventlog.gui.LogInspectorDialog;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Manager to create/show the log inspector dialog.
 *
 * @see LogInspectorDialog
 * @since 3.2
 */
@ApplicationScoped
public class LogInspector
{
    private LogInspectorDialog dialog = null;

    LogInspector()
    {
    }

    /**
     * Show (or bring to front) the log inspector dialog.
     */
    public void showLog()
    {
        if (dialog != null)
        {
            dialog.toFront();
        } else
        {
            dialog = new LogInspectorDialog();
            dialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    dialog = null;
                }
            });
            dialog.setVisible(true);
        }
    }
}
