package gov.takx.cdi;

import com.google.common.collect.Sets;
import gov.takx.api.annotation.ExcludedProducerMethod;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.commons.utils.StringUtils;
import gov.takx.module.DeploymentAttachments;
import gov.takx.module.PluginDeployment;
import gov.takx.platform.attachments.AttachmentKey;
import gov.takx.platform.attachments.AttachmentSet;
import gov.takx.platform.cdi.ContainerBuilder;
import gov.takx.platform.cdi.InstanceBean;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;
import jakarta.enterprise.inject.spi.ProcessProducerMethod;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.weld.bean.ProducerMethod;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.proxy.WeldClientProxy;
import org.jboss.weld.util.BeanMethods;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static gov.takx.module.DeploymentAttachments.IMPLEMENTATION_DEPLOYMENTS;
import static gov.takx.module.DeploymentAttachments.PLUGIN_DEPENDENCY_DEPLOYMENTS;

/**
 * This CDI extension contributes the core services (mostly ApplicationScope beans) into any plugin container
 * created.  They are added as unmanaged beans.
 *
 * @since 2.0.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection") // Created directly, not by ServiceLoader
public class ServiceProviderExtension implements Extension
{
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // List of package prefixes we know are not cross container references
    private static final List<String> IGNORED_TYPE_NAMES = List.of(
            "jakarta",
            "java", // includes javax
            "gov.tak.api",
            "gov.tak.platform",
            "gov.takx.api",
            "gov.takx.commons",
            "gov.takx.core",
            "gov.takx.dataservices",
            "gov.takx.platform",
            "org.jboss.weld");

    private static final DotName PSEUDO_BEAN_NAME = DotName.createSimple(PseudoBean.class.getName());
    private static final DotName INJECTABLE_SERVICE_NAME = DotName.createSimple(InjectableService.class.getName());
    private static final DotName PRODUCES_NAME = DotName.createSimple(Produces.class.getName());

    // The list of types that we want to expose from the core for injection into plugin containers
    private static final List<Class<?>> CORE_BEANS = new ArrayList<>(100);

    // The list of types that have producer methods that we want to expose from the core for injection into plugin containers
    private static final List<Class<?>> PRODUCER_BEANS = new ArrayList<>(100);

    // The VFS scope qualifiers for which we need to create bean definitions
    private static final List<Annotation> VFS_SCOPE_QUALIFIERS = List.of(
            VFSManagerScope.Literal.GLOBAL_INSTANCE,
            VFSManagerScope.Literal.PROJECT_INSTANCE);

    // Set of injectable references to class beans that contain producer methods
    private static final AttachmentKey<AttachmentSet<Object>> PRODUCER_CLASS_BEAN_REFERENCES = AttachmentKey.createSet(Object.class);

    static
    {
        // Scan the index and find all the core beans that are available for plugins to inject
        final Index coreIndex = ContainerBuilder.getCoreContainerIndex();

        coreIndex.getAnnotations(INJECTABLE_SERVICE_NAME).forEach(ai -> {
            final ClassInfo classInfo = ai.target().asClass();
            final String className = classInfo.name().toString();
            final boolean hasProducerMethods = classInfo.methods().stream().anyMatch(method -> method.hasAnnotation(PRODUCES_NAME));

            try
            {
                (hasProducerMethods ? PRODUCER_BEANS : CORE_BEANS).add(Class.forName(className));
            } catch (ClassNotFoundException e)
            {
                logger.error("Failed to load class: {}", className);
            }
        });

        if (logger.isDebugEnabled()) logger.debug("Found {} beans and {} producers", CORE_BEANS.size(), PRODUCER_BEANS.size());
    }

    private final WeldContainer coreContainer; // The core's container (from which we extract services)
    private final PluginDeployment ourDeployment; // The deployment we are processing
    private final String id;
    private final boolean hasImplementations; // Does this deployment declare implementation deployments
    private final ClassLoader classLoader;
    private final Index index;
    private final boolean hasPluginDependencies;
    private final Set<InjectionPoint> possibleNonLocalInjectionPoints = new HashSet<>();
    private final Set<PluginDeployment> transitiveDependencyDeployments = new HashSet<>();
    private boolean complete;

    /**
     * Construct using the given container as the source of services for the current container being processed.
     *
     * @param coreContainer Core service container
     * @param deployment    The plugin deployment we are processing
     */
    public ServiceProviderExtension(WeldContainer coreContainer, PluginDeployment deployment)
    {
        this.coreContainer = coreContainer;
        ourDeployment = deployment;

        id = deployment.getName();
        classLoader = ourDeployment.getAttachment(DeploymentAttachments.CLASS_LOADER);
        index = deployment.getAttachment(DeploymentAttachments.INDEX);
        hasImplementations = !deployment.getAttachmentSet(IMPLEMENTATION_DEPLOYMENTS).isEmpty();

        // Determine if we even need to worry about cross-container references.  These can only happen on deployments
        // that have dependencies on other plugin deployments.
        hasPluginDependencies = !deployment.getAttachmentSet(DeploymentAttachments.PLUGIN_DEPENDENCIES).isEmpty();

        // Calculate all the deployments that might contain cross-container injectable types.
        // This includes all our direct dependencies and implementations, and any of their declared implementations.
        transitiveDependencyDeployments.addAll(ourDeployment.getAttachmentSet(PLUGIN_DEPENDENCY_DEPLOYMENTS));
        transitiveDependencyDeployments.addAll(ourDeployment.getAttachmentSet(IMPLEMENTATION_DEPLOYMENTS));

        ourDeployment.getAttachmentSet(PLUGIN_DEPENDENCY_DEPLOYMENTS)
                .forEach(pd -> transitiveDependencyDeployments.addAll(pd.getAttachmentSet(IMPLEMENTATION_DEPLOYMENTS)));
    }

    /**
     * Process each injection point to record possible cross-container injections.
     *
     * @param pip event to process
     */
    private void processInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip)
    {
        if (complete)
        {
            if (logger.isDebugEnabled()) logger.debug("after complete: {} {}", id, pip.getInjectionPoint());
        } else
        {
            processPossibleCrossContainerInjectionPoint(pip.getInjectionPoint());
        }
    }

    /**
     * Process a producer method, recording a reference to the declaring bean for later use in dependent containers.
     *
     * @param event Event to process
     * @param beanManager This container's bean manager
     * @since 3.4
     */
    private void processProducerMethod(@Observes ProcessProducerMethod<?, ?> event, BeanManager beanManager)
    {
        // Skip producers coming from Weld itself
        if (event.getAnnotatedProducerMethod().getJavaMember().getDeclaringClass().getCanonicalName().startsWith("org.jboss.weld")) return;

        // Only support producer methods for now, consider producer fields later
        if (event.getBean() instanceof ProducerMethod<?, ?> producerMethod)
        {
            final Object beanReference = BeanManagerProxy.unwrap(beanManager)
                    .getInjectableReference(null, producerMethod.getDeclaringBean(),
                            beanManager.createCreationalContext(null));

            ourDeployment.addToAttachmentSet(PRODUCER_CLASS_BEAN_REFERENCES, beanReference);
        }
    }

    /**
     * Process an injection point and record it if it is a potential cross-container injection reference.
     *
     * @param ip Injection point to process
     * @since 2.25
     */
    private void processPossibleCrossContainerInjectionPoint(InjectionPoint ip)
    {
        final Type type = ip.getType();

        // If it is known that it cannot be an XC reference, skip it
        if (cannotBeCrossContainerReference(type)) return;

        // If this deployment has implementations, then any injection point could potentially be resolvable
        // from the implementation container(s).
        if (hasImplementations)
        {
            logger.debug("Possible IMPL XC Ref: {} : {} in {}", type, ip, id);
            possibleNonLocalInjectionPoints.add(ip);
            return;
        }

        if (!hasPluginDependencies) return; // Nothing to do since this deployment has no plugin dependencies

        // If it's definitely not a cross-container ref, then nothing to do
        if (!isCrossContainerReference(type)) return;

        logger.debug("Possible XC Ref: {} in {}", ip, id);

        // Ok, it's a possible cross-container injection, so track it so we can produce bean definitions later.
        possibleNonLocalInjectionPoints.add(ip);
    }

    /**
     * Determine if a given type reference cannot be a cross-container injection reference.  Check for primitive types
     * and packages we know come from the core or from language extensions (like CDI itself).
     *
     * @param type The type to test
     * @return {@code true} if the given type cannot be an XC reference
     * @since 5.2
     */
    private boolean cannotBeCrossContainerReference(Type type)
    {
        final String typeName = type.getTypeName();

        // If it's one of our known core or language types, then it's not
        if (StringUtils.startsWithAny(typeName, IGNORED_TYPE_NAMES)) return true;

        if (type instanceof Class<?> cls)
        {
            return cls.isPrimitive(); // If it's primitive, then it's not
        } else if (!(type instanceof ParameterizedType))
        {
            logger.warn("Don't know how to process injection point of type: {}", type);
            return true;
        }

        return false;
    }

    /**
     * Determine if a given type reference is a cross-container injection reference.  Weed out types found locally
     * within this container.
     *
     * @param type The type to test
     * @return {@code true} if it looks like the referent is in another container
     */
    private boolean isCrossContainerReference(Type type)
    {
        final String typeName = type.getTypeName();
        final DotName dotName = DotName.createSimple(typeName);

        // Test to see if the type can be resolved locally
        final ClassInfo classInfo = index.getClassByName(dotName);

        if (classInfo == null)
        {
            logger.debug("No local class found for {} in {}", typeName, id);

            // Check to see if the type can be found in a deployment for which the current deployment is an implementation
            // (via @ImplementedBy).

            for (PluginDeployment apiDeployment : ourDeployment.getAttachmentSet(DeploymentAttachments.IMPLEMENTATION_FOR))
            {
                final ClassInfo apiClassInfo = apiDeployment.getAttachment(DeploymentAttachments.INDEX).getClassByName(dotName);

                // if we found the class in a deployment for which we are an implementation, then we assume that
                // the type will be resolvable within the current container.

                if (apiClassInfo != null)
                {
                    logger.debug("Found type in API deployment: {}", apiDeployment);
                    return false; // We are the implementor, so not cross-container
                }
            }

            return true; // Can't be resolved locally, and we're not an implementor, so assume it is cross-container
        }

        return false;
    }

    /**
     * Observe AfterBeanDiscovery so we can add all required beans to the plugin deployment.  This includes
     * beans defined in the core and any synthetic beans needed to satisfy cross-container injection.
     */
    private void addBeansToPluginContainers(@Observes AfterBeanDiscovery event, BeanManager beanManager)
    {
        addCoreBeans(event, beanManager);

        findPseudoBeanCrossContainerInjectionPoints(beanManager);
        createCrossContainerSyntheticBeans(event, beanManager);
    }

    /**
     * Observe AfterDeploymentValidation so we know when the container setup is complete.  We need to track this
     * since the Instance Manager will create injection targets at runtime (as @PseudoBean objects are created
     * for the first time), and that will lead to calls into processInjectionPoint.  We want to avoid doing any
     * processing at that point (it's just wasted overhead since the container has already been configured).
     *
     * @since 2.25
     */
    private void doneWithContainer(@Observes AfterDeploymentValidation event)
    {
        logger.debug("Container {} complete", id);

        complete = true;
    }

    /**
     * Add services/beans available in our core container that should be injectable in plugin containers.
     * This includes beans that provide producer methods for additional injectable types from the core.
     *
     * @param event AfterBeanDiscovery event to use to create the bean definitions
     * @since 2.25
     */
    private void addCoreBeans(AfterBeanDiscovery event, BeanManager beanManager)
    {
        // Register our basic core beans
        CORE_BEANS.forEach(beanClass -> addBeanInstance(event, getRegistrationType(beanClass), () -> coreContainer.select(beanClass).get()));

        // Add all the core beans that have producer methods we want to expose to plugin containers.
        //
        // It is assumed that all these objects are ApplicationScoped.  If you add one that is not, you will
        // need to update the processBeanWithProducers method to accept a scope and pass in the appropriate value.

        PRODUCER_BEANS.forEach(beanClass -> processBeanWithProducers(event, beanManager, beanClass));

        // Add in the project and global VFS managers
        for (Annotation vfsScopeQualifier : VFS_SCOPE_QUALIFIERS)
        {
            addBeanInstance(event, IVirtualFileSystemManager.class, Collections.singleton(vfsScopeQualifier),
                    () -> coreContainer.select(IVirtualFileSystemManager.class, vfsScopeQualifier).get());
        }
    }

    /**
     * Add the instance (from the core container) of the given type as a bean to the new container and add all the
     * producer methods defined on the type as well.
     *
     * @param event       container event, used to access bean builder
     * @param beanManager new container's bean manager
     * @param type        The concrete type to process (instantiate within the core container)
     */
    private <T> void processBeanWithProducers(AfterBeanDiscovery event, BeanManager beanManager, Class<T> type)
    {
        // Since we are essentially copying a bean instance between containers, we don't want to add a proxy reference.
        // So, we need a reference to the actual instance (not the proxy) to inject that instance into the new container.

        final T instance = InjectionUtils.getFullyInitializedInstance(coreContainer, type);

        addBeanInstance(event, getRegistrationType(type), () -> instance);
        addProducerMethodsFromClassBean(event, beanManager, type, instance);
    }

    /**
     * Since the CDI bean scanning doesn't process our {@code @PseudoBean} types, we need to do that manually.
     * <p>
     * We're looking for cases of "cross-container injection".
     *
     * @param beanManager CDI Bean manager
     * @since 2.25
     */
    private void findPseudoBeanCrossContainerInjectionPoints(BeanManager beanManager)
    {
        // Scan the current deployment's index and find all classes that are @PseudoBean.
        for (AnnotationInstance annotationInstance : index.getAnnotations(PSEUDO_BEAN_NAME))
        {
            final AnnotationTarget annotationTarget = annotationInstance.target();
            final Class<?> targetClass = getInjectionTargetClass(annotationTarget.toString());

            // Create an InjectionTargetFactory for the targetClass and then create the InjectionTarget.
            // The creation of the injection target will cause all the injection points on that target to
            // be created, and that will cause a call to processInjectionPoint() above.

            logger.debug("Process @PseudoBean: {}", annotationTarget);

            beanManager.getInjectionTargetFactory(beanManager.createAnnotatedType(targetClass))
                    .createInjectionTarget(null);
        }
    }

    /**
     * Create synthetic beans for all the cross container references so bean validation and injection works properly.
     * Take care to avoid creating multiple bean definitions for the same bean instance.
     *
     * @param event AfterBeanDiscovery event to use to create the bean definitions
     * @since 2.25
     */
    private void createCrossContainerSyntheticBeans(AfterBeanDiscovery event, BeanManager beanManager)
    {
        // We now have a set of possible XC injection types. There are 3 categories that these can fall into:
        //
        //   1. A type (typically an interface) is declared in a dependent deployment, but has a bean that resolves
        //      that type in the current container
        //
        //   2. A deployment that has implementation deployments is trying to inject a type declared in the current
        //      deployment, but the bean needed to resolve it is in the implementation deployment
        //
        //   3. A "simple" XC reference where the type and resolvable instance are from a dependent deployment.

        final Set<Object> instances = Sets.newIdentityHashSet();

        // Process any beans that have producer methods coming from our dependency deployments.
        //
        // Do this before processing XC injection points because the producer methods themselves
        // may have injected arguments that could come from another container.

        ourDeployment.getAttachmentSet(PLUGIN_DEPENDENCY_DEPLOYMENTS).stream()
                .flatMap(pd -> pd.getAttachmentSet(PRODUCER_CLASS_BEAN_REFERENCES).stream())
                .forEach(classBeanReference -> processClassWithProducerMethods(event, beanManager, classBeanReference, instances.add(classBeanReference)));

        for (var injectionPoint : possibleNonLocalInjectionPoints)
        {
            // For all these cases, we need to test to see if the injection type is resolvable locally.  If it is,
            // then we should NOT create a synthetic bean, since types 1 and 3 can't be identified any other way.

            if (isResolvable(injectionPoint, beanManager)) continue;

            // Not resolvable locally, so add an XC synthetic bean to satisfy the injection
            final var injectableInstance = getInjectableInstance(injectionPoint);
            final var definingBean = injectableInstance.definingBean;

            // Avoid adding the same object (bean)
            if (instances.add(definingBean))
            {
                if (logger.isDebugEnabled()) logger.debug("add XC bean: {} for: {}", definingBean, injectionPoint.getType());

                // In this case, we need to add the bean using the transitive closure of the defining bean's type
                // to ensure that all injection points that would resolve to it (regardless of the type they
                // specify directly) will be resolved by this bean definition.  We also add all the qualifying
                // annotations for the bean definition.

                event.addBean()
                        .addTransitiveTypeClosure(definingBean.getBeanClass())
                        .addQualifiers(definingBean.getQualifiers())
                        .produceWith(i -> injectableInstance.instance.get());
            }
        }
    }

    /**
     * Process a bean with producer methods, adding a synthetic bean for that bean, and producer beans for all the
     * producer methods found in the given {@code producerClassBeanReference}.
     *
     * @param event container event, used to access bean builder
     * @param beanManager This container's bean manager
     * @param producerClassBeanReference Reference to bean class with producer methods in the dependency's container
     * @param addClassBean {@code true} if we should add a bean instance for the bean containing the producer methods.
     *                     If the consumer had a direct injection point on the producer class, then it will
     *                     already have been added in the code above.  Adding it again would cause an
     *                     "ambiguous dependency" error from Weld.
     * @since 3.4
     */
    @SuppressWarnings("unchecked")
    private <T> void processClassWithProducerMethods(AfterBeanDiscovery event, BeanManager beanManager, T producerClassBeanReference, boolean addClassBean)
    {
        final Class<T> type = (Class<T>) producerClassBeanReference.getClass();

        if (addClassBean)
        {
            logger.debug("Add producer class bean: {}, type = {}", producerClassBeanReference, type);
            addBeanInstance(event, type, () -> producerClassBeanReference);
        }

        addProducerMethodsFromClassBean(event, beanManager, type, producerClassBeanReference);
    }

    /**
     * Determine if the target type for an injection point is resolvable within the given bean manager.
     *
     * @param injectionPoint Injection point of interest
     * @param beanManager    Bean manager to use for type resolution
     * @return {@code true} if the target type is resolvable
     * @since 2.25
     */
    private boolean isResolvable(InjectionPoint injectionPoint, BeanManager beanManager)
    {
        final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        final Set<Bean<?>> beans = beanManager.getBeans(injectionPoint.getType(), qualifiers.toArray(new Annotation[0]));
        final Bean<?> bean = beanManager.resolve(beans);

        if (logger.isDebugEnabled()) logger.debug("isResolvable: {} q={} => {}", injectionPoint.getType(), qualifiers, bean);

        return bean != null;
    }

    /**
     * Obtain the {@code InjectableInstance} for a cross-container injection point.  Search for a resolvable bean
     * in all the transitive dependencies and implementations for this deployment.
     *
     * @param injectionPoint Injection point to resolve
     * @return InjectableInstance holding the instance and bean definition
     * @throws DeploymentException If the injection type can not be resolved
     */
    private InjectableInstance getInjectableInstance(InjectionPoint injectionPoint) throws DeploymentException
    {
        logger.debug("Get XC bean instance for: {}", injectionPoint);

        // If the type isn't resolvable within the container that holds the injected type, then it may be
        // that the implementation is in another jar.  This is configured using the @ImplementedBy annotation
        // somewhere within the original type's jar.
        //
        // For a more complete example, assume Plugin A has an injection point referring to type 'IWhatever',
        // and that type is defined in plugin B (plugin A must have an @PluginDependency on B for this to work).
        // However, there is no bean in Plugin B's container that is resolvable for that type, and Plugin B
        // has declared, using an @ImplementedBy annotation, that its interfaces have implementations in Plugin C.
        // In that case, we need to find the container for Plugin C and try to resolve the injection type there.
        // Keep in mind that a given API jar might have implementations that live in more than one other jar.
        //
        // Another complication is that CDI producer methods can have method arguments injected, so we need to
        // scan all the transitive containers to find potential resolvable instances.

        final var qualifiers = injectionPoint.getQualifiers().toArray(new Annotation[0]);
        final Type targetType = injectionPoint.getType();

        final var injectableInstance = transitiveDependencyDeployments.stream()
                .map(deployment -> deployment.getAttachment(DeploymentAttachments.CONTAINER))
                .filter(Objects::nonNull)
                .map(container -> InjectableInstance.forType(container, targetType, qualifiers))
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> {
                    logger.error("Failed to find an implementation of [{}] in any implementation jar.\nSearched {}",
                            targetType.getTypeName(), transitiveDependencyDeployments.stream().map(PluginDeployment::getName).toList());
                    return new DeploymentException("No implementation found for type " + targetType);
                });

        logger.debug("XC instance {}/{}}", injectableInstance.containerId, injectableInstance.definingBean);

        return injectableInstance;
    }

    /**
     * Find the class that is the target type of an injection point.  This class must be directly visible to the
     * deployment's class loader.
     *
     * @param className Name of class to load
     * @return Class for that name
     * @throws DeploymentException If the given class can not be found
     * @since 2.25
     */
    private Class<?> getInjectionTargetClass(String className) throws DeploymentException
    {
        try
        {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e)
        {
            final String message = String.format("Injection target class [%s] not found in classpath for deployment [%s]",
                    className, id);
            logger.error(message, e);
            throw new DeploymentException(message, e);
        }
    }

    /**
     * Determine the type to use to register the bean in the plugin container. Check to see if {@link InjectableService#registerAs()}
     * has a value other than Void, and if so, return the specified type to register the bean, otherwise return the
     * given {@code type}.
     *
     * @param type The type annotated with {@code InjectableService}
     * @return The type under which to register the bean in the plugin container
     * @since 3.5
     */
    private Class<?> getRegistrationType(Class<?> type)
    {
        final Class<?> registerAs = type.getAnnotation(InjectableService.class).registerAs();
        return registerAs == Void.class ? type : registerAs;
    }

    /**
     * Add a bean instance of the given type to the container.  It will be registered using the given {@code beanType}
     * with no qualifiers, and using the given {@code producer} to produce the instance needed for injection.
     *
     * @param event    For accessing the bean builder
     * @param beanType Type of bean to define
     * @param producer The producer for the instance for this bean type
     */
    private void addBeanInstance(AfterBeanDiscovery event, Type beanType, Supplier<Object> producer)
    {
        addBeanInstance(event, beanType, Collections.emptySet(), producer);
    }

    /**
     * Add a bean instance of the given type to the container.  It will be registered using the given {@code beanType}
     * and {@code qualifiers} using the given {@code producer} to produce the instance needed for injection.
     *
     * @param event    For accessing the bean builder
     * @param beanType Type of bean to define
     * @param producer The producer for the instance for this bean type
     * @since 2.25
     */
    private void addBeanInstance(AfterBeanDiscovery event, Type beanType, Set<Annotation> qualifiers, Supplier<Object> producer)
    {
        event.addBean()
                .addType(beanType)
                .addQualifiers(qualifiers)
                .scope(ApplicationScoped.class)
                .produceWith(i -> producer.get());
    }

    /**
     * Scan the given {@code producerClassBeanReference} for producer methods, registering any found in this container.
     *
     * @param event container event, used to access bean builder
     * @param beanManager This container's bean manager
     * @param type The type of the {@code producerClassBeanReference}
     * @param producerClassBeanReference Producer class bean reference to scan for producer methods
     * @since 3.4
     */
    @SuppressWarnings("unchecked")
    private <T> void addProducerMethodsFromClassBean(AfterBeanDiscovery event, BeanManager beanManager, Class<T> type,
                                                     T producerClassBeanReference)
    {
        // Subtle problem: So far the producer class bean has been added to the AfterBeanDiscovery event, but it is not
        // yet known to this container's BeanManager, so we can't do any form of lookup on it.  Instead, we generate an
        // InstanceBean, allowing for the method scanning needed to find, and later invoke, the producer methods.
        //
        // As a further complication, if the producerClassBeanReference is a Weld proxy, then we need to extract the
        // underlying class in order to properly access generic type information in the methods of that class.

        final T realReference;
        final Class<T> realType;

        if (producerClassBeanReference instanceof WeldClientProxy proxy)
        {
            realReference = (T) proxy.getMetadata().getContextualInstance();
            realType = (Class<T>) realReference.getClass();
        } else
        {
            realReference = producerClassBeanReference;
            realType = type;
        }

        final var producerClassBean = InstanceBean.of(realReference, realType, beanManager);

        BeanMethods.filterMethods(producerClassBean.getEnhancedAnnotated().getDeclaredEnhancedMethods(Produces.class))
                .stream()
                .filter(method -> method.getAnnotation(ExcludedProducerMethod.class) == null)
                .filter(method -> {
                    final var returnType = method.getJavaMember().getReturnType();
                    return cannotBeCrossContainerReference(returnType) || isProducedTypeUsed(returnType);
                })
                .forEach(method -> {
                    logger.debug("add producer method: {}", method);

                    event.addBean(beanManager.createBean(beanManager.createBeanAttributes(method), realType,
                            beanManager.getProducerFactory(method, producerClassBean)));
                });
    }

    /**
     * Scan the possible XC injection points and see if any of them could be satisfied by any type in the type
     * closure of the given {@code returnType}.
     * <p/>
     * NOTE: this is not a complete match.  We are only comparing the types, and ignoring any qualifiers, to keep
     * this simple/fast.
     *
     * @param returnType Return type to test
     * @return {@code true} if the given return type can be assigned to any of the possible XC injection points
     * @since 5.4
     */
    private boolean isProducedTypeUsed(Class<?> returnType)
    {
        final var typeClosure = new HierarchyDiscovery(returnType).getTypeClosure();
        logger.debug("closure of {} : {}", returnType, typeClosure);

        for (var injectionPoint : possibleNonLocalInjectionPoints)
        {
            logger.debug("    check {}", injectionPoint);
            if (isAssignableFromTypeInClosure(injectionPoint.getType(), typeClosure)) return true;
        }

        logger.debug("NOT USED: {}", returnType);
        return false;
    }

    /**
     * @param ipType Type of the injection point
     * @param typeClosure Collection ot types to test against {@code ipType}
     * @return {@code true} if the given {@code ipType} is assignable from any of the types in the {@code typeClosure}.
     * @since 5.4
     */
    private boolean isAssignableFromTypeInClosure(Type ipType, Set<Type> typeClosure)
    {
        final var ipClass = getClassFromType(ipType);

        for (Type tcType : typeClosure)
        {
            final var tcClass = getClassFromType(tcType);
            if (ipClass == null || tcClass == null || ipClass.isAssignableFrom(tcClass)) return true;
        }

        return false;
    }

    /**
     * @return The Class of a given Type, or {@code null} if not a {@link Class} or {@link ParameterizedType}
     * @since 5.4
     */
    @Nullable
    private Class<?> getClassFromType(@Nonnull Type type)
    {
        if (type instanceof Class<?> cls) return cls;
        if (type instanceof ParameterizedType parameterizedType) return (Class<?>) parameterizedType.getRawType();
        return null; // Don't know how to handle this Type
    }

    /**
     * Holder to track a contextual instance and the bean that defines it in a container.  We need both the
     * instance and the defining bean so that we can access the full type closure and any associated qualifying
     * annotations.
     *
     * @param containerId ID of the CDI container holding the given {@code definingBean}
     * @param definingBean CDI bean that defines the injectable type
     * @param instance {@link Instance} that can be used to obtain an actual contextual instance of the bean type.
     *                 We need to use an {@link Instance} so we can delay the instantiation of the
     *                 actual contextual instance until injection is taking place.  Contextual instances
     *                 that have producer methods often need to have state built up before they can be
     *                 obtained, and this delay provides that opportunity.
     *
     * @since 2.25 (signature changed in 5.4)
     */
    private record InjectableInstance(String containerId, Bean<?> definingBean, Instance<?> instance)
    {
        /**
         * Construct an {@code InjectableInstance} for the given type from the given container.  If there is no bean
         * within that container that can resolve the requested {@code targetType}, then a {@code null} is returned.
         *
         * @param container  Container to use to resolve the {@code targetClass}
         * @param targetType Target type to resolve. Generic information should be included when available; if a
         *                   {@code ParameterizedType} is detected, do NOT pass in the raw type or the bean may
         *                   become unresolvable.
         * @param qualifiers Qualifiers specified at injection point
         * @return {@code InjectableInstance} for the target type, or {@code null} if not resolvable within the container
         */
        private static InjectableInstance forType(WeldContainer container, Type targetType, Annotation[] qualifiers)
        {
            final String containerId = container.getId();
            final Instance<?> instance = container.select(targetType, qualifiers);

            if (instance.isResolvable())
            {
                // Special handling to ensure that we don't select a previously created XC synthetic bean.
                // The bean class for an XC synthetic bean will be ServiceProviderExtension instead of the
                // actual bean's class.

                final Bean<?> bean = instance.getHandle().getBean();
                final Class<?> beanClass = bean.getBeanClass();

                if (beanClass != ServiceProviderExtension.class)
                {
                    return new InjectableInstance(containerId, bean, instance);
                } else
                {
                    logger.debug("Skip XC synthetic: {}", bean);
                }
            } else if (instance.isUnsatisfied())
            {
                logger.debug("Container [{}] has no definitions for type [{}]", containerId, targetType);
            } else
            {
                final String beans = StreamSupport.stream(instance.handles().spliterator(), false)
                        .map(h -> h.getBean().toString())
                        .collect(Collectors.joining(", "));

                logger.warn("Implementation container [{}] has ambiguous definitions for type [{}]:\n matching beans [{}]",
                        containerId, targetType, beans);
            }

            return null;
        }
    }
}
