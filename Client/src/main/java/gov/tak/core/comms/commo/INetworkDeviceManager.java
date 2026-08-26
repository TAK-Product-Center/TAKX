package gov.tak.core.comms.commo;

import java.util.List;

/**
 * Contract for network device manager.
 *
 * @since 3.0
 */
public interface INetworkDeviceManager
{
    /**
     * @return list of usable Network Devices with valid hardware interfaces (mac addresses)
     */
    List<NetworkDevice> getNetworkDevices();
}
