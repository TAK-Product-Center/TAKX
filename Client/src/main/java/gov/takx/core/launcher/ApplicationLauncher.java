package gov.takx.core.launcher;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.util.ConfigOptions;
import gov.nasa.worldwind.Configuration;
import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;
import gov.tak.platform.commons.resources.JavaResourceManager;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.api.ui.IUserProxy;
import gov.takx.bootstrap.Bootstrap;
import gov.takx.bootstrap.StartupArgs;
import gov.takx.cdi.LaunchApplicationEvent;
import gov.takx.client.platform.ApplicationController;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.core.view.gui.MainAppFrame;
import gov.takx.core.view.gui.UIInstaller;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.weld.environment.se.bindings.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.swing.SwingUtilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * This is the actual main routine for TAKX.  It can not be invoked directly from the command line.  You must
 * use the bootstrap to invoke this "main" as a module.
 *
 * @since 3.4 (renamed from RaptorLauncher)
 * @see Bootstrap
 */
@ApplicationScoped
public class ApplicationLauncher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String CLOSED_NETWORK_MSG =
            "TAKX has been configured to run on a CLOSED network, so it will not have access to tak.gov or the default internet resources for imagery and elevation data.";

    private final List<String> args;
    private final ProgramEnvironment programEnvironment;
    private final PluginRegistryManager pluginRegistryManager;
    private final ApplicationController applicationController;
    private final IProjectManager projectManager;
    private final IUserProxy userProxy;
    private final IResourceManager resourceManager;

    private MainAppFrame appFrame; // Only used when not running headless

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private ApplicationLauncher(@Parameters List<String> args, ProgramEnvironment programEnvironment, PluginRegistryManager pluginRegistryManager,
                                ApplicationController applicationController, IProjectManager projectManager, IUserProxy userProxy)
    {
        this.args = args;
        this.programEnvironment = programEnvironment;
        this.pluginRegistryManager = pluginRegistryManager;
        this.applicationController = applicationController;
        this.projectManager = projectManager;
        this.userProxy = userProxy;
        resourceManager = new JavaResourceManager();

        logger.debug("Startup args={}", args);
    }

    /**
     * This is the actual method that cranks up the TAKX core.  It is called from the CDI framework because
     * it observes the {@link LaunchApplicationEvent} event.
     */
    public void launch(@Observes LaunchApplicationEvent unused, ISystemMessageManager messageManager, INotificationService notificationService)
    {
        logger.info("OS : {}, version {}, arch {}", SystemUtils.OS_NAME, SystemUtils.OS_VERSION, SystemUtils.OS_ARCH);
        logger.info("JVM: {} ({}-{})", SystemUtils.JAVA_VERSION, SystemUtils.JAVA_VM_VENDOR, SystemUtils.JAVA_VM_VERSION);

        // This is the first place at which program preferences are accessed.  Initializing the preferences can
        // throw exceptions, and we need to catch and properly report them to the user.
        try
        {
            logger.info("Launching {}", programEnvironment.getProgramNameVersionAndBuild());

            final String applicationTitle = PreferenceEntry.APPLICATION_TITLE.getStringValue();
            if (applicationTitle != null && !applicationTitle.equalsIgnoreCase(PreferenceEntry.NAME.getStringValue()))
            {
                logger.info("As Application: {}", applicationTitle);
            }
        } catch (Exception e)
        {
            logger.error("Startup failure", e);
            final String msg = "Errors starting application:\n" + ExceptionUtils.getMessageWithCauses("   ", e);
            userProxy.notify("Startup Failure", msg);
            System.exit(1);
        }

        if (!PreferenceEntry.PRODUCT_FLAVOR.getStringValue().equals(Bootstrap.getFlavor()))
        {
            // TODO DCW TAKX-3843 The product flavor is now derived from the bootstrap jar and cannot be set via preference
            //  We should redesign how this is handled, possibly altering how we populate preferences to allow runtime-only values
            logger.warn("Overriding preference flavor: {} with bootstrap flavor: {}", PreferenceEntry.PRODUCT_FLAVOR.getStringValue(), Bootstrap.getFlavor());
            PreferenceEntry.PRODUCT_FLAVOR.putAndSave(Bootstrap.getFlavor());
        }

        logger.info("ProjectData directory: {}", programEnvironment.getProjectDataDirectory());

        if (PreferenceEntry.CLOSED_NETWORK.getBooleanValue())
        {
            logger.info(CLOSED_NETWORK_MSG);
            messageManager.addMessage(new SystemMessage(SystemLogLevel.SYSTEM_INFO, CLOSED_NETWORK_MSG));
        }

        final boolean headless = programEnvironment.isHeadless();

        if (headless)
        {
            // Setting this property will allow some AWT calls to function without exceptions.  Setting this
            // also allows others to reference this value when programEnvironment isn't available or during
            // static initialization.
            System.setProperty("java.awt.headless", "true");
        }

        // At low resolutions, macOS will return a pixel scaling multiplier of 2. This causes the WorldWindowGLJPanel to be 2x its
        // normal size and only the lower 1/4 of the panel ends up visible. This appears to be a known issue with older versions of JOGL.
        // This setting forces JAWTUtil.getPixelScale() to always return 1.
        if (Configuration.isMacOS()) System.setProperty("nativewindow.awt.nohidpi", "true");

        // Install signal handlers to shut down cleanly
        final InterruptHandler handler = new InterruptHandler();

        installSignalHandler("INT", handler);
        installSignalHandler("TERM", handler);

        // Use programEnvironment.getProjectDataDirectory()?
        File wmmFilename = Path.of("resources", "tak-assets", "wmm_cof").toFile();
        try (InputStream inStream = resourceManager.openRawResource("wmm_cof", ResourceType.Raw);
             FileOutputStream outStream = IOProviderFactory.getOutputStream(wmmFilename))
        {
            FileSystemUtils.copyStream(inStream, outStream);
            ConfigOptions.setOption(KernelConfigOptionConstants.WMM_MODEL_FILE, wmmFilename.getAbsolutePath());
        } catch (IOException e)
        {
            logger.warn("Could not copy wmm_cof from resources.", e);
        }

        try
        {
            // Set the JVM timezone to UTC so the DB can correctly query date values
            // In the future this should most likely be configurable
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            pluginRegistryManager.loadRegistriesInBackground(); // Start loading the plugin registries

            // If we're NOT headless, then install our UI configuration.  This must happen BEFORE firing the
            // RaptorConfiguredEvent, since that causes some UI elements to initialize.
            if (!headless) UIInstaller.installUI();
        } catch (Exception e)
        {
            userProxy.reportError("Startup Failure", "Error starting application: " + e, e);

            System.exit(1);
        }

        // Now that the basic initialization has taken place, crank up the UI or headless operation
        if (headless)
        {
            startHeadless();
        } else
        {
            startUI();
        }

        // Can't use notification service until the UI is configured
        if (PreferenceEntry.CLOSED_NETWORK.getBooleanValue())
        {
            final var notification = Notification.builder(CLOSED_NETWORK_MSG, Notification.Type.Info).build();
            SwingUtilities.invokeLater(() -> notificationService.show(notification, TimeUnit.SECONDS.toMillis(10)));
        }
    }

    /**
     * Bring up the UI for the user.
     */
    private void startUI()
    {
        // For some unknown reason, when we use the Install4J executable configured to show a splash screen,
        // any TCCL we installed previously in UIInstaller.installUI has been reset to the boot class loader.
        // So, setting the TCCL for the EDT here seems to rectify that problem.

        // NOTE: DO NOT change this method and try to use @InvokeOnEDT.  It will not work, and you will end
        // up with strange class loading issues later on when bringing up the globe.
        //
        // In the case that we are already on the EDT, @InvokeOnEDT directly executes this method, and does not
        // call SwingUtilities.invokeLater.  We actually need to force this code to run AFTER whatever the EDT is
        // currently doing.
        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setContextClassLoader(MainAppFrame.class.getClassLoader());

            appFrame = new MainAppFrame();
            appFrame.initFrame();
        });
    }

    @SuppressWarnings("unused")
    @Produces
    private IResourceManager getResourceManager()
    {
        return resourceManager;
    }

    /**
     * @return The main frame for this application, will be {@code null} when running headless or under test
     * @since 2.37
     */
    public MainAppFrame getAppFrame()
    {
        return appFrame;
    }

    /**
     * Start TAKX running in headless mode.
     */
    private void startHeadless()
    {
        final StartupArgs startupArgs = new StartupArgs(args); // So we can test for various command line flags

        // See if we're supposed to create a new project or open an existing one
        // If -create is specified without providing a project name a default name will be generated

        String projectName = startupArgs.getArgValue("project", "proj");

        if (projectName == null && startupArgs.argPresent("create"))
        {
            projectName = projectManager.getDefaultProjectName(); // Create new project name
        }

        if (projectName == null)
        {
            logger.error("I don't know what to do; use -create and/or -project to specify a project.");
            System.exit(1);
        }

        try
        {
            if (projectManager.openProject(projectName)) logger.info("Initialized and ready.");
        } catch (Exception e)
        {
            logger.error("Failed to open project: {}", projectName, e);
            System.exit(1);
        }
    }

    /**
     * Install a signal handler for `name`.
     *
     * @param name    signal to handler
     * @param handler the actual handler, to invoke {@link ApplicationController#shutdown(int)}
     */
    private void installSignalHandler(String name, InterruptHandler handler)
    {
        try
        {
            Signal.handle(new Signal(name), handler);
        } catch (IllegalArgumentException ex)
        {
            logger.error("Failed to install SIG{} handler", name);
        }
    }

    /**
     * Signal handler to cleanly shut down headless operation when an interrupt (Ctrl-C) is received.
     */
    private class InterruptHandler implements SignalHandler
    {
        @Override
        public void handle(Signal signal)
        {
            final int exitCode = 128 + signal.getNumber();
            try
            {
                logger.warn("Interrupted - closing!");

                applicationController.shutdown(exitCode);
            } catch (Throwable e)
            {
                logger.error("Failed to close project", e);
                System.exit(exitCode); // Make sure we exit
            }
        }
    }
}
