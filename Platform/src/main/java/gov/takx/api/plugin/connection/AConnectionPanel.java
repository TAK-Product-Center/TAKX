package gov.takx.api.plugin.connection;

import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.StatusDialog;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.net.URL;
import java.util.Map;

/**
 * Abstract class for Connection Panel.  Collects some of the common functionality of
 * many of the panels, i.e. showing a status dialog, displaying messages.
 *
 */
public abstract class AConnectionPanel extends JPanel implements IConnectionPanel
{
    private final String panelTitle;
    private StatusDialog statusDialog;

    public AConnectionPanel(String title)
    {
        panelTitle = title;
    }

    @Override
    public JPanel getPanel()
    {
        return this;
    }

    @Override
    public String getTitle()
    {
        return panelTitle;
    }

    @Override
    public URL getIcon()
    {
        return null;
    }

    public void destroy()
    {
        hideStatusDialog();
    }

    public void setConfiguration(Map<String, Object> configuration)
    {

    }

    /**
     * Show a connection success dialog
     *
     * @param title title string for dialog
     * @param text  text string to display in dialog
     */
    public void showInfoMessage(final String title, final String text)
    {
        hideStatusDialog();

        // Put UI updates onto the UI thread (EDT)
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                AlwaysOnTopJOptionPane.showMessageDialog(getParentDialog(), text,
                        title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    /**
     * Show a connection success dialog
     *
     * @param text text string to show in error dialog
     */
    public void showErrorMessage(final String text)
    {
        hideStatusDialog();

        // Put UI updates onto the UI thread (EDT)
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                AlwaysOnTopJOptionPane.showMessageDialog(getParentDialog(), text,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    protected void showStatusDialog(final String text)
    {
        // Put UI updates onto the UI thread (EDT)
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (statusDialog == null)
                {
                    statusDialog = new StatusDialog((JDialog) getParentDialog(), true);
                }

                statusDialog.setStatusText(text);

                if (!statusDialog.isVisible())
                {
                    statusDialog.setVisible(true);
                    statusDialog.repaint();
                }
            }
        });
    }

    protected void hideStatusDialog()
    {
        // Put UI updates onto the UI thread (EDT)
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (statusDialog != null)
                {
                    statusDialog.setVisible(false);
                    statusDialog = null;
                }
            }
        });
    }

    private Component getParentDialog()
    {
        Component component = this.getParent();
        while (component != null && (!(component instanceof JDialog)))
        {
            component = component.getParent();
        }
        return component;
    }
}
