package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a test method during who's execution logging should be suppressed for a given set
 * of classes.  The intent of this annotation is to reduce extraneous output (typically warning/error messages)
 * triggered by test operations.  But, if the logging configuration has been set &lt;= WARN on a listed class, it will
 * not be set to OFF.  This allows a developer to easily re-enable logging for debug purposes by adjusting
 * the log4j configuration.
 * <p>
 * This annotation is intended strictly for use within our test classes, and any test classes within consuming projects.
 * <p>
 * It should never be used within production code.
 *
 * @since 2.35
 */
@Internal
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SuppressLogging
{
    Class[] value();
}
