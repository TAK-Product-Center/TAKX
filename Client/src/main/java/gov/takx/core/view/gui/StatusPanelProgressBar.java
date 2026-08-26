package gov.takx.core.view.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.ManagedTaskEvents;
import gov.takx.core.view.gui.tasks.TaskStatusDialog;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.painter.BusyPainter;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * This panel (typically housed in the bottom status bar) presents information on background task execution.
 * If there is a single task executing, then it displays a progress bar and progress messages from that task.
 * It there are multiple tasks executing, then it displays a busy indicator and a message indicating the number
 * of tasks executing.  If the user clicks on this panel, then the background tasks status dialog is launched.
 *
 */
@PseudoBean(singletonObserver = true)
public class StatusPanelProgressBar extends JPanel
{
    @Inject
    private ITaskManager taskManager;

    private boolean singleLayout = true;

    public StatusPanelProgressBar()
    {
        initComponents();

        configureBusyPainter(singleBusyLabel.getBusyPainter());
        configureBusyPainter(multiBusyLabel.getBusyPainter());

        singleBusyLabel.setBusy(false);
        multiBusyLabel.setBusy(false);

        setVisible(false);
    }

    /**
     * Configure the busy painters the way we like them.  Can't do this with JFormDesigner.
     *
     * @param busyPainter Painter to configure
     */
    private void configureBusyPainter(BusyPainter busyPainter)
    {
        busyPainter.setPoints(10);
        busyPainter.setTrailLength(5);
        busyPainter.setHighlightColor(new Color(44, 61, 146).darker()); // TODO KMB: Fix for FlatLaf (TAKX-739)
        busyPainter.setBaseColor(new Color(168, 204, 241).brighter());
    }

    /**
     * Update our layout based on the number of active tasks.
     */
    @InvokeOnEDT
    private void updateLayout()
    {
        // If we have no tasks, then become invisible
        // if we have a single task executing, then show that tasks progress
        // Otherwise, show the busy indicator and the task count

        final List<IManagedTask> tasks = taskManager.getExecutingTasks();
        final int taskCount = tasks.size();

        if (taskCount == 0)
        {
            // Turn off both busy labels, or they continue to animate in the background
            singleBusyLabel.setBusy(false);
            multiBusyLabel.setBusy(false);
        } else if (taskCount == 1)
        {
            installSingleTaskLayout(tasks.get(0));
        } else
        {
            installMultiTaskLayout(taskCount);
        }

        setVisible(taskCount > 0);
    }

    /**
     * Install the UI components/layout for tracking a single task.
     *
     * @param task The task to monitor
     */
    private void installSingleTaskLayout(IManagedTask task)
    {
        if (!singleLayout)
        {
            remove(multiBusyLabel);
            add(pnlSingleTask, new CellConstraints().xy(1, 1));
            revalidate();

            singleLayout = true;
        }

        final ITaskStatusHandler handler = task.getTaskStatusHandler();

        updateTaskData(handler); // Install the current values into our label and progress bar

        singleBusyLabel.setBusy(true);
        singleBusyLabel.setText(task.getName());

        multiBusyLabel.setBusy(false);

        handler.addPropertyChangeListener(new TaskPropertyChangeHandler()); // And register to stay up to date
    }

    /**
     * Update the displayed task status data.
     *
     * @param handler Handler from which to extract useful data
     */
    private void updateTaskData(ITaskStatusHandler handler)
    {
        setSingleMessage(handler.getMessage());

        if (handler.isProgressIndeterminate())
        {
            singleProgressBar.setIndeterminate(true);
        } else
        {
            singleProgressBar.setMinimum(handler.getMinProgress());
            singleProgressBar.setMaximum(handler.getMaxProgress());
            singleProgressBar.setValue(handler.getProgress());
        }
    }

    /**
     * Keep really long strings from taking up all the room, which makes the progress bar too small.
     *
     * @param message Message to display
     */
    private void setSingleMessage(String message)
    {
        if (message != null && message.length() > 45)
        {
            message = message.substring(0, 45) + "...";
        }

        singleBusyLabel.setText(message);
    }

    /**
     * Install the UI layout used when multiple tasks are executing.
     *
     * @param taskCount Number of executing tasks
     */
    private void installMultiTaskLayout(int taskCount)
    {
        if (singleLayout)
        {
            remove(pnlSingleTask);
            add(multiBusyLabel, new CellConstraints().xy(1, 1));
            revalidate();

            singleLayout = false;
        }

        multiBusyLabel.setText(taskCount + " tasks running...");

        multiBusyLabel.setBusy(true);
        singleBusyLabel.setBusy(false);
    }

    private void handleTaskQueued(@SuppressWarnings("unused") @Observes ManagedTaskEvents.TaskQueued event)
    {
        updateLayout();
    }

    private void handleTaskFinished(@SuppressWarnings("unused") @Observes ManagedTaskEvents.TaskFinished event)
    {
        updateLayout();
    }

    private void mouseClicked()
    {
        TaskStatusDialog.getInstance().setVisible(true);
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

            switch (propertyName)
            {
                case ITaskStatusHandler.PROGRESS_INDETERMINATE -> singleProgressBar.setIndeterminate((Boolean) newValue);
                case ITaskStatusHandler.MIN_PROGRESS -> singleProgressBar.setMinimum((Integer) newValue);
                case ITaskStatusHandler.MAX_PROGRESS -> singleProgressBar.setMaximum((Integer) newValue);
                case ITaskStatusHandler.PROGRESS -> singleProgressBar.setValue((Integer) newValue);
                case ITaskStatusHandler.MESSAGE -> setSingleMessage((String) newValue);
            }

            repaint();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        pnlSingleTask = new JPanel();
        singleBusyLabel = new JXBusyLabel();
        singleProgressBar = new JProgressBar();
        multiBusyLabel = new JXBusyLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:pref"));

        //======== pnlSingleTask ========
        {
            pnlSingleTask.setName("pnlSingleTask");
            pnlSingleTask.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- singleBusyLabel ----
            singleBusyLabel.setText("text");
            singleBusyLabel.setName("singleBusyLabel");
            singleBusyLabel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    StatusPanelProgressBar.this.mouseClicked();
                }
            });
            pnlSingleTask.add(singleBusyLabel, cc.xy(1, 1));

            //---- singleProgressBar ----
            singleProgressBar.setName("singleProgressBar");
            singleProgressBar.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    StatusPanelProgressBar.this.mouseClicked();
                }
            });
            pnlSingleTask.add(singleProgressBar, cc.xy(3, 1));
        }
        add(pnlSingleTask, cc.xy(1, 1));

        //---- multiBusyLabel ----
        multiBusyLabel.setText("multi busy label");
        multiBusyLabel.setDelay(300);
        multiBusyLabel.setName("multiBusyLabel");
        multiBusyLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                StatusPanelProgressBar.this.mouseClicked();
            }
        });
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel pnlSingleTask;
    private JXBusyLabel singleBusyLabel;
    private JProgressBar singleProgressBar;
    private JXBusyLabel multiBusyLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
