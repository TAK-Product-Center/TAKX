package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.StringBeanIdentifier;
import org.jboss.weld.bean.attributes.BeanAttributesFactory;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.serialization.spi.BeanIdentifier;

import java.lang.ref.WeakReference;

import static org.jboss.weld.bean.BeanIdentifiers.forManagedBean;

/**
 * A CDI Bean class that represents a pre-existing object instance.  This is needed when we are handling
 * {@link PseudoBean} types that have observer methods and when we are installing
 * core services into plugin containers and those instances include producer methods/fields.
 * <p>
 * In support of {@link PseudoBean} types, we need to support being
 * {@link #retarget(Object) re-targeted} during event distribution.  A single {@code InstanceBean} will be created to
 * handle ALL instances of a given type.  When an event is being distributed, it is necessary to change our target
 * instance to make the Weld core deliver the event to the proper instance.
 *
 * @since 2.6
 */
@Internal
public final class InstanceBean<T> extends AbstractClassBean<T>
{
    // Use a WeakReference to ensure that our bean definition doesn't prevent GC on our instance
    private WeakReference<T> instance;

    /**
     * Creates an injectable bean.
     *
     * @param instance    Object instance to wrap in a bean
     * @param type        The type of the instance class
     * @param beanManager The BeanManager for resolving type information
     * @param <T>         The inferred bean type
     * @return PseudoBean Bean
     * @since 3.0
     */
    public static <T> InstanceBean<T> of(T instance, Class<T> type, BeanManager beanManager)
    {
        final BeanManagerImpl beanManagerImpl = BeanManagerProxy.unwrap(beanManager);
        final EnhancedAnnotatedType<T> annotatedType = beanManagerImpl.createEnhancedAnnotatedType(type);
        final BeanAttributes<T> attributes = BeanAttributesFactory.forBean(annotatedType, beanManagerImpl);

        // Create a custom bean id including the identity hash code of the object.
        final String instanceId = ";inst=" + System.identityHashCode(instance);
        final BeanIdentifier identifier = new StringBeanIdentifier(forManagedBean(annotatedType) + instanceId);

        return new InstanceBean<>(attributes, annotatedType, identifier, beanManagerImpl, instance);
    }

    /**
     * Constructor
     */
    private InstanceBean(BeanAttributes<T> attributes, EnhancedAnnotatedType<T> type, BeanIdentifier identifier, BeanManagerImpl beanManager, T instance)
    {
        super(attributes, type, identifier, beanManager);

        this.instance = new WeakReference<>(instance);
    }

    /**
     * Change our target instance.
     *
     * @param instance Our new instance
     */
    @SuppressWarnings("unchecked")
    void retarget(Object instance)
    {
        if (this.instance.get() != instance)
        {
            this.instance = new WeakReference<>((T) instance);
        }
    }

    @Override
    public T create(CreationalContext<T> creationalContext)
    {
        return instance.get();
    }

    @Override
    public boolean isProxyable()
    {
        return false;
    }

    @Override
    public boolean isPassivationCapableBean()
    {
        return false;
    }

    @Override
    public boolean isPassivationCapableDependency()
    {
        return false;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext)
    {
        // do nothing
    }

    @Override
    protected boolean isInterceptionCandidate()
    {
        return false;
    }

    @Override
    protected void checkType()
    {
        // Do nothing
    }
}
