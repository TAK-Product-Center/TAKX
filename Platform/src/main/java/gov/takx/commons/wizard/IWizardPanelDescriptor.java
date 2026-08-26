package gov.takx.commons.wizard;

import jakarta.annotation.Nullable;

import java.awt.Component;
import java.util.Map;

/**
 * A single panel in a wizard. A wizard panel was a graphical component. Usually this will be an instance of JPanel,
 * but can be any instance of java.awt.Component.
 *
 */
public interface IWizardPanelDescriptor
{
    /**
     * Special identifier used to signal the end of a wizard.
     */
    String FINISH = "commons.wizard.finish";

    /**
     * Special identifier used for the first screen in the wizard.
     */
    String BEGIN = "commons.wizard.begin";

    /**
     * Get the graphical panel component.
     *
     * @return The panel component.
     */
    Component getPanelComponent();

    /**
     * Get the descriptor of the previous panel in the wizard. This is the panel that the user will return to if
     * she clicks the back button.
     *
     * @return Descriptor of the previous wizard panel. If {@code null} is returned the user will not be allowed to go back.
     */
    Object getBackPanelDescriptor();

    /**
     * Get the descriptor of the next panel in the wizard. You can implement a branching wizard by placing logic
     * in this method that will return a different "next panel" depending on what the user has selected.
     * <p/>
     * Return the special {@link #FINISH} identifier to finish the wizard.
     *
     * @return Identifier of the next panel.
     * @see #FINISH
     */
    Object getNextPanelDescriptor();

    /**
     * Set the wizard in which this panel is being used.
     *
     * @param wizard The wizard that this  panel is part of.
     */
    void registerPanel(IWizard wizard);

    /**
     * Called by the framework before the panel is displayed. This method can be used to initialize the panel.
     */
    void aboutToDisplayPanel();

    void displayingPanel();

    /**
     * Called by the framework before the panel is hidden. Use this method to capture user input and store it in
     * the wizard properties map.
     */
    void aboutToHidePanel();

    /**
     * Implement this method if your panel needs to perform validation and possible interaction with the user
     * prior to moving to the next panel when the user presses the next button.
     *
     * @return {@code true} to move forward, {@code false} to stay on the same wizard page
     */
    boolean forwardValidate();

    /**
     * Allow TAKX to store preconfiguration details. This is useful when the user is dropping files on the globe
     * and wants TAKX to remember the settings for the next file of that type.
     *
     * @return the map of properties, or {@code null} if no configuration is needed for this panel.
     * @since 3.0 (moved from IPreconfigurableImportExportWizard and return value modified)
     */
    @Nullable
    Map<String, Object> getPreconfigureProperties();
}
