package gov.takx.api.annotation;

import gov.takx.api.plugin.PluginCategory;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @since 2.0.0
 */
@Target({PARAMETER, FIELD, METHOD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface PluginRegistryType
{
    @Nonbinding
    PluginCategory[] categories() default {};
}
