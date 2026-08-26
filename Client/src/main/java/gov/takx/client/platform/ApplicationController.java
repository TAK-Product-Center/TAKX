package gov.takx.client.platform;

import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.ui.IUserProxy;
import gov.takx.bootstrap.StartupArgs;
import gov.takx.commons.lang.ExceptionList;
import gov.takx.commons.preferences.PreferenceUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.database.exception.FatalDatabaseException;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.project.ProjectSelection;
import gov.takx.core.view.gui.IAppFrame;
import gov.takx.core.view.gui.projectadmin.ProjectExplorer;
import gov.takx.core.view.gui.projectadmin.ProjectSelector;
import gov.takx.platform.cdi.NoopInvocationHandler;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.bindings.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.takx.core.view.gui.projectadmin.ProjectExplorer.StartupMode.STARTUP;

/**
 * A controller for initiating operations on the application client.  Our UI is a smushy mess of view and controller logic
 * with very little model.  In order to support headless operation, we need to have a mechanism to initiating
 * operations on the client without a UI.  This is an initial, not highly architected, cut at such a controller.
 * <p>
 * This will obviously get refactored as we learn more and have time to do a through design of all the operations we
 * want to expose to a scripting environment and refactor the UI to use a "real" controller.
 *
 * @since 1.50 (renamed from RaptorController in 3.4)
 */
@ApplicationScoped
public class ApplicationController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<String> args;
    private final IPersistServiceProvider persistServiceProvider;
    private final IProjectManager projectManager;
    private final Event<ShuttingDown> shuttingDownEvent;
    private final IUserProxy userProxy;

    private IAppFrame appFrame;

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Inject
    public ApplicationController(@Parameters List<String> args, IPersistServiceProvider persistServiceProvider,
                                 IProjectManager projectManager, ApplicationLauncher applicationLauncher,
                                 Event<ShuttingDown> shuttingDownEvent, IUserProxy userProxy)
    {
        this.args = args;
        this.persistServiceProvider = persistServiceProvider;
        this.projectManager = projectManager;
        this.shuttingDownEvent = shuttingDownEvent;
        this.userProxy = userProxy;

        appFrame = applicationLauncher.getAppFrame();
        if (appFrame == null) appFrame = NoopInvocationHandler.forType(IAppFrame.class);
    }

    /**
     * Open a project, either on application startup, or with another project already open (as indicated by the parameter).
     *
     * @param startupMode The mode for opening a project
     * @since 3.3 (moved from MainAppFrame)
     */
    public void openProject(ProjectExplorer.StartupMode startupMode)
    {
        // For opening a project with the application already started, confirm shutdown of the current project first
        if (startupMode != STARTUP && !userProxy.confirmQuit(false)) return;

        final String lastProjectName = projectManager.getLastOpenProjectNameIfExists();
        ProjectSelection projectSelection;

        // Bring up the explorer on startup only if configured to so do; always display it for opening a project with
        // the application already started
        if (startupMode != STARTUP || PreferenceEntry.USE_PROJECT_EXPLORER.getBooleanValue())
        {
            projectSelection = selectProjectViaExplorer(lastProjectName, startupMode);

            if (projectSelection.isCanceled())
            {
                // If the user didn't select a project on startup, our only option is to shutdown the application
                if (startupMode == STARTUP) shutdown(false);
                return;
            }
        } else
        {
            final StartupArgs startupArgs = new StartupArgs(args); // So we can test for various command line flags

            // See if we're supposed to create a new project or open an existing one.
            // If -project is specified with a name, open that
            // If -create is specified a default name will be generated
            // If neither are specified, we default to normal operation.

            String projectName = startupArgs.getArgValue("project", "proj");

            if (projectName != null)
            {
                projectSelection = ProjectSelection.makeExisting(projectManager.getProjectFilePath(projectName));
            } else if (startupArgs.argPresent("create") || lastProjectName.isEmpty())
            {
                // Create a new project
                final String newProjectName = projectManager.getDefaultProjectName();
                final String projectFilePath = projectManager.getProjectFilePath(newProjectName);
                final Boolean wmsOfflineMode = PreferenceEntry.DEFAULT_WMS_OFFLINE_MODE.getBooleanValue();

                logger.info("Creating new project: {}", newProjectName);

                projectSelection = ProjectSelection.makeNew(projectFilePath, null, wmsOfflineMode);
            } else
            {
                // Open last project
                final String projectPath = projectManager.getLastOpenedProjectPath();
                logger.info("Opening project: {}", projectPath);
                projectSelection = ProjectSelection.makeExisting(projectPath);
            }
        }

        internalOpenProject(projectSelection);
    }

    /**
     * Display the project explorer for selection of a project.
     *
     * @param lastProjectName the most recent project (may be {@code null} or an empty String for no recent project)
     * @param startupMode     the mode for the project explorer
     * @return The project selected in the project explore dialog (may indicate cancelled if user closed the dialog
     * without making a selection)
     * @since 3.3
     */
    private ProjectSelection selectProjectViaExplorer(String lastProjectName, ProjectExplorer.StartupMode startupMode)
    {
        ProjectExplorer projectExplorer = new ProjectExplorer(lastProjectName, startupMode);
        projectExplorer.setVisible(true);
        return projectExplorer.getProjectSelection();
    }

    /**
     * Open an existing project at the specified path.
     *
     * @param path path of project file to open (.projx)
     * @since 3.3 (moved from MainAppFrame)
     */
    public void openExistingProject(String path)
    {
        if (userProxy.confirmQuit(false)) internalOpenProject(ProjectSelection.makeExisting(path));
    }

    /**
     * Open the selected project.
     *
     * @param projectSelection The selected project to open
     * @since 3.3 (moved from MainAppFrame)
     */
    private void internalOpenProject(ProjectSelection projectSelection)
    {
        appFrame.showProgressDialog("Loading project...");

        new ErrorReportingSwingWorker<Void, Void>("Error Loading Project Data",
                "Failed to create project session; the log contains additional details.")
        {
            // Track failures so we can break out of an infinite loop of database issues
            private static boolean previousFailure = false;

            // ErrorReportingSwingWorker does not support cancel condition, so this flag added to indicate that
            // state was not loaded, but there is no error to report.
            boolean canceled = false;

            boolean shouldExit = false;

            @Override
            protected boolean isSuccess()
            {
                return super.isSuccess() && !canceled;
            }

            @UseClientClassLoader
            @Override
            protected Void doInBackground() throws Exception
            {
                // Open the project. If the user declines an upgrade, this will return false
                try
                {
                    canceled = !projectManager.openProject(projectSelection, appFrame.getProgressMonitor());

                    // If the load was canceled, then give the user a choice of how to proceed
                    if (canceled)
                    {
                        final Object[] dlgButtons = {"Exit", "Create New"};

                        int result = appFrame.doWithoutLock(() -> AlwaysOnTopJOptionPane.showOptionDialog(
                                "Would you like to EXIT or create a NEW project?",
                                "Choose an action",
                                JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, UIResources.WARNING_ICON,
                                dlgButtons, dlgButtons[JOptionPane.YES_OPTION]));

                        shouldExit = result == JOptionPane.YES_OPTION;
                    }
                } catch (FatalDatabaseException e)
                {
                    // We want to show these directly, and not have them handled as "unexpected"
                    addException(e);
                    if (previousFailure) shouldExit = true;
                    previousFailure = true;
                } catch (RuntimeException e)
                {
                    addException(e);
                    logger.error("Startup Failed", e);
                    shouldExit = true;
                } finally
                {
                    persistServiceProvider.cleanupPersistService();  // Be tidy as we exit
                }

                return null;
            }

            @Override
            protected void cleanup()
            {
                appFrame.disposeProgressDialog();
                appFrame.setResizable(true);
            }

            @Override
            protected void onFailure()
            {
                if (shouldExit)
                {
                    logger.debug("open failure, shutting down");
                    shutdown(1);
                } else
                {
                    logger.debug("open failure, returning to startup");
                    new ReturnToStartupRunnable().run();
                }
            }

            @Override
            protected void showError(final String title, String message, ExceptionList exceptionList)
            {
                if (!canceled)
                {
                    super.showError(title, message, exceptionList);
                }
            }
        }.execute();
    }

    /**
     * Save the current project to the existing project directory.
     *
     * @since 3.3 (moved from MainAppFrame)
     */
    public void saveMenuItemActionPerformed()
    {
        appFrame.showProgressDialog("Saving project...");
        appFrame.setAppFrameLocked(true);

        new ErrorReportingSwingWorker<Void, Void>("Failed to Save Project", "Error while saving project")
        {
            @Override
            protected Void doInBackground()
            {
                projectManager.saveProject(appFrame.getProgressMonitor());
                return null;
            }

            @Override
            protected void cleanup()
            {
                appFrame.disposeProgressDialog();
                appFrame.setAppFrameLocked(false);
            }
        }.execute();
    }

    /**
     * Save the current project to the new selected location.
     *
     * @since 3.3 (moved from MainAppFrame)
     */
    public void saveAsMenuItemActionPerformed()
    {
        final String projectFilePath = new ProjectSelector().selectNewProjectFilePath();

        if (projectFilePath != null && !projectFilePath.isEmpty())
        {
            appFrame.showProgressDialog("Saving project...");
            appFrame.setAppFrameLocked(true);

            new ErrorReportingSwingWorker<Void, Void>("Failed to complete Save As", "Save As failed. Please see logs for errors.")
            {
                @Override
                protected Void doInBackground() throws Exception
                {
                    projectManager.saveProjectAs(projectFilePath, appFrame.getProgressMonitor());
                    return null;
                }

                @Override
                protected void cleanup()
                {
                    appFrame.disposeProgressDialog();
                    appFrame.setAppFrameLocked(false);
                }

                @Override
                protected void onFailure()
                {
                    new ReturnToStartupRunnable().run();
                }
            }.execute();
        }
    }

    /**
     * Shutdown the application.
     *
     * @param confirm Whether the user should be prompted to confirm the shutdown
     * @since 3.3 (moved from MainAppFrame)
     */
    public void shutdown(final boolean confirm)
    {
        shutdown(confirm, false);
    }

    /**
     * Shutdown the application.
     *
     * @param confirm Whether the user should be prompted to confirm the shutdown
     * @param noExit Pass {@code true} to NOT call exit
     * @since 5.5.1
     */
    public void shutdown(final boolean confirm, boolean noExit)
    {
        if (confirm && !userProxy.confirmQuit(true)) return; // User canceled

        // Kick this into the background so the progress dialogs can update as we shut down
        // Don't use the executor service since that gets shutdown as part of the project closing event.
        // This would lead to an InterruptedException on the InvokeAndWait call to handleProjectClosing.
        final var thread = new Thread(() -> shutdown(0, appFrame.getProgressMonitor(), noExit));
        thread.start();

        // If not actually exiting, then we need to wait for the thread to complete.  This logic is required
        // by ContentCleaner.
        if (noExit)
        {
            try
            {
                thread.join();
            } catch (InterruptedException ignored)
            {
            }
        }
    }

    /**
     * Shutdown this application instance, closing and saving any open project, and then exit (using System.exit).
     * The shutdown is forced, so the user will not be queried.
     *
     * @param exitCode Exit code to use in call to {@link System#exit(int)}
     */
    public void shutdown(int exitCode)
    {
        shutdown(exitCode, new NullProgressMonitor(), false);
    }

    /**
     * Shutdown this application instance, closing and saving any open project, and then, unless {@code noExit} is
     * {@code true}, exit (using {@link System#exit}).
     *
     * @param exitCode Exit code to use in call to {@link System#exit(int)}
     * @param noExit Pass {@code true} to NOT call exit
     */
    public void shutdown(int exitCode, IProgressMonitor progressMonitor, boolean noExit)
    {
        // Log message added to meet STIG V-69421, which requires that shutdown events be logged.
        logger.info("Shutting down TAKX");
        shuttingDownEvent.fire(new ShuttingDown(progressMonitor));

        try
        {
            projectManager.closeProject(progressMonitor);
        } catch (Exception e)
        {
            logger.error("Failed to shutdown cleanly", e);
        }

        if (!noExit) System.exit(exitCode); // It's time to die Mr. Bond
    }

    /**
     * Standard runnable callback in the event of a project transition failure.
     * @since 3.3 (moved from MainAppFrame)
     */
    public class ReturnToStartupRunnable implements Runnable
    {
        @Override
        public void run()
        {
            appFrame.disposeProgressDialog();
            if (projectManager != null)
            {
                try
                {
                    projectManager.clearLastOpenedProjectPath();
                } catch (IOException e)
                {
                    PreferenceUtils.showPreferencesErrorMessage(logger, e);
                }
            }
            openProject(STARTUP);
        }
    }
}
