package gov.takx.core.registry;

import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportPluginDefinition;
import gov.takx.api.registry.IImportExportPluginRegistry;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registry for all Import/Export plugins.
 *
 */
@ApplicationScoped
public class ImportExportPluginRegistry extends APluginRegistry<IImportExportPlugin, IImportExportPluginDefinition>
        implements IImportExportPluginRegistry
{
    /**
     * Construct the IO plugin registry.
     */
    public ImportExportPluginRegistry()
    {
        super(PluginCategory.ImportExportPlugin);
    }
}
