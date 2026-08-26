package gov.takx.api.plugin.io;

import gov.takx.api.io.ImportExportResult;

/**
 * Interface to be implemented by classes that want to receive results from {@link IImportExportWizard}s.
 *
 * @since 1.14
 */
public interface IWizardResultListener
{
    /**
     * Notify listeners of results.
     *
     * @param result The results produced by an import/export.
     */
    void onResults(ImportExportResult result);
}
