package gov.takx.api.plugin.device;

import gov.takx.commons.constants.CommServiceType;

/**
 * If an IDevicePluginController implements this interface, a Comms tab will appear on the device map object's
 * preferences dialog for establishing a default comms path.
 *
 */
public interface IDefaultCommsPathManager
{
    /**
     * When the user modifies the default communication path via the Comms tab in the mapObjectPreferencesPanel,
     * the parameter values represent the user selection for default comm path
     *
     * @param commType String representation of the comm type (ie. "SERIAL", see CommServiceTypes)
     * @param commName String representation of the comm name (ie. "COM2")
     * @see CommServiceType
     */
    void setDefaultCommPath(CommServiceType commType, String commName);
}
