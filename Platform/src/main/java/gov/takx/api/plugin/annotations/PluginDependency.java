package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation specifying that a given plugin has a dependency on another plugin deployment.
 * Such a dependency is specified by the name of the plugin's deployment file, without the "jar" suffix.
 * So, a dependency on code deployed in {@code plugin.foobar-1.0.0.jar} would be specified as
 * {@code &#64;PluginDependency("plugin.foobar-1.0.0")}.
 * <p>
 * This annotation may be repeated if multiple plugin dependencies need to be declared.
 *
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PluginDependencies.class)
public @interface PluginDependency
{
    /**
     * @return The name of the plugin deployment, such as {@code "plugin.foobar-1.0.0"}
     */
    String value();

    /**
     * @return {@code true} if this is an optional dependency
     */
    boolean optional() default false;

    /**
     * @return {@code true} if the dependencies resources should be exported from the dependent module
     */
    boolean export() default false;
}
