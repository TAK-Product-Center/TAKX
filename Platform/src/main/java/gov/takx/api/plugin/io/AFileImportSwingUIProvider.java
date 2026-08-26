package gov.takx.api.plugin.io;

import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.commons.wizard.WizardBuilder;
import gov.takx.platform.io.panels.FileToImportPanel;

import java.util.Map;

/**
 * Generic / Basic file import swing UI provider.  Provides only the file selection dialog panel to allow the
 * operator to select which file(s) to import.  In the case where files were dropped on the globe, return
 * {@link IWizardPanelDescriptor#FINISH} for the wizard method {@link IImportExportWizard#getFirstWizardPanel()}
 * to skip this panel.
 * <p>
 * Swing UI Providers for Import Wizards that don't require any UI beyond a {@link FileToImportPanel} can extend
 * this class.
 *
 * @since 2.14
 */
public abstract class AFileImportSwingUIProvider implements IImportExportSwingUIProvider
{
    private IImportExportWizard wizard;

    /**
     * Store the instance of the wizard associated with this I/O swing UI provider.
     *
     * @param wizard the wizard being provided for.
     */
    public AFileImportSwingUIProvider(IImportExportWizard wizard)
    {
        this.wizard = wizard;
    }

    /**
     * Build the wizard panel map consisting of only the {@link FileToImportPanel} panel.  The import wizard can
     * choose to skip this panel altogether, in the case of files dropped on the globe by returning
     * {@link IWizardPanelDescriptor#FINISH} for the wizard method {@link IImportExportWizard#getFirstWizardPanel()}
     *
     * @return the map of wizard panels, in this case, just the {@link FileToImportPanel}.
     */
    @Override
    public Map<String, IWizardPanelDescriptor> createWizardPanels()
    {
        return WizardBuilder.buildPanelMap(new FileToImportPanel(wizard.getFormats(), wizard.getPreconfigurationSettings(), null));
    }
}
