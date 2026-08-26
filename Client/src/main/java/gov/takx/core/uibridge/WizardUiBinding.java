package gov.takx.core.uibridge;

import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.ui.IImportExportSwingUIProvider;
import gov.takx.platform.cdi.NoopInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * Binding for a Import/Export Wizard UI provider.
 * @see gov.takx.api.plugin.annotations.ImportExportSwingUIProvider
 * @since 5.1
 */
public class WizardUiBinding extends AUiBinding<IImportExportSwingUIProvider>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final IImportExportSwingUIProvider EMPTY_IMPORT_EXPORT_SWING_UI_PROVIDER =
            NoopInvocationHandler.forType(IImportExportSwingUIProvider.class);

    WizardUiBinding(Class<? extends IImportExportSwingUIProvider> uiProviderClass)
    {
        super(uiProviderClass);
    }

    /**
     * Construct the UI provider for the given wizard.
     */
    IImportExportSwingUIProvider constructUi(IImportExportWizard wizard)
    {
        try
        {
            return constructUi(uiProviderClass, Map.of(IImportExportWizard.class, wizard));
        } catch (Exception e)
        {
            logger.error("Failed to construct Wizard UI provider of type: {}", uiProviderClass, e);
        }

        return EMPTY_IMPORT_EXPORT_SWING_UI_PROVIDER;
    }
}
