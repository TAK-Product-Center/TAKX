package gov.takx.core.view.gui.data;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.io.Format;
import gov.takx.api.plugin.io.IImportExportPlugin;
import gov.takx.api.plugin.io.IImportExportWizard;
import gov.takx.api.registry.IImportExportPluginRegistry;
import gov.takx.commons.wizard.DefaultPanelDescriptor;
import gov.takx.commons.wizard.IWizard;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import gov.takx.core.ioservice.ImportExportService;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.platform.io.ImportExportMode;
import gov.takx.platform.io.panels.CommonWizardSettings;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wizard panel descriptor to allow user to choose a format for import or export. This panel is the first in the data
 * import and export wizards.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class FormatChooserPanelDescriptor extends DefaultPanelDescriptor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SELECTED_WIZARD = "wizard.io.wizard";

    public static final String IDENTIFIER = "core.view.gui.data.FormatChooserPanel";

    @Inject
    private IImportExportPluginRegistry importExportPluginRegistry;

    @Inject
    private ImportExportService importExportService;

    @Inject
    private SwingUIBinder swingUIBinder;

    private FormatChooserPanelDescriptor(ImportExportMode mode)
    {
        List<IImportExportWizard> wizards = new ArrayList<>();

        for (IImportExportPlugin plugin : importExportPluginRegistry.getPlugins())
        {
            final IImportExportWizard wizard = mode.getWizardCreator().apply(plugin);

            if (wizard != null)
            {
                importExportService.injectWizardFormatsAndDelegate(plugin, wizard);
                wizards.add(wizard);
            }
        }

        FormatChooserPanel panelComponent = new FormatChooserPanel(wizards);
        panelComponent.addPropertyChangeListener(new PropertyEvents());
        setPanelIdentifier(IDENTIFIER);
        setPanelComponent(panelComponent);
    }

    public static FormatChooserPanelDescriptor createForImport()
    {
        return new FormatChooserPanelDescriptor(ImportExportMode.IMPORT);
    }

    public static FormatChooserPanelDescriptor createForExport()
    {
        return new FormatChooserPanelDescriptor(ImportExportMode.EXPORT);
    }

    @Override
    public Object getBackPanelDescriptor()
    {
        return null;
    }

    @Override
    public Object getNextPanelDescriptor()
    {
        IWizard wizard = getWizard();
        if (wizard != null)
        {
            IWizardModel model = wizard.getModel();
            IImportExportWizard ioWizard = (IImportExportWizard) model.getProperty(SELECTED_WIZARD);
            Format format = (Format) model.getProperty(CommonWizardSettings.FORMAT);
            if (ioWizard != null)
            {
                ioWizard.setFormat(format);

                var panels = swingUIBinder.forWizard((IImportExportWizard) wizard).createWizardPanels();
                if (panels == null)
                {
                    logger.error("Wizard {} did not provide any panels", wizard);
                    return null;
                }

                for (Map.Entry<String, IWizardPanelDescriptor> entry : panels.entrySet())
                {
                    wizard.registerWizardPanel(entry.getKey(), entry.getValue());
                }

                model.setProperty(ImportExportWizardSettings.PLUGIN, ioWizard);

                return ioWizard.getFirstWizardPanel();
            }
        }
        return null;
    }

    @Override
    public void aboutToDisplayPanel()
    {
        setNextButtonAccordingToSelectedFormat();
    }

    private void setNextButtonAccordingToSelectedFormat()
    {
        IWizard wizard = getWizard();
        if (wizard != null)
        {
            IImportExportWizard ioWizard = (IImportExportWizard) getWizardModel().getProperty(SELECTED_WIZARD);
            wizard.setNextButtonEnabled(ioWizard != null);
            wizard.giveFocusToNextButton();
        }
    }

    private void selectedFormatChanged(Object newValue)
    {
        IWizardModel model = getWizardModel();
        if (model != null && newValue instanceof FormatChooserPanel.FormatWrapper)
        {
            FormatChooserPanel.FormatWrapper format = (FormatChooserPanel.FormatWrapper) newValue;
            model.setProperty(SELECTED_WIZARD, format.wizard);
            model.setProperty(CommonWizardSettings.FORMAT, format.format);
            setNextButtonAccordingToSelectedFormat();
        }
    }

    private class PropertyEvents implements PropertyChangeListener
    {
        public void propertyChange(PropertyChangeEvent evt)
        {
            if (evt != null && evt.getPropertyName() != null)
            {
                String propertyName = evt.getPropertyName();
                if (propertyName.equals(FormatChooserPanel.SELECTED_FORMAT_PROPERTY))
                {
                    selectedFormatChanged(evt.getNewValue());
                }
            }
        }
    }
}
