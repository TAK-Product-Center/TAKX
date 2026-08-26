package gov.takx.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * APIs marked with the @Experimental annotation at the class or method level will almost certainly change.
 * They can be modified in any way, or even removed, at any time. You should not use or rely on them in any
 * production code. They are purely to allow broad testing and feedback.
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
public @interface Experimental
{
}
