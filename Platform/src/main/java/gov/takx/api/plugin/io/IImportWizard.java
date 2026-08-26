package gov.takx.api.plugin.io;

import gov.takx.api.io.ImportExportResult;
import gov.takx.commons.wizard.IWizardProperties;

/**
 * An Import/Export wizard for importing data.
 *
 * @since 1.3
 */
public interface IImportWizard extends IImportExportWizard
{
    /**
     * Do the import operation. This method is called by the framework when the wizard finishes. The wizard
     * <i>must</i> notify all registered {@link IWizardResultListener}s of the result.
     *
     * @param props           Wizard properties.
     * @param progressMonitor Progress monitor that can be used to update the GUI with Import/Export progress.
     * @return An {@link ImportExportResult} that holds the result of the operation.
     * @throws Exception If anything goes wrong. An error message will be displayed in the GUI.
     */
    ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor) throws Exception;
}
