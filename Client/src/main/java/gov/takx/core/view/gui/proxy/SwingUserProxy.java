package gov.takx.core.view.gui.proxy;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.tasks.IExitAffectingTask;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.preferences.PreferenceUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.persist.RdmBatchExecutorService;
import gov.takx.core.view.gui.ExitWarningDialog;
import gov.takx.core.view.gui.MainAppFrame;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This is the user proxy for when we are running the Swing UI for TAKX.
 *
 * @since 2.0.0
 */
@ApplicationScoped
@Typed(SwingUserProxy.class)
public class SwingUserProxy implements IUserProxy
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final RdmBatchExecutorService rdmBatchExecutorService;
    private final ITaskManager taskManager;
    private final MainAppFrame appFrame;

    private boolean errorVisible = false;

    @Inject
    SwingUserProxy(RdmBatchExecutorService rdmBatchExecutorService, ITaskManager taskManager, ApplicationLauncher applicationLauncher)
    {
        this.rdmBatchExecutorService = rdmBatchExecutorService;
        this.taskManager = taskManager;
        appFrame = applicationLauncher.getAppFrame();
    }

    @InvokeAndWaitOnEDT
    @Override
    public boolean confirmQuit(boolean quitting)
    {
        // Check for active background operations. Ask the user if she still wants to exit. Return from this method
        // if the user does not want to exit.
        if (!checkReadyToExit()) return false;

        int result = JOptionPane.YES_OPTION;

        boolean confirmQuit = UserPreferenceEntry.CONFIRM_QUIT.getBooleanValue();

        if (confirmQuit && quitting)
        {
            // Always save
            final Object[] dlgButtons = {"Yes", "Cancel"};
            final String dlgMessage = "Are you sure you want to quit?";

            // Ask if the user wants to save
            result = AlwaysOnTopJOptionPane.showOptionDialog(
                    dlgMessage,                                      // message
                    "Confirm Quit",                                  // title
                    JOptionPane.WARNING_MESSAGE,                     // message type
                    JOptionPane.YES_NO_OPTION,                       // option type
                    UIResources.WARNING_ICON,                        // icon
                    dlgButtons,                                      // options
                    dlgButtons[JOptionPane.YES_OPTION]);             // Initial value (Yes)
        }

        return result == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean confirmDatabaseUpgrade(String msg)
    {
        return doWithoutApplicationLock(() -> JOptionPane.YES_OPTION == AlwaysOnTopJOptionPane.showConfirmDialog(
                null, msg, "Upgrade Project?", JOptionPane.YES_NO_OPTION));
    }

    @Override
    public <T> T doWithoutApplicationLock(Supplier<T> operation)
    {
        return appFrame.doWithoutLock(operation);
    }

    @Override
    public void notify(String title, String message)
    {
        final Object[] dlgButtons = {"Ok"};

        AlwaysOnTopJOptionPane.showOptionDialog(message, title,
                JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION, UIResources.WARNING_ICON,
                dlgButtons, dlgButtons[JOptionPane.YES_OPTION]);
    }

    @Override
    public void reportPreferencesError(Throwable cause)
    {
        PreferenceUtils.showPreferencesErrorMessage(logger, cause);
    }

    @Override
    public void reportError(String title, String msg, Throwable cause)
    {
        if (!errorVisible)
        {
            errorVisible = true;

            final String errorMsg = cause != null
                    ? msg + ", caused by: " + ExceptionUtils.getNestedMessage(cause)
                    : msg;

            logger.error("{} : {}", title, errorMsg);

            SwingUtilities.invokeLater(() -> {
                try
                {
                    AlwaysOnTopJOptionPane.showErrorDialog(errorMsg, title);
                } finally
                {
                    errorVisible = false;
                }
            });
        } else
        {
            logger.error("Error reported while already displaying an error");
            logger.error(msg, cause);
        }
    }

    @Override
    public void reportWarning(String title, String msg)
    {
        logger.warn("{} : {}", title, msg);

        SwingUtilities.invokeLater(() -> AlwaysOnTopJOptionPane.showMessageDialog(msg, title, JOptionPane.WARNING_MESSAGE));
    }

    @Override
    public void reportMessage(String title, String msg)
    {
        SwingUtilities.invokeLater(() -> AlwaysOnTopJOptionPane.showMessageDialog(msg, title, JOptionPane.INFORMATION_MESSAGE));
    }

    /**
     * Check for incomplete background operations. Ask the user if they still want to exit.
     *
     * @return true if nothing prevents exit or the user wants to exit, false if not.
     */
    private boolean checkReadyToExit()
    {
        final Map<TaskGroup, List<String>> msgMap = new EnumMap<>(TaskGroup.class);

        // If the task indicates we shouldn't exit, then collect the message it wants to display
        taskManager.getExitAffectingTasks().stream()
                .filter(task -> !task.allowExit())
                .forEach(task -> addExitMessage(msgMap, task));

        // Add RDMs still waiting in batches
        final int outstandingRDMCount = rdmBatchExecutorService.getOutstandingRDMCount();
        if (outstandingRDMCount > 0)
        {
            final String countString = NumberFormat.getIntegerInstance().format(outstandingRDMCount);
            msgMap.put(TaskGroup.RDM_PERSISTENCE,
                    Collections.singletonList(countString + " RDMs waiting to to be persisted"));
        }

        // If none of the tasks actually contributed a message, then we can exit
        if (msgMap.isEmpty()) return true;

        // Otherwise, construct the dialog to show the user all the tasks they might affect if they exit.

        final ExitWarningDialog exitWarning = new ExitWarningDialog(msgMap);
        exitWarning.setVisible(true);

        return exitWarning.getConfirmExit();
    }

    /**
     * Add the exit message from the given task to our map of messages.
     *
     * @param msgMap Message map to update
     * @param task   Task affecting our exit
     */
    private void addExitMessage(Map<TaskGroup, List<String>> msgMap, IExitAffectingTask task)
    {
        final TaskGroup taskGroup = task.getTaskGroup();
        List<String> messages = msgMap.computeIfAbsent(taskGroup, k -> new ArrayList<>());

        final String exitMessage = task.getExitMessage();
        if (exitMessage != null)
        {
            messages.add(exitMessage);
        }
    }
}
