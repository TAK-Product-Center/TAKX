package gov.takx.test.mock.plugin.io;

import gov.takx.api.io.Format;
import gov.takx.api.plugin.annotations.ImportExportSwingUIProvider;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.commons.wizard.WizardBuilder;
import gov.takx.platform.io.panels.FileToImportPanel;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Generic I/O Swing UI Provider for testing Swing Binding on the I/O API
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@ImportExportSwingUIProvider(forWizard = TestImportWizard2.class)
public class TestImportWizardSwingUIProvider implements IImportExportSwingUIProvider
{
    private final List<Format> formats;
    private final Map<String, Object> preconfSettings;

    public TestImportWizardSwingUIProvider(List<Format> formats, Map<String, Object> preconfSettings)
    {
        this.formats = formats;
        this.preconfSettings = preconfSettings;
    }

    @Nonnull
    @Override
    public Map<String, IWizardPanelDescriptor> createWizardPanels()
    {
        return WizardBuilder.buildPanelMap(new FileToImportPanel(formats, preconfSettings, null));
    }
}
