package gov.takx.platform.utils;

import gov.takx.api.annotation.PseudoBean;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.bean.RIBean;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.config.WeldConfiguration;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.event.ObserverFactory;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.metadata.cache.MetaAnnotationStore;
import org.jboss.weld.proxy.WeldClientProxy;
import org.jboss.weld.proxy.WeldConstruct;
import org.jboss.weld.resolution.Resolvable;
import org.jboss.weld.resolution.TypeSafeObserverResolver;
import org.jboss.weld.util.BeanMethods;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Utility methods to support various CDI-related operations, mostly in support of injection processing.
 *
 * @since 2.0.0
 */
public class InjectionUtils
{
    /**
     * Retrieve the given annotation from the injection point, if it exists.
     *
     * @param injectionPoint current injection point
     * @param targetType     target type
     * @param <T>            annotation type
     * @return Optional annotation instance extracted from the injection point which matches the given type; {@link Optional#empty()} if not found
     * @since 3.4 (first parameter changed to Jakarta InjectionPoint)
     */
    public static <T extends Annotation> Optional<T> getAnnotation(InjectionPoint injectionPoint, Class<T> targetType)
    {
        final Annotated annotated = injectionPoint.getAnnotated();

        T result = annotated.getAnnotation(targetType);

        if (result == null)
        {
            for (Annotation annotation : annotated.getAnnotations())
            {
                result = annotation.annotationType().getAnnotation(targetType);

                if (result != null)
                {
                    return Optional.of(result);
                }
            }
        }

        return Optional.ofNullable(result);
    }

    /**
     * Retrieve the given annotation from the bean that is the target of the injection point.
     *
     * @param injectionPoint current injection point
     * @param annotationType desired annotation type
     * @param <T>            annotation type
     * @return Optional annotation instance extracted from the bean containing the injection point which matches the given type; {@link Optional#empty()} if not found
     * @since 3.4 (first parameter changed to Jakarta InjectionPoint)
     */
    public static <T extends Annotation> Optional<T> getAnnotationOnBean(InjectionPoint injectionPoint, Class<T> annotationType)
    {
        return Optional.ofNullable(injectionPoint.getBean().getBeanClass().getAnnotation(annotationType));
    }

    /**
     * Return an instantiated and fully initialized object of the given type within the given container.  The given
     * type <em>must</em> resolve to exactly one bean, or a {@link DeploymentException} will be thrown.
     *
     * @param container CDI container within which to construct the instance manager
     * @param type      The type of object to instantiate
     * @return Instance manager
     * @throws DeploymentException If the given type does not resolve to exactly one bean
     */
    public static <T> T getFullyInitializedInstance(WeldContainer container, Class<T> type)
    {
        final T instance = container.select(type).get();

        //noinspection unchecked
        return (instance instanceof WeldClientProxy proxy)
                ? (T) proxy.getMetadata().getContextualInstance()
                : instance;
    }

    /**
     * Get the requested annotation from the given class.  Locate the "original" type for the given type,
     * see {@link #getOriginalType(Class)}, and retrieve the annotation from that class.
     *
     * @param type            The type to inspect
     * @param annotationClass The annotation class to retrieve
     * @return Annotation from the "original" type
     * @see Class#getAnnotation(Class)
     * @see #getOriginalType(Class)
     * @since 2.38
     */
    public static <A extends Annotation> A getAnnotationFromOriginalType(Class<?> type, Class<A> annotationClass)
    {
        final A annotation = type.getAnnotation(annotationClass);

        return annotation == null
                ? getOriginalType(type).getAnnotation(annotationClass)
                : annotation;
    }

    /**
     * Get the "original" (before processing by Weld) class for the given type.
     * <p>
     * If the given type is not a Weld construct, then return it.
     * <p>
     * If the given type is a Weld client proxy, then return it since it will have all the same annotations
     * as the original.
     * <p>
     * Otherwise (for all other Weld constructs), run up the super-class hierarchy until we find the first
     * non-Weld construct class, and return that, since it will be the original type.
     *
     * @param type The type to inspect
     * @return The original type
     * @since 2.38
     */
    public static Class<?> getOriginalType(Class<?> type)
    {
        if (WeldClientProxy.class.isAssignableFrom(type)) return type;

        while (type != null && WeldConstruct.class.isAssignableFrom(type))
        {
            type = type.getSuperclass();
        }

        return type;
    }

    /**
     * Get a collection of all the observer methods (any method that uses an {@link jakarta.enterprise.event.Observes}
     * annotation) within a given bean type.
     *
     * @param beanManager        Bean manager to use to resolve type information
     * @param beanType           The type of the bean to scan
     * @param targetBeanSupplier A supplier to produce a target bean instance, needed to support
     *                           {@link PseudoBean} objects since the container does not
     *                           know about them
     * @param <T>                Inferred bean type
     * @return Collection of observer methods on the given bean type
     * @since 3.0
     */
    public static <T> Set<ObserverMethod<?>> getAllObserverMethods(BeanManager beanManager, Class<T> beanType, Supplier<Bean<T>> targetBeanSupplier)
    {
        final BeanManagerImpl beanManagerImpl = BeanManagerProxy.unwrap(beanManager);

        final Collection<EnhancedAnnotatedMethod<?, ? super T>> observerMethods =
                BeanMethods.getObserverMethods(beanManagerImpl.createEnhancedAnnotatedType(beanType));

        if (!observerMethods.isEmpty())
        {
            // Convert the observer method types into actual ObserverMethod instances on the supplied target bean
            final Bean<T> targetBean = targetBeanSupplier.get();

            return observerMethods.stream()
                    .map(m -> ObserverFactory.create(m, (RIBean<T>) targetBean, beanManagerImpl, false))
                    .collect(Collectors.toSet());
        } else
        {
            return Collections.emptySet();
        }
    }

    /**
     * Resolve a given set of observer methods against a given resolvable (event type), returning those methods
     * that match the given resolvable.
     * <p>
     * An exception will be thrown if this method is called after the CDI container has been shutdown.
     *
     * @param beanManager     Bean manager to use to resolve type information
     * @param resolvable      Resolvable type (e.g., event type plus qualifiers) to match against observer methods
     * @param observerMethods Collection of observer methods to resolve from
     * @return List of observers matching the given resolvable
     * @since 3.0
     */
    public static List<? extends ObserverMethod<?>> resolveObservers(BeanManager beanManager, Resolvable resolvable, Set<ObserverMethod<?>> observerMethods)
    {
        // Resolve the observer methods that match the event (Resolvable)
        final ServiceRegistry services = BeanManagerProxy.unwrap(beanManager).getServices();

        final TypeSafeObserverResolver resolver = new TypeSafeObserverResolver(services.get(MetaAnnotationStore.class),
                observerMethods, services.get(WeldConfiguration.class));

        //noinspection unchecked
        return (List<ObserverMethod<?>>) resolver.resolve(resolvable, false).getAllObservers();
    }

    private InjectionUtils()
    {
    }
}
