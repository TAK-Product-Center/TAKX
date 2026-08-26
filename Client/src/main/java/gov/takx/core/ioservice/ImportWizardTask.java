package gov.takx.core.ioservice;

import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.commons.wizard.IWizardModel;

/**
 * Implementation of managed task to handle import wizard operations.
 *
 * @since 1.42
 */
public class ImportWizardTask extends AImportExportTask<IImportWizard>
{
    public ImportWizardTask(String name, IImportWizard wizard, IWizardModel model)
    {
        super(name, wizard, model);
    }

    @Override
    protected ImportExportResult doOperation() throws Exception
    {
        return wizard.doImport(model, getTaskStatusHandler());
    }
}
