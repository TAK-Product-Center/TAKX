package gov.takx.api.plugin.io;

import gov.takx.api.plugin.ARaptorPlugin;
import gov.takx.api.plugin.annotations.ImportExportPlugin;

/**
 * Base class for Import Export implementations.  Plugin Developers are strongly encouraged to extend this
 * class rather than implement the IImportExportPlugin interface.
 *
 * @since 2.13
 */
public abstract class AImportExportPlugin extends ARaptorPlugin<ImportExportPlugin> implements IImportExportPlugin
{
    protected AImportExportPlugin()
    {
        super(ImportExportPlugin.class);
    }
}
