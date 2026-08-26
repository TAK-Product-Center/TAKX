package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a single configuration value (name and value) for a preconfigured connection.
 *
 * @see DefinePreconfiguredConnection
 * @since 2.20
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConfigurationValues.class)
public @interface ConfigurationValue
{
    /**
     * The name of the configuration parameter to set.  Consult the connection plugin documentation for its
     * supported configuration parameters.
     * <p>
     * This is a required property.
     *
     * @return configuration element name
     */
    String name();

    /**
     * The value of the configuration parameter to set.
     * <p>
     * This is a required property.
     *
     * @return configuration element value
     */
    String value();
}
