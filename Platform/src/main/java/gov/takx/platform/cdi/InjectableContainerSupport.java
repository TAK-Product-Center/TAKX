package gov.takx.platform.cdi;

import gov.takx.api.annotation.PseudoBean;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;

import java.lang.annotation.Annotation;

/**
 * This class can be used as a base class or as a delegate to access a container and its beans.
 *
 * @since 2.0.0
 */
@PseudoBean
public class InjectableContainerSupport
{
    @Inject
    protected WeldContainer container;

    /**
     * @return The container (may be {@code null} if running under test without a configured CDI container)
     */
    public WeldContainer getContainer()
    {
        return container;
    }

    /**
     * Get a bean from the container.
     *
     * @param beanClass  Class of bean to obtain
     * @param qualifiers the additional required qualifiers, if any
     * @return bean instance or {@code null} if running under test without a configured CDI container
     */
    public <T> T getBean(Class<T> beanClass, Annotation... qualifiers)
    {
        return container == null ? null : container.select(beanClass, qualifiers).get();
    }

    /**
     * Get a bean from the container.  This overload is needed so that the method is properly located when called from
     * within a BeanShell script and no qualifiers are specified.  It (BeanShell) isn't resolving the method above, with
     * the varargs qualifiers.
     *
     * @param beanClass  Class of bean to obtain
     * @param <T> Inferred type of bean
     * @return bean instance or {@code null} if running under test without a configured CDI container
     * @since 3.2
     */
    public <T> T getBean(Class<T> beanClass)
    {
        return container == null ? null : container.select(beanClass).get();
    }
}
