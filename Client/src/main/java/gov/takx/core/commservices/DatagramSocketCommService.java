package gov.takx.core.commservices;

import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * UDP comm service.
 *
 */
class DatagramSocketCommService extends ACommService
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int port;
    private InetAddress address;
    private DatagramSocket socket;
    private final boolean client;
    private int readBufferSize = -1;

    DatagramSocketCommService(CommServiceType commType, int rBufferSize, ICommListener commListener)
    {
        super(commType);
        client = (CommServiceType.DATAGRAM_CLIENT == commType);
        readBufferSize = rBufferSize;

        if (commListener != null)
        {
            addCommPathListener(commListener);
        }
    }

    protected boolean openResources()
    {
        boolean result = false;
        name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());
        String addressAndPort[] = name.split(":");
        if (addressAndPort.length == 2)
        {
            try
            {
                address = InetAddress.getByName(addressAndPort[0]);
                port = Integer.parseInt(addressAndPort[1]);
            } catch (UnknownHostException e)
            {
                logger.error("Unable to find and open {}", name);
            }
        }

        if (port != 0)
        {
            try
            {
                if (address == null)
                {
                    address = InetAddress.getByName("localhost");
                }

                if (client)
                {
                    socket = new DatagramSocket();
                    socket.setBroadcast(true);              // Enable sending / receiving broadcast packets
                } else
                {
                    if (address.isMulticastAddress())
                    {
                        socket = new MulticastSocket(port);
                        socket.setReuseAddress(true);
                        ((MulticastSocket) socket).joinGroup(address);
                    } else
                    {
                        socket = new DatagramSocket(port, address);
                        socket.setBroadcast(true);              // Enable sending / receiving broadcast packets
                        //              socket.setSoTimeout(10000);             // UDP socket, on .receive() waits for 10 seconds, then continues
                    }
                }

                DatagramSocketCommIn myCommIn = new DatagramSocketCommIn(this, readBufferSize);
                myCommIn.startCommIn(socket);
                commIn = myCommIn;

                DatagramSocketCommOut myCommOut = new DatagramSocketCommOut(port, address);
                myCommOut.startCommOut(socket);
                commOut = myCommOut;

                result = true;
            } catch (SocketException e)
            {
                logger.error("Cannot open UDP port for {}", name);
            } catch (UnknownHostException e)
            {
                logger.error("Cannot retrive localhost address");
            } catch (IOException e)
            {
                logger.error("Error trying to create multicast socket for address: {} on port {}", address, port);
            }
        } else
        {
            logger.error("Need a UDP port to bind to");
        }

        return result;
    }

    protected void closeResources(boolean wasExpected)
    {
        if (socket != null)
        {
            socket.close();
            socket = null;
        }
    }
}
