package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarConfiguration;
import jakarta.annotation.Nonnull;

/**
 * Listener for configuration changes caused by the {@link ToolbarConfigurationDialog}
 *
 * @since 2.0 (Renamed in 3.4)
 */
public interface IToolbarConfigurationDialogListener
{
    /**
     * Called when a new configuration was added by the {@link ToolbarConfigurationDialog}
     *
     * @param configuration The configuration that was added
     */
    void configurationAdded(@Nonnull ToolbarConfiguration configuration);

    /**
     * Called when a configuration was modified by the {@link ToolbarConfigurationDialog}
     * @param oldConfigurationName The old configuration name (in case it changed)
     * @param newConfiguration The configuration that was modified
     */
    void configurationChanged(@Nonnull String oldConfigurationName, @Nonnull ToolbarConfiguration newConfiguration);
}
