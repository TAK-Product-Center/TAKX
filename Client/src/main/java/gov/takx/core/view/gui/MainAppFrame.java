package gov.takx.core.view.gui;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.FinalizeToolbars;
import gov.takx.api.event.ITARTaggedContentDiscovered;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectOpening;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.project.IProjectManager;
import gov.takx.bootstrap.FlavorConstants;
import gov.takx.client.platform.ApplicationController;
import gov.takx.commons.lang.ExceptionList;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.preferences.WindowSettings;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.toolbar.ToolbarManager;
import gov.takx.core.view.gui.docking.DockingManager;
import gov.takx.core.view.gui.projectadmin.ProjectExplorer;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import gov.takx.core.view.gui.toolbar.IToolbar;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.hibernate.HibernateException;
import org.hibernate.exception.JDBCConnectionException;
import org.jdesktop.swingx.HorizontalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static gov.takx.core.project.ServiceOrdering.START_DockingSystem;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * The main frame for the application.
 *
 */
@PseudoBean
public class MainAppFrame extends JFrame implements IAppFrame
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int MINIMUM_APPLICATION_WINDOW_WIDTH = 800;
    private static final int MINIMUM_APPLICATION_WINDOW_HEIGHT = 600;
    private static final String ITAR_EAR_EXPORT_CONTROLLED = " [ITAR/EAR Export Controlled]";

    private static final AWTEventListener BLOCKING_EVENT_LISTENER = new BlockingMouseEventListener();

    /**
     * Standard screen max width. Anything wider will be treated like an ultra-wide screen.
     *
     * @since 5.1 
     **/
    private static final int STANDARD_SCREEN_MAX_WIDTH = 2560;

    @Inject
    private ProjectSnapshotService projectSnapshotService;

    @Inject
    private ProgramEnvironment programEnvironment;

    @Inject
    private ToolbarConfigurationManager toolbarConfigurationManager;

    @Inject
    private ToolbarManager toolbarManager;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private ApplicationController applicationController;

    @Inject
    private IPreferenceManager preferenceManager;

    @Inject
    @ProgramPreference(PreferenceEntry.WINDOW_SETTINGS_NODE)
    private WindowSettings windowSettings;

    @Inject
    @ProgramPreference(PreferenceEntry.SIMPLIFIED_USER_INTERFACE)
    private boolean simplifiedUI;

    @Inject
    private IToolbar toolbar;

    @Inject
    private Event<FinalizeToolbars> finalizeToolbarsEvent;

    @Inject
    @ProgramPreference(PreferenceEntry.PRODUCT_FLAVOR)
    private String flavor;

    private final JPanel persistentComponentsPanel = new JPanel(new HorizontalLayout(5));
    private final JSeparator persistentComponentsSeparator = new JSeparator(SwingConstants.VERTICAL);

    private BufferedImage largeAppImage;
    private boolean locked;
    private boolean isResizing;
    private boolean itarControlled;
    private ProgressDialog currentProgressDialog;
    private MainAppPanel mainAppPanel;

    public MainAppFrame()
    {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    @PostConstruct
    private void postConstruct()
    {
        // On Mac and Linux, we need our application icon to be 128x128, or it displays incorrectly in their OS "task" bar
        // TODO DCW TAKX-3911 using classloader just for now so we don't clutter the logs with errors from UIResources
        final var classLoader = UIResources.class.getClassLoader();
        URL largeAppIconUrl = classLoader.getResource(PLATFORM_ICONS.getDirectoryPath() + "logo_128.png");
        URL appIconUrl = classLoader.getResource(PLATFORM_ICONS.getDirectoryPath() + "logo_36.png");
        largeAppImage = ImageUtils.getImage(largeAppIconUrl, false);
        BufferedImage defaultAppImage = ImageUtils.getImage(appIconUrl, false);

        setIconImages(Arrays.asList(defaultAppImage, largeAppImage));

        if (simplifiedUI)
        {
            getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        } else
        {
            getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            initToolbar();
        }
    }

    private void initToolbar()
    {
        // Configure the persistent component container and its separator
        persistentComponentsSeparator.setVisible(false);
        persistentComponentsPanel.add(persistentComponentsSeparator);
        persistentComponentsPanel.addContainerListener(new PersistentComponentsListener());

        // Create a panel with the toolbar occupying the main space, and the "persistent" components panel on the right
        final var toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add((Component) toolbar, BorderLayout.CENTER);
        toolbarPanel.add(persistentComponentsPanel, BorderLayout.EAST);

        add(toolbarPanel, BorderLayout.NORTH);

        // Some Linux platforms don't support a task bar
        if (Taskbar.isTaskbarSupported())
        {
            final Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) taskbar.setIconImage(largeAppImage);
        }
    }

    public void initFrame()
    {
        logger.trace("initFrame()");

        configureSize();
        initComponents();

        if (!PreferenceEntry.USE_MAX_WINDOW_BOUNDS.getBooleanValue())
        {
            if (windowSettings.hadSavedWindowDimensions())
            {
                setLocation(windowSettings.getX(), windowSettings.getY());
            } else
            {
                final int screenWidth = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                        .getDefaultConfiguration().getBounds().width;

                // Center horizontally
                setLocation((screenWidth - getWidth()) / 2, 0);
            }
        }

        if (!simplifiedUI) configureToolbar();

        setVisible(true);

        applicationController.openProject(ProjectExplorer.StartupMode.STARTUP); // open the initial project

        logger.trace("initFrame() complete");
    }

    @Override
    public void addPersistentComponent(@Nonnull Component component)
    {
        persistentComponentsPanel.add(component);

    }

    @Override
    public void removePersistentComponent(@Nonnull Component component)
    {
        persistentComponentsPanel.remove(component);
    }

    @InvokeAndWaitOnEDT
    private void onDockingStarted(@Observes @Priority(START_DockingSystem + 1) StartProjectServices ignored, DockingManager dockingManager)
    {
        if (!programEnvironment.isHeadless())
        {
            mainAppPanel.onDockingStarted();
            dockingManager.showPrimaryGlobe(); // And ensure the globe has the initial focus

            CompletableFuture.runAsync(() -> finalizeToolbarsEvent.fire(new FinalizeToolbars()));
        }
    }

    private void configureSize()
    {
        final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        // Use GraphicsConfiguration.getBounds and Toolkit.getScreenInsets to retrieve screen information for
        // THIS single screen. If multiple screens are used GraphicsEnvironment.getMaximumWindowBounds will
        // return bounds that span all attached screens.
        final GraphicsConfiguration gc = env.getDefaultScreenDevice().getDefaultConfiguration();
        final Rectangle bounds = gc.getBounds();
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        final int widthInsets = insets.left + insets.right;
        final int heightInsets = insets.top + insets.bottom;

        // check the width and height to ensure we have at least some screen real-estate and set to bounds - any window system insets (task bars, etc.)
        final boolean useInsets = (bounds.width > 0) && (bounds.width > widthInsets) && (bounds.height > 0) && (bounds.height > heightInsets);

        final int fullScreenWidth;
        final int fullScreenHeight;

        if (useInsets)
        {
            fullScreenWidth = bounds.width - widthInsets;
            fullScreenHeight = bounds.height - heightInsets;
        } else
        {
            // Default to a small value if the discovered bounds seem invalid.
            fullScreenWidth = MINIMUM_APPLICATION_WINDOW_WIDTH;
            fullScreenHeight = MINIMUM_APPLICATION_WINDOW_HEIGHT;
        }

        final int width;
        final int height;

        if (PreferenceEntry.USE_MAX_WINDOW_BOUNDS.getBooleanValue())
        {
            width = fullScreenWidth;
            height = fullScreenHeight;
        } else if (!windowSettings.hadSavedWindowDimensions())
        {
            width = Math.min(STANDARD_SCREEN_MAX_WIDTH, fullScreenWidth); // don't use full width on ultra-wide monitors
            height = fullScreenHeight;
        } else
        {
            // Use the saved settings, but don't let the TAKX dialog be larger than the current screen size
            width = Math.min(windowSettings.getWidth(), fullScreenWidth);
            height = Math.min(windowSettings.getHeight(), fullScreenHeight);
        }

        setMinimumSize(new Dimension(MINIMUM_APPLICATION_WINDOW_WIDTH, MINIMUM_APPLICATION_WINDOW_HEIGHT));
        setSize(width, height);
    }

    private void initComponents()
    {
        logger.debug("initComponents()");

        setGlassPane(new DragGlassPane());
        updateFrameTitle(false);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                applicationController.shutdown(true);
            }
        });

        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                isResizing = true;
            }
        });

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (isResizing)
                {
                    setCursor(Cursor.getDefaultCursor());
                    isResizing = false;
                }
            }
        });

        mainAppPanel = new MainAppPanel();

        add(mainAppPanel);

        logger.debug("initComponents() done");
    }

    /**
     * Toolbar-exclusive setup.
     */
    private void configureToolbar()
    {
        // Create our tabs
        toolbarManager.createStandardTabs();

        // Load saved configurations.
        toolbarConfigurationManager.loadConfigurations();
    }

    @Override
    public void setAppFrameLocked(boolean locked)
    {
        if (locked)
        {
            if (!this.locked) Toolkit.getDefaultToolkit().addAWTEventListener(BLOCKING_EVENT_LISTENER, AWTEvent.MOUSE_EVENT_MASK);
        } else
        {
            if (this.locked) Toolkit.getDefaultToolkit().removeAWTEventListener(BLOCKING_EVENT_LISTENER);
        }
        this.locked = locked;
    }

    @Override
    @InvokeAndWaitOnEDT
    public <T> T doWithoutLock(Supplier<T> operation)
    {
        final boolean wasLocked = locked;

        setAppFrameLocked(false);
        final T result = operation.get();
        if (wasLocked) setAppFrameLocked(true);

        return result;
    }

    /**
     * A plugin with ITAR content has been discovered.  Update our title.
     */
    @InvokeOnEDT
    private void handleITARContent(@Observes ITARTaggedContentDiscovered ignored)
    {
        if (itarControlled) return; // Already marked as such

        logger.debug("Marking as ITAR controlled!");

        itarControlled = true;
        setTitle(getTitle() + ITAR_EAR_EXPORT_CONTROLLED);
    }

    /**
     * Handle a project closing event.  Lock the frame, kill off the dialogs, reset the frame title, and clear the
     * project state from the main app panel.
     */
    @InvokeAndWaitOnEDT
    private void handleProjectClosing(@Observes ProjectClosing ignored)
    {
        logger.debug("Handle ProjectClosing");

        showProgressDialog("Closing Project...");

        setAppFrameLocked(true);

        try
        {
            updateFrameTitle(false);
            clearState(currentProgressDialog);
        } catch (Exception e)
        {
            logger.error("Failure propagating clear state operation to all UI components", e);

            // tell the user about the failure
            showError("Failed to clear UI state", "Unable to properly cleanup the UI components", e);
        }
    }

    /**
     * Save off any state in the App Frame, such as the current window position.
     *
     * @param event The Project Saving event.
     * @since 2.31
     */
    private void handleProjectSaving(@SuppressWarnings("unused") @Observes ProjectSaving event)
    {
        logger.debug("Handle ProjectSaving");

        saveWindowSettings();
    }

    /**
     * Clear state for the components we manage
     *
     * @param progressMonitor The progress monitor to update.
     */
    private void clearState(IProgressMonitor progressMonitor)
    {
        mainAppPanel.clearState(progressMonitor);
    }

    /**
     * Handle a project opening event.
     */
    @InvokeOnEDT
    private void handleProjectOpening(@Observes ProjectOpening event)
    {
        if (logger.isDebugEnabled()) logger.debug("handleProjectOpening on: {}", event.getProjectName());

        setAppFrameLocked(true);
    }

    /**
     * Handle a project opened event.  The project is now open, so we need to load the state into the UI.
     */
    @InvokeOnEDT
    private void handleProjectOpened(@Observes ProjectOpened event)
    {
        logger.debug("Handle ProjectOpened");
        try
        {
            updateFrameTitle(true);
            mainAppPanel.loadState(getProgressMonitor()); // Load our tab and panel states
        } catch (Exception e)
        {
            logger.error("Exception loading state", e);

            // tell the user about the failure
            showError("Error Loading Project Data", "Failed to load UI state", e);
        } finally
        {
            disposeProgressDialog();
            setAppFrameLocked(false);
        }
    }

    /**
     * Display an error to the user.  This must be called on the EDT.
     *
     * @param title   Title to use on the dialog
     * @param message Initial message to prefix to exception messages
     * @param ex      The exception that caused the failure
     */
    private void showError(final String title, String message, Throwable ex)
    {
        showError(title, message, new ExceptionList(ex));
    }

    /**
     * Display an error to the user.  This must be called on the EDT.
     *
     * @param title         Title to use on the dialog
     * @param message       Initial message to prefix to exception messages
     * @param exceptionList The exception list that caused the failure
     */
    private void showError(final String title, String message, ExceptionList exceptionList)
    {
        final StringBuilder sb = new StringBuilder();

        if (exceptionList.anyInstanceOf(JDBCConnectionException.class, HibernateException.class))
        {
            sb.append("There is a problem connecting to the database.\n\n");
        }
        sb.append(message)
                .append(", errors were:\n")
                .append(exceptionList.getMessage("   "));

        AlwaysOnTopJOptionPane.showErrorDialog(sb.toString(), title);
    }

    /**
     * Handle the application shutting down, show our progress dialog to give the user feedback.
     * Kick off final actions to save window settings and shutdown the main panel.
     */
    @InvokeAndWaitOnEDT
    private void handleShuttingDown(@SuppressWarnings("unused") @Observes @Priority(0) ShuttingDown event)
    {
        logger.debug("Handle ShuttingDown");

        // We need to take the final snapshot before the shutdown processing starts (since that tears down the globes)
        projectSnapshotService.createProjectSnapShot();

        // Get this shown first
        showProgressDialog("Closing program...");
        setAppFrameLocked(true);

        try
        {
            saveWindowSettings();
            mainAppPanel.shutdown(getProgressMonitor());
        } catch (Exception e)
        {
            logger.error("Error closing UI", e);
            showError("Error closing UI", "A failure occurred while closing the UI components.", e);
        }
    }

    /**
     * Saves the current window position to the preference manager if the {@link PreferenceEntry#SAVE_WINDOW_SETTINGS} preference entry is true.
     *
     * @since 2.31
     */
    private void saveWindowSettings()
    {
        if (PreferenceEntry.SAVE_WINDOW_SETTINGS.getBooleanValue())
        {
            final Rectangle r = getBounds();
            new WindowSettings(r.x, r.y, r.width, r.height).save();
            preferenceManager.savePreferences();
        }
    }

    @Override
    @InvokeAndWaitOnEDT
    public void showProgressDialog(String status)
    {
        final ProgressDialog progressDialog = getProgressDialog();

        progressDialog.setMaximum(IProgressMonitor.INDETERMINATE);
        progressDialog.setText(status);
        progressDialog.setVisible(true);
    }

    @InvokeAndWaitOnEDT
    private ProgressDialog getProgressDialog()
    {
        if (currentProgressDialog == null)
        {
            currentProgressDialog = new ProgressDialog(this)
            {
                @SuppressWarnings("unused")
                private void onProjectClosing(@Observes ProjectClosing event)
                {
                    // Override AStandardDialog; this dialog should stay up until the project is closed
                }

                @SuppressWarnings("unused")
                private void onProjectClosed(@Observes ProjectClosed event)
                {
                    dispose();
                }
            };
            currentProgressDialog.setSubstatus("");
            currentProgressDialog.setModal(false);
        }
        return currentProgressDialog;
    }

    @Override
    public IProgressMonitor getProgressMonitor()
    {
        return getProgressDialog();
    }

    @Override
    public void disposeProgressDialog()
    {
        if (currentProgressDialog != null)
        {
            currentProgressDialog.setVisible(false);
            currentProgressDialog.dispose();
            currentProgressDialog = null;
        }
    }

    /**
     * Update the App Frame title based on the configured preferences.
     *
     * @param includeProjectName Pass {@code true} to include the project name in the title
     */
    private void updateFrameTitle(boolean includeProjectName)
    {
        StringBuilder title = new StringBuilder();

        // Always start with the frame title. It will be either the custom name
        // specified in the ProgramPreferences.xml "ApplicationTitle" entry or the
        // global program name specified in the "Name" entry.
        final String applicationTitle = PreferenceEntry.APPLICATION_TITLE.getStringValue();
        title.append(applicationTitle != null ? applicationTitle : PreferenceEntry.NAME.getStringValue());

        // Add flavor indicator if configured
        if (!FlavorConstants.CIV.equals(flavor)) title.append("-").append(flavor);

        // Add on the version, unless it is suppressed.
        if (!PreferenceEntry.SUPPRESS_PROGRAM_VERSION.getBooleanValue())
        {
            title.append(" ");
            title.append(programEnvironment.getProgramDisplayVersion());
        }

        // Optionally add the project name to the title.
        if (includeProjectName && !PreferenceEntry.SUPPRESS_PROJECT_NAME.getBooleanValue())
        {
            title.append(" : ").append(projectManager.getProjectName());
        }

        // Add in developer edition tag if this is not a final release build.
        if (!programEnvironment.getVersionPreReleaseTag().isEmpty())
        {
            title.append(" (Developer's Edition)");
        }

        // Check ITAR/EAR restrictions
        if (itarControlled)
        {
            title.append(ITAR_EAR_EXPORT_CONTROLLED);
        }

        setTitle(title.toString());
    }

    // This is a hack to make the frame behave similarly to native macOS windows,
    // refusing to move past the menu bar (see CORE-3189).
    //
    // There are many problems with this hack:
    //
    // * It doesn't know the actual size of the menu bar (it can vary depending on screen DPI)
    // * It doesn't actually know whether the current screen _has_ a menu bar at all
    // * It only fixes things for the main TAKX window, others can still get stuck...
    // * It relies on the undocumented fact that the particular JRE I'm using now
    //   has all resize implementations delegating to reshape(...)
    // * It relies on untested assumptions about how multiple screen support on Mac interacts
    //   with the AWT
    //
    // but still, it beats losing your window off the top of the screen...
    //
    // This hack will probably eventually cause a "why is the window being weird?" report from
    // a user with multiple screens.  IMO, this is better than a hundred "my window is stuck
    // forever at the top of the screen!" reports, most of them from me since this happens all
    // the time to me.
    @SuppressWarnings("deprecation")
    @Override
    public void reshape(int x, int y, int width, int height)
    {
        if (SystemUtils.IS_OS_MAC)
        {
            final int lastY = getY();

            if (y != lastY)
            {
                // menu bar on Mac OS X at standard resolution is 20 px tall
                final int yMin = getGraphicsConfiguration().getBounds().y + 20;
                if (y < yMin)
                {
                    y = yMin;

                    final int lastH = getHeight();

                    if (lastH != height)
                    {
                        height = lastH + lastY - y;
                    }
                }
            }
        }

        super.reshape(x, y, width, height);
    }

    /**
     * Event listener to consume all mouse events.  This supports "locking" the main frame during startup.
     * @since 5.2
     */
    private static class BlockingMouseEventListener implements AWTEventListener
    {
        @Override
        public void eventDispatched(AWTEvent event)
        {
            final int id = event.getID();
            if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_DRAGGED)
            {
                ((MouseEvent) event).consume();
            }
        }
    }

    /**
     * Monitor for add/remove of persistent components, and control the visibility of the separator accordingly.
     * @since 5.4
     */
    private class PersistentComponentsListener implements ContainerListener
    {
        @Override
        public void componentAdded(ContainerEvent e)
        {
            persistentComponentsSeparator.setVisible(shouldBeVisible());
        }

        @Override
        public void componentRemoved(ContainerEvent e)
        {
            persistentComponentsSeparator.setVisible(shouldBeVisible());
        }

        private boolean shouldBeVisible()
        {
            return persistentComponentsPanel.getComponentCount() > 1; // 1 for the separator itself
        }
    }
}
