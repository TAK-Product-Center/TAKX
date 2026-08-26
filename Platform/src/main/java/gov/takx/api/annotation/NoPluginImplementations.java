package gov.takx.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interfaces marked with the @NoPluginImplementations annotation should <strong>NEVER</strong>
 * be implemented by a class in a plugin.
 * <p>
 * The interfaces in question are generally in a jar that is visible to a plugin strictly due to
 * build hierarchy requirements, and the interface in question is not to be implemented by a plugin.
 *
 * @since 2.0.0
 */
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoPluginImplementations
{
}
