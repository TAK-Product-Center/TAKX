package gov.takx.commons.wizard;

import javax.swing.Icon;

/**
 * Data and meta-information for a wizard.
 *
 */
public interface IWizardModel extends IWizardProperties
{
    /**
     * Get the wizard panel for an identifier.
     *
     * @param id The identifier to lookup.
     * @return Wizard panel for {@code id} or {@code null} if none is found.
     */
    public IWizardPanelDescriptor getWizardPanel(Object id);

    /**
     * Register a new wizard panel.
     *
     * @param id    The identifier for this panel.
     * @param panel The panel to register.
     */
    public void registerWizardPanel(Object id, IWizardPanelDescriptor panel);

    /**
     * Get the current wizard panel.
     *
     * @return The current panel.
     */
    public IWizardPanelDescriptor getCurrentPanel();

    /**
     * Set the current wizard panel.
     *
     * @param id A wizard panel identifier.
     */
    public boolean setCurrentPanel(Object id);

    /**
     * Get the text of the "Back" button.
     *
     * @return Back button text.
     */
    public String getBackButtonText();

    /**
     * Set the text of the "Back" button.
     *
     * @param newText New text for the button.
     */
    public void setBackButtonText(String newText);

    /**
     * Get the text of the "Next" button.
     *
     * @return Next button text.
     */
    public String getNextButtonText();

    /**
     * Set the text of the "Next" button.
     *
     * @param newText New text for the button.
     */
    public void setNextButtonText(String newText);

    /**
     * Get the text of the "Cancel" button.
     *
     * @return Cancel button text.
     */
    public String getCancelButtonText();

    /**
     * Set the text of the "Cancel" button.
     *
     * @param newText New text for the button.
     */
    public void setCancelButtonText(String newText);

    /**
     * Is the "Back" button enabled?
     *
     * @return true if the button is enabled.
     */
    public Boolean isBackButtonEnabled();

    /**
     * Set the "Back" button to enabled or disabled.
     *
     * @param newValue true if the button should be enabled.
     */
    public void setBackButtonEnabled(Boolean newValue);

    /**
     * Is the "Next" button enabled?
     *
     * @return true if the button is enabled.
     */
    public Boolean isNextButtonEnabled();

    /**
     * Set the "Next" button to enabled or disabled.
     *
     * @param newValue true if the button should be enabled.
     */
    public void setNextButtonEnabled(Boolean newValue);

    /**
     * Is the "Cancel" button enabled?
     *
     * @return true if the button is enabled.
     */
    public Boolean isCancelButtonEnabled();

    /**
     * Set the "Cancel" button to enabled or disabled.
     *
     * @param newValue true if the button should be enabled.
     */
    public void setCancelButtonEnabled(Boolean newValue);

    /**
     * Get the icon for the "Back" button.
     *
     * @return the icon for the button.
     */
    public Icon getBackButtonIcon();

    /**
     * Set the icon for the "Back" button.
     *
     * @param newIcon New icon for the button.
     */
    public void setBackButtonIcon(Icon newIcon);

    /**
     * Get the icon for the "Next" button.
     *
     * @return the icon for the button.
     */
    public Icon getNextButtonIcon();

    /**
     * Set the icon for the "Next" button.
     *
     * @param newIcon New icon for the button.
     */
    public void setNextButtonIcon(Icon newIcon);

    /**
     * Get the icon for the "Cancel" button.
     *
     * @return the icon for the button.
     */
    public Icon getCancelButtonIcon();

    /**
     * Set the icon for the "Cancel" button.
     *
     * @param newIcon New icon for the button.
     */
    public void setCancelButtonIcon(Icon newIcon);

    public Icon getIconProperty(String propertyName);
}
