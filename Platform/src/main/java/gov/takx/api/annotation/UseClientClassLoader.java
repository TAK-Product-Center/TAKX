package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods in the client code base that can be called indirectly from a plugin
 * that needs to re-instate the client module's class loader.  This is typically needed for operations that will
 * use hibernate where it needs to reify a class defined in the client module (like MapPoint), or will need
 * to access resources contained in the Client jar (like AudioManager).
 *
 * @since 1.41
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseClientClassLoader
{
}
