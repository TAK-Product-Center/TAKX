package gov.takx.core.view.gui.tasks;

import com.jgoodies.forms.factories.Borders;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.tasks.IExitAffectingTask;
import gov.takx.api.tasks.IManagedTask;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.ManagedTaskEvents;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.core.tasks.ACancelableManagedTask;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.core.config.Configurator;
import org.jdesktop.swingx.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This dialog presents a set of panels, one for each executing background task.  This shows the status message,
 * and progress data for each task.  Tasks that support it can be canceled.  Tasks that provide location data
 * will have a "go to" button enabled.
 *
 * @since 1.43
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TaskStatusDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int STATUS_PANEL_SPACING = 4;

    private static TaskStatusDialog instance = null;

    @Inject
    private ITaskManager taskManager;

    // All the panels that we've created during one "visible" session
    private final List<TaskStatusPanel> statusPanels = new ArrayList<>();

    public static TaskStatusDialog getInstance()
    {
        return (instance == null) ? (instance = new TaskStatusDialog(JFrame.getFrames()[0])) : instance;
    }

    private TaskStatusDialog(Frame owner)
    {
        super(owner);
        initComponents();

        contentPanel.setLayout(new VerticalLayout(STATUS_PANEL_SPACING));

        // Give focus to the OK button instead of defaulting to a button in one of our status panels
        okButton.requestFocus();
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible == isVisible()) return; // do nothing if we're already as desired

        cleanupPanels(); // Simplest to always just cleanup any existing panels

        if (visible)
        {
            makeTaskStatusPanels();
            fixSizing();
        } else
        {
            dispose();
        }

        super.setVisible(visible);
    }

    /**
     * Make a set of status panels, one per executing task.
     */
    private void makeTaskStatusPanels()
    {
        taskManager.getExecutingTasks().forEach(this::addPanelForTask);
    }

    /**
     * Create a status panel for the given task and add it to our display.
     *
     * @param task Task to handle
     */
    private void addPanelForTask(IManagedTask task)
    {
        final TaskStatusPanel statusPanel = new TaskStatusPanel(task);
        contentPanel.add(statusPanel);
        statusPanels.add(statusPanel);

        revalidate();
        repaint();
    }

    /**
     * Remove the status panel for the given task.
     *
     * @param task Task for which to remove the status panel
     */
    private void removePanelForTask(IManagedTask task)
    {
        final Iterator<TaskStatusPanel> iterator = statusPanels.iterator();

        while (iterator.hasNext())
        {
            final TaskStatusPanel statusPanel = iterator.next();

            if (statusPanel.getTask() == task)
            {
                if (logger.isDebugEnabled()) logger.debug("Remove [{}]: {}", task.getName(), statusPanel);

                iterator.remove();
                contentPanel.remove(statusPanel);
                statusPanel.cleanup();
            }
        }

        // If we are now empty, then go away
        if (statusPanels.isEmpty())
        {
            setVisible(false);
        } else
        {
            revalidate(); // Get everything redrawn properly
            repaint();
        }
    }

    /**
     * Cleanup all the panels we've created.  We do this every time we're becoming visible so we can create a clean
     * new set of panels.
     */
    private void cleanupPanels()
    {
        contentPanel.removeAll();

        statusPanels.forEach(TaskStatusPanel::cleanup);

        statusPanels.clear();
    }

    /**
     * We want to allow the vertical scrollbar to work normally, but we don't want a horizontal scrollbar.  We
     * want the status panels to shrink/grow with the viewport.  So, fix the size of the content panel to accomplish
     * that.
     */
    private void fixSizing()
    {
        if (statusPanels.isEmpty()) return;

        // Determine the proper height for the panel based on the content
        final int panelHeight = statusPanels.get(0).getSize().height;
        final int count = statusPanels.size();
        final int height = panelHeight * count + STATUS_PANEL_SPACING * count;

        // Force the content panel width to match the current viewport size
        final int width = Math.max(250, scrollPane.getViewport().getSize().width - 2);

        contentPanel.setPreferredSize(new Dimension(width, height));
    }

    @InvokeOnEDT
    private void handleTaskQueued(@Observes ManagedTaskEvents.TaskQueued event)
    {
        addPanelForTask(event.getTask());
    }

    @InvokeOnEDT
    private void handleTaskFinished(@Observes ManagedTaskEvents.TaskFinished event)
    {
        removePanelForTask(event.getTask());
    }

    /**
     * When the user clicks OK, go away.
     */
    private void okButtonActionPerformed()
    {
        setVisible(false);
    }

    private void thisComponentResized()
    {
        fixSizing();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        scrollPane = new JScrollPane();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setTitle("Background Task Status");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("this");
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                thisComponentResized();
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== scrollPane ========
            {
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setName("scrollPane");

                //======== contentPanel ========
                {
                    contentPanel.setName("contentPanel");
                    contentPanel.setLayout(new VerticalLayout());
                }
                scrollPane.setViewportView(contentPanel);
            }
            dialogPane.add(scrollPane, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.PAGE_END);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPane;
    private JPanel contentPanel;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * For testing.
     */
    public static void main(String[] args)
    {
        Configurator.reconfigure(new File("deployment/bin/log4j2.xml").toURI());

        JFrame f = new JFrame();
        f.setPreferredSize(new Dimension(600, 200));
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);

        final TaskStatusDialog taskStatusDialog = new TaskStatusDialog(f);

        final MockTaskManager manager = new MockTaskManager();
        taskStatusDialog.taskManager = manager;

        manager.execute(new MockTask("Task #1", "This is a really long message to display in the dialog"));
        manager.execute(new MockTask("Task #2", "This is just another message"));
        manager.execute(new MockTask("Task #3", "Something Cool!"));

        taskStatusDialog.setVisible(true);
    }

    /**
     * Mock test manager so we can test this dialog.
     */
    private static class MockTaskManager implements ITaskManager
    {
        private final List<IManagedTask> tasks = new ArrayList<>();

        @Override
        public ITaskStatusHandler execute(String name, Runnable runnable)
        {
            return null;
        }

        @Override
        public ITaskStatusHandler execute(TaskGroup taskGroup, String name, Runnable runnable)
        {
            return null;
        }

        @Override
        public ITaskStatusHandler execute(IManagedTask task)
        {
            tasks.add(task);
            return null;
        }

        @Override
        public int getExecutingTaskCount()
        {
            return tasks.size();
        }

        @Override
        public List<IManagedTask> getExecutingTasks()
        {
            return Collections.unmodifiableList(tasks);
        }

        @Override
        public List<IExitAffectingTask> getExitAffectingTasks()
        {
            return null;
        }

        @Override
        public void setTaskGroupExecutionLimit(TaskGroup taskGroup, int limit) throws IllegalArgumentException
        {
            /*empty*/
        }

        @Override
        public int getTaskGroupExecutionLimit(TaskGroup taskGroup)
        {
            return 0;
        }
    }

    /**
     * Mock task so we can test.
     */
    private static class MockTask extends ACancelableManagedTask
    {
        private MockTask(String name, String msg)
        {
            super(TaskGroup.OTHER, name);

            getTaskStatusHandler().printMessage(msg);
        }

        @Override
        public void execute()
        {
        }

        @Override
        public boolean allowCancel()
        {
            return true;
        }

        @Override
        public void doCancel()
        {
        }
    }
}
