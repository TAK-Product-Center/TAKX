package gov.takx.api.registry;

import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportPluginDefinition;

/**
 * Maintains a registry of all known IOPlugin instances.
 *
 */
public interface IImportExportPluginRegistry extends IPluginRegistry<IImportExportPlugin, IImportExportPluginDefinition>
{
}
