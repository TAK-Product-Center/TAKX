package gov.takx.module;

import gov.takx.api.annotation.InstanceNumber;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.PreconfiguredConnectionAdded;
import gov.takx.api.event.TestProjectDestroyed;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.notification.Notification.Type;
import gov.takx.api.platform.NameFixingCallable;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.DefinePreconfiguredConnection;
import gov.takx.api.plugin.annotations.ImplementedBy;
import gov.takx.api.plugin.app.IAppPluginDefinition;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.PreconfiguredConnection;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.registry.PluginInitializer;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.cdi.IndexUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The PluginModuleManager is responsible for creating the plugin module loader, locating the plugin jar files and
 * preparing them for use.  It also manages the VFS temp space for all mounted jar files.
 *
 * @see PluginDeployment
 * @see DeploymentContainerManager
 * @see DeploymentModuleBuilder
 * @since 1.36
 */
@ApplicationScoped
public class PluginModuleManager implements IPluginModuleManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PLUGINS_VFS_DIR = "/plugins"; // Where we mount our plugins in the VFS
    private static final String PLUGINS_DIR = "plugins"; // Real relative path to plugins directory

    private static final DotName IMPLEMENTED_BY_TYPE = DotName.createSimple(ImplementedBy.class.getName());
    private static final String IMPLEMENTED_BY_NAME = "@" + ImplementedBy.class.getSimpleName();

    private static final Predicate<PluginDeployment> DEPLOYABLE_PREDICATE = d -> d.getAttachment(DeploymentAttachments.DEPLOYABLE);

    private TempFileProvider tempFileProvider;

    private final IConnectionPluginRegistry connectionPluginRegistry;
    private final IndexUtils indexUtils;
    private final DeploymentContainerManager deploymentContainerManager;
    private final ICoreAppManager coreAppManager;
    private final INotificationService notificationService;
    private final PluginInitializer pluginInitializer;
    private final Event<PluginAdded> pluginAddedEvent;
    private final Event<PreconfiguredConnectionAdded> preconfiguredConnectionAddedEvent;

    private final boolean underTest;
    private final boolean useConcurrentDeployment;

    // Unfortunately, PML is not proxyable since the class it extends, ModuleLoader, has final methods in it.
    // So we can't just use normal injection, we have to manage it directly.
    private final PluginModuleLoader pluginModuleLoader = new PluginModuleLoader();

    private final Map<VirtualFile, PluginDeployment> pluginDeployments = new ConcurrentHashMap<>();
    private final Map<String, PluginDeployment> pluginDeploymentsByModuleId = new ConcurrentHashMap<>();

    private VirtualFile pluginsDirVFS;
    private Closeable pluginsMountHandle;

    @Inject
    PluginModuleManager(IProgramEnvironment programEnvironment, @InstanceNumber int instanceNumber, IndexUtils indexUtils,
                        IConnectionPluginRegistry connectionPluginRegistry,
                        DeploymentContainerManager deploymentContainerManager, ICoreAppManager coreAppManager,
                        INotificationService notificationService, PluginInitializer pluginInitializer,
                        Event<PluginAdded> pluginAddedEvent, Event<PreconfiguredConnectionAdded> preconfiguredConnectionAddedEvent)
    {
        this.indexUtils = indexUtils;
        this.connectionPluginRegistry = connectionPluginRegistry;

        underTest = programEnvironment.isPlatformUnderTest();
        useConcurrentDeployment = programEnvironment.isConcurrentDeployment();
        this.deploymentContainerManager = deploymentContainerManager;
        this.coreAppManager = coreAppManager;
        this.notificationService = notificationService;
        this.pluginInitializer = pluginInitializer;
        this.pluginAddedEvent = pluginAddedEvent;
        this.preconfiguredConnectionAddedEvent = preconfiguredConnectionAddedEvent;

        try
        {
            // Mount the real plugin's dir into our VFS
            pluginsDirVFS = VFS.getChild(PLUGINS_VFS_DIR); // Our "plugins" directory in VFS
            pluginsMountHandle = VFS.mountReal(new File(PLUGINS_DIR), pluginsDirVFS); // Mount the real "plugins" on it

            // Ensure that our temp area is specific to this application instance
            final String prefix = "pluginmgr-" + instanceNumber + "-";

            // Create our temp provider, and clean out any content from previous runs that may still be hanging around
            tempFileProvider = TempFileProvider.create(prefix, Executors.newSingleThreadScheduledExecutor(), true);
        } catch (IOException e)
        {
            logger.error("Failed to initialize plugin VFS", e);
        }
    }

    @Override
    public void loadAllPlugins()
    {
        if (logger.isDebugEnabled()) logger.debug("Load plugins, native platform: {}", Module.getPlatformIdentifier());

        try
        {
            // If we are under test, then use a special deployment finder since we don't want to load all plugins.
            // Under real use, we use a standard deployment finder that locates all .jar files.

            final VfsFileFinder finder = underTest
                    ? VfsFileFinder.TEST_PLUGIN_FINDER
                    : VfsFileFinder.JAR_FINDER;

            loadPlugins(finder.find(pluginsDirVFS));  // Scan for plugin deployments
        } catch (IOException e)
        {
            logger.error("Failed to access plugins directory", e);
        }
    }

    @Override
    public void loadPluginJars(List<File> jarFiles)
    {
        logger.debug("Load plugin jars: {}", jarFiles);

        try
        {
            final var jarFileNames = jarFiles.stream().map(File::getName).toList();
            final VirtualFileFilter filter = file -> jarFileNames.contains(file.getName());

            final var deployments = loadPlugins(new VfsFileFinder(filter).find(pluginsDirVFS));
            deploymentContainerManager.loadContainers(deployments);

            // Fire plugin added events for all plugins in all the deployments just created
            parallelStreamOfDeployableDeployments(deployments)
                    .map(deployment -> deployment.getAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS))
                    .flatMap(Collection::stream)
                    .forEach(definition -> pluginAddedEvent.fire(new PluginAdded(definition)));

            // And any new preconfigured connections
            streamOfDeployableDeployments(deployments)
                    .flatMap(deployment -> deployment.getAttachmentList(DeploymentAttachments.PRECONFIGURED_CONNECTIONS).stream())
                    .forEach(pc -> preconfiguredConnectionAddedEvent.fire(new PreconfiguredConnectionAdded(pc)));

            final int size = jarFiles.size();
            final String message = size + " plugin " + (size == 1 ? "jar" : "jars") + " installed";
            notificationService.show(Notification.builder(message, Type.Success).build());

            // Handle App plugins by asking the user if they want to install and run them.
            if (!underTest) processNewAppPlugins(deployments);
        } catch (IOException e)
        {
            logger.error("Failed to access plugins directory", e);
            notificationService.show(Notification.builder("Failed to install plugins", Type.Error).build());
        }
    }

    /**
     * Process any new App plugins discovered when hot loading plugin deployments.
     * Ask the user if they want to install and run them.
     */
    private void processNewAppPlugins(List<PluginDeployment> deployments)
    {
        // Collect all the App plugin definitions that don't have an auto-start mode configured
        final var appPlugins = deployments.stream()
                .map(d -> d.getAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS))
                .flatMap(Collection::stream)
                .filter(pd -> pd.getPluginCategory() == PluginCategory.AppPlugin)
                .map(IAppPluginDefinition.class::cast)
                .filter(pd -> pd.getAutoStartMode() == AutoStartMode.none)
                .toList();

        if (!appPlugins.isEmpty())
        {
            final String message = "New App Plugin(s) added - Do you want to start these plugin(s):\n\n"
                    + appPlugins.stream().map(IPluginDefinition::getName).collect(Collectors.joining("\n"));

            final int response = AlwaysOnTopJOptionPane.showConfirmDialog(null, message, null, JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION)
            {
                appPlugins.forEach(pd -> {
                    final var plugin = pd.getInstance();
                    if (plugin != null)
                    {
                        final String pluginName = plugin.getName();
                        logger.debug("Start App '{}'", pluginName);
                        try
                        {
                            final var appInstance = coreAppManager.getAppInstance(plugin);
                            if (appInstance != null) appInstance.start();
                        } catch (Exception e)
                        {
                            final String errMsg = "Failed to start App '" + pluginName + "'";
                            logger.warn(errMsg, e);
                            notificationService.show(Notification.builder(errMsg, Type.Error).build());
                        }
                    }
                });
            }
        }
    }

    @Override
    public void createTestDeployment(@Nonnull IPluginDefinition<?> pluginDefinition)
    {
        final String pluginFileName = pluginDefinition.getPluginClass().getName();
        final VirtualFile pluginFile = pluginsDirVFS.getChild(pluginFileName); // Find/create the file in VFS

        // Just create the deployment.  No need to stand up a container as these tests use the "current" container
        pluginDeployments.put(pluginFile, new PluginDeployment(pluginFile, pluginDefinition));

        // Since we're not creating a CDI container, we need to get the plugin initialized now
        pluginInitializer.initialize(pluginDefinition);
    }

    @Override
    public URL getPluginResource(@Nonnull String resourceName)
    {
        // Loop through all the deployments and try to locate the resource
        return streamOfAllDeployableDeployments()
                .map(d -> d.getAttachment(DeploymentAttachments.CLASS_LOADER).getResource(resourceName))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    @Override
    public File getPluginJarMountPoint(@Nonnull ClassLoader pluginClassLoader)
    {
        return streamOfDeployableDeployments(pluginDeployments.values(), d -> d.getAttachment(DeploymentAttachments.CLASS_LOADER).equals(pluginClassLoader))
                .map(PluginDeployment::getMountPoint)
                .findAny()
                .orElse(null);
    }

    @Override
    public File getPluginJarMountPoint()
    {
        return getPluginJarMountPoint(Thread.currentThread().getContextClassLoader());
    }

    @Nonnull
    @Override
    public Optional<PluginDeployment> findDeploymentForPlugin(@Nonnull Class<? extends IRaptorPlugin> pluginClass)
    {
        return streamOfAllDeployments(deployment -> deployment.getAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS).stream()
                .anyMatch(pd -> pd.getPluginClass().equals(pluginClass)))
                .findAny();
    }

    @Nonnull
    @Override
    public Optional<PluginDeployment> findDeploymentForClassLoader(@Nonnull ClassLoader classLoader)
    {
        return streamOfDeployableDeployments(pluginDeployments.values(), d -> d.getAttachment(DeploymentAttachments.CLASS_LOADER).equals(classLoader))
                .findAny();
    }

    @Nonnull
    @Override
    public List<PluginDeployment> getAllDeployableDeployments()
    {
        return streamOfAllDeployableDeployments().collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<PluginDeployment> getAllKnownDeployments()
    {
        return new ArrayList<>(pluginDeployments.values());
    }

    @Nonnull
    @Override
    public List<PluginDeployment> getDeploymentsForServiceScanning()
    {
        final Stream<PluginDeployment> deploymentStream = underTest
                ? getAllKnownDeployments().stream().limit(1)
                : getAllDeployableDeployments().stream();

        return deploymentStream.collect(Collectors.toList());
    }

    @Override
    public <T> void processDeploymentServices(@Nonnull Class<T> type, @Nonnull BiConsumer<PluginDeployment, T> handler)
    {
        getDeploymentsForServiceScanning().forEach(deployment -> deployment.getAttachment(DeploymentAttachments.CONTAINER)
                .select(type, Any.Literal.INSTANCE)
                .handlesStream()
                .map(Instance.Handle::get)
                .forEach(instance -> handler.accept(deployment, instance)));
    }

    /**
     * Process all the given plugin files in parallel and get them deployed.
     *
     * @param pluginFiles Files to process
     * @return Generated deployments
     */
    private List<PluginDeployment> loadPlugins(List<VirtualFile> pluginFiles)
    {
        logger.debug("Load plugins: {}", pluginFiles);

        long start = System.currentTimeMillis();

        // Perform a pipeline where each primary stage consists of a number of parallel operations.
        // The stages are:
        //
        // 1. Build deployments out of each jar and scan for plugins
        // 2. Construct the DeploymentModuleBuilder for each plugin
        // 3. Invoke the DeploymentModuleBuilder's to build the modules
        // 4. Find all "implementation containers" for deployments using @ImplementedBy
        // 5. Construct the reverse "dependents" attachment for each deployment
        // 6. Finalize and install any preconfigured connection definitions

        // 1. Find all the plugins defined in the jars and create plugin definitions for them
        final var deployments = parallelStreamOf(pluginFiles)
                .map(pluginFile -> new PluginDeploymentCreator(pluginFile).call())
                .filter(Objects::nonNull)
                .toList();

        if (logger.isDebugEnabled()) logger.debug("Plugin file scan took: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();

        // 2. Construct the deployment modules
        parallelStreamOfDeployableDeployments(deployments).forEach(deployment -> new ModuleBuilderCreator(deployment).run());

        if (logger.isDebugEnabled()) logger.debug("CTX creation took: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();

        // 3. Build the modules
        parallelStreamOfDeployableDeployments(deployments).forEach(deployment -> new ModuleBuilderRunner(deployment).run());

        // 4. Find all "implementation containers" for deployments using @ImplementedBy
        findImplementationContainers(deployments);

        // 5. Construct the reverse "dependents" attachment for each deployment
        findDependentContainers(deployments);

        // 6. Finalize and install any preconfigured connection definitions
        finalizePreconfiguredConnections(deployments);

        // I know, this is commented out code, but over the years, it is always difficult to remember where to call this
//        deploymentContainerManager.testLoad(); // Uncomment to perform a rapid CDI load test
//        System.exit(0);

        if (logger.isDebugEnabled()) logger.debug("CTX init took: {}", System.currentTimeMillis() - start);

        return deployments;
    }

    /**
     * Process all the preconfigured connections attached to each deployment.  We have to perform this as
     * a "post-process" step since these configurations refer to other plugins, and we needed to have them all
     * available to properly validate the preconfigured connection settings.
     *
     * @param deployments Deployments to process
     *
     * @since 2.20 (signature change in 5.7)
     */
    private void finalizePreconfiguredConnections(Collection<PluginDeployment> deployments)
    {
        // Process all the preconfigured connections defined within this deployment

        // Note: Must use connectionPluginRegistry since calling PluginRegistryManager.getConnectionPluginRegistry()
        // leads to waiting for all registries to load, and deadlocks us.
        streamOfDeployableDeployments(deployments)
                .flatMap(deployment -> deployment.getAttachmentList(DeploymentAttachments.PRECONFIGURED_CONNECTIONS).stream())
                .filter(this::validatePreconfiguredConnection)
                .forEach(connectionPluginRegistry::addPreconfiguredConnection);
    }

    /**
     * Scan each deployment and locate any {@link ImplementedBy} deployments it refers to.  Add those to the
     * {@link DeploymentAttachments#IMPLEMENTATION_DEPLOYMENTS} attachment and add
     * {@link DeploymentAttachments#IMPLEMENTATION_FOR} on the implementing deployment.
     * <p>
     * We have to perform this as a post-process step since these annotations refer to other plugins/deployments,
     * and we needed to have them all available to properly resolve the relations.
     *
     * @param deployments Deployments to process
     * @since 2.25 (signature change in 5.7)
     */
    private void findImplementationContainers(Collection<PluginDeployment> deployments)
    {
        // Scan each deployment and locate any "ImplementedBy" deployments (done in parallel)
        streamOfDeployableDeployments(deployments).forEach(deployment -> {
            final Index index = deployment.getAttachment(DeploymentAttachments.INDEX);

            indexUtils.getAnnotationInstances(index, IMPLEMENTED_BY_TYPE)
                    .map(ai -> resolveDeploymentByPartialNameMatch(ai.value().asString(), deployment, IMPLEMENTED_BY_NAME))
                    .filter(Objects::nonNull)
                    .forEach(impl -> {
                        logger.debug("Found implementation: {}", impl);
                        deployment.addToAttachmentSet(DeploymentAttachments.IMPLEMENTATION_DEPLOYMENTS, impl);
                        impl.addToAttachmentSet(DeploymentAttachments.IMPLEMENTATION_FOR, deployment);
                    });
        });
    }

    /**
     * Scan all the deployments and build the {@link DeploymentAttachments#PLUGIN_DEPENDENTS} attachment.
     * This is the reverse linkage of {@link DeploymentAttachments#PLUGIN_DEPENDENCIES}.  Also, create a
     * set of {@link PluginDeployment} for the direct dependencies.  This will be used later by the
     * {@link gov.takx.cdi.ServiceProviderExtension} when handling cross-container bean references.
     *
     * @param deployments Deployments to process
     * @since 2.35 (signature change in 5.7)
     */
    private void findDependentContainers(Collection<PluginDeployment> deployments)
    {
        // Scan each deployment to locate dependencies and build the reverse "dependent" associations
        streamOfDeployableDeployments(deployments).forEach(deployment -> {
            final Set<String> dependencies = deployment.getAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENCIES);

            dependencies.forEach(moduleId -> {
                final PluginDeployment dependency = pluginDeploymentsByModuleId.get(moduleId);

                if (dependency != null)
                {
                    deployment.addToAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENCY_DEPLOYMENTS, dependency);
                    dependency.addToAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENTS, deployment);
                } else
                {
                    logger.error("Failed to locate deployment [{}] for : {}", moduleId, deployment.getName());
                }
            });
        });

        if (logger.isDebugEnabled())
        {
            streamOfDeployableDeployments(deployments).forEach(deployment -> {
                final Set<PluginDeployment> dependents = deployment.getAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENTS);
                if (!dependents.isEmpty()) logger.debug("{} dependents: {}", deployment.getName(), dependents);
            });
        }
    }

    @Override
    public PluginDeployment resolveDeploymentByPartialNameMatch(@Nonnull String moduleId, @Nonnull PluginDeployment referringDeployment, @Nonnull String dependencyType)
    {
        logger.debug("Resolve deployment for: {}", moduleId);

        // Try to find the matching deployment
        final var deployments = streamOfAllDeployments(deployment -> deployment.getName().startsWith(moduleId)).toList();

        if (deployments.size() == 1)
        {
            // Use the deployment we matched
            final PluginDeployment matchedDeployment = deployments.get(0);
            final String matchedId = matchedDeployment.getAttachment(DeploymentAttachments.MODULE_ID);

            if (!moduleId.equals(matchedId))
            {
                logger.debug("replace {} with {}", moduleId, matchedId);
                return matchedDeployment;
            }
        } else
        {
            // Otherwise, report the failure

            final JarFile jarFile = referringDeployment.getAttachment(DeploymentAttachments.JAR_FILE);
            final String jarName = new File(jarFile.getName()).getName(); // Get last component of name

            if (deployments.isEmpty())
            {
                logger.error("""
                        Plugin jar [{}] contains an {} on [{}] that can not be resolved.
                           There are no matching plugin deployments that start with that name.
                        """,
                        jarName, dependencyType, moduleId);
            } else
            {
                logger.error("""
                        Plugin jar [{}] contains an ambiguous {} on [{}].
                           This matches more than one plugin deployment: {}
                        """,
                        jarName, dependencyType, moduleId,
                        deployments.stream().map(PluginDeployment::getName).collect(Collectors.toList()));
            }
        }

        return null;
    }

    /**
     * Validate that the connection plugin referenced in a {@code PreconfiguredConnection} is present in the plugin
     * registry and that the referenced plugin is a Connection plugin.
     *
     * @param preconfiguredConnection PreconfiguredConnection to validate
     * @return {@code true} if validation passes, {@code false} otherwise
     * @see PreconfiguredConnection
     * @see DefinePreconfiguredConnection
     * @since 2.20
     */
    private boolean validatePreconfiguredConnection(PreconfiguredConnection preconfiguredConnection)
    {
        final String family = preconfiguredConnection.getFamily();
        final String type = preconfiguredConnection.getType();

        // Note: need to use connectionPluginRegistry since calling PluginRegistryManager.getConnectionPluginRegistry()
        // leads to waiting for all registries to load, and deadlocks us.

        final Optional<IConnectionPluginDefinition> optionalDefinition = connectionPluginRegistry.getPluginDefinition(family, type);

        if (optionalDefinition.isEmpty())
        {
            final String jarName = getJarNameForClassLoader(preconfiguredConnection.getClassLoader());

            logger.error("""
                    Plugin jar [{}] contains an @DefinePreconfiguredConnection on connection [{}] that can not be resolved.
                       There are no connection plugins with that family and type.
                    """,
                    jarName, KeyUtils.getUniqueKey(family, type));
            return false;
        }

        return true;
    }

    /**
     * Get the name of the jar (plugin deployment) associated with the given class loader.
     *
     * @param classLoader Class loader to search for
     * @return The name of the jar for that deployment
     * @since 2.20
     */
    private String getJarNameForClassLoader(ClassLoader classLoader)
    {
        final PluginDeployment deployment = findDeploymentForClassLoader(classLoader).orElseThrow(
                () -> new DeploymentException("Deployment for class loader disappeared: " + classLoader));
        return new File(deployment.getAttachment(DeploymentAttachments.JAR_FILE).getName()).getName(); // Get last component of name
    }

    /**
     * Obtain a stream of all deployments filtered by the given predicate.
     *
     * @param predicate Predicate to use to filter the deployment stream
     * @return A stream of the plugin deployments that match the given predicate
     * @since 2.25 (renamed in 5.7)
     */
    private Stream<PluginDeployment> streamOfAllDeployments(Predicate<PluginDeployment> predicate)
    {
        return streamOfDeployments(pluginDeployments.values(), predicate);
    }

    /**
     * Obtain a stream of all deployments filtered by the given predicate.
     *
     * @param deployments Deployments to process
     * @param predicate Predicate to use to filter the deployment stream
     * @return A stream of the plugin deployments that match the given predicate
     * @since 2.25 (signature change in 5.7)
     */
    private Stream<PluginDeployment> streamOfDeployments(Collection<PluginDeployment> deployments, Predicate<PluginDeployment> predicate)
    {
        return deployments.stream().filter(predicate);
    }

    /**
     * @return A stream of only the DEPLOYABLE plugin deployments
     * @since 2.20 (renamed in 5.7)
     */
    private Stream<PluginDeployment> streamOfAllDeployableDeployments()
    {
        return streamOfDeployments(pluginDeployments.values(), DEPLOYABLE_PREDICATE);
    }

    /**
     * @param deployments Deployments to process
     * @return A stream of only the DEPLOYABLE plugin deployments
     * @since 2.20 (signature change in 5.7)
     */
    private Stream<PluginDeployment> streamOfDeployableDeployments(Collection<PluginDeployment> deployments)
    {
        return streamOfDeployments(deployments, DEPLOYABLE_PREDICATE);
    }

    /**
     * Obtain a stream of all DEPLOYABLE deployments further filtered by the given predicate.
     *
     * @param deployments Deployments to process
     * @param predicate Additional predicate to apply to stream elements
     * @return A stream of only the DEPLOYABLE plugin deployments
     * @since 2.25 (signature change in 5.7)
     */
    private Stream<PluginDeployment> streamOfDeployableDeployments(Collection<PluginDeployment> deployments, Predicate<PluginDeployment> predicate)
    {
        return streamOfDeployments(deployments, DEPLOYABLE_PREDICATE.and(predicate));
    }

    /**
     * Obtain a (potentially parallel) stream of only the DEPLOYABLE plugin deployments.
     * <p>
     * If concurrent deployment is enabled, then the returned stream will be a parallel stream, otherwise a regular,
     * sequential stream is returned.
     *
     * @param deployments Deployments to process
     * @return A stream of only the DEPLOYABLE plugin deployments
     * @since 2.25 (signature change in 5.7)
     */
    private Stream<PluginDeployment> parallelStreamOfDeployableDeployments(Collection<PluginDeployment> deployments)
    {
        return parallelStreamOf(deployments).filter(DEPLOYABLE_PREDICATE);
    }

    /**
     * Obtain a (potentially parallel) stream of all the elements of the given source collection.
     * <p>
     * If concurrent deployment is enabled, then the returned stream will be a parallel stream, otherwise a regular,
     * sequential stream is returned.
     * <p>
     * When debugging, it is easy for breakpoints to get missed when running in parallel.  This mechanism allows
     * you to force a single threaded execution by specifying {@code -DconcurrentDeployment=false}.
     *
     * @param source Source collection on which to create the stream
     * @return A stream over the given source
     * @since 2.25
     */
    private <T> Stream<T> parallelStreamOf(Collection<T> source)
    {
        return useConcurrentDeployment && source.size() > 10 // Don't go parallel if too few elements
                ? source.parallelStream()
                : source.stream();
    }

    /**
     * Reset to an initial, clean state.  For test purposes only.
     */
    private void reset(@SuppressWarnings("UnusedParameters") @Observes TestProjectDestroyed event)
    {
        pluginDeployments.values().forEach(PluginDeployment::cleanup);

        pluginModuleLoader.reset();
        pluginDeployments.clear();
    }

    /**
     * Do our best to clean up any files copied into our VFS temp area.
     * <p>
     * Tagged with {@code @PreDestroy} so that whenever the CDI container is destroying this bean (like when we
     * shut down), this method will get called. This used to be done via a shutdown hook in {@link ApplicationLauncher}, but
     * that had race conditions with the container shutdown.
     */
    @PreDestroy
    private void cleanup()
    {
        // NOTE: If you add logging statements here, and are wondering why they don't show up when TAKX shuts
        // down, it's because the loggers also have shutdown hooks, and since all the hooks are threads,
        // they can shut down in the middle of this method, causing the output to be lost.

        try
        {
            pluginDeployments.values().forEach(PluginDeployment::cleanup);
            tempFileProvider.close();
        } catch (IOException e)
        {
            logger.error("Failed to cleanup plugins", e);
        }

        VFSUtils.safeClose(pluginsMountHandle);
    }

    /**
     * Parallel plugin deployment creation.  The deployment is responsible for all VFS handling of the jar
     * including identifying sub-jars, native libs, etc.
     */
    private final class PluginDeploymentCreator extends NameFixingCallable<PluginDeployment>
    {
        private final VirtualFile pluginFile;

        PluginDeploymentCreator(VirtualFile pluginFile)
        {
            super("JARSCAN " + pluginFile.getName());
            this.pluginFile = pluginFile;
        }

        @Override
        protected PluginDeployment doCall()
        {
            logger.debug("Create deployment from: {}", pluginFile);

            // Construct the deployment
            try
            {
                final var deployment = new PluginDeployment(pluginFile, tempFileProvider);

                deployment.putAttachment(DeploymentAttachments.PLUGIN_MODULE_LOADER, pluginModuleLoader);
                deployment.prepareDeployment();

                pluginDeployments.put(pluginFile, deployment);
                pluginDeploymentsByModuleId.put(deployment.getModuleIdentifier(), deployment);

                return deployment;
            } catch (Exception e)
            {
                logger.error("Failed to process plugin jar: {}", pluginFile.getName(), e);
            }
            return null;
        }
    }

    /**
     * Parallel plugin module builder creation.
     */
    private final class ModuleBuilderCreator extends NameFixingRunnable
    {
        private final PluginDeployment deployment;

        ModuleBuilderCreator(PluginDeployment deployment)
        {
            super("CTXBUILD " + deployment.getName());
            this.deployment = deployment;
        }

        @Override
        protected void doRun()
        {
            if (logger.isDebugEnabled()) logger.debug("Process plugin file: {}", deployment.getName());

            try
            {
                // Construct the module builder for this plugin deployment
                // Instantiating the builder makes the module Id available for other module's dependencies
                //
                // Construct the builder according to our "under test" setting

                final IDeploymentModuleBuilder builder = underTest
                        ? new TestDeploymentModuleBuilder(deployment)
                        : new DeploymentModuleBuilder(deployment);

                deployment.putAttachment(DeploymentAttachments.MODULE_BUILDER, builder);

                builder.addModuleSpecToLoader();
            } catch (Exception e)
            {
                logger.error("Failed to process plugin jar: {}", deployment.getName(), e);
            }
        }
    }

    /**
     * Parallel plugin builder execution.  This is the final step and fully realizes the plugin module.
     */
    private final class ModuleBuilderRunner extends NameFixingRunnable
    {
        private final PluginDeployment deployment;

        ModuleBuilderRunner(PluginDeployment deployment)
        {
            super("CTXINIT " + deployment.getName());
            this.deployment = deployment;
        }

        @Override
        protected void doRun()
        {
            try
            {
                deployment.getAttachment(DeploymentAttachments.MODULE_BUILDER).build(); // Build it!
                return;
            } catch (ModuleNotFoundException e)
            {
                logger.error("Failed to load plugins from jar {}\n   due to missing module {}",
                        deployment.getName(), e.getMessage());
            } catch (IOException | ModuleLoadException e)
            {
                logger.error("Failed to construct Module for plugin jar: {}", deployment.getName(), e);
            } catch (ClassNotFoundException e)
            {
                if (!underTest)
                {
                    logger.error("Exception loading plugin from {}", deployment.getName(), e);
                }
            }

            // In all error cases, mark the deployment as NOT deployable
            deployment.putAttachment(DeploymentAttachments.DEPLOYABLE, false);
        }
    }
}
