package gov.takx.core.view.gui.wizard;

import gov.nasa.worldwind.util.wizard.WizardPanelDescriptor;
import gov.takx.commons.wizard.IWizardPanelDescriptor;

import java.awt.Component;

/**
 * This class wraps a WorldWind WizardPanelDescriptor interface around an {@link IWizardPanelDescriptor}.
 *
 */
class WizardPanelDescriptorWrapper implements WizardPanelDescriptor
{
    private final IWizardPanelDescriptor panelDescriptor;

    public WizardPanelDescriptorWrapper(IWizardPanelDescriptor descriptor)
    {
        panelDescriptor = descriptor;
    }

    public IWizardPanelDescriptor getPanelDescriptor()
    {
        return panelDescriptor;
    }

    @Override
    public Component getPanelComponent()
    {
        return panelDescriptor.getPanelComponent();
    }

    @Override
    public Object getBackPanelDescriptor()
    {
        return panelDescriptor.getBackPanelDescriptor();
    }

    @Override
    public Object getNextPanelDescriptor()
    {
        return panelDescriptor.getNextPanelDescriptor();
    }

    @Override
    public void registerPanel(gov.nasa.worldwind.util.wizard.Wizard wizard)
    {
        panelDescriptor.registerPanel(new Wizard(wizard));
    }

    @Override
    public void aboutToDisplayPanel()
    {
        panelDescriptor.aboutToDisplayPanel();
    }

    @Override
    public void displayingPanel()
    {
        panelDescriptor.displayingPanel();
    }

    @Override
    public void aboutToHidePanel()
    {
        panelDescriptor.aboutToHidePanel();
    }
}
