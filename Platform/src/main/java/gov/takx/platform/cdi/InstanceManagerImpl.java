package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.cdi.InstanceManager;
import gov.takx.api.event.cdi.PseudoBeanCreationEvent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.ObjectUtils;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation responsible for post-creation injection on new object instances.
 * There are several sub-cases that need to be excluded from injection processing:
 * <ol>
 * <li>
 * We don't inject interfaces, so if the direct type is an interface, we skip it.
 * </li>
 * <p>
 * <li>
 * When an annotated class has nested classes, they will be captured by the aspect's join point.  We want
 * to avoid injecting those classes (unless they are also directly annotated with @PseudoBean).
 * </li>
 * <p>
 * <li>
 * If the direct type is NOT directly annotated, then we don't inject it.  It will either be a nested
 * type or a super-class.
 * </ol>
 *
 * @see InstanceManager
 * @since 2.0.0
 */
@Internal
@Singleton
public class InstanceManagerImpl implements InstanceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Big enough so that we shouldn't have to resize during normal execution (unless plugins include a LOT of
    // PseudoBean classes).
    private static final int INITIAL_SIZE = 500;

    private final BeanManager beanManager;
    private final InjectableObserverManager observerManager;
    private final CreationalContext<Object> nullCreationalContext;
    private final String containerId;

    /**
     * Cache used to avoid creating the InjectionTargets repeatedly.  Building the AnnotatedType and the
     * InjectionTarget is costly, so cache the results.
     */
    private final Map<Class<?>, InjectionTarget<Object>> injectionCache = new ConcurrentHashMap<>(INITIAL_SIZE);

    /**
     * Track ignored class names so we can quickly skip them later.
     */
    private final Set<String> ignoredClasses = ConcurrentHashMap.newKeySet();

    /**
     * Track class names known to have an "inherited" @PseudoBean annotation.
     */
    private final Set<String> inheritedPseudoBeanClasses = ConcurrentHashMap.newKeySet();

    /**
     * Track the closest super-class (value) of each class (key) that is annotated with @PseudoBean.
     */
    private final Map<Class<?>, Class<?>> closestPseudoBeanSuperClass = new ConcurrentHashMap<>(INITIAL_SIZE);

    /**
     * Broadcast event for completed injection of {@link PseudoBean} instances.
     */
    private final Event<PseudoBeanCreationEvent> pseudoBeanCreationEventEvent;

    private final boolean underTest;

    /**
     * Track the current object being injected so producers can get access to it.  This is required
     * since there is no way to get hold of the object instance from an InjectionPoint, which is all a producer
     * has access to.
     * <p>
     * Note this is tracked in a static since we don't care which actual instance manager is processing the current
     * injection.  We just need to know which object is being processed on the current thread.  If we wanted to
     * keep this non-static, we'd need to provide some other linkage to allow the producer to determine which
     * instance manager instance to query.  This seemed like the better solution.
     */
    private static final ThreadLocal<Object> currentInstance = new ThreadLocal<>();

    /**
     * CDI Constructor.
     *
     * @since 3.0
     */
    @Inject
    InstanceManagerImpl(WeldContainer container, BeanManager beanManager, InjectableObserverManager observerManager,
                        Event<PseudoBeanCreationEvent> pseudoBeanCreationEventEvent, @PlatformUnderTest boolean underTest)
    {
        this.beanManager = beanManager;
        this.observerManager = observerManager;
        this.pseudoBeanCreationEventEvent = pseudoBeanCreationEventEvent;
        this.underTest = underTest;

        nullCreationalContext = beanManager.createCreationalContext(null);

        containerId = "[" + container.getId() + "] "; // Container id for logging

        logger.debug("{} initialized", containerId);
    }

    @Override
    public Object getCurrentInjectionInstance()
    {
        return currentInstance.get();
    }

    @Override
    public void inject(Object instance, Class<?> directType, boolean inherited)
    {
        //noinspection unchecked
        final Class<Object> instanceType = (Class<Object>) instance.getClass();
        final String className = instanceType.getName();
        final String directTypeName = directType.getName();

        // If the direct type is an interface, we know we can skip it since we don't support @PseudoBean interfaces.
        if (directType.isInterface())
        {
            logger.trace("{}Skip interface injection: {}", containerId, directTypeName);
            return;
        }

        // If it's already marked as ignored, then skip it
        if (ignoredClasses.contains(directTypeName))
        {
            logger.trace("{}Skip Ignored: {}", containerId, className);
            return;
        }

        // At this point, we need to see if the direct type is annotated, directly or indirectly (@PseudoBean is
        // an annotation on an annotation on the direct class).  To keep this code as efficient as possible, we
        // only make the indirect test if "inherited" is true.

        final boolean directTypeIsAnnotated = inherited
                ? (inheritedPseudoBeanClasses.contains(directTypeName) || hasInheritedPseudoBean(directType))
                : directType.isAnnotationPresent(PseudoBean.class);

        // If the direct type is NOT annotated, then we never want to process it.

        if (!directTypeIsAnnotated)
        {
            logger.trace("{}Skip non-annotated type: {}", containerId, directTypeName);
            ignoredClasses.add(directTypeName); // Ignore it from now on
            return;
        }

        // Since the direct type is annotated, there are two cases to handle:
        //
        // 1. If the instance type is not annotated, then we are processing an annotated base class, we should
        //    inject it if there are no classes deeper in the derivation hierarchy that are also marked as PseudoBean.
        //    This supports annotated base classes without requiring that the derived class also be annotated, but
        //    ensures that we don't perform the injection until we are processing the most derived class that is
        //    marked as PseudoBean.
        //
        // 2. If the instance is annotated, but the instance and direct types are different, then we can assume
        //    we're processing a super-class constructor that is also annotated, so we should ignore it and wait
        //    for the final constructor of the more derived type.
        //
        // These both boil down to a test on whether the closest super-class (including the instance type itself)
        // that is marked as @PseudoBean is the direct type.

        final Class<?> pseudoBeanSuperClass = findClosestPseudoBeanSuperClass(instanceType);

        if (pseudoBeanSuperClass != directType)
        {
            logger.trace("{}Skip annotated super-class: {}", containerId, directTypeName);
            return;
        }

        // Find the injection target, create on demand as needed
        final InjectionTarget<Object> injectionTarget = injectionCache.computeIfAbsent(instanceType,
                k -> beanManager.getInjectionTargetFactory(beanManager.createAnnotatedType(instanceType)).createInjectionTarget(null));

        if (logger.isDebugEnabled()) logger.debug("{}Inject: {}", containerId, ObjectUtils.identityToString(instance));

        // To mimic the CDI container as closely as possible, inject it and then call the post construct method (if any)
        // and wire in any @Observes methods
        try
        {
            currentInstance.set(instance);

            injectionTarget.inject(instance, nullCreationalContext); // Inject it
            injectionTarget.postConstruct(instance); // PostConstruct it

            observerManager.handleNewInstance(instance); // Wire up observers

            // Only fire this event when running tests
            if (underTest) pseudoBeanCreationEventEvent.fire(new PseudoBeanCreationEvent(instance));
        } catch (Exception e)
        {
            logger.error("{} Failed to inject {}", containerId, injectionTarget, e);
        } finally
        {
            currentInstance.remove();
        }
    }

    /**
     * Determine if the given type is directly annotated with PseudoBean or if any annotations are
     * annotated with PseudoBean, recursively through all annotations.
     *
     * @param directType The type to test
     * @return {@code true} if the type is marked PseudoBean, either directly or indirectly
     */
    private boolean hasInheritedPseudoBean(Class<?> directType)
    {
        final String directTypeName = directType.getName();
        final boolean tracing = logger.isTraceEnabled();

        if (tracing) logger.trace("Test: {}", directTypeName);

        for (Annotation annotation : directType.getAnnotations())
        {
            final Class<? extends Annotation> annotationType = annotation.annotationType();

            final String annotationTypeName = annotationType.getName();
            if (annotationTypeName.startsWith("java"))
            {
                if (tracing) logger.trace("Skip known false: {}", annotationTypeName);
                continue;
            }
            if (annotationType.equals(PseudoBean.class))
            {
                if (tracing) logger.trace("Found PseudoBean on: {}", directTypeName);
                inheritedPseudoBeanClasses.add(directTypeName);
                return true;
            } else if (hasInheritedPseudoBean(annotationType))
            {
                if (tracing) logger.trace("Add sub-annotation: {}", directTypeName);
                inheritedPseudoBeanClasses.add(directTypeName);
                return true;
            }
        }

        if (tracing) logger.trace("Not found");
        return false;
    }

    /**
     * Scan the super-class hierarchy of the give type and return the first class that is annotated
     * with {@link PseudoBean}.  This may be the given original type if it is annotated.
     *
     * @param type Type to process
     * @return Closest super-class that is tagged with {@link PseudoBean}
     */
    private Class<?> findClosestPseudoBeanSuperClass(Class<?> type)
    {
        // Even though using Map.computeIfAbsent would look cleaner here, it forces the creation of temporary
        // objects for capturing values into the lambda.  So, do it old school to avoid those allocations.
        Class<?> pseudoBeanSuper = closestPseudoBeanSuperClass.get(type);
        if (pseudoBeanSuper == null)
        {
            Class<?> s = type;
            while (s != null && !inheritedPseudoBeanClasses.contains(s.getName()) && !s.isAnnotationPresent(PseudoBean.class))
            {
                s = s.getSuperclass();
            }

            if (s != null)
            {
                pseudoBeanSuper = s;
                closestPseudoBeanSuperClass.put(type, s);
            }
        }
        return pseudoBeanSuper;
    }

    public String toString()
    {
        return "InstanceManager{cid=" + containerId + "}";
    }
}
