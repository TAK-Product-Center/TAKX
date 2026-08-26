package gov.takx.core.ioservice;

import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.commons.wizard.IWizardModel;

/**
 * Implementation of managed task to handle export wizard operations.
 *
 * @since 1.42
 */
public class ExportWizardTask extends AImportExportTask<IExportWizard>
{
    public ExportWizardTask(String name, IExportWizard wizard, IWizardModel model)
    {
        super(name, wizard, model);
    }

    @Override
    protected ImportExportResult doOperation() throws Exception
    {
        return wizard.doExport(model, getTaskStatusHandler());
    }
}
