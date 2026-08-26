package gov.takx.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark interfaces in Platform that are available for injection into plugins.
 *
 * @since 3.4
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectableService
{
    /**
     * Many injectable services that have producer methods are declared in the Client module, meaning that the
     * direct class is not available to plugins.  That is ok if all they offer are producer methods, and no service
     * methods for use by plugins.  When they do have service methods, they are defined by an interface that IS
     * visible to plugins (typically in Platform).  This parameter indicates the class/interface under which the service
     * bean itself should be registered within plugin CDI containers.
     * <p/>
     * The default of {@link Void} indicates that the bean should be registered by its direct type.
     *
     * @return The class/interface under which this service should be registered
     */
    Class<?> registerAs() default Void.class;
}
