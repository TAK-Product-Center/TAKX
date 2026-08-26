package gov.takx.api.plugin.io;

import gov.takx.commons.wizard.ILinearWizardPanelDescriptor;
import gov.takx.commons.wizard.IWizard;
import jakarta.annotation.Nullable;

import javax.swing.JPanel;

import java.awt.Component;
import java.util.Map;

/**
 * Abstract implementation of a wizard panel that inherits from JPanel. This panel
 * implementation is designed for a linear wizard (that does not contain branching logic),
 * but could be used to implement a branching wizard by overriding {@link #getBackPanelDescriptor()}
 * and {@link #getNextPanelDescriptor()}.
 * <p/>
 * <p>Plugin panels are encouraged to extend this class.
 *
 */
public abstract class AWizardPanel extends JPanel implements ILinearWizardPanelDescriptor
{
    /**
     * Wizard that is using this wizard panel, set using {@link #registerPanel(IWizard)}.
     */
    protected IWizard wizard;

    /**
     * Identifier of the panel that comes before this one in the wizard.
     */
    protected String previousPanelIdentifier;

    /**
     * Identifier of the panel that comes next in the wizard.
     */
    protected String nextPanelIdentifier;

    private boolean panelIsBeingSkipped = false;

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
    public void setNextPanel(String identifier)
    {
        nextPanelIdentifier = identifier;
    }

    @Override
    public void setPreviousPanel(String identifier)
    {
        previousPanelIdentifier = identifier;
    }

    /**
     * This object is the panel component.
     *
     * @return this
     */
    @Override
    public Component getPanelComponent()
    {
        return this;
    }

    @Override
    public String getBackPanelDescriptor()
    {
        return previousPanelIdentifier;
    }

    @Override
    public String getNextPanelDescriptor()
    {
        return nextPanelIdentifier;
    }

    @Override
    public void registerPanel(IWizard wizard)
    {
        this.wizard = wizard;
        if (isPanelBeingSkipped())
        {
            aboutToDisplayPanel();
            aboutToHidePanel();
        }
    }

    public boolean isPanelBeingSkipped()
    {
        return panelIsBeingSkipped;
    }

    public void setPanelIsBeingSkipped(boolean panelIsBeingSkipped)
    {
        this.panelIsBeingSkipped = panelIsBeingSkipped;
    }

    @Override
    @Nullable
    public Map<String, Object> getPreconfigureProperties()
    {
        return null;
    }
}
