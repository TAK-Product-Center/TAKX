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
 * CustomTesterCommService extends ACommService, a service only for testing specific test cases so
 * that connections to non-existent external service can be mocked.
 * @since 5.2
 */
class CustomTesterCommService extends ACommService
{
    private Socket ethernetSocket;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // idleTime is the current time in ms when this service was stopped, but not shutdown
    private long idleTime = 0;

    /**
     * Constructor for CustomTesterCommService.
     *
     * @param commListener The ICommListener to register as a Comm Path Listener, which will get notified of input bytes
     *                     and changes to the state of the comm path. {@code null} can be provided here if no listeners
     *                     is to be registered at this time. Listeners can be registered later using the
     *                     {@link #addCommPathListener(ICommListener)} method.
     */
    public CustomTesterCommService(int ignored, ICommListener commListener)
    {
        super(CommServiceType.CUSTOM);

        if (commListener != null)
        {
            addCommPathListener(commListener);
        }
    }

    public CustomTesterCommService(CommServiceType type, String commPathName)
    {
        super(type);
        name = commPathName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * note -currently mirroring Ethernet Service structure except not
     * opening Ethernet socket yet while we have no mock service.
     */
    @SuppressWarnings("unused")
    @Override
    protected boolean openResources()
    {
        boolean result = false;
        idleTime = 0;

        name = (String) properties.get(CommServiceConstants.PORT_NAME.toString());

        String[] addressAndPort = name.split(":", -1);
        if (addressAndPort.length >= 2)
        {
            try
            {
                InetAddress addr = InetAddress.getByName(addressAndPort[0]);
                int portNumber = Integer.parseInt(addressAndPort[1]);
                // if a mock service was running on our server, we could start an Ethernet Socket here,
                // but without that, we'll skip it since it will throw exception on connection refused
                result = true;
            } catch (UnknownHostException e)
            {
                logger.debug("Unknown host exception for port name = {}", name, e);
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

    /**
     * Called when a comm service is restarted... restart with current properties
     * @return false on error, true on success
     */
    @Override
    public boolean restart()
    {
        boolean status = startCommService(properties);
        if (status)
        {
            ((EthernetCommIn) commIn).restart();
        }
        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
