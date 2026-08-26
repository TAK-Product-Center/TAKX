package gov.takx.api.annotation;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used along with @Inject to have the application instance number injected into an int variable.
 *
 * @since 2.0.0
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface InstanceNumber
{
}
