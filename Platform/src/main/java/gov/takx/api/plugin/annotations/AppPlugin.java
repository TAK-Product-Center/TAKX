package gov.takx.api.plugin.annotations;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.plugin.app.IAppPluginManager.AutoStartMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation to mark an App plugin class. Placing this annotation on a class declaration defines
 * it as an App plugin, and it will be initialized as the TAKX core is started.
 * <p>
 * This annotation also declares the class as an {@link ApplicationScoped} bean within the plugin's CDI container.
 *
 * @since 2.0.0
 */
@Stereotype
@ApplicationScoped
@Named
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AppPlugin
{
    /**
     * @return Base plugin specification (required)
     */
    PluginSpecification spec();

    /**
     * @return Plugin descriptor
     */
    PluginDescriptor descriptor();

    /**
     * Determines whether this plugin can be run when TAKX is running 'headless'. If {@code false} and
     * TAKX is running headless, then the app plugin will not be registered.
     *
     * @return {@code true} if this plugin can run headless, The default value is {@code true} for backward compatibility.
     * @since 2.25
     * @deprecated No longer use as TAKX cannot run headless
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    boolean canRunHeadless() default true;

    /**
     * Specifies how this App should be treated at startup.
     *
     * @return The auto-startup mode to use
     * @since 5.7
     */
    AutoStartMode autoStartMode() default AutoStartMode.none;
}
