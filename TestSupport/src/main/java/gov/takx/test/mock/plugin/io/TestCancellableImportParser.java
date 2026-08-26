package gov.takx.test.mock.plugin.io;

import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.ICancellableImportParser;
import gov.takx.api.plugin.io.IImportMonitor;

/**
 * Test parser for testing the {@link TestCancellableImportWizard}
 *
 */
public class TestCancellableImportParser implements ICancellableImportParser
{
    private IImportMonitor monitor;

    @Override
    public ImportExportResult parse()
    {
        ImportExportResult result = new ImportExportResult();
        result.setSuccess(true);
        monitor.onDataMessage(null);
        return result;
    }

    @Override
    public void cancel()
    {

    }

    @Override
    public void setMonitor(IImportMonitor monitor)
    {
        this.monitor = monitor;
    }
}
