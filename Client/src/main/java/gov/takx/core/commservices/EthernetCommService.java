package gov.takx.core.commservices;

import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Ethernet port service.  Note:  Name of port is ipaddress:port or ipaddress:port:id.
 * E.g., 127.0.0.1:80, or 127.0.0.1:80:MyDevice
 *
 */
class EthernetCommService extends ACommService
{
    private Socket ethernetSocket;
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // idleTime is the current time in ms when this service was stopped, but not shutdown
    private long idleTime = 0;

    private int bufferSize;

    /**
     * Constructor for EthernetCommService, which is really a TCP/IP Comm Service.
     *
     * @param bufferSize   The max buffer size of each TCP/IP read.  A value of -1 implies the default of 1024.
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state of the comm path. {@code null} can be provided here if no listener
     *                     is to be registered at this time. Listeners can be registered later using the
     *                     {@link this#addCommPathListener(ICommListener)} method.
     */
    EthernetCommService(int bufferSize, ICommListener commListener)
    {
        super(CommServiceType.CLIENT_SOCKET);
        this.bufferSize = bufferSize;

        if (commListener != null)
        {
            addCommPathListener(commListener);
        }
    }

    protected boolean openResources()
    {
        boolean result = false;
        idleTime = 0;

        name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());

        String addressAndPort[] = name.split(":");
        if (addressAndPort.length >= 2)
        {
            try
            {
                InetAddress addr = InetAddress.getByName(addressAndPort[0]);
                int portNumber = Integer.parseInt(addressAndPort[1]);
                ethernetSocket = new Socket(addr, portNumber);

                // Since this service can be stopped and restarted, commIn may still be around.
                if (commIn == null) commIn = new EthernetCommIn(this, bufferSize);
                commIn.startCommIn(ethernetSocket.getInputStream());

                commOut = new CommOut(name);
                commOut.startCommOut(ethernetSocket.getOutputStream());

                result = true;
                logger.debug("Successful opening resources for ethernet port {}", name);
            } catch (UnknownHostException e)
            {
                logger.debug("Unknown host exception for port name = {}", name, e);
            } catch (IOException e)
            {
                logger.debug("Cannot acquire socket connection for port name = {}", name, e);
            }
        } else
        {
            logger.error("Illegal Ethernet port name = {}.  Should be <ip_address>:<port> or <ip_address>:<port>:<id>.", name);
        }

        return result;
    }

    /**
     * Stop the comm service but don't notify the comm services manager... we don't want it to be removed
     * from the list of comm services just yet, in the case where the socket was closed unexpectedly.
     */
    @Override
    public void commInterrupted()
    {
        if (!keepAlive)
        {
            terminationManager.terminateCommService(this);
        }
        stopCommService(false);
    }

    /**
     * Stop the comm service.  This method is overridden in this comm service, because in the case of
     * server sockets that were closed unexpectedly, we may want to keep them around for a little while
     * in case another connection is made from the same ipaddress and port.
     *
     * @param wasExpected boolean value true if manually stopped, false if connection was dropped or on error.
     */
    @Override
    public void stopCommService(boolean wasExpected)
    {
        isRunning = false;

        if (commIn != null)
        {
            commIn.stopCommIn(wasExpected);
            if (wasExpected || !keepAlive)
            {
                commIn.shutdownCommIn(wasExpected);
                commIn = null;
            }
        }

        closeResources(wasExpected);

        if (commOut != null)
        {
            commOut.stopCommOut();
            commOut = null;
        }

        idleTime = System.currentTimeMillis();
    }

    /////////////////////////////////////////
    // PROTECTED methods

    /**
     * Called when a comm service is restarted... restart with current properties
     *
     * @return false on error, true on success
     */
    public boolean restart()
    {
        boolean status = startCommService(properties);
        if (status)
        {
            ((EthernetCommIn) commIn).restart();
        }
        return status;
    }

    protected void closeResources(boolean wasExpected)
    {
        if (ethernetSocket != null)
        {
            try
            {
                ethernetSocket.close();
                if (logger.isDebugEnabled()) logger.debug("Closed resources for ethernet port {}", name);
            } catch (IOException e)
            {
                logger.error("IOException: ", e);
            }
            ethernetSocket = null;
        }
    }

    /**
     * Setter method for socket, which is needed since this service can be restarted with a new socket.
     *
     * @param socket the Socket
     */
    protected void setSocket(Socket socket)
    {
        this.ethernetSocket = socket;
    }

    protected long getIdleTime()
    {
        return idleTime;
    }

    /**
     * Shutdown the service, which is currently idle
     */
    protected void shutdownIdleService()
    {
        if (properties != null)
        {
            properties.clear();
            properties = null;
        }

        if (idleTime > 0)
        {
            commIn.shutdownCommIn(false);
            commIn = null;
        } else
        {
            logger.warn("Trying to shutdown an idle service which is NOT idle.");
        }
    }
}
