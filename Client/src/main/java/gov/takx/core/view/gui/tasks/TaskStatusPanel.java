package gov.takx.core.view.gui.tasks;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.tasks.ICancelableManagedTask;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskCompletionStatus;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.TitledBorder;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * An instance of this panel is created for every background task currently executing.
 * It displays the latest progress data from the task.
 *
 * @since 1.43
 */
public class TaskStatusPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IManagedTask task;
    private final TaskPropertyChangeHandler changeHandler = new TaskPropertyChangeHandler();
    private final TitledBorder border;

    private static final ImageIcon GOTO_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_right.png");

    public TaskStatusPanel(IManagedTask task)
    {
        this.task = task;

        initComponents();
        btnCancel.setIcon(UIResources.CANCEL_ICON);
        btnGoTo.setIcon(GOTO_ICON);

        // Make the buttons appear flat. This is achieved in FlatLAF by making them borderless
        btnCancel.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        btnGoTo.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);

        border = new TitledBorder(task.getName()); // Put the task name in the titled border
        setBorder(border);

        final ITaskStatusHandler handler = task.getTaskStatusHandler();

        handler.addPropertyChangeListener(changeHandler); // Register to stay up to date
        loadTaskData(handler);
        updateCompletionStatus(handler);
    }

    /**
     * @return The task this panel is monitoring
     */
    public IManagedTask getTask()
    {
        return task;
    }

    /**
     * Cleanup our stuff.
     */
    public void cleanup()
    {
        task.getTaskStatusHandler().removePropertyChangeListener(changeHandler);
    }

    /**
     * Install the current task status data into the display.
     *
     * @param handler Handler from which to extract useful data
     */
    private void loadTaskData(ITaskStatusHandler handler)
    {
        lblStatus.setText(handler.getMessage());

        if (handler.isProgressIndeterminate())
        {
            progressBar.setIndeterminate(true);
        } else
        {
            progressBar.setIndeterminate(false);
            progressBar.setMinimum(handler.getMinProgress());
            progressBar.setMaximum(handler.getMaxProgress());
            progressBar.setValue(handler.getProgress());
        }

        // See if it has any location data, and if so, show the "go to" button
        btnGoTo.setVisible(task.hasLocationInfo());
    }

    /**
     * Update the display based on a new completion status.
     *
     * @param handler Handler from which to extract useful data
     */
    private void updateCompletionStatus(ITaskStatusHandler handler)
    {
        // Update the panel title based on the completion status of the task.
        final IManagedTask task = handler.getTask();
        final TaskCompletionStatus completionStatus = handler.getCompletionStatus();

        if (completionStatus != TaskCompletionStatus.INCOMPLETE)
        {
            btnCancel.setVisible(false); // Can't cancel once it's complete

            // Update our title if the task was canceled or threw an exception

            if (completionStatus != TaskCompletionStatus.DONE)
            {
                String pfx = "";

                if (completionStatus == TaskCompletionStatus.CANCELED)
                {
                    pfx = "Canceled: ";
                } else if (completionStatus == TaskCompletionStatus.EXECPTION)
                {
                    pfx = "ERROR: ";

                    final String tip = "Task failed with an error: " + handler.getException();

                    // Add a tool tip to show the user that an exception has occurred
                    lblStatus.setToolTipText(tip);
                    progressBar.setToolTipText(tip);
                }

                border.setTitle(pfx + task.getName());
            }
        } else
        {
            // Still running, so if it's cancellable, show the cancel button
            final boolean cancellable = task instanceof ICancelableManagedTask && ((ICancelableManagedTask) task).allowCancel();

            btnCancel.setVisible(cancellable);
        }
    }

    /**
     * Handle the user pressing the cancel/clear button.  This button is only visible when one or the other action
     * is appropriate.  If the task is complete, then we clear the panel.  If it is not, then we cancel it.
     */
    private void btnCancelActionPerformed()
    {
        if (task.getTaskStatusHandler().getCompletionStatus() == TaskCompletionStatus.INCOMPLETE)
        {
            if (task instanceof ICancelableManagedTask)
            {
                ((ICancelableManagedTask) task).cancel();
            }
        } else
        {
            setVisible(false); // Go away
        }
    }

    /**
     * The Go To button was clicked, so determine the location from the status handler and navigate the globe.
     */
    private void btnGoToActionPerformed()
    {
        task.makeGotoAction().invoke();
    }

    /**
     * Simple class to trigger UI update when a property changes on the task status handler.
     */
    private class TaskPropertyChangeHandler implements PropertyChangeListener
    {
        @InvokeOnEDT
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            final String propertyName = evt.getPropertyName();
            final Object newValue = evt.getNewValue();

            if (!propertyName.equals(ITaskStatusHandler.PROGRESS)) logger.debug(evt.toString());

            switch (propertyName)
            {
                case ITaskStatusHandler.COMPLETION_STATUS -> updateCompletionStatus((ITaskStatusHandler) evt.getSource());
                case ITaskStatusHandler.PROGRESS_INDETERMINATE -> progressBar.setIndeterminate((Boolean) newValue);
                case ITaskStatusHandler.MIN_PROGRESS -> progressBar.setMinimum((Integer) newValue);
                case ITaskStatusHandler.MAX_PROGRESS -> progressBar.setMaximum((Integer) newValue);
                case ITaskStatusHandler.PROGRESS -> progressBar.setValue((Integer) newValue);
                case ITaskStatusHandler.MESSAGE -> lblStatus.setText((String) newValue);
                case ITaskStatusHandler.LOCATION_OF_INTEREST, ITaskStatusHandler.SECTOR_OF_INTEREST, ITaskStatusHandler.MAP_OBJECT_KEY -> btnGoTo.setVisible(newValue != null);
                default -> logger.warn("Unknown property on TaskStatusHandler: {}", evt);
            }

            repaint();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        lblStatus = new JLabel();
        progressBar = new JProgressBar();
        btnCancel = new JButton();
        btnGoTo = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new TitledBorder("Task Name"));
        setName("this");
        setLayout(new FormLayout(
                "default:grow, 2*($lcgap, default)",
                "fill:default, $lgap, fill:default"));

        //---- lblStatus ----
        lblStatus.setText("task status message");
        lblStatus.setName("lblStatus");
        add(lblStatus, cc.xywh(1, 1, 3, 1));

        //---- progressBar ----
        progressBar.setName("progressBar");
        add(progressBar, cc.xy(1, 3));

        //---- btnCancel ----
        btnCancel.setToolTipText("Cancel this task");
        btnCancel.setName("btnCancel");
        btnCancel.addActionListener(e -> btnCancelActionPerformed());
        add(btnCancel, cc.xy(3, 3));

        //---- btnGoTo ----
        btnGoTo.setToolTipText("Go to location");
        btnGoTo.setName("btnGoTo");
        btnGoTo.addActionListener(e -> btnGoToActionPerformed());
        add(btnGoTo, cc.xy(5, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JButton btnCancel;
    private JButton btnGoTo;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
