package gov.takx.core.commservices;

import gov.takx.api.plugin.commservices.ICommSocketListener;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

/**
 * Comm service for client connections to our server socket that is listening on a specified port.
 * This service can be stopped without being shutdown, such as in the event of an unexpected
 * close.  In this case, the service will be kept around for a period of time in case a client
 * re-connects on the same port/address and it will be "reused".
 *
 * @see ServerSocketListener
 */
class ServerSocketCommService extends ACommService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int port;
    private Socket socket;

    private InputStream commInInputStream;
    private ICommSocketListener socketListener;

    // idleTime is the current time in ms when this service was stopped, but not shutdown
    private long idleTime = 0;

    ServerSocketCommService(Socket socket, ICommSocketListener socketListener, CommServiceType commServiceType)
    {
        super(commServiceType);
        this.socket = socket;
        this.socketListener = socketListener;
    }

    @Override
    protected boolean openResources()
    {
        boolean result = false;
        idleTime = 0;

        String portString = (String) properties.get(CommServiceConstants.PORT_NAME.toString());
        if (portString != null && !portString.isEmpty()) port = Integer.parseInt((portString));
        String clientAddress = (String) properties.get(CommServiceConstants.PORT_ADDRESS.toString());
        name = clientAddress + ":" + port;

        logger.debug("Starting ServerSocketCommService name:  {}", name);

        try
        {
            logger.debug("Server Socket Comm Service on port {} for {}", port, clientAddress);

            commOut = new CommOut(name);
            commOut.startCommOut(socket.getOutputStream());

            // If this service is simply being re-started, the CommIn will already be there
            if (commIn == null) commIn = new EthernetCommIn(this, -1);
            commInInputStream = socket.getInputStream();

            if (socketListener != null)
            {
                socketListener.socketClientAccepted(this);
                commIn.registerCommListener(socketListener);
            }

            logger.debug("Success starting ServerSocketCommService name:  {}", name);

            result = true;
        } catch (IOException e)
        {
            logger.error("Cannot acquire socket connection for name:  {}", name);
        } catch (NullPointerException e)
        {
            //
            // [CORE-305]  The flaky network at Trident Spectre exercises has proven that the socket can be lost
            // between being opened inside of ServerSocketListener and being invoked here!  This catch protects us
            // from flaky networks.
            logger.error("Server Socket Comm Service on port {} for {} lost comms unexpectedly", port, clientAddress);
        }

        // ServerSocketListener will manually start the commIn by calling startCommIn.
        // RPTR-957.  Opening too early will start reading data before parsers are injected.

        return result;
    }

    /**
     * Stop the comm service but don't notify the comm services manager... we don't want it to be removed
     * from the list of comm services just yet, in the case where the socket was closed unexpectedly.
     */
    @Override
    public void commInterrupted()
    {
        stopCommService(false);
    }

    /**
     * Stop the comm service.  This method is overridden in this comm service, because in the case of
     * server sockets that were closed unexpectedly, we may want to keep them around for a little while
     * in case another connection is made from the same ip address and port.
     *
     * @param wasExpected boolean value true if manually stopped, false if connection was dropped or on error.
     */
    @Override
    public void stopCommService(boolean wasExpected)
    {
        isRunning = false;
        if (properties != null)
        {
            properties.clear();
            properties = null;
        }

        if (commIn != null)
        {
            commIn.stopCommIn(wasExpected);
            if (wasExpected)
            {
                commIn.shutdownCommIn(true);
                commIn = null;
            } else if (logger.isDebugEnabled())
            {
                logger.debug("Stopped ServerSocketCommService without shutting it down");
            }
        }

        closeResources(wasExpected);    // dispose the socket.

        if (commOut != null)
        {
            commOut.stopCommOut();
            commOut = null;
        }

        idleTime = System.currentTimeMillis();
    }

    /**
     * This method starts the comm in thread and initiates the wait for data. This was broken out of
     * the {@code openResources} method so that the DeviceGateway would have a chance to send the
     * parsers to CommIn BEFORE CommIn started reading data.
     * <p>
     * NOTE: Might need to do this for the other comm services as well, but it was a particular issue
     * for server sockets that might be opened, receive all their data and be closed again before the
     * parsers ever got there.  See RPTR-957
     */
    protected void startCommIn()
    {
        if (commIn != null) commIn.startCommIn(commInInputStream);
    }

    @Override
    protected void closeResources(boolean wasExpected)
    {
        if (socket != null)
        {
            try
            {
                socket.close();
                logger.debug("Closed ServerSocketCommService port name: {}.  wasExpected = {}", name, wasExpected);
            } catch (IOException e)
            {
                logger.error("IOException", e);
            }
            socket = null;
        }
    }

    /**
     * Setter method for socket, which is needed since this service can be restarted with a new socket.
     *
     * @param socket the Socket
     */
    protected void setSocket(Socket socket)
    {
        this.socket = socket;
    }

    long getIdleTime()
    {
        return idleTime;
    }

    @Override
    public boolean restart()
    {
        ((EthernetCommIn) commIn).restart();
        return true;
    }

    /**
     * Shutdown the service, which is currently idle
     */
    void shutdownIdleService()
    {
        if (idleTime > 0)
        {
            if (commIn != null)
            {
                commIn.shutdownCommIn(false);
                commIn = null;
            }
            socketListener = null;
        } else
        {
            logger.warn("Trying to shutdown an idle service which is NOT idle.");
        }
    }
}
