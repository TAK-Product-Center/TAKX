package gov.takx.module;

import com.google.common.collect.Lists;
import gov.takx.api.aspects.PostInitInjector;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.annotations.ImplementedBy;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.cdi.ServiceProviderExtension;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.registry.PluginInitializer;
import gov.takx.core.uibridge.SwingUIResolver;
import gov.takx.platform.cdi.ContainerBuilder;
import gov.takx.platform.cdi.CrossContainerEventDistributor;
import gov.takx.platform.logging.NullProgressMonitor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Inject;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Application service that is responsible for creating and destroying plugin deployment CDI containers
 * as part of the project session services.
 * <p>
 * The creation and destruction of containers is performed in parallel.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class DeploymentContainerManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DotName EXTENSION_TYPE = DotName.createSimple(Extension.class.getName());

    private final IProgramEnvironment programEnvironment;
    private final IPluginModuleManager pluginModuleManager;
    private final IMapObjectManager mapObjectManager;
    private final PluginInitializer pluginInitializer;
    private final IConnectionManager connectionManager;

    @Inject
    DeploymentContainerManager(IProgramEnvironment programEnvironment, IPluginModuleManager pluginModuleManager,
                               IMapObjectManager mapObjectManager, PluginInitializer pluginInitializer,
                               IConnectionManager connectionManager)
    {
        this.programEnvironment = programEnvironment;
        this.pluginModuleManager = pluginModuleManager;
        this.mapObjectManager = mapObjectManager;
        this.pluginInitializer = pluginInitializer;
        this.connectionManager = connectionManager;
    }

    /**
     * For testing, force the creation of the CDI containers prior to opening a project.
     */
    @SuppressWarnings("unused")
    public void testLoad()
    {
        makeContainers(pluginModuleManager.getAllDeployableDeployments(), new NullProgressMonitor());
    }

    /**
     * Construct the CDI container for a given deployments.  This is used when plugin jars are hot deployed at runtime.
     *
     * @param deployments The deployments to process
     * @since 5.7
     */
    public void loadContainers(List<PluginDeployment> deployments)
    {
        logger.debug("Create plugin containers for: {}", deployments);

        makeContainers(deployments, new NullProgressMonitor());
    }

    /**
     * Construct CDI containers for each plugin deployment.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_DeploymentContainerManager) StartProjectServices event)
    {
        final long start = System.currentTimeMillis();
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        progressMonitor.setText("Creating plugin containers");

        makeContainers(pluginModuleManager.getAllDeployableDeployments(), progressMonitor); // Bring up a CDI container for every applicable deployment

        if (logger.isDebugEnabled()) logger.debug("CDI init took: {}", System.currentTimeMillis() - start);
    }

    /**
     * shutdown plugins and destroy all CDI containers.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_DeploymentContainerManager) StopProjectServices event)
    {
        event.getProgressMonitor().setText("Destroying plugin containers");

        // Now we can iterate through the deployments and destroy the container for each
        final long start = System.currentTimeMillis();

        destroyContainers(); // Destroy the CDI container for every applicable deployment

        // Now that we've destroyed all the containers, we need to flush the cache of InstanceManagerImpl's
        // in the post-init injector.  This is not ideal (having this code know to do this), but I can't think
        // of a better way to get an Aspect to play in our event lifecycle (it can't use CDI events since it is
        // not a container managed bean, and it can't be "@PseudoBean" since it is the code that handles PseudoBean
        // classes).

        PostInitInjector.clear();

        if (logger.isDebugEnabled()) logger.debug("CDI destroy took: {}", System.currentTimeMillis() - start);
    }

    /**
     * Construct, start, and wait for futures to build CDI containers for all deployments in the proper sequence
     * to ensure deployment dependencies are respected.
     *
     * @param initialDeployments Deployments to process
     */
    private void makeContainers(List<PluginDeployment> initialDeployments, IProgressMonitor progressMonitor)
    {
        final var startupOrdering = makeStartupOrdering(initialDeployments);

        final int count = (int) startupOrdering.stream().mapToLong(List::size).sum();
        progressMonitor.setSubstatusMaximum(count);

        int level = 0;
        for (List<PluginDeployment> deployments : startupOrdering)
        {
            if (logger.isDebugEnabled()) logger.debug("Start level {} containers", level++);

            deploymentStream(deployments).forEach(d -> new PluginContainerBuilder(d, progressMonitor).run());
        }

        // Now that the containers are created, configure cross-container event delivery for all the deployments
        startupOrdering.stream()
                .flatMap(this::deploymentStream)
                .forEach(this::configureCrossContainerEventDelivery);
    }

    /**
     * Determine and install the set of dependent deployments for cross-container event distribution.
     *
     * @param deployment Deployment to process
     * @since 2.35
     */
    private void configureCrossContainerEventDelivery(PluginDeployment deployment)
    {
        if (Boolean.TRUE.equals(deployment.getAttachment(DeploymentAttachments.CROSS_CONTAINER_EVENTS_CONFIGURED)))
        {
            logger.debug("Deployment already configured; skipping {}", deployment);
            return;
        }

        final var deploymentContainer = deployment.getAttachment(DeploymentAttachments.CONTAINER);

        if (deploymentContainer == null) return; // If the deployment failed, don't try to configure it

        // Determine the dependents of this deployment:
        //
        // 1. All direct dependents (specified by @PluginDependency)
        // 2. All deployments for which this deployment is the implementation (specified by @ImplementedBy)

        final var directDependents = deployment.getAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENTS);
        final var implementationFor = deployment.getAttachmentSet(DeploymentAttachments.IMPLEMENTATION_FOR);

        if (logger.isDebugEnabled())
        {
            logger.debug("{}:\n   direct:{}\n   implFor: {}", deployment.getName(), directDependents, implementationFor);
        }

        // Combine those sets and map those to the containers for each dependent deployment

        final Set<WeldContainer> dependentContainers = Stream
                .concat(directDependents.stream(), implementationFor.stream())
                .map(dep -> {
                    final WeldContainer container = dep.getAttachment(DeploymentAttachments.CONTAINER);
                    if (container == null)
                    {
                        logger.error("No container found for [{}] which is a dependent of {}", dep.getName(), deployment.getName());
                    }
                    return container;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Install the set of dependent containers into the event distributor for this deployment
        deploymentContainer
                .select(CrossContainerEventDistributor.class).get()
                .setDependentContainers(dependentContainers);

        deployment.putAttachment(DeploymentAttachments.CROSS_CONTAINER_EVENTS_CONFIGURED, true);
    }

    /**
     * Shutdown and wait for futures to destroy CDI containers for all deployments in the proper sequence
     * to ensure deployment dependencies are respected.
     *
     * @since 2.25
     */
    private void destroyContainers()
    {
        // Shutdown containers in the reverse order we started them
        final var shutdownOrdering = Lists.reverse(makeStartupOrdering(pluginModuleManager.getAllDeployableDeployments()));
        int level = shutdownOrdering.size() - 1;

        for (List<PluginDeployment> deployments : shutdownOrdering)
        {
            if (logger.isDebugEnabled()) logger.debug("Stop level {} containers", level--);

            deploymentStream(deployments).forEach(d -> new PluginContainerDestructor(d).run());
        }
    }

    /**
     * Get the appropriate (parallel or sequential) stream for the given deployments based on our configuration.
     *
     * @param deployments Deployments from which to generate the stream
     * @return parallel or sequential stream of deployments
     * @see IProgramEnvironment#isConcurrentDeployment()
     * @since 2.25
     */
    private Stream<PluginDeployment> deploymentStream(List<PluginDeployment> deployments)
    {
        return programEnvironment.isConcurrentDeployment() ? deployments.parallelStream() : deployments.stream();
    }

    /**
     * Scan the deployments and create the ordered lists for a properly sequenced startup.  Each "level" contains
     * the list of deployments to initialize, and that can be done in parallel.  Deployments in level N+1 have
     * dependencies on level N, and so, must wait until level N has been fully initialized before they can be started.
     * This leveling ensures that all plugins that have dependencies on other plugins are started after their
     * dependents.
     * <p>
     * This algorithm, although home-grown, produces results similar to the Coffman-Graham algorithm for leveled
     * grouping of a topological sort with no boundary constraint on the width of a level.
     * <p>
     * See <a href="https://en.wikipedia.org/wiki/Coffman%E2%80%93Graham_algorithm">Coffman-Graham Algorithm</a>
     *
     * @return ordered list of deployments to start, each sub-list can be started in parallel
     * @since 2.11
     */
    private List<List<PluginDeployment>> makeStartupOrdering(List<PluginDeployment> deployments)
    {
        final var nodeMap = makeDependencyNodeMap(deployments);

        // Iteratively scan the deployment set, adding deployments with satisfied dependencies to the current level.
        // Instead of actually removing nodes from the working set, we just ignore them if they are already placed.
        // The overhead of the set manipulations would be higher than the time to test and skip a node.

        final List<List<PluginDeployment>> orderedDeployments = new ArrayList<>();
        int unplacedCount = nodeMap.size();

        while (unplacedCount > 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Fill level {}, unplaced count = {}", orderedDeployments.size(), unplacedCount);
            }

            // Find all the deployments that can be placed at this level
            final List<PluginDeployment> satisfiedDeployments = getAndMarkSatisfiedDeployments(nodeMap);

            orderedDeployments.add(satisfiedDeployments); // Add this new level to our ordered collection

            unplacedCount -= satisfiedDeployments.size();
        }

        if (logger.isDebugEnabled())
        {
            int i = 0;
            for (List<PluginDeployment> list : orderedDeployments)
            {
                logger.debug("Level {} has {} deployments:\n   {}", i++, list.size(), StringUtils.join(list, "\n   ", null));
            }
        }
        return orderedDeployments;
    }

    /**
     * At each level we can place all nodes where all the dependencies for that node have already been placed
     * (or that have no dependencies, which, of course, only happens on the very first pass).
     *
     * @param nodeMap Dependency node map
     * @return List of deployments that have all dependencies satisfied and can be placed at the current level
     * @since 2.25
     */
    private List<PluginDeployment> getAndMarkSatisfiedDeployments(Map<String, DependencyNode> nodeMap)
    {
        // Note that if our node map doesn't include a given id, then it is a reference to an "internal" deployment,
        // (currently just DB plugins) and we can ignore it for our purposes (since it doesn't have a CDI container).

        final List<DependencyNode> satisfiedNodes = nodeMap.values().stream()
                .filter(node -> !node.placed)
                .peek(node -> dumpNode(node, nodeMap))
                .filter(node -> node.dependencies.stream().allMatch(id -> !nodeMap.containsKey(id) || nodeMap.get(id).placed))
                .toList();

        // If we found no nodes at this level, then our algorithm has failed us somehow
        if (satisfiedNodes.isEmpty())
        {
            logger.error("Failed to determine ordered levels for container startup, not converging!");
            throw new RuntimeException("Plugin Deployment startup sequencing failed!");
        }

        // Don't be tempted to merge these two stream operations.  We have to wait until the initial pass
        // is done *before* we can mark any of the nodes at this level as "placed."

        // Mark the satisfied nodes as placed

        return satisfiedNodes.stream()
                .peek(node -> node.placed = true)
                .map(node -> node.deployment)
                .collect(Collectors.toList());
    }

    /**
     * Build the node map (from node name to {@code DependencyNode}) for all the deployments.
     * Each {@code DependencyNode} holds the collection of declared dependencies and implementations for a deployment.
     *
     * @param deployments Deployments to process
     * @return Node map
     * @since 2.25 (signature change in 5.7)
     */
    private Map<String, DependencyNode> makeDependencyNodeMap(List<PluginDeployment> deployments)
    {
        final int deploymentCount = deployments.size();

        final Map<String, DependencyNode> nodeMap = new HashMap<>(deploymentCount);

        // Construct the list of deployments to process and the map from deployment id to dependency node
        for (var deployment : deployments)
        {
            final var node = new DependencyNode(deployment);
            nodeMap.put(node.getName(), node);
        }

        // In order to prevent dependency cycles between api/implementation jars (@ImplementedBy), we need to
        // make another pass through the dependency nodes and remove the dependency from the implementing jar
        // to the jar for which it is the implementation.
        //
        // We need the dependency from the API to the IMPL so that all other jars that depend on the API have a
        // transitive dependency on the IMPL.

        for (DependencyNode node : nodeMap.values())
        {
            for (String implementationName : node.implementations)
            {
                if (logger.isDebugEnabled()) logger.debug("Remove {} from {}", node.name, implementationName);

                final DependencyNode implementationNode = nodeMap.get(implementationName);

                if (implementationNode != null)
                {
                    implementationNode.dependencies.remove(node.name);
                } else
                {
                    logger.warn("Implementation node missing: {}", implementationName);
                }
            }
        }
        return nodeMap;
    }

    /**
     * Dump a node's vital data if tracing.
     */
    private void dumpNode(DependencyNode node, Map<String, DependencyNode> nodeMap)
    {
        if (!logger.isTraceEnabled()) return;

        logger.trace("Node {}", node);

        node.dependencies.forEach(id -> logger.trace("   Dep: {}{}", id,
                nodeMap.containsKey(id) ? (" is placed " + nodeMap.get(id).placed) : " is internal/ignored"));
    }

    /**
     * Simple node to organize containers based on their dependencies.  The node tracks a given deployment
     * and whether it has been placed into a startup level yet.
     * <p>
     * There's a twist here in order to support the use of {@link ImplementedBy}.
     * We need to consider any deployment that is specified in an {@code ImplementedBy} to be the equivalent of
     * a dependency, since the implementation deployment's container must be started before any deployment that
     * declares a dependency on the deployment that declares the {@code ImplementedBy} relation.
     *
     * @since 2.11
     */
    private static final class DependencyNode
    {
        final PluginDeployment deployment;
        final String name;
        final Set<String> dependencies;
        final Set<String> implementations;

        boolean placed; // Has this deployment been placed in a level yet?

        public DependencyNode(PluginDeployment deployment)
        {
            this.deployment = deployment;
            name = deployment.getAttachment(DeploymentAttachments.MODULE_ID);

            final Set<String> pluginDependencies = deployment.getAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENCIES);
            final Set<PluginDeployment> implementationDeployments = deployment.getAttachmentSet(DeploymentAttachments.IMPLEMENTATION_DEPLOYMENTS);

            implementations = implementationDeployments.stream()
                    .map(d -> d.getAttachment(DeploymentAttachments.MODULE_ID))
                    .collect(Collectors.toSet());

            dependencies = new HashSet<>(pluginDependencies.size() + implementationDeployments.size());

            dependencies.addAll(pluginDependencies); // Add direct/declared dependencies
            dependencies.addAll(implementations); // Add all the implementation deployments as additional dependencies

            logger.debug("Create: {}", this);
        }

        public String getName()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return "DependencyNode{" +
                    "name=" + name +
                    ", placed=" + placed +
                    ", dependencies=" + dependencies +
                    ", implementations=" + implementations +
                    '}';
        }
    }

    /**
     * Parallel CDI container build and plugin UI resolution.
     */
    private final class PluginContainerBuilder extends NameFixingRunnable
    {
        private final PluginDeployment deployment;
        private final IProgressMonitor progressMonitor;

        PluginContainerBuilder(PluginDeployment deployment, IProgressMonitor progressMonitor)
        {
            super("CDIBUILD " + deployment.getName());
            this.deployment = deployment;
            this.progressMonitor = progressMonitor;
        }

        @Override
        protected void doRun()
        {
            // If our container has already been built (which can happen when a "hot deployed" plugin deployment
            // has dependencies on deployments/containers that were previously created), then we're done.
            if (deployment.getAttachment(DeploymentAttachments.CONTAINER) != null) return;

            final String pluginContainerId = deployment.getName();

            logger.debug("Build CDI container for: {}", pluginContainerId);

            try
            {
                final ClassLoader classLoader = deployment.getAttachment(DeploymentAttachments.CLASS_LOADER);
                final Index index = deployment.getAttachment(DeploymentAttachments.INDEX);

                final WeldContainer container = ContainerBuilder
                        .forPlugin(pluginContainerId)
                        .useIndex(index)
                        .setClassLoader(classLoader)
                        .addExtension(new ServiceProviderExtension(ContainerBuilder.getCoreContainer(), deployment))
                        .addExtensions(getExtensionsInDeployment(index, classLoader))
                        .initialize();

                deployment.putAttachment(DeploymentAttachments.CONTAINER, container); // Attach the container to the deployment

                new SwingUIResolver(deployment).resolveUIs(); // And add all the UI bindings

                // Go through and run the initializer for every plugin
                deployment.getAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS).forEach(pluginInitializer::initialize);
            } catch (Throwable e)
            {
                logger.error("Failed to build CDI container for: {}", pluginContainerId, e);
                deployment.putAttachment(DeploymentAttachments.DEPLOYABLE, false);
            }

            progressMonitor.incrementSubstatus();
        }

        /**
         * Find any CDI extensions defined in the plugin deployment.
         *
         * @param index       Annotation index to use
         * @param classLoader Class loader to use to load extension classes
         * @return Array of extensions, may be empty
         * @since 2.35
         */
        @SuppressWarnings("unchecked")
        private Class<Extension>[] getExtensionsInDeployment(Index index, ClassLoader classLoader)
        {
            return index.getAllKnownImplementors(EXTENSION_TYPE)
                    .stream()
                    .map(classInfo -> getClass(classInfo, classLoader))
                    .filter(Objects::nonNull)
                    .toArray(Class[]::new);
        }

        /**
         * Attempts to load a {@code Class} from the simple class name provided in {@code classInfo}.
         *
         * @param classInfo Metadata about a class and the annotations contained within it.
         * @return The class whose metadata is captured within {@code classInfo} if successful; otherwise {@code null}.
         * @since 2.34.1
         */
        private Class<?> getClass(ClassInfo classInfo, ClassLoader classLoader)
        {
            try
            {
                return Class.forName(classInfo.name().toString(), false, classLoader);
            } catch (Exception exception)
            {
                logger.warn("Failed to retrieve class object from: ", exception);
            }

            return null;
        }
    }

    /**
     * Parallel CDI container destructor.
     */
    private final class PluginContainerDestructor extends NameFixingRunnable
    {
        private final PluginDeployment deployment;

        PluginContainerDestructor(PluginDeployment deployment)
        {
            super("CDIDESTROY " + deployment.getName());
            this.deployment = deployment;
        }

        @SuppressWarnings("resource")
        @Override
        protected void doRun()
        {
            final String deploymentName = deployment.getName();

            try
            {
                logger.debug("Destroy Deployment: {}", deploymentName);

                final WeldContainer container = deployment.getAttachment(DeploymentAttachments.CONTAINER);

                // If we don't have a container, then our startup somehow failed,
                // and there won't be any plugin instances to shut down, so don't try.

                if (container != null)
                {
                    // TODO: Need to shutdown all plugins cleanly here (except apps, see CoreAppManager)

                    // Shutdown all the device plugins in the deployment
                    deployment.getAttachmentList(DeploymentAttachments.PLUGIN_DEFINITIONS).stream()
                            .filter(pd -> pd.getPluginCategory() == PluginCategory.DevicePlugin)
                            .forEach(this::shutdownDevicePlugin);

                    final ClassLoader deploymentClassLoader = deployment.getAttachment(DeploymentAttachments.CLASS_LOADER);

                    // Shutdown all open connections in this container
                    connectionManager.getConnectionControllers().stream()
                            .map(IComposedConnectionController::getPluginConnectionController)
                            .filter(Objects::nonNull)
                            .filter(pcc -> pcc.getClass().getClassLoader() == deploymentClassLoader)
                            .forEach(pcc -> connectionManager.removeConnection(pcc.getUniqueKey(), false));

                    // Now shutdown the container itself
                    logger.debug("Shutdown CDI container: {}", deploymentName);
                    container.shutdown();
                    deployment.removeAttachment(DeploymentAttachments.CONTAINER); // No more container
                }
            } catch (Exception e)
            {
                logger.error("Failed to destroy container for{}", deploymentName, e);
                deployment.putAttachment(DeploymentAttachments.DEPLOYABLE, false);
            }
        }

        /**
         * Shutdown a single device plugin given its plugin definition.
         *
         * @param pluginDefinition Plugin definition to process
         */
        private void shutdownDevicePlugin(IPluginDefinition<?> pluginDefinition)
        {
            try
            {
                if (logger.isDebugEnabled()) logger.debug("Shutdown plugin: {}", pluginDefinition.getUniqueKey());

                mapObjectManager.shutdownPluginControllers(pluginDefinition); // Shutdown all MOC instances

                final IDevicePlugin devicePlugin = (IDevicePlugin) pluginDefinition.getInstance();

                // We can end up with a null instance in tests
                if (devicePlugin != null)
                {
                    devicePlugin.shutdownPlugin(); // And then the plugin itself
                } else
                {
                    // Ok, null instance. If this is during testing, then that is normal for a container
                    // test that never stood up the plugins.  Under normal operation, this is an error.
                    if (!programEnvironment.isPlatformUnderTest())
                    {
                        logger.error("Failed to obtain device plugin instance: {} - can't shut it down", pluginDefinition.getPluginClass());
                    }
                }
            } catch (Exception e)
            {
                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception shutting down plugin {}", pluginDefinition.getUniqueKey(), e);
            }
        }
    }
}
