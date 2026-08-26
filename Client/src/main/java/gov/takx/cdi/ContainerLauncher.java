package gov.takx.cdi;

import com.atakmap.map.EngineLibrary;
import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.jogamp.opengl.GLProfile;
import gov.tak.api.engine.map.RenderContextFactory;
import gov.takx.api.url.Schemes;
import gov.takx.bootstrap.ApplicationUrlStreamHandlerFactory;
import gov.takx.bootstrap.Bootstrap;
import gov.takx.bootstrap.DeploymentRoots;
import gov.takx.bootstrap.StartupArgs;
import gov.takx.bootstrap.Version;
import gov.takx.core.configuration.BareApplicationInstance;
import gov.takx.core.configuration.BareProgramEnvironment;
import gov.takx.core.mapobject.MapObjectChangeMonitor;
import gov.takx.core.platform.CoreExecutorService;
import gov.takx.dataservices.persist.ARaptorMessage;
import gov.takx.logging.SystemMessageAppender;
import gov.takx.platform.cdi.AnnotationIndexer;
import gov.takx.platform.cdi.ContainerBuilder;
import gov.takx.url.Base64UrlStreamHandler;
import gov.takx.url.IconSetUrlStreamHandler;
import gov.takx.url.UiResourceUrlStreamHandler;
import gov.takx.url.VfsUrlStreamHandler;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.weld.environment.se.StartMain;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.WeldSEProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * This is the actual main routine for TAKX. It can not be invoked directly from the command line. You must
 * use the bootstrap to invoke this "main" as a module.
 *
 * @see Bootstrap
 * @since 2.0.0 (renamed from RaptorContainerLauncher in 3.4)
 */
public class ContainerLauncher
{
    @SuppressWarnings("NonConstantLogger") // Will get set after logging is configured
    private static Logger logger;

    private static String deploymentName;

    // Our pre-CDI environment
    private static final BareApplicationInstance instance = new BareApplicationInstance();
    private static final BareProgramEnvironment programEnvironment = new BareProgramEnvironment(instance);

    // The alternatives we want to activate during standard execution
    protected static final Class<?>[] ALTERNATIVES = {CoreExecutorService.class, MapObjectChangeMonitor.class};

    // An empty extensions set
    protected static final Extension[] NO_EXTENSIONS = {};

    /**
     * The actual MAIN entry point to startup TAKX. We just crank up the CDI container and the rest takes care of itself.
     *
     * @param args Command line args
     */
    public static void main(String[] args)
    {
        initLogging();

        setInitialProperties();
        configureTakKernel();
        installUrlProtocolHandlers();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Thread unexpectedly terminated with exception: {}, {}", t.getName(), e.getMessage(), e));

        if (logger.isDebugEnabled()) logger.debug("Launch args: {}", Arrays.toString(args));

        final Module module = Module.forClass(ContainerLauncher.class);

        //noinspection resource
        final WeldContainer container = initializeContainer(module, Collections.emptySet(), ALTERNATIVES, NO_EXTENSIONS, args);

        // Now that our container has been initialized, we can add our system message appender
        container.select(SystemMessageAppender.class).get().start();

        // And finally fire the event that ApplicationLauncher is waiting for
        container.event().select(LaunchApplicationEvent.class).fire(new LaunchApplicationEvent());
    }

    /**
     * Install our custom URL protocol handlers.
     * @since 5.6
     */
    private static void installUrlProtocolHandlers()
    {
        ApplicationUrlStreamHandlerFactory.registerStreamHandler(Schemes.BASE64, new Base64UrlStreamHandler());
        ApplicationUrlStreamHandlerFactory.registerStreamHandler(Schemes.ICONSET, new IconSetUrlStreamHandler());
        ApplicationUrlStreamHandlerFactory.registerStreamHandler(Schemes.UI_RESOURCE, new UiResourceUrlStreamHandler());
        ApplicationUrlStreamHandlerFactory.registerStreamHandler(Schemes.VFS, new VfsUrlStreamHandler());
    }

    /**
     * Configure the static elements of takkernel that must be done before we can stand up our container.
     */
    private static void configureTakKernel()
    {
        // Make sure we register our RenderContext SPI before the EngineLibrary
        RenderContextFactory.registerSpi(gov.takx.core.globe.tak.JOGLRenderContext.SPI);

        // Configure the databases prior to starting, the device id is used as part of the cipher key
        // for storing encrypted data in the databases.
        String deviceId = "TAKX";

        AtakCertificateDatabase.setDeviceId(deviceId);
        AtakAuthenticationDatabase.setDeviceId(deviceId);

        // Force the engine runtime files (like databases) to be placed in a specific named directory by setting
        // TAK_APP_DATA_PATH. For some reason, if this is not done, things will work at normal runtime, but when running
        // tests this initialization will fail with an ArrayIndexOutOfBoundsException.

        System.setProperty("TAK_APP_DATA_PATH", programEnvironment.getGlobalFilesDir() + "/kernel");
        EngineLibrary.initialize();
    }

    /**
     * Initialize log4j logging.
     *
     * @since 2.37
     */
    private static void initLogging()
    {
        if (!instance.isDefaultInstance())
        {
            // Set the property used in generating the log file name, see log4j-appenders.xml
            System.setProperty("takx.instance.suffix", "-" + instance.getInstanceNumber());
        }

        if (Version.getPreReleaseTag().contains("SNAPSHOT"))
        {
            System.setProperty("LOG_BUILD", "DEBUG");
        }

        // Install our logging configuration
        Configurator.reconfigure(new File("log4j2.xml").toURI());

        logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        final Configuration configuration = LoggerContext.getContext(false).getConfiguration();

        logger.info("log4j will watch for modifications to '{}' every {} seconds.",
                configuration.getConfigurationSource().getLocation(),
                configuration.getWatchManager().getIntervalSeconds());

        // The takkernel, due to its ported implementation of the Android "Log" class, is misusing SLF4J in a way
        // that makes log4j receive an argument list where it can't determine that the last element is an instance of
        // Throwable when it is null, causing it to emit warnings about mismatched placeholders and arguments.
        // So, set the status logger's level to ERROR to prevent a constant stream of warning messages.
        StatusLogger.getLogger().getFallbackListener().setLevel(Level.ERROR);
    }

    /**
     * Main routine invoked by our test infrastructure. Build a minimal container, but don't bring up the core.
     *
     * @param testRootClassNames Names of classes to use to locate any test roots
     * @param alternatives       The alternatives to activate
     * @param extensions         Extensions to activate
     * @return the CDI container
     */
    public static WeldContainer testMain(Set<String> testRootClassNames, Class<?>[] alternatives, Extension... extensions)
    {
        // Under test, we don't need to set the normal SSL properties or the WW configuration,
        // so, don't call setInitialProperties.
        //
        // In fact, installing our custom WW configuration will cause some tests to fail.

        configureTakKernel();
        installUrlProtocolHandlers();

        // Since we're not running under JBoss Modules, we have to install our URL stream handler factory directly
        URL.setURLStreamHandlerFactory(new ApplicationUrlStreamHandlerFactory());

        final WeldContainer container =
                initializeContainer(null,
                        testRootClassNames,
                        alternatives,
                        extensions,
                        "-reindex", // Force a reindex when testing
                        "-headless" // Force headless when testing
                );

        // And fire the event that will make ApplicationLauncher setup for a test
        container.event().select(LaunchTestContainerEvent.class).fire(new LaunchTestContainerEvent());

        return container;
    }

    /**
     * Set all the system properties we need prior to actual launch.
     */
    private static void setInitialProperties()
    {
        // Set the location of the World Wind configuration file
        System.setProperty("gov.nasa.worldwind.config.file", "conf/worldwind.properties");
    }

    /**
     * Initialize the CDI container we'll be running the core in.
     *
     * @param module             The module defining our class path, {@code null} if not using modules (as in testing)
     * @param testRootClassNames Names of classes to use to locate any test roots (should be empty when not running tests)
     * @param alternatives       Alternative classes to enable
     * @param args               Command line arguments
     * @return The container
     */
    private static WeldContainer initializeContainer(Module module, Set<String> testRootClassNames, Class<?>[] alternatives, Extension[] extensions, String... args)
    {
        final boolean testing = module == null;

        deploymentName = testing ? "test-takx" : module.getName();

        final StartupArgs startupArgs = new StartupArgs(args);

        final Index index = prepareIndex(testRootClassNames, startupArgs); // Get the annotation index created and internalized

        if ("true".equals(System.getProperty("renderdoc", "false")))
        {
            System.setProperty("heavyweight-globe", "true");
            GLProfile.initSingleton();
        }

        // Ok, ready to launch. Bring up the CDI container.

        CDI.setCDIProvider(new WeldSEProvider());

        StartMain.PARAMETERS = args; // Enable use of @Parameters annotation

        WeldContainer container = ContainerBuilder.forCore(module)
                .useIndex(index) // Start with the basics
                .setClassLoader(ContainerLauncher.class.getClassLoader())
                // For now, comment out the use of the RunnableDecorator since we don't actually use @ThreadScope, yet,
                // so no loss of functionality by turning it off
                //
//                .addDecorator(RunnableDecorator.class) // Since we use ThreadScoped, we need to add the RunnableDecorator
                .alternatives(alternatives) // Enable the specified set of alternatives
                .extensions(extensions) // Enable the specified set of extensions
                .initialize();

        // By setting the container on the ARaptorMessage base class, we can avoid performing an injection
        // operation on every RDM created.
        ARaptorMessage.setContainer(container);

        return container;
    }

    /**
     * Prepare the index and inject it into our class transformer. Force a re-index if the user specified
     * the -reindex flag. If the index is missing, it is constructed automatically.
     *
     * @param testRootClassNames Names of classes to use to locate any test roots (should be empty when not running tests)
     * @param args               Program arguments
     */
    private static Index prepareIndex(Set<String> testRootClassNames, StartupArgs args)
    {
        // See if we should rebuild the annotation index prior to startup
        final Set<File> deploymentRoots = DeploymentRoots.getClientDeploymentRoots(testRootClassNames);
        final AnnotationIndexer indexer = new AnnotationIndexer(programEnvironment.getProjectDataDirectory(), deploymentName, deploymentRoots);

        if (args.argPresent("reindex"))
        {
            indexer.recreateIndex();
        }

        final Index index = indexer.readIndex();

        if (index == null)
        {
            logger.error("The annotations index is missing!");
            System.exit(1);
        }

        return index;
    }
}
