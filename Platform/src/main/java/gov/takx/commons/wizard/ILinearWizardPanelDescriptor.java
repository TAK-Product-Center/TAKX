package gov.takx.commons.wizard;

/**
 * A wizard panel that does not require branching logic. The panels before and after this panel will not change
 * based on user input, so connections can be set once when the wizard is created.
 *
 * @since 1.3
 */
public interface ILinearWizardPanelDescriptor extends IWizardPanelDescriptor
{
    /**
     * Get a unique identifier for this panel. Often the panel classname is used.
     *
     * @return Unique identifier for the panel. The identifier must be unique within the wizard.
     */
    public String getIdentifier();

    /**
     * Set the identifier of the panel that comes after this one.
     *
     * @param identifier The identifier for the next panel.
     */
    public void setNextPanel(String identifier);

    /**
     * Set the identifier of the panel that comes before this one.
     *
     * @param identifier The identifier for the previous panel.
     */
    public void setPreviousPanel(String identifier);
}
