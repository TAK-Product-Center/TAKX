package gov.takx.test.mock.plugin.io;

import gov.takx.platform.logging.NullImportExportProgressMonitor;

/**
 * Implementation of IImportExportProgressMonitor for use in import/export tests that counts and tracks complete status.
 *
 * @since 2.20
 */
@SuppressWarnings("unused") // Used in plugin tests
public class TestImportExportProgressMonitor extends NullImportExportProgressMonitor
{
    public boolean complete = false;
    public int counts = 0;

    @Override
    public void setProgress(int progress)
    {
        counts = progress;
    }

    @Override
    public void setComplete()
    {
        complete = true;
    }
}
