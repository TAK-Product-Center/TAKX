package gov.takx.api.plugin.annotations;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.shapes.IShapeService;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

/**
 * Qualifying annotation for injecting services that are tied to a specific plugin.
 * <p>
 * Currently, this is only supported for {@link IShapeService} and {@link IVirtualFileSystem},
 * but other services will likely be added in the future.
 * <p>
 * Use like this:
 * <pre>
 *    &#64;Inject
 *    &#64;ForPlugin(family = "AppFamily", type = "AppType", category = PluginCategory.AppPlugin)
 *    private IShapeService shapeService;
 * </pre>
 *
 * @see IShapeService
 * @see IVirtualFileSystem
 * @since 2.38
 */
@Qualifier
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface ForPlugin
{
    /**
     * @return The family of the plugin.
     */
    @Nonbinding
    String family();

    /**
     * @return The type of the plugin.
     */
    @Nonbinding
    String type();

    /**
     * @return The plugin category for which to get the shape service. Only app, device, and import/export plugins are
     * supported.
     */
    @Nonbinding
    PluginCategory category();
}
