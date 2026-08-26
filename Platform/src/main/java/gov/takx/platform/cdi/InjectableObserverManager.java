package gov.takx.platform.cdi;

import com.google.common.collect.MapMaker;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.cdi.InstanceManager;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.inject.Inject;
import org.apache.commons.lang3.ObjectUtils;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.resolution.Resolvable;
import org.jboss.weld.resolution.ResolvableBuilder;
import org.jboss.weld.util.reflection.HierarchyDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This class is responsible for providing support for using {@link Observes} annotations within a non-CDI bean
 * that is marked with {@link PseudoBean}.  This allows beans that are not part of the
 * normal CDI container to participate in the CDI event distribution.
 *
 * @see InstanceManager
 * @since 2.0.0
 */
@Internal
@ApplicationScoped
public class InjectableObserverManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Big enough so that we shouldn't have to resize during normal execution (unless plugins include a LOT of
    // observers in @PseudoBean classes).
    private static final int INITIAL_SIZE = 200;

    private final BeanManager beanManager;
    private final String containerId;

    /**
     * A cache of resolvable instances, keyed by event type.
     */
    private final Map<ResolvableCacheKey, Resolvable> cachedResolvables = new ConcurrentHashMap<>(INITIAL_SIZE);

    /**
     * Collection of instance types that have already been scanned for observer methods.
     */
    private final Map<Class<?>, Boolean> observersScanned = new ConcurrentHashMap<>(INITIAL_SIZE);

    /**
     * Track which types have observer methods on them to optimize which instances we track.
     */
    private final Set<Class<?>> typesWithObservers = ConcurrentHashMap.newKeySet(INITIAL_SIZE);

    /**
     * All observer methods located so far.  Used to resolve against a given event being fired.
     */
    private final Set<ObserverMethod<?>> observerMethods = ConcurrentHashMap.newKeySet(INITIAL_SIZE);

    /**
     * A cache of resolved observer methods, keyed by a Resolvable.  The cost of resolving the entire list of
     * 'observerMethods' (above) against a given Resolvable is fairly high.  So, cache the resolved subset of
     * those methods for each Resolvable.  However, as new PseudoBean types are scanned, we will need to clear
     * out this cache since we don't have any way to know what Resolvables might be used in the future.  The
     * good news is that object scanning happens once per type, whereas event firing happens way more often.
     */
    private final Map<Resolvable, List<? extends ObserverMethod<?>>> observerMethodsPerResolvable = new ConcurrentHashMap<>();

    /**
     * Map from an PseudoBean type to the InstanceBean derived from it.  Used so we can re-target an instance
     * bean to each instance as events are being fired.
     */
    private final Map<Class<?>, InstanceBean<?>> instanceBeanForType = new ConcurrentHashMap<>(INITIAL_SIZE);

    /**
     * Tracks {@link PseudoBean} types marked as singleton observers.
     */
    private final Set<Class<?>> singletonObservers = ConcurrentHashMap.newKeySet();

    /**
     * Map from an instance class to all the instances of that class.  Used to fire events to instances.
     */
    private final Map<Class<?>, Set<Object>> instancesByType = new ConcurrentHashMap<>(INITIAL_SIZE);

    @Inject
    InjectableObserverManager(WeldContainer container)
    {
        beanManager = container.getBeanManager();

        containerId = "[" + container.getId() + "]"; // Get the container name for logging

        if (logger.isDebugEnabled()) logger.debug("{} initialized", containerId);
    }

    /**
     * Process a new object instance.  If it is the first time we've seen its type, then scan it for observers
     * and record the results.  If this instance's type has observer methods, then add it to the map of instances
     * that will be notified if an event resolves to a method on this instance's type.
     *
     * @param instance Object instance to process
     */
    public void handleNewInstance(Object instance)
    {
        final Class<?> type = instance.getClass();

        // If this is the first time we've encountered a given type, then scan it for observer methods
        observersScanned.computeIfAbsent(type, k -> {
            if (shouldHandle(type)) scanForObservers(instance);
            return true;
        });

        // If this type has observer methods, then track this instance
        if (typesWithObservers.contains(type))
        {
            synchronized (instancesByType)
            {
                final var typeInstances = getTypeInstances(type);

                // If this is a singleton observer, then remove other instances from the observer map
                if (singletonObservers.contains(type))
                {
                    logger.debug("Remove prior instances of singletonObserver: {}", type);
                    typeInstances.clear();
                }

                if (logger.isDebugEnabled())
                {
                    logger.debug("Add new observer [{}] = {}", type, ObjectUtils.identityToString(instance));
                }

                typeInstances.add(instance);
            }
        }
    }

    /**
     * Determine if we should handle this type. If the class is annotated with  {@link ApplicationScoped}, we don't.
     *
     * @param type Type to test
     * @return {@code true} if we should process it
     * @since 3.0
     */
    private boolean shouldHandle(Class<?> type)
    {
        if (type.getAnnotation(ApplicationScoped.class) != null)
        {
            logger.debug("{} Skip @AppScoped bean: {}", containerId, type);
            return false;
        }

        return true;
    }

    /**
     * Get the list of instances for a given type. The returned list will never be {@code null}, and is created as an
     * empty list on first retrieval.
     *
     * @param type Class of observer
     * @return Collection of references to observers of that type
     */
    private Set<Object> getTypeInstances(Class<?> type)
    {
        return instancesByType.computeIfAbsent(type, k -> {
            logger.debug("New observer type: {}", type);
            // The underlying map also needs to be concurrent
            return Collections.newSetFromMap(new MapMaker().initialCapacity(INITIAL_SIZE).weakKeys().makeMap());
        });
    }

    /**
     * Scan the given instance for any observer methods.
     *
     * @param instance Object to process
     */
    @SuppressWarnings("unchecked")
    private <T> void scanForObservers(T instance)
    {
        final Class<T> type = (Class<T>) instance.getClass();

        logger.debug("{} Scanning: {}", containerId, type);

        // See if this class is directly annotated with @PseudoBean, and if so, determine if it is a singleton.
        final PseudoBean pseudoBean = type.getAnnotation(PseudoBean.class);
        if (pseudoBean != null && pseudoBean.singletonObserver())
        {
            logger.debug("{} Add singletonObserver type: {}", containerId, type);
            singletonObservers.add(type);
        }

        final Set<ObserverMethod<?>> methods = InjectionUtils.getAllObserverMethods(beanManager, type,
                supplyAndRecordInstanceBean(instance, type));

        if (!methods.isEmpty())
        {
            if (logger.isTraceEnabled()) logger.trace("{} Observers in {}: {}", containerId, type, methods);

            typesWithObservers.add(type); // This type has observer methods
            observerMethods.addAll(methods);

            // Brute force, but we have no way of knowing the ultimate Resolvable that will be used to fire events,
            // so we just clear out the cache.  This isn't as bad as it seems since events will typically be fired
            // way more frequently than new @PseudoBean classes with observers will be discovered.
            observerMethodsPerResolvable.clear();
        }
    }

    /**
     * Most {@link PseudoBean} types do not include Observer methods, so we want to avoid the overhead of creating
     * a new {@link InstanceBean} for each one scanned.  The supplier returned by this method will create the
     * {@code InstanceBean} and record it in {@code instanceBeanPerType}.  The returned supplier is only invoked if
     * there are actually observer methods on the {@link PseudoBean} type.
     *
     * @param instance PseudoBean instance
     * @param type     Type of instance
     * @param <T>      Inferred type
     * @return Supplier of {@link InstanceBean} for the given instance/type
     * @since 3.0
     */
    private <T> Supplier<Bean<T>> supplyAndRecordInstanceBean(T instance, Class<T> type)
    {
        return () -> {
            final InstanceBean<T> instanceBean = InstanceBean.of(instance, type, beanManager);
            instanceBeanForType.put(type, instanceBean);
            return instanceBean;
        };
    }

    /**
     * Observe all events that take place in this container.
     * <p/>
     * ATM, this method has to exist, it has to observe ALL events, and it has to have the {@link EventMetadata}
     * parameter, or when an event is passed to {@link GuaranteedObserverNotifier#notifySyncObservers},
     * the {@code metadata} value will be null, and it won't be able to properly determine the set of observers to
     * deliver the event to.
     */
    private void observe(@Observes @Priority(3000) @Any Object event, EventMetadata ignored)
    {
        // No-op
    }

    /**
     * Resolve the recorded observer methods against the given event type and metadata (the source of the event,
     * which carries any qualifiers on the event type).
     *
     * @param eventType Type of event being fired
     * @param metadata  Metadata about the event (e.g., qualifiers)
     * @return List of observer methods that observe the given fully qualified event
     * @since 3.0
     */
    private List<? extends ObserverMethod<?>> resolveObserverMethods(Class<?> eventType, EventMetadata metadata)
    {
        final var cacheKey = new ResolvableCacheKey(eventType, metadata);
        var resolvable = cachedResolvables.get(cacheKey);

        if (resolvable == null)
        {
            resolvable = new ResolvableBuilder(metadata.getType(), BeanManagerProxy.unwrap(beanManager))
                    .addTypes(new HierarchyDiscovery(eventType).getTypeClosure())
                    .addQualifiers(metadata.getQualifiers())
                    .create();

            cachedResolvables.put(cacheKey, resolvable);
        }

        return observerMethodsPerResolvable.computeIfAbsent(resolvable,
                k -> InjectionUtils.resolveObservers(beanManager, k, observerMethods).stream()
                        .map(this::makeRetargetingObserverMethod)
                        .toList());
    }

    /**
     * Create a {@link RetargetingObserverMethod} wrapping the given observer method.
     *
     * @param observerMethod Observer to wrap
     * @return wrapped observer
     * @since 5.2
     */
    @Nonnull
    private <T> ObserverMethod<T> makeRetargetingObserverMethod(@Nonnull ObserverMethod<T> observerMethod)
    {
        final Class<?> beanType = observerMethod.getBeanClass();
        final InstanceBean<?> instanceBean = instanceBeanForType.get(beanType);

        synchronized (instancesByType)
        {
            return new RetargetingObserverMethod<>(containerId, observerMethod, instanceBean,
                    () -> getTypeInstances(beanType).stream()
                            .filter(Objects::nonNull) // Skip those that have been reaped
                            .toList());
        }
    }

    /**
     * Get the list of observers for the given event type.
     *
     * @param eventType Type of event being fired
     * @param metadata  Metadata about the event (e.g., qualifiers)
     * @return List of observers, may be empty
     * @since 5.2
     */
    @Nonnull
    List<? extends ObserverMethod<?>> getEventObservers(@Nonnull Class<?> eventType, @Nonnull EventMetadata metadata)
    {
        return resolveObserverMethods(eventType, metadata);
    }

    /**
     * Remove a given observer from instancesByType.
     *
     * @param observer Observer instance to remove
     */
    public void removeObserver(Object observer)
    {
        logger.debug("Removing observer: {}", observer);

        synchronized (instancesByType)
        {
            getTypeInstances(observer.getClass()).remove(observer);
        }
    }

    @Override
    public String toString()
    {
        return "InjectableObserverManager{" +
                "containerId='" + containerId + '\'' +
                '}';
    }

    /**
     * Key into the resolvable cache.  We need to take into account the event type and all qualifiers
     * to uniquely identify a resolvable.
     */
    private static final class ResolvableCacheKey
    {
        private final Class<?> eventType;
        private final Set<Annotation> qualifiers;
        private final int hashCode;

        public ResolvableCacheKey(Class<?> eventType, EventMetadata metadata)
        {
            this.eventType = eventType;
            qualifiers = metadata.getQualifiers();
            hashCode = (eventType.hashCode() * 31) + qualifiers.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ResolvableCacheKey that = (ResolvableCacheKey) o;

            return eventType.equals(that.eventType) && qualifiers.equals(that.qualifiers);
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }
    }

    /**
     * Implementation of {@link ObserverMethod} that delegates most calls to a given {@code target} observer method.
     * The notification handling is overridden to fire the event to all existing instances of the bean type of
     * the target observer.
     * 
     * @param containerId Id of container we're in
     * @param target Target observer method
     * @param instanceBean Bean backing the pseudo bean that has the observer method
     * @param instanceSupplier Supplier of current instances
     * @param <X> inferred instance type
     * @param <T> inferred event type
     * @since 5.2
     */
    private record RetargetingObserverMethod<X, T>(String containerId, ObserverMethod<T> target, InstanceBean<?> instanceBean,
            Supplier<List<X>> instanceSupplier) implements ObserverMethod<T>
    {
        /**
         * Overridden to notify all instances of the pseudo bean.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public void notify(T event)
        {
            final Class<?> eventType = event.getClass();

            // synchronize so if multiple threads fire the same event they don't step on each other.
            synchronized (instanceBean)
            {
                instanceSupplier.get().forEach(instance -> {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Fire {} to {} with priority {} in {}", eventType.getSimpleName(), target, getPriority(), containerId);
                    }

                    try
                    {
                        instanceBean.retarget(instance); // Retarget the instance bean to this instance
                        target.notify(event);
                    } catch (Exception e)
                    {
                        logger.error("{} Failed to send event [{}] of type [{}] to observer {}", containerId, event, eventType, target, e);
                    }
                });
            }
        }

        @Override
        public void notify(EventContext<T> eventContext)
        {
            notify(eventContext.getEvent());
        }

        @Override
        public Class<?> getBeanClass()
        {
            return target.getBeanClass();
        }

        @Override
        public Bean<?> getDeclaringBean()
        {
            return target.getDeclaringBean();
        }

        @Override
        public Type getObservedType()
        {
            return target.getObservedType();
        }

        @Override
        public Set<Annotation> getObservedQualifiers()
        {
            return target.getObservedQualifiers();
        }

        @Override
        public Reception getReception()
        {
            return target.getReception();
        }

        @Override
        public TransactionPhase getTransactionPhase()
        {
            return target.getTransactionPhase();
        }

        @Override
        public int getPriority()
        {
            return target.getPriority();
        }

        @Override
        public boolean isAsync()
        {
            return target.isAsync();
        }

        @Override
        public String toString()
        {
            return "RetargetingObserverMethod{" +
                    "containerId='" + containerId + '\'' +
                    ", target=" + target +
                    '}';
        }
    }
}
