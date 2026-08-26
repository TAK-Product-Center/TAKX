package gov.takx.core.configuration;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.bootstrap.StartupArgs;
import gov.takx.bootstrap.Version;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.cdi.ContainerBuilder;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.environment.se.bindings.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Manages information and setup of the running environment.  It ensures the program root directory is
 * properly initialized and generates various path names to elements within the execution environment.
 */
@InjectableService(registerAs = IProgramEnvironment.class)
@ApplicationScoped
public class ProgramEnvironment extends BareProgramEnvironment implements IProgramEnvironment
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // System property key to enable/disable concurrent CDI container deployment. Defaults to true.
    // If false, then the Weld CDI handling will also be forced to not use concurrency.
    private static final String SYS_PROP_CONCURRENT_DEPLOYMENT = "concurrentDeployment";

    // Property key to use to enable/disable concurrent deployment handling Weld
    private static final String SYS_PROP_WELD_CONCURRENT_DEPLOYMENT = "org.jboss.weld.bootstrap.concurrentDeployment";

    private static final String DEFAULT_GLOBE_LAYERS_XML = "conf/DefaultGlobeLayers.xml";
    public static final String DEFAULT_DOCKING_XML = "conf/DefaultDocking.xml";

    static final String GLOBE_LAYERS_XML = "GlobeLayers.xml";
    public static final String DOCKING_XML = "Docking.xml";

    private static final String BUILD_CONFIGURATION_FILE_NAME = "conf/BuildConfiguration.txt";
    private static final String GL_CLASS_NAME = "com.jogamp.opengl.GL";

    private final List<String> args;
    private final WeldContainer container;

    private boolean platformUnderTest;   // Assume not under test

    // Whether we are running headless (with no Swing UI).
    protected boolean headless;

    private boolean concurrentDeployment = true; // Assume concurrent plugin scanning/deployment

    private String projectRoot;
    private File globeLayersFile;
    private File layersDir;

    @Inject
    ProgramEnvironment(BareApplicationInstance applicationInstance, WeldContainer container, @Parameters List<String> args)
    {
        super(applicationInstance);
        this.container = container;
        this.args = args;
    }

    @PostConstruct
    private void postConstruct()
    {
        final StartupArgs startupArgs = new StartupArgs(args);

        // Initialize "under test" based on the container name
        platformUnderTest = container.getId().equals(ContainerBuilder.TEST_CONTAINER_NAME);

        // Being under test forces headless
        if (platformUnderTest || startupArgs.argPresent("headless")) setHeadless(true);

        determineConcurrency();

        try
        {
            createGlobeLayersFile();
            createLayersDirectory();
            createDockingFile();
        } catch (IOException e)
        {
            logger.error("Failed to initialize program environment", e);
        }
    }

    /**
     * @return Produces a {@code boolean} value indicating if we are in headless mode or not
     */
    @Produces
    @Headless
    private boolean produceHeadless()
    {
        return headless;
    }

    /**
     * @return if we are running headless
     */
    @Override
    public boolean isHeadless()
    {
        return headless;
    }

    /**
     * Set if this TAKX session is to operate in a headless fashion. When running headless, no Swing UI
     * components will be created, but the core system will function as normal.
     *
     * @param headless Pass true to run headless
     */
    @Override
    public void setHeadless(boolean headless)
    {
        this.headless = headless;
        AlwaysOnTopJOptionPane.setHeadless(this.headless, platformUnderTest);
    }

    @Override
    public boolean isPlatformUnderTest()
    {
        return platformUnderTest;
    }

    /**
     * @return Produces a {@code boolean} value indicating if the platform is under test
     * @since 2.36
     */
    @Produces
    @PlatformUnderTest
    private boolean producePlatformUnderTest()
    {
        return platformUnderTest;
    }

    @Override
    public boolean isConcurrentDeployment()
    {
        return concurrentDeployment;
    }

    @Override
    public File getInstanceProgramPropertiesFile()
    {
        return new File(getProgramPropertiesPath(applicationInstance.getInstanceNumber()));
    }

    @Override
    public File getInstanceUserPreferencesFile()
    {
        return new File(getUserPreferencesPath(applicationInstance.getInstanceNumber()));
    }

    @Override
    public File getInstanceToolPreferencesFile()
    {
        return new File(getToolPreferencesPath(applicationInstance.getInstanceNumber()));
    }

    @Override
    public String getPlatformVersionString()
    {
        return Version.getVersionString();
    }

    @Override
    public String getVersionPreReleaseTag()
    {
        return Version.getPreReleaseTag().trim();
    }

    @Override
    public String getProgramBuild()
    {
        return Version.getShortBuildNumber();
    }

    @Override
    public String getProgramBuildDate()
    {
        return Version.getBuildDate();
    }

    @Override
    public String getGlobeLayersFilePath()
    {
        return getProgramRoot() + File.separator + PROJECT_DATA + File.separator + GLOBE_LAYERS_XML;
    }

    @Override
    public String getDockingFilePath()
    {
        return getProgramRoot() + File.separator + PROJECT_DATA + File.separator + DOCKING_XML;
    }

    @Override
    public String getGlobeLayersDirectoryPath()
    {
        return getProgramRoot() + File.separator + PROJECT_DATA + File.separator + "layers";
    }

    @Override
    public File getGlobeLayersFile()
    {
        return globeLayersFile;
    }

    @Override
    public File getGlobeLayersConfigurationDirectory()
    {
        return layersDir;
    }

    /**
     * @return The configured program name, from the preference {@link PreferenceEntry#NAME}
     */
    public String getProgramName()
    {
        return PreferenceEntry.NAME.getStringValue();
    }

    @Override
    public String getProgramNameVersionAndBuild()
    {
        return getProgramName()
                + "-" + PreferenceEntry.PRODUCT_FLAVOR.getStringValue()
                + " Version " + getProgramDisplayVersion() + " Patch " + getProgramDisplayPatch()
                + " Build " + getProgramBuild() + ", built on " + getProgramBuildDate();
    }

    /**
     * @return The platform version minus the patch number, but including the pre-release tag
     */
    public String getProgramDisplayVersion()
    {
        String version = getPlatformVersionString();
        int lastIdx = version.lastIndexOf('.');
        version = lastIdx > 0 ? version.substring(0, lastIdx) : version;

        final String preReleaseTag = getVersionPreReleaseTag();
        if (!preReleaseTag.isEmpty()) version += " " + preReleaseTag;
        return version;
    }

    private String getProgramDisplayPatch()
    {
        return String.valueOf(getPlatformVersion().getPatch());
    }

    /**
     * Obtain the {@link PlatformVersion} instance from the version specified in the version preferences
     *
     * @return {@link PlatformVersion} instance
     * @since 1.3
     */
    @Produces
    public PlatformVersion getPlatformVersion()
    {
        return PlatformVersion.CURRENT_PLATFORM_VERSION;
    }

    /**
     * Set the root of the project directory (i.e. the directory that contains the project directories.
     * TODO: This seems disconnected from {@link #getProjectDataDirectory()}
     *
     * @param projectRoot Project root to use
     */
    public void setProjectRoot(String projectRoot)
    {
        this.projectRoot = projectRoot;
    }

    /**
     * @return the project root directory as set with {@link #setProjectRoot(String)}.
     */
    public String getProjectRoot()
    {
        return projectRoot;
    }

    /**
     * See if we're allowed to perform concurrent plugin deployment handling.
     */
    private void determineConcurrency()
    {
        concurrentDeployment = Boolean.parseBoolean(System.getProperty(SYS_PROP_CONCURRENT_DEPLOYMENT, "true"));

        // If it's not enabled, then also disable concurrency in Weld.
        if (!concurrentDeployment)
        {
            logger.info("Deployment concurrency disabled");

            System.setProperty(SYS_PROP_WELD_CONCURRENT_DEPLOYMENT, "false");
        }
    }

    private void createGlobeLayersFile() throws IOException
    {
        globeLayersFile = new File(getGlobeLayersFilePath());

        if (!globeLayersFile.exists())
        {
            FileUtils.copyFileOrResource(DEFAULT_GLOBE_LAYERS_XML, globeLayersFile);
        }
    }

    private void createLayersDirectory() throws IOException
    {
        layersDir = new File(getGlobeLayersDirectoryPath());

        if (!layersDir.exists())
        {
            FileUtils.copy(new File("conf", "layers"), layersDir);
        }
    }

    private void createDockingFile() throws IOException
    {
        final File dockingFile = new File(getDockingFilePath());

        if (!dockingFile.exists())
        {
            FileUtils.copyFileOrResource(DEFAULT_DOCKING_XML, dockingFile);
        }
    }

    /**
     * Get a string that shows the primary properties and settings in use.
     *
     * @return String description
     */
    public String getProgramProperties()
    {
        final StringBuilder sb = new StringBuilder();

        final String appTitle = PreferenceEntry.APPLICATION_TITLE.getStringValue();
        final String appName = PreferenceEntry.NAME.getStringValue();

        if (appTitle != null && !appName.equalsIgnoreCase(appTitle)) sb.append(appTitle).append("\nRunning on: ");

        sb.append(getProgramNameVersionAndBuild()).append('\n');

        // Pull the build configuration content
        final File buildConfigFile = new File(BUILD_CONFIGURATION_FILE_NAME);
        if (buildConfigFile.exists())
        {
            var configLines = FileUtils.readLines(buildConfigFile);
            sb.append("Build Configuration: ").append(StringUtils.join(configLines, ", ", null)).append('\n');
        } else
        {
            logger.debug("File {} does not exist.", buildConfigFile);
        }
        sb.append('\n');

        sb.append("OS   : ").append(SystemUtils.OS_NAME).append(", version ").append(SystemUtils.OS_VERSION)
                .append(", arch ").append(SystemUtils.OS_ARCH).append('\n');
        sb.append("JVM  : ").append(SystemUtils.JAVA_VERSION).append(" (").append(SystemUtils.JAVA_VM_VENDOR)
                .append('-').append(SystemUtils.JAVA_VM_VERSION).append(")\n");
        sb.append("JOGL : ").append(getJoglVersion()).append('\n');

        final Runtime runtime = Runtime.getRuntime();
        sb.append("\nSystem Properties:\n");
        sb.append("   Processors      : ").append(runtime.availableProcessors()).append('\n');
        sb.append("   Free JVM memory : ").append(SizeFormatter.formatEstimate(runtime.freeMemory())).append('\n');
        sb.append("   Max JVM memory  : ").append(SizeFormatter.formatEstimate(runtime.maxMemory())).append('\n');
        sb.append("   Total JVM memory: ").append(SizeFormatter.formatEstimate(runtime.totalMemory())).append('\n');

        return sb.toString();
    }

    /**
     * @return The JOGL implementation version
     * @since 3.4
     */
    private String getJoglVersion()
    {
        try
        {
            final Package pkg = getClass().getClassLoader().loadClass(GL_CLASS_NAME).getPackage();
            return pkg == null
                    ? "ERROR: Can't get package containing " + GL_CLASS_NAME
                    : pkg.getImplementationVersion();
        } catch (ClassNotFoundException e)
        {
            return "ERROR: Can't load class " + GL_CLASS_NAME;
        }
    }
}
