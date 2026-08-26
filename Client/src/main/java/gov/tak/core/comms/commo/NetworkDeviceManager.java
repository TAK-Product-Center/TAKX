package gov.tak.core.comms.commo;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for network device manager.
 *
 * @since 3.0
 */
@ApplicationScoped
public class NetworkDeviceManager implements INetworkDeviceManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<NetworkDevice> networkDevices = new ArrayList<>();

    /**
     * CDI Constructor.
     */
    NetworkDeviceManager()
    {
        try
        {
            Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(NetworkDeviceManager::isValid)
                    .map(NetworkDevice::create)
                    .forEach(networkDevices::add);
        } catch (SocketException e)
        {
            logger.error("Error enumerating Network Interfaces", e);
        }
    }

    @Override
    public List<NetworkDevice> getNetworkDevices()
    {
        return Collections.unmodifiableList(networkDevices);
    }

    /**
     * Checks to see if a given NetworkInterface can be used as a valid Network Device.  This includes checking that
     * the HW address is accessible, and for known interfaces to avoid (like Emulator, Virtual, and Loopback).
     *
     * @param networkInterface network interface to check
     * @return {@code true} if the interface is considered valid
     */
    private static boolean isValid(NetworkInterface networkInterface)
    {
        try
        {
            if (networkInterface.getHardwareAddress() == null) return false;

            final String name = networkInterface.getDisplayName();

            return !name.contains("Emulator") && !name.contains("Virtual") && !name.contains("Loopback")
                    && !name.contains("Teredo Tunneling Pseudo-Interface");
        } catch (SocketException e)
        {
            return false;
        }
    }
}
