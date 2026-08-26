package gov.takx.core.view.gui.preferences.toolbar;

import jakarta.annotation.Nonnull;

/**
 * Listener for a name change from the rename dialog
 *
 * @since 2.0 (Renamed in 3.4)
 */
public interface IToolbarConfigurationRenameListener
{
    void updateName(@Nonnull String newName);
}
