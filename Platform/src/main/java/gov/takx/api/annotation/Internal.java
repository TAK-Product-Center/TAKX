package gov.takx.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * APIs marked with the @Internal annotation at the class or method level are not meant for consumption
 * by plugins.  At the class level, if it is on an interface, it means that a plugin should <strong>NEVER</strong>
 * attempt to implement it.
 * <p>
 * The classes or interfaces in question are generally in a jar that is visible to a plugin strictly due to
 * build hierarchy requirements, and the class/interface/method in question is not to be used/implemented by
 * a plugin.
 * <p>
 * On a method, it means that the method is public due to compilation requirements for the core, but it is not
 * intended to be invoked by a plugin.
 *
 * @since 2.0.0
 */
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.PACKAGE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Internal
{
}
