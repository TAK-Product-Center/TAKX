package gov.takx.test.mock.plugin.io;

import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.ACancellableImportWizard;
import gov.takx.api.plugin.io.ICancellableImportParser;
import gov.takx.api.plugin.io.IImportExportPluginDelegate;
import gov.takx.commons.wizard.IWizardProperties;

import java.util.Map;

/**
 * Test wizard for testing properties and logic of {@link ACancellableImportWizard}
 *
 */
public class TestCancellableImportWizard extends ACancellableImportWizard
{
    /**
     * @return Delegate for testing purposes
     * @since 2.38
     */
    public IImportExportPluginDelegate getDelegate()
    {
        return delegate;
    }

    @Override
    protected ImportExportResult validateFileForImport(String filename)
    {
        ImportExportResult result = new ImportExportResult();
        result.setSuccess(true);
        return result;
    }

    @Override
    protected ImportExportResult preParseFile(String filename)
    {
        ImportExportResult result = new ImportExportResult();
        result.setSuccess(true);
        result.setDataPointCount(42);
        return result;
    }

    @Override
    protected ICancellableImportParser createImportParser(String filePath, Map<String, Object> affectedObjects, IWizardProperties props)
    {
        return new TestCancellableImportParser();
    }
}
