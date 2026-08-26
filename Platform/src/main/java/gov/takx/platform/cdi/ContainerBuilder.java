package gov.takx.platform.cdi;

import gov.takx.api.plugin.annotations.AppPlugin;
import gov.takx.api.plugin.annotations.ConnectionPlugin;
import gov.takx.api.plugin.annotations.DevicePlugin;
import gov.takx.api.plugin.annotations.ImportExportPlugin;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.api.CDI11Bootstrap;
import org.jboss.weld.bootstrap.api.Service;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.TypeDiscoveryConfiguration;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.Deployment;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.environment.deployment.WeldBeanDeploymentArchive;
import org.jboss.weld.environment.deployment.WeldDeployment;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.module.ObserverNotifierFactory;
import org.jboss.weld.resources.spi.ClassFileServices;
import org.jboss.weld.resources.spi.ResourceLoader;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builder class to produce Weld CDI containers.  Each container is initialized from a specific module, which might
 * be the TAKX core or a plugin deployment.
 *
 * @since 2.0.0
 */
public class ContainerBuilder extends Weld
{
    /**
     * The name (id) of the core container when created during testing.
     *
     * @since 2.36
     */
    public static final String TEST_CONTAINER_NAME = "TEST";

    private ClassLoader classLoader;
    private Index index;
    private boolean coreBuilder = false;
    private static final AtomicReference<WeldContainer> coreContainer = new AtomicReference<>();
    private static final AtomicReference<Index> coreContainerIndex = new AtomicReference<>();

    private ContainerBuilder()
    {
    }

    /**
     * Construct a container builder for a given plugin deployment name.
     *
     * @param pluginName Name of plugin deployment
     * @return new builder
     */
    public static ContainerBuilder forPlugin(String pluginName)
    {
        return new ContainerBuilder()
                .initForName(pluginName)
                .setCoreBuilder(false)
                .withPluginBeanDefiningAnnotations();
    }

    /**
     * Construct the core container builder. Set the module to use for resource loading.
     *
     * @param module Module to use, {@code null} if not using modules
     * @return new builder
     */
    public static ContainerBuilder forCore(Module module)
    {
        return new ContainerBuilder()
                .initForName(module != null ? module.getName() : TEST_CONTAINER_NAME)
                .setCoreBuilder(true);
    }

    /**
     * Configure the container with our additional plugin defining annotations.
     * @since 3.4
     */
    @SuppressWarnings("unchecked")
    private ContainerBuilder withPluginBeanDefiningAnnotations()
    {
        addBeanDefiningAnnotations(AppPlugin.class, DevicePlugin.class, ConnectionPlugin.class, ImportExportPlugin.class);
        return this;
    }

    /**
     * Set whether this builder is for the core or not.
     *
     * @param coreBuilder Pass {@code true} if this is the core builder
     * @return this
     */
    private ContainerBuilder setCoreBuilder(boolean coreBuilder)
    {
        this.coreBuilder = coreBuilder;
        return this;
    }

    /**
     * Initialize this builder using the given name as the container ID.
     *
     * @param name Container Id/name
     * @return this
     */
    @SuppressWarnings("unchecked")
    private ContainerBuilder initForName(String name)
    {
        // Initial configuration for our container:
        //  - We don't need a beans.xml file
        //  - We support singletons, to avoid the use of proxies, specialization, and alternatives (for testing)

        resetAll().containerId(name)
                .property(JAVAX_ENTERPRISE_INJECT_SCAN_IMPLICIT, true) // So we don't have to have a beans.xml file
                .addBeanDefiningAnnotations(Singleton.class, Specializes.class, Alternative.class);

        return this;
    }

    /**
     * Specify the annotation index to use for discovery.
     *
     * @param index Index to use
     * @return this
     */
    public ContainerBuilder useIndex(Index index)
    {
        this.index = index;
        return this;
    }

    /**
     * Overridden so we can return our derived instance.
     */
    @Override
    public ContainerBuilder setClassLoader(ClassLoader classLoader)
    {
        super.setClassLoader(classLoader);
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public Weld resetAll()
    {
        index = null;
        return super.resetAll();
    }

    /**
     * {@inheritDoc}
     * <p>
     * If this is the core container builder, then track the container as the "core" container.
     *
     * @return initialized container
     * @see #getCoreContainer()
     * @see StartupHandler
     */
    @Override
    public WeldContainer initialize()
    {
        StartupHandler.setClassLoader(classLoader); // Associate the classloader with this thread

        final WeldContainer container = super.initialize();

        if (coreBuilder)
        {
            coreContainer.set(container); // Record the core container instance
            coreContainerIndex.set(index);
        }

        return container;
    }

    /**
     * @return The "core" container's index
     * @since 3.4
     */
    public static Index getCoreContainerIndex()
    {
        return coreContainerIndex.get();
    }

    /**
     * @return The "core" container
     */
    public static WeldContainer getCoreContainer()
    {
        return coreContainer.get();
    }

    @Override
    protected Deployment createDeployment(ResourceLoader resourceLoader, CDI11Bootstrap bootstrap)
    {
        final Iterable<Metadata<Extension>> extensions = getExtensions();
        final TypeDiscoveryConfiguration typeDiscoveryConfiguration = bootstrap.startExtensions(extensions);
        final Set<Class<? extends Annotation>> beanDefiningAnnotations = new HashSet<>(typeDiscoveryConfiguration.getKnownBeanDefiningAnnotations());

        // Add all custom bean defining annotations registered via Weld.addBeanDefiningAnnotations()
        beanDefiningAnnotations.addAll(extendedBeanDefiningAnnotations);

        // Don't enable @ThreadScoped for now, since we really don't use it and it is adding performance overhead
//        beanDefiningAnnotations.add(ThreadScoped.class);

        final IndexedDiscoveryStrategy strategy = new IndexedDiscoveryStrategy(resourceLoader, bootstrap, beanDefiningAnnotations,
                getContainerId(), buildSyntheticBeansXml(), index);

        final Set<WeldBeanDeploymentArchive> beanDeploymentArchives = new HashSet<>(strategy.performDiscovery());
        final Deployment deployment = new TakxDeployment(resourceLoader, bootstrap, beanDeploymentArchives, getExtensions());

        // Add in our services
        final ServiceRegistry services = deployment.getServices();
        services.add(ClassFileServices.class, strategy.getClassFileServices()); // For Jandex use

        // We want to modify the delivery semantics of events just a little.  We want to ensure that an event
        // gets delivered to all observers even when one of the observer invocations throws an exception.
        // So, install our own custom observer notifier.

        services.add(ObserverNotifierFactory.class, GuaranteedObserverNotifier.FACTORY);

        return deployment;
    }

    /**
     * Custom deployment class so we can install our own service registry.  The default Weld startup installs a
     * ObserverNotifier service.  Without our own custom service registry, this would overwrite any service definition
     * we pre-install in the deployment, and we never have a chance to re-install our notifier service.  So, this
     * deployment uses a special registry that won't allow the service to get overwritten.
     */
    private static final class TakxDeployment extends WeldDeployment
    {
        private final ServiceRegistry serviceRegistry;

        /**
         * @param resourceLoader         Resource Loader to use
         * @param bootstrap              Weld bootstrap
         * @param beanDeploymentArchives The set should be mutable so that additional bean deployment archives can be eventually added
         * @param extensions             Extensions to initialize
         */
        TakxDeployment(ResourceLoader resourceLoader, Bootstrap bootstrap, Set<WeldBeanDeploymentArchive> beanDeploymentArchives, Iterable<Metadata<Extension>> extensions)
        {
            super(resourceLoader, bootstrap, beanDeploymentArchives, extensions);

            // Due to an internal change in the Weld implementation, they've broken the simple mechanism we used
            // to install our own service registry.  Our superclass now calls getServices() from the constructor,
            // which is before this class' constructor is complete (and thus our serviceRegistry is null), leading
            // to an NPE during construction.  So we have to play some games and make a copy of the service registry
            // after our super-class construction is complete.

            final ServiceRegistry existingRegistry = super.getServices();
            serviceRegistry = new NoOverrideRegistry();
            serviceRegistry.addAll(existingRegistry.entrySet());
        }

        /**
         * @return Overridden to return our custom service registry
         */
        @Override
        public ServiceRegistry getServices()
        {
            // This little gem (in concert with the code in the constructor) deals with a change in our
            // superclass' handling of the service registry during construction.
            return serviceRegistry == null ? super.getServices() : serviceRegistry;
        }
    }

    /**
     * Implementation of service registry that doesn't allow our notifier service to be overridden once it is set.
     */
    private static final class NoOverrideRegistry extends SimpleServiceRegistry
    {
        @Override
        public <S extends Service> void add(Class<S> type, S service)
        {
            if (type == ObserverNotifierFactory.class)
            {
                addIfAbsent(type, service);
            } else
            {
                super.add(type, service);
            }
        }
    }
}
