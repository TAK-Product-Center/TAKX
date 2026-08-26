package gov.takx.platform.cdi;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.weld.bootstrap.api.Bootstrap;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.environment.deployment.WeldBeanDeploymentArchive;
import org.jboss.weld.environment.deployment.discovery.BeanArchiveBuilder;
import org.jboss.weld.environment.deployment.discovery.BeanArchiveScanner;
import org.jboss.weld.environment.deployment.discovery.jandex.Jandex;
import org.jboss.weld.environment.deployment.discovery.jandex.JandexDiscoveryStrategy;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 * Specialized discovery strategy implementation that uses Jandex for rapid discovery of annotated beans.
 * The {@link JandexDiscoveryStrategy} implementation, and the way that Weld initializes discovery strategies, forces
 * us to use an extended class and NOT use normal service loading, so we can better control how we scan for beans.
 *
 * @since 3.4
 */
class IndexedDiscoveryStrategy extends JandexDiscoveryStrategy
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String id;
    private final BeansXml beansXml;
    private final Index index;

    IndexedDiscoveryStrategy(ResourceLoader resourceLoader, Bootstrap bootstrap,
                             Set<Class<? extends Annotation>> initialBeanDefiningAnnotations, String id, BeansXml beansXml, Index index)
    {
        super(resourceLoader, bootstrap, initialBeanDefiningAnnotations, BeanDiscoveryMode.ANNOTATED);

        this.id = id;
        this.beansXml = beansXml;
        this.index = index;
    }

    @Override
    public void setScanner(BeanArchiveScanner beanArchiveScanner)
    {
        logger.warn("Ignoring scanner: {}", beanArchiveScanner);
    }

    @Override
    public Set<WeldBeanDeploymentArchive> performDiscovery()
    {
        final BeanArchiveBuilder builder = makeBeanArchiveBuilder();
        beforeDiscovery(Set.of(builder));
        final WeldBeanDeploymentArchive archive = processAnnotatedDiscovery(builder);
        return Set.of(archive);
    }

    /**
     * @return archive builder initialized with our config and classes from the index
     */
    private BeanArchiveBuilder makeBeanArchiveBuilder()
    {
        final BeanArchiveBuilder builder = new BeanArchiveBuilder();
        builder.setId(id);
        builder.setBeansXml(beansXml);
        builder.setAttribute(Jandex.INDEX_ATTRIBUTE_NAME, index);

        // Add all known classes from the index
        for (ClassInfo classInfo : index.getKnownClasses()) builder.addClass(classInfo.name().toString());

        // Add our startup handler so we can record the container and its class loader BEFORE any observers
        // in the new container receive a 'Startup' event.
        builder.addClass(StartupHandler.class.getName());

        // Add handling for @PseudoBean and @Observes usage within @PseudoBean instances
        // These have to be done per-container since a plugin container's index doesn't include Platform classes
        builder.addClass(InstanceManagerImpl.class.getName());
        builder.addClass(InjectableObserverManager.class.getName());
        builder.addClass(CrossContainerEventDistributor.class.getName());

        return builder;
    }
}
