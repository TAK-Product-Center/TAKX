package gov.takx.test;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.net.auth.OAuthAccessToken;
import gov.tak.platform.client.eud.EndPoints;
import gov.tak.platform.client.eud.MapSources;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.TestProjectDestroyed;
import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.app.IAppPluginManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.persist.BackgroundDeleteManager;
import gov.takx.core.project.DefaultProjectFactory;
import gov.takx.core.project.ProjectSelection;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.test.utils.DatabaseTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;

/**
 * Abstract base class for all integration tests. It provides the basic TAKX startup handling.
 *
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AIntegrationTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public String projectName;
    public final Set<Class<? extends IRaptorPlugin>> pluginClassesToRegister = new HashSet<>();

    protected IDatabaseConnectionManager mgr;
    protected MockedStatic<MapSources> mockedMapSources;

    /**
     * Construct an integration test using the simple name of the test subclass as the project name.   The
     * program root will be set as {@link #DEFAULT_PROGRAM_ROOT}, which is appropriate for integration tests
     * in the main TAKX project.
     *
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     * @since 2.19
     */
    protected AIntegrationTest(Class<?>... classesUnderTest)
    {
        this(null, classesUnderTest);
    }

    /**
     * Construct an integration test using the supplied project name.  The program root will be set as
     * {@link #DEFAULT_PROGRAM_ROOT}, which is appropriate for integration tests in the main TAKX repo.
     *
     * @param projectName      Name of project to create.  If this project already exists, it will be deleted and recreated.
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     */
    protected AIntegrationTest(String projectName, Class<?>... classesUnderTest)
    {
        this(projectName, false, classesUnderTest);
    }

    /**
     * Construct an integration test using the supplied project name.
     *
     * @param projectName                Name of project to create.  If this project already exists,
     *                                   it will be deleted and recreated.
     * @param useWorkingDirAsProgramRoot Pass {@code true} to use the current working directory (the value of the
     *                                   system property {@code user.dir}) as the program root.  If {@code false},
     *                                   the program root will be set as {@link #DEFAULT_PROGRAM_ROOT}.
     * @param classesUnderTest           The list of classes that are under test.  This is used to locate the containing
     *                                   jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                                   and, thus, are added to the Weld container constructed for the test.
     */
    protected AIntegrationTest(String projectName, boolean useWorkingDirAsProgramRoot, Class<?>... classesUnderTest)
    {
        this(projectName,
                useWorkingDirAsProgramRoot
                        ? System.getProperty("user.dir")
                        : DEFAULT_PROGRAM_ROOT,
                classesUnderTest);
    }

    /**
     * Construct an integration test using the supplied project name and program root.
     *
     * @param projectName      Name of project to create.  If this project already exists, it will be deleted and recreated.
     * @param programRoot      Path to program root to use for project data
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     */
    protected AIntegrationTest(String projectName, String programRoot, Class<?>... classesUnderTest)
    {
        super(programRoot, classesUnderTest);

        if (logger.isInfoEnabled())
        {
            logger.info("Initialize integration test: {}, project={}", getClass().getSimpleName(), projectName);
        }

        // If no name is specified, derive it from the class name
        this.projectName = projectName == null ? getClass().getSimpleName() : projectName;

        destroyProject();
    }

    /**
     * Specify the plugin classes to register on startup.
     *
     * @param pluginClasses Varargs list of plugin classes to register
     * @since 2.19
     */
    @SafeVarargs
    protected final void setPluginsToRegister(Class<? extends IRaptorPlugin>... pluginClasses)
    {
        pluginClassesToRegister.addAll(Arrays.asList(pluginClasses));
    }

    /**
     * Add one or more plugin classes to register at startup.
     *
     * @param pluginClasses Varargs list of plugin classes to register
     * @since 2.29
     */
    @SuppressWarnings("unused")
    @SafeVarargs
    protected final void addPluginsToRegister(Class<? extends IRaptorPlugin>... pluginClasses)
    {
        pluginClassesToRegister.addAll(Arrays.asList(pluginClasses));
    }

    /**
     * Get the plugin instance for the given family/type.
     *
     * @param family plugin family
     * @param type   plugin type
     * @param <T>    Inferred type
     * @return Plugin instance
     * @throws RuntimeException If the plugin can not be found
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPluginInstance(String family, String type)
    {
        return (T) getPluginInspector().getPluginDefinition(family, type)
                .map(IPluginDefinition::getInstance)
                .orElseThrow(() -> new RuntimeException("Missing plugin: " + family + "/" + type));
    }

    /**
     * Do all the work to start a headless TAKX session.
     *
     * @throws IOException if a file operation fails
     */
    @BeforeAll
    public void setupAIntegrationTest() throws IOException
    {
        logger.info("Setup integration test: {}", projectName);

        // Initialize a static mock of MapSources to prevent network calls.
        mockedMapSources = mockStatic(MapSources.class);
        mockedMapSources.when(() -> MapSources.get(eq(EndPoints.MAP_SOURCES), (OAuthAccessToken) isNull()))
                .thenReturn(new MapSources());

        destroyProject(); // We probably have initialized the Project already

        // Since we are running in the same JVM instance as previous tests, we need to reset the
        // project factory to the default instance so we can run the "real" core.
        projectManager.setProjectFactory(new DefaultProjectFactory());

        // Load the plugin registries
        pluginRegistryManager.loadRegistries();

        registerPlugins();

        // If the project directory exists, nuke it for a fresh start
        File projectDir = projectManager.getProjectDirectory(projectName);
        if (projectDir.exists())
        {
            FileUtils.deleteDirectory(projectDir);
        }

        openProject(true);

        postProjectOpen();
    }

    /**
     * Subclasses can override this method to perform any activities they need after the project has been opened,
     * such as adding initial data to the database or obtaining references to platform services.
     */
    protected void postProjectOpen()
    {
        /* nothing here */
    }

    /**
     * Register all the plugins that have been specified by {@link #setPluginsToRegister(Class[])}.
     * <p>
     * Also, Ensure the hypersonic DB plugin is registered.
     */
    private void registerPlugins()
    {
        // Verify that all the plugin classes being used are configured for AOP weaving
        final Set<String> pluginClassNames = pluginClassesToRegister.stream()
                .map(Class::getName)
                .collect(Collectors.toSet());

        validateWeaving(pluginClassNames);

        pluginClassesToRegister.forEach(pluginRegistryManager::registerPlugin);
    }

    protected void openProject(boolean createNew) throws IOException
    {
        final NullProgressMonitor progressMonitor = new NullProgressMonitor();
        final String projectPath = projectManager.getProjectFilePath(projectName);

        ProjectSelection projectSelection = createNew
                ? ProjectSelection.makeNew(projectPath, null, true)
                : ProjectSelection.makeExisting(projectPath);

        logger.debug("Open project: {}", projectPath);

        assertThat(projectManager.isValidSession()).isFalse();

        projectManager.openProjectForTest(projectSelection, progressMonitor, () -> {
            preLoadState(); // Give subclasses a chance to do some setup
            DatabaseTestUtils.setupDatabase(createNew);
        }, true);

        mgr = getBean(IPersistServiceProvider.class).getPersistService().getConnectionMgr();
    }

    /**
     * Subclasses can override this method to perform any processing they want just prior to the loadState operation.
     */
    protected void preLoadState()
    {
    }

    /**
     * Check the event log model count against the expected number of entries. Event log inserts are done on the EDT,
     * so this check must be done on the EDT as well.
     *
     * @since 1.25.7
     */
    protected void checkEventLogEntryCount(final int expectedEventCount)
    {
        final IEventLogPersist eventLogPersist = getBean(IEventLogPersist.class);
        assertThat(eventLogPersist.countDate(null, null)).isEqualTo(expectedEventCount);
    }

    /**
     * Tear down the TAKX project session and database properly.
     */
    @AfterAll
    public void teardownAIntegrationTest()
    {
        logger.info("Tearing down {}", projectName);

        try
        {
            onTeardown();
        } catch (Exception e)
        {
            logger.error("teardown failed on: {}", projectName, e);
        }

        destroyProject();

        mgr.dropTables();

        // Close the static MapSources mock
        mockedMapSources.close();

        // Be clean - remove the project directory that we created.
        File projectDir = projectManager.getProjectDirectory(projectName);
        if (projectDir.exists())
        {
            FileUtils.deleteDirectory(projectDir);
        }
    }

    /**
     * Perform any post-test cleanup.  Use this method instead of directly overriding teardown().  That ensures
     * we can trap and report exceptions taking place during shutdown.  TestNG will otherwise silently ignore the
     * exceptions, and you get to scratch your head for an hour wondering why the database wasn't cleaned up
     * properly.
     */
    protected void onTeardown()
    {
    }

    /**
     * Shutdown the core - close the project session.
     */
    protected void shutdown()
    {
        logger.info("shutdown: {}", projectName);

        final NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();
        container.event().select(ShuttingDown.class).fire(new ShuttingDown(nullProgressMonitor));

        getBean(BackgroundDeleteManager.class).suspendWork(); // Have to do this before we tear down persistence
        projectManager.closeProject(nullProgressMonitor);

        container.event().select(TestProjectDestroyed.class).fire(new TestProjectDestroyed());
        stopService(IPersistServiceProvider.class);
    }

    /**
     * Reboot the core.  This will restart the database, load the plugin registry,
     * login, and re-open the project.
     * <p/>
     * Note: this should only be called after {@link #shutdown()}.
     *
     * @throws IOException If opening the project fails
     */
    protected void reboot() throws IOException
    {
        logger.info("reboot: {}", projectName);

        mgr.closeAllSessions(false);

        // Load the plugin registries
        pluginRegistryManager.loadRegistries();

        registerPlugins();

        openProject(false);

        postProjectOpen();
    }

    /**
     * Start the specified app, installing it if necessary.
     *
     * @param family  App plugin family
     * @param type    App plugin type
     * @param <T>     Inferred plugin type
     * @return The app plugin
     * @since 2.20 (moved here from APluginIntegrationTest)
     */
    protected <T> T startApp(String family, String type, String ignored) throws Exception
    {
        getBean(IAppPluginManager.class).getApp(family, type).start(); // Start the app

        //noinspection unchecked
        return (T) getPluginInspector().getPlugin(family, type);
    }

    /**
     * Given an app plugin instance (usually created by {@link #startApp(String, String, String)}), stop the plugin.
     *
     * @param appPlugin The app plugin to stop
     * @since 5.7
     */
    protected void stopApp(IAppPlugin appPlugin)
    {
        final var appInstance = getBean(IAppPluginManager.class).getInstalledAppByClass(appPlugin.getClass());
        if (appInstance != null)
        {
            //noinspection CatchMayIgnoreException
            try
            {
                appInstance.stop();
            } catch (Exception e)
            {
                fail("Error uninstalling app during test: " + appPlugin.getClass(), e);
            }
        }
    }

    /**
     * Given an app plugin instance (usually created by {@link #startApp(String, String, String)}), stop the plugin.
     *
     * @param appPlugin The app plugin to stop
     * @since 2.20 (moved here from APluginIntegrationTest)
     * @deprecated since 5.7, use {@link #stopApp(IAppPlugin)} instead
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    protected void stopAndUninstallApp(IAppPlugin appPlugin)
    {
        stopApp(appPlugin);
    }
}
