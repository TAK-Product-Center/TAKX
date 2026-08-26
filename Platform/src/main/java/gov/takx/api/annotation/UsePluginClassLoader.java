package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods in a plugin's code base that can be called indirectly from things like
 * the EDT (an ActionListener for example) that need to re-instate the plugin's class loader as the Thread Context Class Loader.
 * <p>
 * This is typically needed for UI call backs that need to perform dynamic class instantiation.
 *
 * @since 2.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsePluginClassLoader
{
}
