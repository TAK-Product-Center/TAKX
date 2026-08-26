package gov.takx.core.registry;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.PluginRegistryType;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.registry.IPluginRegistry;
import gov.takx.platform.utils.InjectionUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This service is responsible for manufacturing instances of {@link IPluginInspector}, possibly
 * scoped to a specific plugin category.
 *
 * @since 2.0.0
 */
@InjectableService
@ApplicationScoped
public class PluginInspectorManager
{
    @Inject
    private PluginRegistryManager pluginRegistryManager;

    /**
     * Getter and producer for a PluginInspector on all registries.  This method will block until all the registries
     * have been loaded (which happens in the background).
     *
     * @return A plugin inspector covering all plugin categories
     */
    @Produces
    @Dependent
    public IPluginInspector getPluginInspector()
    {
        pluginRegistryManager.waitForPluginRegistriesToLoad();
        return new PluginInspector(pluginRegistryManager.allRegistries());
    }

    /**
     * Create a plugin inspector of the specified set of plugin categories.
     *
     * @param categories Categories of interest
     * @return plugin inspector covering the specified categories
     */
    public IPluginInspector getPluginInspector(PluginCategory... categories)
    {
        final List<IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>>> registries =
                Arrays.stream(categories)
                        .map(pluginRegistryManager::getRegistry)
                        .collect(Collectors.toList());

        return new PluginInspector(registries);
    }

    /**
     * Producer method for generating PluginInspector instances.
     *
     * @param injectionPoint The annotated injection point so we can extract the category list
     * @return dependent instance of a plugin inspector configured on the specified categories
     */
    @Produces
    @Dependent
    @PluginRegistryType
    private IPluginInspector produceInspector(InjectionPoint injectionPoint)
    {
        final PluginRegistryType prt = InjectionUtils.getAnnotation(injectionPoint, PluginRegistryType.class)
                .orElseThrow(() -> new IllegalStateException("No @PluginRegistryType on: " + injectionPoint));

        return getPluginInspector(prt.categories());
    }
}
