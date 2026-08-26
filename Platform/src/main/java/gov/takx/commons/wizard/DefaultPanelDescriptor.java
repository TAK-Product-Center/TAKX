package gov.takx.commons.wizard;

import jakarta.annotation.Nullable;

import javax.swing.JPanel;

import java.awt.Component;
import java.util.Map;

/**
 */
public class DefaultPanelDescriptor implements IWizardPanelDescriptor
{
    private static final String DEFAULT_PANEL_IDENTIFIER = "wizard.DefaultPanelIdentifier";

    private IWizard wizard;
    private Object panelIdentifier;
    private Component panelComponent;

    public DefaultPanelDescriptor()
    {
        this.panelIdentifier = DEFAULT_PANEL_IDENTIFIER;
        this.panelComponent = new JPanel();
    }

    public DefaultPanelDescriptor(Object id, Component panel)
    {
        if (id == null) throw new IllegalArgumentException("Parameter 'id' may not be null");
        if (panel == null) throw new IllegalArgumentException("Parameter 'panel' may not be null");

        this.panelIdentifier = id;
        this.panelComponent = panel;
    }

    public IWizard getWizard()
    {
        return this.wizard;
    }

    public IWizardModel getWizardModel()
    {
        return this.wizard != null ? this.wizard.getModel() : null;
    }

    public Object getPanelIdentifier()
    {
        return this.panelIdentifier;
    }

    public void setPanelIdentifier(Object panelIdentifier)
    {
        if (panelIdentifier == null) throw new IllegalArgumentException("Parameter 'panelIdentifier' may not be null");

        this.panelIdentifier = panelIdentifier;
    }

    @Override
    public Component getPanelComponent()
    {
        return this.panelComponent;
    }

    public void setPanelComponent(Component panel)
    {
        if (panel == null) throw new IllegalArgumentException("Parameter 'panel' may not be null");

        this.panelComponent = panel;
    }

    @Override
    public Object getBackPanelDescriptor()
    {
        return null;
    }

    @Override
    public Object getNextPanelDescriptor()
    {
        return null;
    }

    public void registerPanel(IWizard wizard)
    {
        this.wizard = wizard;
    }

    /**
     * Default action is to do nothing.
     */
    @Override
    public void aboutToDisplayPanel()
    {
        // do nothing
    }

    /**
     * Default action is to do nothing.
     */
    @Override
    public void displayingPanel()
    {
        // do nothing
    }

    /**
     * Default action is to do nothing.
     */
    @Override
    public void aboutToHidePanel()
    {
        // do nothing
    }

    /**
     * Override this method if your panel needs to perform validation and possible interaction with the user
     * prior to moving to the next panel when the user presses the next button.
     *
     * @return {@code true} to move forward, {@code false} to stay on the same wizard page
     */
    @Override
    public boolean forwardValidate()
    {
        return true;
    }

    @Override
    @Nullable
    public Map<String, Object> getPreconfigureProperties()
    {
        return null;
    }
}
