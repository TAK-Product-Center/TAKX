package gov.takx.api.ui;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * Interface providing the ability to add a panel (tabbed) to the User Preferences dialog.  Note that the
 * preferences panel don't survive the project lifecycle, and will have to be added from a plugin on project
 * open.  Custom panels will receive a shutdown method just before being removed.
 *
 * @since 1.49
 */
@InjectableService
@NoPluginImplementations
public interface IPreferenceSpaceManager
{
    /**
     * Contribute a user preference panel.  Suggested time to add the user preference panel is on the start()
     * method of the app plugin.
     *
     * @param name  Unique name for panel, to be displayed in the tab
     * @param panelSupplier Supplier to use to create instances of the User preference panel
     */
    void addUserPreferencePanel(@Nonnull String name, @Nonnull Supplier<IProgramPreferencesPanel> panelSupplier);

    /**
     * Remove a previously contributed panel.  Suggested time to remove a user preference panel is on the stop()
     * method of the app plugin
     *
     * @param name Unique name of panel to remove.
     */
    void removeUserPreferencePanel(@Nonnull String name);
}
