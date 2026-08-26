package gov.takx.api.plugin.annotations;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Named;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation to mark an Import/Export plugin class. Placing this annotation on a class declaration defines
 * it as an Import/Export plugin, and it will be initialized as the TAKX core is started.
 * <p>
 * This annotation also declares the class as an {@link ApplicationScoped} bean within the plugin's CDI container.
 *
 * @since 2.13
 */
@SuppressWarnings("unused")
@Stereotype
@ApplicationScoped
@Named
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ImportExportPlugin
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
     * @return import / export formats.
     */
    ImportExportFormat[] formats();
}
