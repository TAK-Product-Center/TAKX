package gov.tak.core.comms.commo;

import gov.takx.commons.security.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.NetworkInterface;
import java.net.SocketException;

/**
 * Class to abstract data from Network Interface (hiding all the {@code SocketExceptions} that can arise by accessing it).
 *
 * @since 3.0
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
public class NetworkDevice
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final byte[] NO_MAC_ADDRESS = new byte[0];

    /**
     * Constructs a network device from the given network interface.
     *
     * @param networkInterface network interface from which to pull data
     */
    public static NetworkDevice create(NetworkInterface networkInterface)
    {
        try
        {
            return new NetworkDevice(networkInterface.getHardwareAddress(), networkInterface.getDisplayName());
        } catch (SocketException e)
        {
            // The system is pretty hosed if we can't get the HW address, so just report and use an empty address
            logger.error("Failed to get HW address from: {}", networkInterface, e);

            return new NetworkDevice(NO_MAC_ADDRESS, networkInterface.getDisplayName());
        }
    }

    private final byte[] macAddress;
    private final String displayName;

    /**
     * @param macAddress  HW address from network interface
     * @param displayName Display name from network interface
     */
    private NetworkDevice(byte[] macAddress, String displayName)
    {
        this.macAddress = macAddress;
        this.displayName = displayName;
    }

    /**
     * @return The hardware address of the network interface
     * @see NetworkInterface#getHardwareAddress()
     */
    public byte[] getMacAddress()
    {
        return macAddress;
    }

    /**
     * @return The display name of the network interface
     * @see NetworkInterface#getDisplayName()
     */
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return "NetworkDevice{HW addr=" + EncodingUtils.bytesToHex(macAddress) + " : " + displayName + "}";
    }
}
