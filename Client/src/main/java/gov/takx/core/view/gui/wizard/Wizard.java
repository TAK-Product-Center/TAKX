package gov.takx.core.view.gui.wizard;

import gov.takx.commons.wizard.IWizard;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import jakarta.annotation.Nonnull;

import javax.swing.JDialog;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

/**
 */
public class Wizard implements IWizard
{
    private gov.nasa.worldwind.util.wizard.Wizard wizard;

    public static final int FINISH_RETURN_CODE = gov.nasa.worldwind.util.wizard.Wizard.FINISH_RETURN_CODE;
    public static final int CANCEL_RETURN_CODE = gov.nasa.worldwind.util.wizard.Wizard.CANCEL_RETURN_CODE;
    public static final int ERROR_RETURN_CODE = gov.nasa.worldwind.util.wizard.Wizard.ERROR_RETURN_CODE;
    public static final int CLOSED_RETURN_CODE = gov.nasa.worldwind.util.wizard.Wizard.CLOSED_RETURN_CODE;

    public static final Object FINISH = gov.nasa.worldwind.util.wizard.Wizard.FINISH;

    public Wizard()
    {
        wizard = new gov.nasa.worldwind.util.wizard.Wizard();
    }

    public Wizard(Dialog owner)
    {
        wizard = new gov.nasa.worldwind.util.wizard.Wizard(owner);
    }

    public Wizard(Frame owner)
    {
        wizard = new gov.nasa.worldwind.util.wizard.Wizard(owner);
    }

    public Wizard(@Nonnull gov.nasa.worldwind.util.wizard.Wizard wizard)
    {
        this.wizard = wizard;
    }

    @Override
    public IWizardPanelDescriptor getWizardPanel(Object id)
    {
        WizardPanelDescriptorWrapper wrapper = (WizardPanelDescriptorWrapper) wizard.getWizardPanel(id);
        return wrapper.getPanelDescriptor();
    }

    @Override
    public void registerWizardPanel(Object id, IWizardPanelDescriptor panel)
    {
        wizard.registerWizardPanel(id, new WizardPanelDescriptorWrapper(panel));
    }

    @Override
    public IWizardPanelDescriptor getCurrentPanel()
    {
        WizardPanelDescriptorWrapper wrapper = (WizardPanelDescriptorWrapper) wizard.getCurrentPanel();
        return wrapper.getPanelDescriptor();
    }

    @Override
    public void setCurrentPanelDescriptor(Object id)
    {
        wizard.setCurrentPanelDescriptor(id);
    }

    @Override
    public WizardModel getModel()
    {
        return new WizardModel(wizard.getModel());
    }

    @Override
    public int getReturnCode()
    {
        return wizard.getReturnCode();
    }

    @Override
    public Window getOwner()
    {
        return wizard.getOwner();
    }

    @Override
    public JDialog getDialog()
    {
        return wizard.getDialog();
    }

    @Override
    public boolean isModal()
    {
        return wizard.isModal();
    }

    @Override
    public void setModal(boolean b)
    {
        wizard.setModal(b);
    }

    @Override
    public String getTitle()
    {
        return wizard.getTitle();
    }

    @Override
    public void setTitle(String title)
    {
        wizard.setTitle(title);
    }

    @Override
    public int showModalDialog()
    {
        return wizard.showModalDialog();
    }

    @Override
    public boolean isBackButtonEnabled()
    {
        return wizard.isBackButtonEnabled();
    }

    @Override
    public void setBackButtonEnabled(boolean b)
    {
        wizard.setBackButtonEnabled(b);
    }

    @Override
    public boolean isNextButtonEnabled()
    {
        return wizard.isNextButtonEnabled();
    }

    @Override
    public void setNextButtonEnabled(boolean b)
    {
        wizard.setNextButtonEnabled(b);
    }

    public boolean isCancelButtonEnabled()
    {
        return wizard.isCancelButtonEnabled();
    }

    @Override
    public void setCancelButtonEnabled(boolean b)
    {
        wizard.setCurrentPanelDescriptor(b);
    }

    @Override
    public void giveFocusToBackButton()
    {
        wizard.giveFocusToBackButton();
    }

    @Override
    public void giveFocusToNextButton()
    {
        wizard.giveFocusToNextButton();
    }

    @Override
    public void giveFocusToCancelButton()
    {
        wizard.giveFocusToCancelButton();
    }
}
