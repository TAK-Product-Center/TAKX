package gov.takx.api.ui;

import gov.takx.commons.wizard.IWizardPanelDescriptor;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Contract for how an Import/Export Wizard provides the needed Swing UI components for the TAKX Swing UI
 * expression (thick client).  The {@link IWizardPanelDescriptor} itself is not a Swing Component but contains
 * methods pertaining to Swing components.
 *
 * @since 2.13
 */
public interface IImportExportSwingUIProvider
{
    /**
     * Create a map of all the panels that may appear in the wizard. Each panel is given an identifier, and can be
     * randomly accessed via the identifier string.
     * <p/>
     * Plugins <b>must not</b> cache this map- it <b>must</b> be created new with each call.
     *
     * @return Newly instantiated map of wizard panel, may be empty, must not be {@code null}.
     */
    @Nonnull
    Map<String, IWizardPanelDescriptor> createWizardPanels();
}
