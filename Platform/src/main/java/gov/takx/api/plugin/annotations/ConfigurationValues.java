package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow repeated use of {@link ConfigurationValue}.
 *
 * @since 2.20
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationValues
{
    /**
     * @return Array of {@link ConfigurationValue} entries
     */
    ConfigurationValue[] value();
}
