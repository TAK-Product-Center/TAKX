package gov.takx.core.registry;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.registry.IPluginRegistry;
import gov.takx.api.registry.NoSuchPluginException;
import gov.takx.api.registry.PluginState;
import gov.takx.core.ioservice.ImportExportService;
import gov.takx.platform.io.ImportExportMode;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides read-only insight into all the various TAKX plugin registries.
 *
 * @since 1.4
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PluginInspector implements IPluginInspector
{
    // The list of registries we are configured to "inspect"
    private final List<IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>>> registries = new ArrayList<>();

    @Inject
    private ICoreAppManager appManager;

    @Inject
    private ImportExportService importExportService;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    /**
     * Construct an inspector on the supplied list of registries.
     *
     * @param registries List of registries to handle (usually 1 or all)
     */
    public PluginInspector(List<IPluginRegistry<? extends IRaptorPlugin, ? extends IPluginDefinition<? extends IRaptorPlugin>>> registries)
    {
        this.registries.addAll(registries);
    }

    @Override
    public Collection<IRaptorPlugin> listInstalledPlugins()
    {
        return installedPluginsAsStream().collect(Collectors.toList());
    }

    @Override
    public Collection<String> listInstalledFamilies()
    {
        return pluginDefinitionsAsStream().map(IPluginDefinition::getFamily).collect(Collectors.toSet());
    }

    @Override
    public boolean isPluginInstalled(String family, String type)
    {
        return pluginDefinitionsAsStream().anyMatch(pd -> pd.getFamily().equals(family) && pd.getType().equals(type));
    }

    @Override
    public boolean isPluginInstalled(String family, String type, String version)
    {
        return pluginDefinitionsAsStream().anyMatch(pd -> pd.getFamily().equals(family) && pd.getType().equals(type) && pd.getVersion().equals(version));
    }

    @Override
    public boolean isFamilyInstalled(String family)
    {
        return pluginDefinitionsAsStream().anyMatch(pd -> pd.getFamily().equals(family));
    }

    @Override
    public PluginState getPluginState(String family, String type) throws NoSuchPluginException
    {
        // Search all apps. If an app is found, set the state to either running or stopped.
        Optional<PluginState> state = appManager.getApps().stream()
                .filter(app -> app.getApp().getFamily().equals(family) && app.getApp().getType().equals(type))
                .map(app -> app.isRunning() ? PluginState.running : PluginState.stopped)
                .findFirst();

        // If no app is found, search the plugins. If a plugin is found, the state is installed.
        if (state.isEmpty()) state = getPluginDefinition(family, type).map(p -> PluginState.installed);

        // If the state has remained unset, it is because the plugin was not found in either
        // the set of apps or plugins.
        return state.orElseThrow(() -> new NoSuchPluginException("No plugin found for family=" + family + ", type=" + type));
    }

    @Override
    public IImportExportWizard getPlugin(String family, String type, ImportExportMode mode)
    {
        IImportExportWizard wizard = null;

        final IImportExportPlugin plugin = pluginRegistryManager.getImportExportPluginRegistry().getPlugin(family, type);

        if (plugin != null)
        {
            wizard = mode.getWizardCreator().apply(plugin);
            if (wizard != null)
            {
                importExportService.injectWizardFormatsAndDelegate(plugin, wizard);
            }
        }

        if (wizard == null)
        {
            throw new NoSuchPluginException("Unable to create an IImportExportWizard of family " + family + ", type " + type + ", in mode " + mode);
        }

        return wizard;
    }

    @Override
    public IRaptorPlugin getPlugin(String family, String type) throws NoSuchPluginException
    {
        return getPluginDefinition(family, type)
                .map(IPluginDefinition::getInstance)
                .orElseThrow(() -> new NoSuchPluginException("No plugin found for family=" + family + ", type=" + type));
    }

    @Override
    public Optional<? extends IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinition(String family, String type)
    {
        final Optional<IPluginDefinition<? extends IRaptorPlugin>> definitionOptional = pluginDefinitionsAsStream()
                .filter(pd -> pd.getFamily().equals(family) && pd.getType().equals(type))
                .findFirst();

        // If we have one locally, return that.
        if (definitionOptional.isPresent()) return definitionOptional;

        return mapObjectManager.getPluginDefinition(family, type); // Otherwise, ask the MOM
    }

    @Override
    public Set<IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinitions()
    {
        return pluginDefinitionsAsStream().collect(Collectors.toSet());
    }

    @Override
    public boolean isExportControlled()
    {
        return pluginDefinitionsAsStream().anyMatch(pd -> isExportControlled());
    }

    /**
     * @return All the installed plugins (from all registries) as a flat stream
     */
    private Stream<? extends IRaptorPlugin> installedPluginsAsStream()
    {
        pluginRegistryManager.waitForPluginRegistriesToLoad();
        return registries.stream().flatMap(r -> r.getPlugins().stream()).filter(Objects::nonNull);
    }

    /**
     * @return Plugin definitions for all the plugins (from all registries) as a flat stream
     */
    private Stream<IPluginDefinition<? extends IRaptorPlugin>> pluginDefinitionsAsStream()
    {
        pluginRegistryManager.waitForPluginRegistriesToLoad();
        return registries.stream().flatMap(r -> r.getPluginDefinitions().stream());
    }
}
