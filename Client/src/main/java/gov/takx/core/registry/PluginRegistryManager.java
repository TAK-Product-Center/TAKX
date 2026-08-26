package gov.takx.core.registry;

import gov.takx.api.event.ITARTaggedContentDiscovered;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.registry.IAppPluginRegistry;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.registry.IImportExportPluginRegistry;
import gov.takx.api.registry.IPluginRegistry;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.module.IPluginModuleManager;
import gov.takx.module.PluginDefinitionFactory;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is responsible for processing the scanned plugins directory for jar files and loading the plugins configured in
 * those jar files.
 *
 */
@ApplicationScoped
public class PluginRegistryManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IPluginModuleManager pluginModuleManager;
    private final PluginDefinitionFactory pluginDefinitionFactory;
    private final IAppPluginRegistry appPluginRegistry;
    private final IDevicePluginRegistry devicePluginRegistry;
    private final IConnectionPluginRegistry connectionPluginRegistry;
    private final IImportExportPluginRegistry importExportPluginRegistry;
    private final ISyntheticPluginRegistry syntheticPluginRegistry;
    private final Event<ITARTaggedContentDiscovered> itarTaggedContentDiscoveredEvent;

    private final Map<PluginCategory, IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>>> registryMap = new EnumMap<>(PluginCategory.class);

    private Thread backgroundPluginLoader;

    /**
     * CDI constructor.
     */
    @Inject
    PluginRegistryManager(IPluginModuleManager pluginModuleManager, PluginDefinitionFactory pluginDefinitionFactory,
                          IAppPluginRegistry appPluginRegistry, IDevicePluginRegistry devicePluginRegistry,
                          IConnectionPluginRegistry connectionPluginRegistry, IImportExportPluginRegistry importExportPluginRegistry,
                          ISyntheticPluginRegistry syntheticPluginRegistry,
                          Event<ITARTaggedContentDiscovered> itarTaggedContentDiscoveredEvent)
    {
        this.pluginModuleManager = pluginModuleManager;
        this.pluginDefinitionFactory = pluginDefinitionFactory;
        this.appPluginRegistry = appPluginRegistry;
        this.devicePluginRegistry = devicePluginRegistry;
        this.connectionPluginRegistry = connectionPluginRegistry;
        this.importExportPluginRegistry = importExportPluginRegistry;
        this.syntheticPluginRegistry = syntheticPluginRegistry;
        this.itarTaggedContentDiscoveredEvent = itarTaggedContentDiscoveredEvent;

        // Fill out the registry map
        allRegistries().forEach(r -> registryMap.put(r.getPluginCategory(), r));
    }

    /**
     * At startup, have each registry validate their plugins.
     * @since 3.4
     */
    private void validatePlugins(@Observes @Priority(ServiceOrdering.START_PluginRegistryManager) StartProjectServices event)
    {
        allRegistries().forEach(IPluginRegistry::validatePlugins);
    }

    /**
     * Load all the plugins into the registries.
     */
    public void loadRegistries()
    {
        pluginModuleManager.loadAllPlugins();

        syntheticPluginRegistry.generateSyntheticPlugins();
    }

    /**
     * Kick off the plugin registry loading in the background.  If you use this method, you should call
     * {@link #waitForPluginRegistriesToLoad()} before you perform any code that needs to have the plugin registries
     * fully loaded.
     */
    public void loadRegistriesInBackground()
    {
        if (backgroundPluginLoader != null)
        {
            throw new IllegalStateException("Background plugin loading has already been initiated");
        }

        backgroundPluginLoader = new Thread("PluginRegistryManager")
        {
            @Override
            public void run()
            {
                loadRegistries();
            }
        };

        backgroundPluginLoader.start();
    }

    /**
     * Wait for the background loading of the plugin registries to complete.  If loading was done in the foreground,
     * then this method will not block.  If {@link #loadRegistriesInBackground()} was used, then this method will
     * block until the thread performing the load completes.
     */
    public void waitForPluginRegistriesToLoad()
    {
//        logger.error("wait", new RuntimeException());
        if (backgroundPluginLoader != null)
        {
            try
            {
                backgroundPluginLoader.join();
            } catch (InterruptedException e)
            {
                logger.error("Failed to wait for plugin registry loading to complete", e);
            }
        }
    }

    /**
     * Get the registry that manages plugins of the given category.
     *
     * @param category Plugin category of interest
     * @return Registry that manages that plugin category
     */
    public IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>> getRegistry(PluginCategory category)
    {
        return registryMap.get(category);
    }

    /**
     * Register the given plugin class.  Manufacture a plugin definition from the plugin class and register it.
     * <p>
     * This method is for use by tests only. If a plugin with the same family/type and a newer version is already
     * registered, then this registration request will be denied, and {@code false} will be returned.
     *
     * @param pluginClass The class of the plugin to register
     * @since 2.19
     */
    public void registerPlugin(Class<? extends IRaptorPlugin> pluginClass)
    {
        final var pluginDefinition = pluginDefinitionFactory.definitionFor(pluginClass);
        pluginModuleManager.createTestDeployment(pluginDefinition); // Create a stub deployment
        registerPlugin(pluginDefinition);
    }

    /**
     * Register the given plugin definition.  If a plugin with the same family/type and a newer version is already
     * registered, then this registration request will be denied, and {@code false} will be returned.
     *
     * @param pluginDefinition Definition of plugin to register
     * @return {@code true} if the plugin was added successfully, {@code false} otherwise
     */
    public boolean registerPlugin(IPluginDefinition<?> pluginDefinition)
    {
        if (!getRegistry(pluginDefinition.getPluginCategory()).addPlugin(pluginDefinition))
        {
            return false;
        }

        // Check to see if this plugin is marked as ITAR controlled, and if so, fire a notification event
        if (pluginDefinition.isExportControlled())
        {
            itarTaggedContentDiscoveredEvent.fire(new ITARTaggedContentDiscovered(pluginDefinition));
        }

        return true;
    }

    public IAppPluginRegistry getAppPluginRegistry()
    {
        waitForPluginRegistriesToLoad();
        return appPluginRegistry;
    }

    public IConnectionPluginRegistry getConnectionPluginRegistry()
    {
        waitForPluginRegistriesToLoad();
        return connectionPluginRegistry;
    }

    public IDevicePluginRegistry getDevicePluginRegistry()
    {
        waitForPluginRegistriesToLoad();
        return devicePluginRegistry;
    }

    public IImportExportPluginRegistry getImportExportPluginRegistry()
    {
        waitForPluginRegistriesToLoad();
        return importExportPluginRegistry;
    }

    /**
     * @return The synthetic plugin registry, blocking until plugin registries are loaded.
     * @since 3.0
     */
    public ISyntheticPluginRegistry getSyntheticPluginRegistry()
    {
        waitForPluginRegistriesToLoad();
        return syntheticPluginRegistry;
    }

    /**
     * @return A stream containing all the plugin registries
     */
    public List<IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>>> allRegistries()
    {
        waitForPluginRegistriesToLoad();
        return Stream.of(appPluginRegistry, connectionPluginRegistry, devicePluginRegistry, importExportPluginRegistry, syntheticPluginRegistry)
                .collect(Collectors.toList());
    }
}
