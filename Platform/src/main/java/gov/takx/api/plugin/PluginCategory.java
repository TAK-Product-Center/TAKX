package gov.takx.api.plugin;

import gov.takx.api.plugin.app.IAppPlugin;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.io.IImportExportPlugin;

import java.lang.annotation.Annotation;

/**
 * Identifies the various plugin types supported by TAKX.
 *
 * @since 1.3
 */
public enum PluginCategory
{
    AppPlugin(gov.takx.api.plugin.annotations.AppPlugin.class, IAppPlugin.class),
    ConnectionPlugin(gov.takx.api.plugin.annotations.ConnectionPlugin.class, IConnectionPlugin.class),
    DevicePlugin(gov.takx.api.plugin.annotations.DevicePlugin.class, IDevicePlugin.class),
    ImportExportPlugin(gov.takx.api.plugin.annotations.ImportExportPlugin.class, IImportExportPlugin.class),
    SyntheticPlugin(null, null);

    private static final PluginCategory[] VALUES = values();

    private final Class<? extends Annotation> annotationClass;
    private final Class<? extends IRaptorPlugin> requiredInterface;

    PluginCategory(Class<? extends Annotation> annotationClass, Class<? extends IRaptorPlugin> requiredInterface)
    {
        this.annotationClass = annotationClass;
        this.requiredInterface = requiredInterface;
    }

    /**
     * @return The annotation used to define plugins for this category
     * @since 3.0
     */
    public Class<? extends Annotation> getAnnotationClass()
    {
        return annotationClass;
    }

    /**
     * @return The interface that a plugin of this category must implement
     * @since 3.0
     */
    public Class<? extends IRaptorPlugin> getRequiredInterface()
    {
        return requiredInterface;
    }

    /**
     * Given a plugin class, determine the plugin category.
     *
     * @param pluginClass Plugin class of interest
     * @return Plugin category for plugin, {@code null} if not found
     * @since 3.0
     */
    public static PluginCategory getCategoryForPlugin(Class<? extends IRaptorPlugin> pluginClass)
    {
        for (PluginCategory category : VALUES)
        {
            if (category.annotationClass != null && pluginClass.isAnnotationPresent(category.annotationClass)) return category;
        }
        return null;
    }
}
