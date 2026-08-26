package gov.takx.api.ui;

import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Contract for how an App provides the needed Swing UI components for the TAKX Swing UI expression (thick client).
 *
 * @since 2.0.0
 */
public interface IAppSwingUIProvider
{
    /**
     * Get the app's configuration panels, or an empty list if the app is not configurable.
     * <p/>
     * UI components provided will be disposed after use, so new instances should be produced on every call
     * to this method.  If a panel returned implements {@link IResourceCleaner}, then its
     * {@link IResourceCleaner#releaseResources()} method will be called when the core is finished with the panel,
     * typically when the containing dialog is closed.
     *
     * @return The list of configuration panels or and empty list if the app is not configurable.
     * @since 5.0 (renamed with new signature)
     */
    @Nonnull
    default List<IPluginConfigurationPanel> getConfigurationPanels()
    {
        return Collections.emptyList();
    }
}
