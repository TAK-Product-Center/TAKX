package gov.takx.core.view.gui;

import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.toolbar.ButtonType;

/**
 */
public class DevicesPanel extends DraggableObjectPanel
{
    public static final String BUTTON_TOOLTIP_TEXT = "Place the selected Device on the globe.";

    public DevicesPanel()
    {
        super(ButtonType.DEVICES.getDisplayText());
        setButtonTooltip(BUTTON_TOOLTIP_TEXT);
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>All of the devices currently available are displayed below. Add a new device to the globe by selecting the device and pressing the Add button.  Devices can also be added by dragging and dropping the device icon onto the globe.</html>";
    }

    @Override
    protected boolean passesFilter(IDevicePluginDefinition definition)
    {
        // Only devices that allow manual creation
        return definition.isManualCreationAllowed() && definition.getPluginCategory() == PluginCategory.DevicePlugin;
    }
}
