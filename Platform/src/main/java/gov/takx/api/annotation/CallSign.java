package gov.takx.api.annotation;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used along with @Inject to have the local TAKX's Call Sign injected into a {@link String} variable.
 *
 * @since 3.0
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface CallSign
{
}
