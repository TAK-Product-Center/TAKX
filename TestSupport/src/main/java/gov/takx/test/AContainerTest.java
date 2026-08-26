package gov.takx.test;

import com.atakmap.map.EngineLibrary;
import com.google.common.io.Resources;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.TestProjectDestroyed;
import gov.takx.api.event.cdi.PseudoBeanCreationEvent;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.bootstrap.AopClassTransformer;
import gov.takx.bootstrap.DeploymentRoots;
import gov.takx.bootstrap.PreferencesFactoryConstants;
import gov.takx.cdi.ContainerLauncher;
import gov.takx.client.platform.ApplicationController;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.registry.PluginInspectorManager;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.module.PluginDefinitionFactory;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.InjectionUtils;
import gov.takx.test.alternative.DirectExecutorService;
import gov.takx.test.mock.mapobject.TestMapObjectChangeMonitor;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.util.TypeLiteral;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.resolution.Resolvable;
import org.jboss.weld.resolution.ResolvableBuilder;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.takx.platform.logging.LoggerUtils.LOG4J_CONFIGURATION;
import static gov.takx.platform.logging.LoggerUtils.LOG4J_PROPERTIES;
import static gov.takx.platform.logging.LoggerUtils.LOG4J_XML;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for a test class that needs a minimally configured TAKX CDI container initialized
 * before the test code runs.
 * <p>
 * It is assumed that the tests have been run with our AOP agent installed (via -javaagent).
 *
 * @since 2.0.0
 */
@ExtendWith(BlacksmithExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public abstract class AContainerTest
{
    @SuppressWarnings("NonConstantLogger")
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static boolean loggerInitialized;

    // The default program root location, set to the current temp directory.
    public static String DEFAULT_PROGRAM_ROOT = System.getProperty("java.io.tmpdir");

    static
    {
        // Configure our custom Preferences factory
        System.setProperty("java.util.prefs.PreferencesFactory", PreferencesFactoryConstants.PREFERENCES_FACTORY_CLASS);

        try
        {
            // Get full path to temp.  This removes the DOS 8.3 use of '~' characters on Windows.
            // The takkernel reports the use of '~' as a "soft" exception, this quiets those warnings.
            // This isn't a problem with core tests, but exceptions are reported on plugin tests.
            DEFAULT_PROGRAM_ROOT = new File(DEFAULT_PROGRAM_ROOT).getCanonicalPath();
        } catch (IOException ignored)
        {
        }
    }

    protected static final String FALLBACK_LOG4J_XML = "/fallback-log4j2.xml";

    // The alternatives we want to activate during testing
    protected static final Class<?>[] ALTERNATIVES = {DirectExecutorService.class, TestMapObjectChangeMonitor.class};

    private static boolean initialized;
    private static Set<String> initialTestClasses; // Initial set of classes under test
    private static Set<File> initialDeploymentRoots; // Initial set of deployment roots determined from initialTestClasses
    private static String initialTestClass; // Name of test that caused initialization of initialTestClasses

    // Cached observer methods keyed by observing type
    private static final Map<Class<?>, Set<ObserverMethod<?>>> observerMethodsByType = new ConcurrentHashMap<>();

    protected static WeldContainer container;
    protected static BeanManager beanManager;

    protected static PluginRegistryManager pluginRegistryManager;
    protected static PluginInspectorManager pluginInspectorManager;
    protected static ProgramEnvironment programEnvironment;
    protected static IPreferenceManager preferenceManager;
    protected static ICoreAppManager appManager;
    protected static IProjectManager projectManager;
    protected static ApplicationController applicationController;
    protected static PluginDefinitionFactory pluginDefinitionFactory;

    /**
     * Construct and initialize the container if it hasn't been already.
     *
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     */
    protected AContainerTest(Class<?>... classesUnderTest)
    {
        initialize(DEFAULT_PROGRAM_ROOT, classesUnderTest);
    }

    /**
     * Construct using the given program root.
     *
     * @param programRoot      Program root to set prior to initializing the container
     * @param classesUnderTest The list of classes that are under test.  This is used to locate the containing
     *                         jar/directory so all classes included there are scanned for Bean defining Annotations,
     *                         and, thus, are added to the Weld container constructed for the test.
     */
    protected AContainerTest(String programRoot, Class<?>... classesUnderTest)
    {
        initialize(programRoot, classesUnderTest);
    }

    /**
     * Configure the blacksmith extension to pull bean information from the {@link #container}.
     * Any {@link gov.tak.blacksmith.mock.Mocked} requested will be provided by Mockito,
     * and the resulting instance will be cached for the lifespan of the current test class.
     */
    @BeforeAll
    public final void setupBlacksmith()
    {
        if (container == null) throw new IllegalStateException("SeContainer has not been initialized");
        MockStrategy strategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(strategy)
                .withContainer(container));
    }

    /**
     * Start the service of the given class by sending it a {@link StartProjectServices} event.
     *
     * @param serviceClass Class of service to start
     */
    public void startService(Class<?> serviceClass)
    {
        fireEventToServiceInstance(serviceClass, new StartProjectServices(new NullProgressMonitor()));
    }

    /**
     * Start the service of the given class by sending it a {@link StartPlugins} event.
     *
     * @param serviceClass Class of service to start
     */
    public void startServiceUsingStartPlugins(Class<?> serviceClass)
    {
        fireEventToServiceInstance(serviceClass, new StartPlugins(new NullProgressMonitor()));
    }

    /**
     * Stop the service of the given class by sending it a {@link StopProjectServices} event.
     *
     * @param serviceClass Class of service to stop
     */
    public void stopService(Class<?> serviceClass)
    {
        fireEventToServiceInstance(serviceClass, new StopProjectServices(new NullProgressMonitor()));
    }

    /**
     * Fire a given event to the observer method(s) on the requested service type.  If the given service type is an
     * interface, then it must resolve to a single bean implementing that interface.  If there is more than
     * one such implementation, an exception will be thrown.
     *
     * @param serviceType Service ype to which to fire the event
     * @param event       The event to fire to observer methods on that service
     * @param <S>         Inferred service type
     * @param <T>         Inferred event type
     * @since 3.0
     */
    @SuppressWarnings("unchecked")
    protected <S, T> void fireEventToServiceInstance(Class<S> serviceType, T event)
    {
        final Bean<S> bean = container.select(serviceType).getHandle().getBean();

        // Use the bean class in case an interface was passed in.  We need to resolve observers against the
        // bean instance (which has the observer method), and not the interface (which doesn't).
        final Class<S> beanClass = (Class<S>) bean.getBeanClass();

        final Set<ObserverMethod<?>> observerMethods = observerMethodsByType.computeIfAbsent(serviceType,
                k -> InjectionUtils.getAllObserverMethods(beanManager, beanClass, () -> bean));

        if (logger.isTraceEnabled())
        {
            logger.trace("Observer Methods: {}: {}", serviceType.getSimpleName(), observerMethods);
        }

        // Resolve the observer methods that match the event
        final Resolvable resolvable = new ResolvableBuilder(serviceType, BeanManagerProxy.unwrap(beanManager))
                .addTypes(new HierarchyDiscovery(event.getClass()).getTypeClosure())
                .create();

        final var resolvedObservers = InjectionUtils.resolveObservers(beanManager, resolvable, observerMethods);

        logger.trace("Resolved observers: {}", resolvedObservers);

        // And invoke all matching observer methods
        resolvedObservers.forEach(o -> ((ObserverMethod<Object>) o).notify(event));
    }

    /**
     * Initialize our test environment:
     * <ul>
     * <li>Determine our set of class roots (for CDI scanning)</li>
     * <li>Set the {@link ProgramEnvironment#SYSPROP_PROJECTDATA_DIR} environment variable to the given {@code programRoot}</li>
     * <li>Crank up a minimal CDI container</li>
     * </ul>
     */
    private void initialize(String programRoot, Class<?>... classesUnderTest)
    {
        initLogging();

        final Set<String> testRootClassNames = validateClassesUnderTest(programRoot, classesUnderTest);

        if (initialized) return; // Already initialized and loaded

        // Ensure that all tests run with WMS access set to offline
        Configuration.setValue(AVKey.OFFLINE_MODE, "true");

        System.setProperty(ProgramEnvironment.SYSPROP_PROJECTDATA_DIR, programRoot);

        logger.debug("Test Roots: {}", testRootClassNames);

        // Now crank up the container
        container = ContainerLauncher.testMain(testRootClassNames, ALTERNATIVES,
                new SelfRegisteringExtension(this),
                new BlacksmithManagedBeanExtension());
        beanManager = container.getBeanManager();

        // Grab managed beans for use within the tests
        programEnvironment = getBean(ProgramEnvironment.class);
        preferenceManager = getBean(IPreferenceManager.class);
        pluginRegistryManager = getBean(PluginRegistryManager.class);
        pluginInspectorManager = getBean(PluginInspectorManager.class);
        appManager = getBean(ICoreAppManager.class);
        projectManager = getBean(IProjectManager.class);
        applicationController = getBean(ApplicationController.class);
        pluginDefinitionFactory = getBean(PluginDefinitionFactory.class);

        // Delete our project data directory when the JVM exits
        String projectDataDirectory = programEnvironment.getProjectDataDirectory(); // programEnvironment is not valid after container TEST shutdown
        TestCleanup.addCleanupAction(() -> FileUtils.deleteDirectory(new File(projectDataDirectory)));

        PreferenceEntry.RDM_BATCHING.put(false); // Always disable RDM batching

        // Enable the EventLogManager, even if it is disabled by default
        PreferenceEntry.ENABLE_EVENT_LOG.put(true);

        // Ensure that any unit tests have the natives needed by the TAK Kernel loaded prior to executing.
        EngineLibrary.initialize();

        initialized = true;
    }

    /**
     * Validate that EVERY container test uses the same deployment roots (as determined from the classes under test).
     * <p>
     * The fact that the gradle/testNG test scanning code essentially selects a random class as
     * the first one to instantiate leads to all kinds of difficult to diagnose problems if all the tests
     * don't specify the same set of classesUnderTest (more correctly, the deployment roots determined from
     * those classes).
     *
     * @param programRoot      Program root to set prior to initializing the container
     * @param classesUnderTest Classes under test as specified by this test class
     * @return Set of names of classes under test to process
     * @throws IllegalStateException If the current deployment roots are not the same as the initial roots
     * @since 2.36
     */
    private Set<String> validateClassesUnderTest(String programRoot, Class<?>... classesUnderTest) throws IllegalStateException
    {
        final Set<String> classesUnderTestNames =
                Stream.concat(getTestRootClassNames().stream(), Stream.of(classesUnderTest)
                        .map(Class::getName))
                        .collect(Collectors.toSet());

        if (initialized)
        {
            // Verify that different test classes don't try to initialize with different program roots
            final String currentRoot = System.getProperty(ProgramEnvironment.SYSPROP_PROJECTDATA_DIR);
            if (!programRoot.equals(currentRoot))
            {
                throw new IllegalStateException("Conflicting programRoot: " + programRoot + " != " + currentRoot);
            }

            logger.trace("Test: {} == {}", initialTestClasses, classesUnderTestNames);

            if (!initialTestClasses.equals(classesUnderTestNames))
            {
                // Ok, we have an initial failure when comparing classes under test.  However, some test suites
                // will use different class names from the same deployment root, and we need to determine if
                // the actual roots have changed.  So, gather the roots and compare them.

                final Set<File> deploymentRoots = DeploymentRoots.getDeploymentRoots(classesUnderTestNames);

                if (!initialDeploymentRoots.equals(deploymentRoots))
                {
                    reportDeploymentMismatch(classesUnderTestNames, deploymentRoots);
                }
            }
        } else
        {
            // Warn if any of the CUTs are in packages that are not being woven during tests
            validateWeaving(classesUnderTestNames);

            // Install our initial values

            initialTestClass = getClass().getSimpleName();
            initialTestClasses = new HashSet<>(classesUnderTestNames); // Copy to avoid any future modifications
            initialDeploymentRoots = DeploymentRoots.getDeploymentRoots(initialTestClasses);

            logger.debug("CUT: {} = {}", initialTestClass, classesUnderTestNames);
            logger.debug("Roots: {}", initialDeploymentRoots);
        }

        // We didn't include the current class name in the test above (since it will always be different),
        // but we do need to include it in the final list of class roots to inspect.

        classesUnderTestNames.add(getClass().getName());  // Always ensure the jar with the test is included

        return classesUnderTestNames;
    }

    /**
     * Validate that all the given classes are in packages that will get woven during test execution.  It is a common
     * mistake when using package names other than {@code gov.takx...} to fail to include those package prefixes
     * in the build scripts test configuration (via {@code configureStandardPlugin}).
     *
     * @param classesUnderTestNames Class names to validate
     * @since 2.36
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void validateWeaving(Set<String> classesUnderTestNames)
    {
        final List<String> notWoven = classesUnderTestNames.stream()
                .filter(className -> !AopClassTransformer.isWovenDuringTests(className))
                .toList();

        if (!notWoven.isEmpty())
        {
            // Collect unique package names
            final Set<String> packageNames = notWoven.stream()
                    .map(className -> StringUtils.substringBeforeLast(className, "."))
                    .collect(Collectors.toSet());

            System.err.println("WARNING: Not all packages for classes under test have been configured for AOP handling." +
                    "\n   Unhandled packages: " + packageNames);

            System.err.println("""
                    This typically leads to failures, mostly NPEs, that are difficult to track down.
                    In order to properly handle these classes, you'll need to update your build script
                    to specify the necessary package prefixes in the call to 'configureStandardPlugin'.""");
        }
    }

    /**
     * Report a mismatch in the selected deployment roots between the current test and the test that was used to
     * initialize our state.
     *
     * @param classesUnderTestNames Names of classes under test for this test class
     * @param deploymentRoots       Deployment roots derived from classesUnderTestNames
     * @throws IllegalStateException to force test execution to fail
     * @since 2.36
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void reportDeploymentMismatch(Set<String> classesUnderTestNames, Set<File> deploymentRoots) throws IllegalStateException
    {
        final Set<String> commonCuts = intersect(initialTestClasses, classesUnderTestNames);
        final Set<String> lostCuts = subtract(initialTestClasses, commonCuts);
        final Set<String> addedCuts = subtract(classesUnderTestNames, commonCuts);

        final Set<File> commonRoots = intersect(initialDeploymentRoots, deploymentRoots);
        final Set<File> lostRoots = subtract(initialDeploymentRoots, commonRoots);
        final Set<File> addedRoots = subtract(deploymentRoots, commonRoots);

        final StringBuilder sb = new StringBuilder();

        sb.append("ERROR: Test [").append(getClass().getSimpleName())
                .append("] configured different classes under test than initialized by [")
                .append(initialTestClass).append(']');
        if (!lostCuts.isEmpty()) sb.append("\n   Classes lost: ").append(lostCuts);
        if (!addedCuts.isEmpty()) sb.append("\n   Classes added: ").append(addedCuts);

        final String msg = sb.toString();

        System.err.println("\n==============================================================================");
        System.err.println(msg);

        System.err.println("""
                These changes in classes under test lead to a difference in the 'deployment roots',
                which are the directories and jar files scanned during CDI container creation.  Changes are:""");
        if (!lostRoots.isEmpty()) System.err.println("   Roots lost: " + lostRoots);
        if (!addedRoots.isEmpty()) System.err.println("   Roots added: " + addedRoots);

        System.err.println("""
                The CDI container for test execution can only be initialized a single time.
                However, the test first instantiated by gradle is essentially random.
                For that reason, you must ensure that all test classes specify the same set of classes under test.""");
        System.err.println("==============================================================================");

        throw new IllegalStateException(msg);
    }

    /**
     * @return a new set, which is the intersection of the given sets
     * @since 2.36
     */
    private <T> Set<T> intersect(Set<T> a, Set<T> b)
    {
        final Set<T> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        return intersection;
    }

    /**
     * @return a new set, which is set 'b' subtracted from set 'a'
     * @since 2.36
     */
    private <T> Set<T> subtract(Set<T> a, Set<T> b)
    {
        final Set<T> difference = new HashSet<>(a);
        difference.removeAll(b);
        return difference;
    }

    /**
     * Get a list of representative class names so that the TAKX bootstrap can locate the file (jar or directory)
     * that represents the deployment root for each of the classes. This allows classes from that jar/directory to be
     * accessible from the CDI container. Any non-core modules with integration tests should override this method to
     * add a class from that module.
     *
     * @return List of classes to use to locate the deployment roots
     */
    protected List<String> getTestRootClassNames()
    {
        List<String> testRootClassNames = new ArrayList<>();
        testRootClassNames.add("gov.takx.test.AIntegrationTest");

        return testRootClassNames;
    }

    /**
     * @return A newly created plugin inspector with access to all plugin registries
     */
    protected IPluginInspector getPluginInspector()
    {
        return pluginInspectorManager.getPluginInspector();
    }

    /**
     * Obtain a contextual instance of the given bean type.
     *
     * @param type Type of bean to obtain
     * @return bean instance
     */
    public <T> T getBean(Class<T> type)
    {
        return container.select(type).get();
    }

    /**
     * Obtain a contextual instance of the given bean type.
     *
     * @param type       Type of bean to obtain
     * @param qualifiers the additional required qualifiers, if any
     * @return bean instance
     * @since 2.35
     */
    public <T> T getBean(Class<T> type, Annotation... qualifiers)
    {
        return container.select(type, qualifiers).get();
    }

    /**
     * Read a resource and return it as a String.
     *
     * @param resourcePath Path of resource to read
     * @return Resource content as a string
     * @see Resources#toString(java.net.URL, java.nio.charset.Charset)
     * @since 3.0
     */
    @Nonnull
    protected String getResourceAsString(@Nonnull String resourcePath) throws IOException
    {
        final URL resourceUrl = getClass().getResource(resourcePath);
        assertThat(resourceUrl).isNotNull(); // Addresses IJ warning AND fails faster, so we can diagnose failure easier
        return Resources.toString(resourceUrl, StandardCharsets.UTF_8);
    }

    /**
     * Initialize the logging configuration.  See if "log4j2.configurationFile" has been set as a system property.
     * If so, then we're done.
     * <p>
     * If not, then see if we can find a "log4j2.properties" or "log4j2.xml" resource on the class path.
     * If so, then we're done.
     * <p>
     * If not, then load in our fallback configuration as a resource (since it is bundled in the test jar).
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void initLogging()
    {
        if (loggerInitialized) return;

        loggerInitialized = true;

        // If the configuration was specified using a system property, then leave it as is
        if (System.getProperty(LOG4J_CONFIGURATION) != null) return;

        // See if a configuration exists on the classpath (which will get loaded by default).
        // If NOT, then load in our fallback configuration to just give something.

        final ClassLoader classLoader = getClass().getClassLoader();

        final URL xmlUrl = classLoader.getResource(LOG4J_XML);
        final URL propsUrl = classLoader.getResource(LOG4J_PROPERTIES);

        if (xmlUrl == null && propsUrl == null)
        {
            final URL fallback = getClass().getResource(FALLBACK_LOG4J_XML);

            if (fallback != null)
            {
                System.err.println("No LOG4J configuration found, installing a fallback from: " + fallback);

                try
                {
                    Configurator.reconfigure(fallback.toURI());

                    // Reinitialize our logger from the new configuration
                    logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
                } catch (URISyntaxException e)
                {
                    System.err.println("Failed to initialize LOG4J");
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
            } else
            {
                System.err.println(FALLBACK_LOG4J_XML + " not found; LOG4J not configured");
            }
        }
    }

    /**
     * Destroy the current project and reset test state by firing a {@link TestProjectDestroyed} event.
     */
    protected void destroyProject()
    {
        projectManager.closeProject(new NullProgressMonitor());
        container.event().select(TestProjectDestroyed.class).fire(new TestProjectDestroyed());
    }

    /**
     * Delete all map objects that currently exist.
     *
     * @since 3.0
     */
    protected void deleteAllMapObjects()
    {
        final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
        mapObjectManager.getMapObjectControllers().forEach(moc -> mapObjectManager.deleteMapObject(moc.getMapObject()));
    }

    /**
     * This extension is responsible for registering the AContainerTest instance that initialized the container
     * into the container, allowing PseudoBean targets to access things like {@link AContainerTest#destroyProject}.
     */
    @SuppressWarnings("CdiManagedBeanInconsistencyInspection")
    private record SelfRegisteringExtension(AContainerTest aContainerTest) implements Extension
    {

        @SuppressWarnings("AnonymousInnerClassMayBeStatic")
        private void registerContainer(@Observes AfterBeanDiscovery event)
        {
            final Type containerTestType = new TypeLiteral<AContainerTest>()
            {
            }.getType();

            event.addBean().addType(containerTestType).produceWith(i -> aContainerTest);
        }
    }

    /**
     * This extension ensures {@link gov.takx.api.annotation.PseudoBean} instances can be tracked
     * for updating within the {@link BlacksmithExtension}.
     */
    private static class BlacksmithManagedBeanExtension implements Extension
    {
        private void observe(@Observes PseudoBeanCreationEvent event)
        {
            // The environment singleton is only used by 'BlacksmithExtension'
            // so for now we can directly pass it along.
            BlacksmithExtension.interceptRuntimeManagedBean(event.bean());
        }
    }
}
