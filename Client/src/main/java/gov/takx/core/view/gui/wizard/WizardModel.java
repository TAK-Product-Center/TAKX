package gov.takx.core.view.gui.wizard;

import gov.takx.commons.wizard.IWizardModel;
import gov.takx.commons.wizard.IWizardPanelDescriptor;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;

import java.beans.PropertyChangeListener;

/**
 * This class wraps the WorldWind wizard implementation in a {@link IWizardModel} interface.
 *
 */
public class WizardModel implements IWizardModel
{
    private gov.nasa.worldwind.util.wizard.WizardModel model;

    public static final String CURRENT_PANEL_DESCRIPTOR = "wizard.CurrentPanelDescriptor";
    public static final String BACK_BUTTON_TEXT = "wizard.BackButtonText";
    public static final String NEXT_BUTTON_TEXT = "wizard.NextButtonText";
    public static final String CANCEL_BUTTON_TEXT = "wizard.CancelButtonText";
    public static final String BACK_BUTTON_ENABLED = "wizard.BackButtonEnabled";
    public static final String NEXT_BUTTON_ENABLED = "wizard.NextButtonEnabled";
    public static final String CANCEL_BUTTON_ENABLED = "wizard.CancelButtonEnabled";
    public static final String BACK_BUTTON_ICON = "wizard.BackButtonIcon";
    public static final String NEXT_BUTTON_ICON = "wizard.NextButtonIcon";
    public static final String CANCEL_BUTTON_ICON = "wizard.CancelButtonIcon";

    /**
     * Create a new wizard model.
     */
    public WizardModel()
    {
        model = new gov.nasa.worldwind.util.wizard.WizardModel();
    }

    /**
     * Create a wrapper around a WorldWind wizard model.
     *
     * @param model Wizard model to wrap.
     */
    WizardModel(@Nonnull gov.nasa.worldwind.util.wizard.WizardModel model)
    {
        this.model = model;
    }

    @Override
    public IWizardPanelDescriptor getWizardPanel(Object id)
    {
        WizardPanelDescriptorWrapper wrapper = (WizardPanelDescriptorWrapper) model.getWizardPanel(id);
        return wrapper.getPanelDescriptor();
    }

    @Override
    public void registerWizardPanel(Object id, IWizardPanelDescriptor panel)
    {
        model.registerWizardPanel(id, new WizardPanelDescriptorWrapper(panel));
    }

    @Override
    public IWizardPanelDescriptor getCurrentPanel()
    {
        WizardPanelDescriptorWrapper wrapper = (WizardPanelDescriptorWrapper) model.getCurrentPanel();
        return wrapper.getPanelDescriptor();
    }

    @Override
    public boolean setCurrentPanel(Object id)
    {
        return model.setCurrentPanel(id);
    }

    @Override
    public String getBackButtonText()
    {
        return model.getBackButtonText();
    }

    @Override
    public void setBackButtonText(String newText)
    {
        model.setBackButtonText(newText);
    }

    @Override
    public String getNextButtonText()
    {
        return model.getNextButtonText();
    }

    @Override
    public void setNextButtonText(String newText)
    {
        model.setNextButtonText(newText);
    }

    @Override
    public String getCancelButtonText()
    {
        return model.getCancelButtonText();
    }

    @Override
    public void setCancelButtonText(String newText)
    {
        model.setCancelButtonText(newText);
    }

    @Override
    public Boolean isBackButtonEnabled()
    {
        return model.isBackButtonEnabled();
    }

    @Override
    public void setBackButtonEnabled(Boolean newValue)
    {
        model.setBackButtonEnabled(newValue);
    }

    @Override
    public Boolean isNextButtonEnabled()
    {
        return model.isNextButtonEnabled();
    }

    @Override
    public void setNextButtonEnabled(Boolean newValue)
    {
        model.setNextButtonEnabled(newValue);
    }

    @Override
    public Boolean isCancelButtonEnabled()
    {
        return model.isCancelButtonEnabled();
    }

    @Override
    public void setCancelButtonEnabled(Boolean newValue)
    {
        model.setCancelButtonEnabled(newValue);
    }

    @Override
    public Icon getBackButtonIcon()
    {
        return model.getBackButtonIcon();
    }

    @Override
    public void setBackButtonIcon(Icon newIcon)
    {
        model.setBackButtonIcon(newIcon);
    }

    @Override
    public Icon getNextButtonIcon()
    {
        return model.getIconProperty(NEXT_BUTTON_ICON);
    }

    @Override
    public void setNextButtonIcon(Icon newIcon)
    {
        model.setNextButtonIcon(newIcon);
    }

    @Override
    public Icon getCancelButtonIcon()
    {
        return model.getCancelButtonIcon();
    }

    @Override
    public void setCancelButtonIcon(Icon newIcon)
    {
        model.setCancelButtonIcon(newIcon);
    }

    @Override
    public Icon getIconProperty(String propertyName)
    {
        return model.getIconProperty(propertyName);
    }

    @Override
    public Object getProperty(String propertyName)
    {
        return model.getProperty(propertyName);
    }

    @Override
    public String getStringProperty(String propertyName)
    {
        return model.getStringProperty(propertyName);
    }

    @Override
    public Boolean getBooleanProperty(String propertyName)
    {
        return model.getBooleanProperty(propertyName);
    }

    @Override
    public Integer getIntegerProperty(String propertyName)
    {
        return model.getIntegerProperty(propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object newValue)
    {
        model.setProperty(propertyName, newValue);
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        model.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        model.removePropertyChangeListener(listener);
    }
}
