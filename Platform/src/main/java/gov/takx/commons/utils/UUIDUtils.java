package gov.takx.commons.utils;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

/**
 * The UUID utilities.
 *
 */
public final class UUIDUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final TimeBasedGenerator TYPE_1_GENERATOR;
    private static final NameBasedGenerator TYPE_3_GENERATOR;

    static
    {
        EthernetAddress address = getCanonicalMACAddress();
        TYPE_1_GENERATOR = Generators.timeBasedGenerator(address);
        TYPE_3_GENERATOR = Generators.nameBasedGenerator(); // No namespace, default digester
    }

    private UUIDUtils()
    {
    }

    /**
     * @return the generated type 1 (time/location-based) UUID.
     */
    public static UUID generateType1UUID()
    {
        return TYPE_1_GENERATOR.generate();
    }

    /**
     * @param name the name on which to base the UUID
     * @return the generated type 3 (name-based) UUID.
     */
    public static UUID generateType3UUID(String name)
    {
        return TYPE_3_GENERATOR.generate(name);
    }

    /**
     * @return the canonical MAC address for this machine, or a dummy address if one could not be determined.
     */
    private static EthernetAddress getCanonicalMACAddress()
    {
        List<EthernetAddress> addresses = new ArrayList<>();

        try
        {
            for (Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements();)
            {
                NetworkInterface networkInterface = enumeration.nextElement();

                if (!networkInterface.isLoopback())
                {
                    byte[] address = networkInterface.getHardwareAddress();

                    if (address != null && address.length == 6)
                    {
                        try
                        {
                            addresses.add(new EthernetAddress(address));
                        } catch (Exception exception)
                        {
                            logger.error("Error obtaining hardware MAC address for interface '{}'", networkInterface.getName(), exception);
                        }
                    }
                }
            }
        } catch (SocketException exception)
        {
            logger.error("Error obtaining hardware MAC addresses:", exception);
        }

        Collections.sort(addresses);

        EthernetAddress address;
        if (addresses.isEmpty())
        {
            logger.warn("No network interface found, using a dummy address to generate UUID");
            address = EthernetAddress.constructMulticastAddress();
        } else
        {
            address = addresses.get(0);
        }

        logger.debug("Canonical MAC address: {}", address);
        return address;
    }
}
