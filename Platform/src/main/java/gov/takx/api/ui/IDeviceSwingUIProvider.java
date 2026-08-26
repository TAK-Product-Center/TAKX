package gov.takx.api.ui;

import gov.tak.api.annotation.Nullable;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.plugin.device.IDeviceConfigurationDelegate;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;

import javax.swing.JPanel;

import java.util.List;

/**
 * Contract for how a device plugin controller provides the needed Swing UI components for the TAKX
 * Swing UI expression (thick client).
 * <p/>
 * UI components created here will be disposed after use, so new instances should be produced on every call
 * to these methods.  If a panel returned implements {@link IResourceCleaner}, then its
 * {@link IResourceCleaner#releaseResources()} method will be called when the core is finished with the panel,
 * typically when the containing dialog is closed.
 *
 * @see IResourceCleaner
 * @since 2.0.0
 */
public interface IDeviceSwingUIProvider
{
    /**
     * Create the primary panel for the map object that will be inserted into the map object dialog. The map object
     * dialog is the dialog the operator is presented when double-clicking on an icon.
     *
     * @return panel implementing IMapObjectPluginPanel
     * @since 5.0 (renamed from getPrimaryPreferencePanel)
     */
    IRaptorPanel getPrimaryPanel();

    /**
     * Configuration panel that will be positioned above the list of panels and stay present no matter which list
     * panel is selected.
     *
     * @return the primary configuration panel, or null if one is not required (default null).
     * @since 5.1 (moved from old API interface IDeviceConfigurationManager)
     */
    @Nullable
    default IRaptorPanel getPrimaryConfigurationPanel()
    {
        return null;
    }

    /**
     * List of custom configuration panels.  Configuration panels will be added to the list of core TAKX map object
     * configuration panels that are displayed as tabs when the user clicks on the "Configuration" button in the map object
     * dialog.  Configuration panels in general contain controls for modifying a device's settings.
     * <p>
     * This list is going to be automatically cleared by the Map Object Configuration dialog when the dialog is disposed
     * to protect from UI component hanging around and being unexpectedly recycled.  Therefore, it is imperative that
     * this list contain newly instantiated components every time it is called.
     *
     * @return list of configuration panels implementing {@link IPluginConfigurationPanel}
     * @since 5.0
     */
    List<IPluginConfigurationPanel> getConfigurationPanels();

    /**
     * Provides a custom panel displayed in the Help Dialog when the user right-clicks on a device and chooses Help
     * from the popup context menu.
     * <p>
     * If no custom help is available for the device, return {@code null} (the default).
     *
     * @return Panel containing help on how to use the device plugin, or {@code null} if no help is provided
     */
    JPanel getPluginHelpPanel();

    /**
     * Map Object Configuration Delegate is injected for access to the Configuration Dialog panels.
     *
     * @param delegate injected delegate
     * @since 5.2 (moved from old API interface IDeviceConfigurationManager)
     */
    default void setMapObjectConfigurationDelegate(IDeviceConfigurationDelegate delegate)
    {
    }

    /**
     * @return {@code true} to exclude the core configuration panels, default = {@code false}
     * @since 5.2
     */
    default boolean excludeCoreConfigurationPanels()
    {
        return false;
    }
}
