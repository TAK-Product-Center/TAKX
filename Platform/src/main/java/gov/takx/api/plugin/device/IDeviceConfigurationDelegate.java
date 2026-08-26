package gov.takx.api.plugin.device;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.ListSelectionListener;

/**
 * Delegate injected into the {@link gov.takx.api.ui.IDeviceSwingUIProvider} to provide access to some features
 * of the configuration dialog.
 * 
 * @since 5.2 (no longer provides the standard plugin delegate methods)
 */
@SuppressWarnings("unused") // plugins
@NoPluginImplementations
public interface IDeviceConfigurationDelegate
{
    /**
     * Register a listener for selection changes in the configuration panels list.
     *
     * @param listener listener to register
     */
    void registerPanelSelectListener(@Nonnull ListSelectionListener listener);

    /**
     * Unregister a listener from selection changes to the configuration panel list.
     *
     * @param listener listener to unregister
     */
    void unregisterPanelSelectListener(ListSelectionListener listener);

    /**
     * Add a new {@code IPluginConfigurationPanel} to the configuration dialog.
     *
     * @param configurationPanel panel to add
     */
    void addConfigurationPanel(@Nonnull IPluginConfigurationPanel configurationPanel);

    /**
     * Remove an existing {@code IPluginConfigurationPanel} from the configuration dialog.
     *
     * @param configurationPanel panel to remove
     */
    void removeConfigurationPanel(@Nonnull IPluginConfigurationPanel configurationPanel);

    /**
     * Change the title of a panel as displayed in the list of panels on the left side of the configuration
     * dialog.
     *
     * @param configPanel panel to rename
     * @param newTitle    new title for the panel
     */
    void changePanelTitle(@Nonnull IPluginConfigurationPanel configPanel, @Nonnull String newTitle);

    /**
     * @return the selected configuration panel, or null if there is no selection
     */
    @Nullable
    IPluginConfigurationPanel getSelectedConfigurationPanel();

    /**
     * Sets the dirty flag to true for the configuration dialog.
     */
    void setConfigurationDirty();

    /**
     * Select a panel in the configuration dialog by the name that is displayed on the left side panel list.  If 
     * the name is not found in the panel list, or is {@code null}, no change in selection will occur.
     * 
     * @param panelName name of the panel to select
     */
    void selectPanel(@Nullable String panelName);
}
