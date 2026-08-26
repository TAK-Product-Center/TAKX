package gov.takx.bootstrap;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.selector.BasicContextSelector;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.log.StreamModuleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

import static gov.takx.bootstrap.ApplicationModuleFinder.CLIENT_MODULE_ID;

/**
 * Initial entry point to bootstrap TAKX.  This is the "main" entry of the bootstrap module, loaded from the
 * command line by JBoss modules.
 * <p/>
 * Bootstrap is also responsible for validating that appropriate signatures are in place before allowing the
 * overall application to run in {@code MIL} mode.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "PMD.LooseCoupling"})
public final class Bootstrap
{
    private static final String DEFAULT_FLAVOR = FlavorConstants.CIV;

    @SuppressWarnings("NonConstantLogger") // Will get set after logging is configured
    private static Logger logger;

    private static String flavor;

    private static boolean developerMode;

    /**
     * Main entry point, called by JBoss modules.
     */
    public static void main(String[] args)
    {
        initLogging();

        final var startupArgs = new StartupArgs(args);

        closeSplashScreen(startupArgs);

        if (showHelpIfNeeded(startupArgs)) return; // Short-circuit if the user just wants to see the usage message

        determineFlavorSupport();
        runTakxClientModule(startupArgs);
    }

    /**
     * @return the flavor detected in the bootstrap jar or {@link #DEFAULT_FLAVOR} if one could not be found
     * @since 5.5
     */
    public static String getFlavor()
    {
        return flavor;
    }

    /**
     * @return {@code true} if we in "developer" mode, meaning we were signed with the developer certificate, {@code false} otherwise
     * @since 5.2
     */
    public static boolean isDeveloperMode()
    {
        return developerMode;
    }

    /**
     * Initialize the logging subsystem.
     * @since 3.2
     */
    private static void initLogging()
    {
        // Force the use of a single context across all class loaders
        System.setProperty("log4j2.contextSelector", BasicContextSelector.class.getName());

        Configurator.reconfigure(new File("bootstrap.log4j2.xml").toURI());

        logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    }

    /**
     * Determine if our jar contains the MIL flavor manifest attribute and is properly signed.
     *
     * @since 5.2 (renamed from determineMilSupport in 5.5)
     */
    private static void determineFlavorSupport()
    {
        final URL location = Bootstrap.class.getProtectionDomain().getCodeSource().getLocation();
        final String protocol = location.getProtocol();

        logger.debug("Bootstrap jar location: {}", location);

        if (!"jar".equals(protocol)) throw new IllegalStateException("Cannot locate Bootstrap jar from: " + location);

        try
        {
            try (JarFile jarFile = ((JarURLConnection) location.openConnection()).getJarFile())
            {
                String jarFlavor = FlavorUtils.getFlavor(jarFile);
                Verification verification = JarVerifier.verify(jarFile);
                logger.debug("Bootstrap verification: {} and flavor: {}", verification, jarFlavor);
                if (jarFlavor != null && verification == Verification.None)
                {
                    logger.info("No bootstrap verification, ignoring bootstrap flavor: {}", jarFlavor);
                    jarFlavor = null;
                }
                developerMode = verification == Verification.Developer;
                if (jarFlavor != null)
                {
                    flavor = jarFlavor.toUpperCase();
                } else
                {
                    logger.info("No bootstrap flavor, defaulting to {}", DEFAULT_FLAVOR);
                    flavor = DEFAULT_FLAVOR;
                }
            }
        } catch (IOException e)
        {
            throw new IllegalStateException("Cannot validate Bootstrap jar: " + location, e);
        }
    }

    /**
     * Configure JBoss modules and run the TAKX Client module.
     *
     * @since 2.37
     */
    private static void runTakxClientModule(StartupArgs args)
    {
        // Enable logging in JBoss modules, if configured
        if (args.argPresent("logModuleLoading")) Module.setModuleLogger(new StreamModuleLogger(System.out));

        // Load the client module using our custom module loader
        try
        {
            ApplicationModuleLoader.INSTANCE.loadModule(CLIENT_MODULE_ID).run(args.getOriginalArgs());
        } catch (ModuleLoadException | ReflectiveOperationException e)
        {
            logger.error("Failed to load initial module: " + CLIENT_MODULE_ID, e);
        }
    }

    /**
     * If we're headless and a graphics environment is present and a splash screen was configured, then close it.
     *
     * @since 2.37
     */
    private static void closeSplashScreen(StartupArgs args)
    {
        if (args.argPresent("headless") && !GraphicsEnvironment.isHeadless())
        {
            final SplashScreen splashScreen = SplashScreen.getSplashScreen();
            if (splashScreen != null) splashScreen.close();
        }
    }

    /**
     * Scan the args and see if any of them are a plea for help.
     *
     * @param args command line args
     * @return {@code true} if we responded to someone's call for help
     */
    private static boolean showHelpIfNeeded(StartupArgs args)
    {
        // Check to see if the user wants help and if so then display it and exit
        final boolean wantsHelp = args.argPresent("help", "?");

        if (wantsHelp)
        {
            final String helpMessage = """
                    usage: takx [options]

                    options:
                        -Dinstance=<value>          Set the TAKX instance number to enable running multiple
                                                    instances on the same machine.
                        -Dprojectdata.dir=<value>   Set the ProjectData location to an absolute path.
                        -logModuleLoading           Enable debug logging of module loading
                        -headless                   Run without a graphical user interface, useful for
                                                    running a TAKX instance from the command line.
                        -create                     Create a new project.
                                                    If '-project' is not specified, a default name is used.
                        -project <name>             Name of project to open or create.

                    Note: for JVM options run "java -help\"""";

            System.out.println(helpMessage); // NOPMD
        }

        return wantsHelp;
    }

    private Bootstrap()
    {
        /* no instances */
    }
}
