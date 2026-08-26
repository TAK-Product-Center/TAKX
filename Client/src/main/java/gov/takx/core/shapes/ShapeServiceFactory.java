package gov.takx.core.shapes;

import gov.takx.api.annotation.ExcludedProducerMethod;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.annotations.ForPlugin;
import gov.takx.api.plugin.app.IAppPluginDelegate;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.api.shapes.IShapeService;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for managing instances of {@link ShapeService}.
 *
 * @since 2.38
 */
@InjectableService
@ApplicationScoped
public class ShapeServiceFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static Map<PluginCategory, String> categoryPrefixMap = new EnumMap<>(PluginCategory.class);

    static
    {
        categoryPrefixMap.put(PluginCategory.AppPlugin, "app_");
        categoryPrefixMap.put(PluginCategory.ImportExportPlugin, "io_");
    }

    /**
     * Map of shape service instances, keyed by the shape prefix.
     */
    private final Map<String, ShapeService> shapeServiceMap = new ConcurrentHashMap<>();

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        // Some delegates don't clean up their shape services (features and import/export), so just clean up any remaining here
        shapeServiceMap.forEach((k, ss) -> ss.dispose());
        shapeServiceMap.clear();
    }

    /**
     * Produce the shape service for the app plugin specified in the annotation.
     * <p>
     * Note: the annotation values are non-binding, so the values selected in the {@link ForPlugin} annotation on this
     * producer method are just placeholder values and are not meant to imply that this method is only supported for apps.
     *
     * @param injectionPoint Injection point so we can obtain the annotation values
     * @return The shape service associated with the specified app plugin.
     */
    @Produces
    @ForPlugin(family = "", type = "", category = PluginCategory.AppPlugin)
    private IShapeService produceShapeService(InjectionPoint injectionPoint)
    {
        ForPlugin forPluginAnnotation = InjectionUtils.getAnnotation(injectionPoint, ForPlugin.class)
                .orElseThrow(() -> new IllegalStateException("No @ForPlugin on: " + injectionPoint));

        PluginCategory pluginCategory = forPluginAnnotation.category();

        if (!isInjectionSupportedForCategory(pluginCategory))
        {
            logger.error("Unsupported category {} used for injection point: {}", pluginCategory, injectionPoint);
            return null;
        }

        String family = forPluginAnnotation.family();
        String type = forPluginAnnotation.type();

        if (!isPluginDefinitionPresent(family, type, pluginCategory))
        {
            logger.error("Failed to locate plugin with family '{}', type '{}', and category {} at injection point: {}",
                    family, type, pluginCategory, injectionPoint);
            return null;
        }

        return getOrCreateShapeService(null, pluginCategory, KeyUtils.getUniqueKey(family, type));
    }

    /**
     * Determines if injection is supported for the provided category.
     *
     * @param pluginCategory The plugin category to check
     * @return True if injection is supported.
     */
    private boolean isInjectionSupportedForCategory(PluginCategory pluginCategory)
    {
        return !getCategoryPrefix(pluginCategory).isEmpty();
    }

    /**
     * Looks up the plugin in the appropriate plugin registry to ensure it exists.
     *
     * @param family         The plugin family
     * @param type           The plugin type
     * @param pluginCategory The plugin category
     * @return True if a plugin definition was found for the provided family, type, and category.
     */
    private boolean isPluginDefinitionPresent(String family, String type, PluginCategory pluginCategory)
    {
        // We can't just use IPluginInspector here because it doesn't pay attention to plugin category, and it's quite
        // possible a plugin has two plugins of different categories with the same family and type.
        return pluginRegistryManager.getRegistry(pluginCategory).getPluginDefinition(family, type).isPresent();
    }

    /**
     * Gets the prefix to use for the provided category.
     *
     * @param pluginCategory The plugin category for which to get the prefix (may be {@code null} if prefix is for core)
     * @return The prefix or an empty string if there is no prefix for the provided category.
     */
    private String getCategoryPrefix(PluginCategory pluginCategory)
    {
        return categoryPrefixMap.getOrDefault(pluginCategory, "");
    }

    /**
     * Gets or creates a shape service exclusively for use by the TAKX core.
     * <p>
     * This is a producer method, allowing injection of the core shape service.
     *
     * @return The shape service for the TAKX core.
     */
    @Produces
    @ExcludedProducerMethod
    public ShapeService getOrCreateCoreShapeService()
    {
        return getOrCreateShapeService(null, null, "");
    }

    /**
     * Gets or creates a shape service for the provided app delegate.
     *
     * @param delegate The delegate for which to get the shape service
     * @return The shape service associated with the provided delegate.
     */
    public ShapeService getOrCreateShapeService(IAppPluginDelegate delegate)
    {
        return getOrCreateShapeService(null, PluginCategory.AppPlugin, delegate.getAppId());
    }

    /**
     * Gets or creates a shape service for the provided import/export delegate.
     *
     * @param delegate The delegate for which to get the shape service
     * @return The shape service associated with the provided delegate.
     */
    public ShapeService getOrCreateShapeService(IImportExportPluginDelegate delegate)
    {
        return getOrCreateShapeService(null, PluginCategory.ImportExportPlugin, delegate.getUniqueKey());
    }

    /**
     * Gets or creates a shape service for the provided map object controller.
     *
     * @param mapObjectController The map object controller for which to get the shape service
     * @return The shape service associated with the provided map object controller.
     */
    public ShapeService getOrCreateShapeService(IMapObjectController mapObjectController)
    {
        IMapObject mapObject = mapObjectController.getMapObject();
        return getOrCreateShapeService(mapObject, PluginCategory.DevicePlugin, mapObject.getKey());
    }

    /**
     * Gets or creates a shape service with the provided owner, plugin category, and unique key.
     *
     * @param owner          The owning map object (may be {@code null})
     * @param pluginCategory The category of the plugin
     * @param uniqueKey      The unique key of the plugin
     * @return The shape service associated with the provided owner and prefix.
     */
    private ShapeService getOrCreateShapeService(IMapObject owner, PluginCategory pluginCategory, String uniqueKey)
    {
        return shapeServiceMap.computeIfAbsent(createPrefix(pluginCategory, uniqueKey), k -> new ShapeService(owner, k));
    }

    /**
     * Creates a shape service prefix for the provided plugin category and unique key.
     *
     * @param pluginCategory The category of the plugin
     * @param uniqueKey      The unique key of the plugin
     * @return The prefix for the shape service.
     */
    private String createPrefix(PluginCategory pluginCategory, String uniqueKey)
    {
        String categoryPrefix = getCategoryPrefix(pluginCategory);
        String basePrefix = categoryPrefix + uniqueKey;

        // Only add underscores if the category and/or unique key is non-empty
        return basePrefix.isEmpty() ? basePrefix : ("_" + basePrefix + "_");
    }

    /**
     * Disposes the shape service and removes it from the map.
     *
     * @param shapeService The shape service to dispose
     */
    public void disposeShapeService(ShapeService shapeService)
    {
        ShapeService removedService = shapeServiceMap.remove(shapeService.getShapePrefix());

        if (removedService != null)
        {
            removedService.dispose();
        }
    }
}
