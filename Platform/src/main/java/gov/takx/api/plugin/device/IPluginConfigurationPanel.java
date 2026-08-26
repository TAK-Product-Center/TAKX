package gov.takx.api.plugin.device;

import gov.takx.api.plugin.IMutableRaptorPanel;
import gov.takx.commons.swing.IPanelStateListenerReceiver;

/**
 * Contract for plugin configuration panels.
 *
 * @since 5.0 (renamed from IDevicePluginPreferencesPanel)
 */
public interface IPluginConfigurationPanel extends IMutableRaptorPanel, IPanelStateListenerReceiver
{
}
