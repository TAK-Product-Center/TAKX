package gov.takx.commons.wizard;

import javax.swing.JDialog;

import java.awt.Window;

/**
 * The wizard controller.
 *
 */
public interface IWizard
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
    public void setCurrentPanelDescriptor(Object id);

    /**
     * Get the wizard model. The model contains meta-data about how
     * the navigation buttons are labeled, etc.
     *
     * @return wizard model
     */
    public IWizardModel getModel();

    /**
     * Get the wizard's return code.
     *
     * @return The code returned by the wizard when it finished.
     */
    public int getReturnCode();

    /**
     * Get the window that owns the wizard dialog.
     *
     * @return Window that owns the wizard.
     */
    public Window getOwner();

    /**
     * Get the wizard dialog.
     *
     * @return The JDialog that displays the wizard.
     */
    public JDialog getDialog();

    /**
     * Is the wizard dialog modal?
     *
     * @return true if the dialog is modal.
     */
    public boolean isModal();

    /**
     * Set the wizard dialog to modal or not modal.
     *
     * @param b true if the wizard should be modal.
     */
    public void setModal(boolean b);

    /**
     * Get the wizard title. This title is displayed in the title bar
     * of the wizard dialog.
     *
     * @return The wizard title.
     */
    public String getTitle();

    /**
     * Set the wizard title. This title is displayed in the title bar
     * of the wizard dialog.
     *
     * @param title The new title.
     */
    public void setTitle(String title);

    public int showModalDialog();

    /**
     * Is the "Back" button enabled?
     *
     * @return true if the button is enabled.
     */
    public boolean isBackButtonEnabled();

    /**
     * Set the "Back" button to enabled or disabled.
     *
     * @param b true if the button should be enabled.
     */
    public void setBackButtonEnabled(boolean b);

    /**
     * Is the "Next" button enabled?
     *
     * @return true if the button is enabled.
     */
    public boolean isNextButtonEnabled();

    /**
     * Set the "Next" button to enabled or disabled.
     *
     * @param b true if the button should be enabled.
     */
    public void setNextButtonEnabled(boolean b);

    /**
     * Is the "Cancel" button enabled?
     *
     * @return true if the button is enabled.
     */
    public boolean isCancelButtonEnabled();

    /**
     * Set the "Cancel" button to enabled or disabled.
     *
     * @param b true if the button should be enabled.
     */
    public void setCancelButtonEnabled(boolean b);

    /**
     * Give focus to the "Back" button.
     */
    public void giveFocusToBackButton();

    /**
     * Give focus to the "Next" button.
     */
    public void giveFocusToNextButton();

    /**
     * Give focus to the "Cancel" button.
     */
    public void giveFocusToCancelButton();
}
