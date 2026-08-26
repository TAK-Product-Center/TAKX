package gov.takx.api.registry;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.io.ImportExportMode;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Identify what plugins are installed, which apps are running within the current TAKX context.
 *
 * @since 1.4
 */
@NoPluginImplementations
public interface IPluginInspector
{
    /**
     * Provides a read-only {@link IRaptorPlugin} list of all installed plugins for the given plugin category.  The
     * IRaptorPlugin instances are effectively clones of the actual plugins; they cannot be cast to the actual
     * plugin nor will any modifications to metadata cascade back to the actual plugin registry.
     *
     * @return List of installed TAKX plugins
     */
    Collection<IRaptorPlugin> listInstalledPlugins();

    /**
     * Provides a read-only list of installed plugin families.
     *
     * @return List of installed plugin family names
     */
    Collection<String> listInstalledFamilies();

    /**
     * Determine if the given plugin family has been installed.
     *
     * @param family Plugin family name
     * @return true, if any plugin is registered with the given family
     * @since 1.6
     */
    boolean isFamilyInstalled(String family);

    /**
     * Convenience method to determine if a plugin is installed.
     *
     * @param family Plugin family name
     * @param type   Plugin type name
     * @return true, if the plugin is installed
     */
    boolean isPluginInstalled(String family, String type);

    /**
     * Convenience method to determine if a specific version of a plugin is installed.
     *
     * @param family  Plugin family name
     * @param type    Plugin type name
     * @param version Plugin version
     * @return true, if the plugin is installed
     */
    boolean isPluginInstalled(String family, String type, String version);

    /**
     * Attempt to locate a plugin with the given family and type name.
     *
     * @param family Plugin family name.
     * @param type   Plugin type name.
     * @return {@link IRaptorPlugin} if a match is found, or {@code null} otherwise.
     * @since 1.6
     */
    IRaptorPlugin getPlugin(String family, String type);

    /**
     * Attempt to locate an Import-Export wizard with the given family and type name.  Import-Export plugins are
     * stateless, and the returned instance can be used to directly trigger the Import Export Wizard, skipping the
     * initial screen that typically prompts the user to select a specific plugin.
     *
     * @param family Family name
     * @param type   Type name
     * @param mode   Directional mode
     * @return IImportExportWizard, if a match is found, or {@code null} otherwise.
     * @throws NoSuchPluginException when the family, type, mode combination does not exist.
     */
    IImportExportWizard getPlugin(String family, String type, ImportExportMode mode);

    /**
     * Return the current state of the specified plugin.
     *
     * @param family Plugin family name.
     * @param type   Plugin type name.
     * @return {@link PluginState} indicating if the plugin is currently installed, running or stopped.
     * @since 1.6
     */
    PluginState getPluginState(String family, String type);

    /**
     * Get the plugin definition for the given family/type, searching all plugin registries.
     * If the given family is {@link KeyUtils#FILE_FAMILY}, then we manufacture a synthetic definition for it.
     *
     * @param family Family of plugin
     * @param type   Typeof plugin
     * @return Optional plugin definition
     * @since 2.0.0
     */
    Optional<? extends IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinition(String family, String type);

    /**
     * Get all of the plugin definitions for our configured registries.
     *
     * @return Set off all plugin definitions from the configured registries
     * @since 2.0.0
     */
    Set<IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinitions();

    /**
     * If any of the plugins report that they are export controlled then we want to mark TAKX in some way
     * to indicate to the user that we have an export controlled configuration.
     *
     * @return export control status
     */
    boolean isExportControlled();
}
