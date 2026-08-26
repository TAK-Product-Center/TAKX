package gov.takx.core.plugin;

import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.plugin.device.ADevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IPluginConfigurationPanel;
import gov.takx.api.ui.IDeviceSwingUIProvider;

import javax.swing.JPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Default (essentially empty) implementation of {@link IDevicePluginController}.
 *
 * @since 3.0
 */
public class DefaultDevicePluginController extends ADevicePluginController implements IDeviceSwingUIProvider
{
    /**
     * @param family family namespace
     * @param type   type
     * @param unitId device unit id
     */
    public DefaultDevicePluginController(String family, String type, String unitId)
    {
        super(family, type, unitId);
    }

    @Override
    public void receiveMessage(IRaptorDataMessage rdm)
    {
    }

    @Override
    public IRaptorPanel getPrimaryPanel()
    {
        return null;
    }

    @Override
    public List<IPluginConfigurationPanel> getConfigurationPanels()
    {
        return new ArrayList<>();
    }

    @Override
    public JPanel getPluginHelpPanel()
    {
        return null;
    }
}
