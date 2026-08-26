package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommEventDelegate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * UDP Comm port data out. Contains implementations of interface methods exposed through the API to allow for
 * sending data out the port.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class DatagramSocketCommOut implements IDatagramSocketCommOut
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private DatagramSocket socket;
    private final int port;
    private InetAddress address;

    @Inject
    private ICommEventDelegate commEventDelegate;

    /**
     * Constructor stores the port and the address to write to.
     *
     * @param port    port number
     * @param address IP Address
     */
    protected DatagramSocketCommOut(int port, InetAddress address)
    {
        this.port = port;
        if (address != null)
        {
            this.address = address;
        } else
        {
            try
            {
                this.address = InetAddress.getLocalHost();
            } catch (UnknownHostException e)
            {
                logger.error("Cannot retrieve localhost address");
            }
        }
    }

    /**
     * Initialized the CommOut by injecting the UDP socket.
     *
     * @param socket the UDP socket
     */
    @Override
    public void startCommOut(DatagramSocket socket)
    {
        this.socket = socket;
    }

    /**
     * Not used for UDP
     *
     * @param input unused
     */
    @Override
    public void startCommOut(OutputStream input)
    {
    }

    /**
     * Stop the CommOut by nulling out the socket.
     */
    @Override
    public void stopCommOut()
    {
        socket = null;
    }

    /**
     * API method implementation to write to output stream.
     *
     * @param data   bytes to write
     * @param length number of bytes to write
     * @throws IOException on write failure
     */
    @Override
    public void write(byte[] data, int length) throws IOException
    {
        if (socket != null && socket.isBound())
        {
            commEventDelegate.publishCommOut(address + ":" + port);

            DatagramPacket packet = new DatagramPacket(data, length, address, port);
            logger.debug("Sending {} bytes to {}:{}", length, address, port);
            socket.send(packet);
        } else
        {
            logger.error("Cannot write to UDP socket");
            throw new IOException("Cannot write to UDP socket");
        }
    }

    /**
     * API method implementation to write to output stream, with addition parameter of custom location.
     *
     * @param data     bytes to write
     * @param length   number of bytes to write
     * @param location custom location to write data to.
     * @throws IOException on write failure
     */
    @Override
    public void write(byte[] data, int length, String location) throws IOException
    {
        if (socket != null && socket.isBound())
        {
            String[] addressAndPort = location.split(":");
            if (addressAndPort.length > 0)
            {
                try
                {
                    InetAddress locationAddress = InetAddress.getByName(addressAndPort[0]);
                    int locationPort = addressAndPort.length == 2 ? Integer.parseInt(addressAndPort[1]) : port;

                    commEventDelegate.publishCommOut(locationAddress + ":" + locationPort);

                    DatagramPacket packet = new DatagramPacket(data, length, locationAddress, locationPort);
                    logger.debug("Sending {} bytes to {}:{}", length, locationAddress, locationPort);

                    socket.send(packet);
                } catch (UnknownHostException e)
                {
                    logger.error("Unable to find and open {}", location);
                }
            }
        } else
        {
            logger.error("Cannot write to UDP socket");
            throw new IOException("Cannot write to UDP socket");
        }
    }

    /**
     * API method to write data in plain text.
     *
     * @param text text data to write
     * @throws IOException on write failure
     */
    @Override
    public void write(String text) throws IOException
    {
        if (socket != null && socket.isBound())
        {
            commEventDelegate.publishCommOut(address + ":" + port);

            final byte[] bytes = text.getBytes();

            socket.send(new DatagramPacket(bytes, bytes.length, address, port));
        } else
        {
            logger.error("Cannot write to UDP socket");
            throw new IOException("Cannot write to UDP socket");
        }
    }
}
