package gov.takx.core.gateway;

import gov.takx.api.commservices.ICommService;
import gov.takx.api.plugin.commservices.IWritableCommPath;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Delegate injected into IGatewayController giving plugin developer ability to send framed
 * data out the comm port.
 *
 */
public class FramerDelegate implements IGatewayFramerDelegate
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String sourceName;
    private final ICommService commService;
    private final String deviceKey;

    protected FramerDelegate(String sourceName, ICommService service, String deviceKey)
    {
        this.commService = service;
        this.sourceName = sourceName;
        this.deviceKey = deviceKey;
    }

    @Override
    public String getDeviceUniqueKey()
    {
        return deviceKey;
    }

    /**
     * Write an array of bytes out the comm port.
     *
     * @param bytes  bytes to send to the comm port
     * @param length number of bytes to send
     */
    public void sendToCommOut(byte[] bytes, int length) throws IOException
    {
        if (!commService.isCommOpen())
        {
            commService.restart();
            if (logger.isDebugEnabled())
            {
                logger.debug("Reopened comm service writable comm = {}", commService.getWritableCommPath());
            }
        }

        IWritableCommPath comm = commService.getWritableCommPath();

        if (comm != null)
        {
            if (logger.isTraceEnabled()) logger.trace("Writing {} byes of data out port: {}", length, sourceName);
            comm.write(bytes, length);
        } else
        {
            logger.error("Error trying to write {} byes of data out port: {}", length, sourceName);
        }
    }

    /**
     * Write an array of bytes out the comm path.  Additional location parameter is for advanced routing (UDP)
     *
     * @param bytes    bytes to send to the comm port
     * @param length   number of bytes to send
     * @param location String value of the location this data must be sent (for advanced routing)
     */
    public void sendToCommOut(byte[] bytes, int length, String location) throws IOException
    {
        IWritableCommPath comm = commService.getWritableCommPath();

        if (comm != null)
        {
            if (logger.isTraceEnabled()) logger.trace("Writing {} byes of data out location {}", length, location);
            comm.write(bytes, length, location);
        }
    }

    /**
     * Writes a line of text out the comm path.  Like "println", will append a carriage return / line feed.
     *
     * @param text String to send to the comm port
     */
    public void sendToCommOut(String text) throws IOException
    {
        IWritableCommPath comm = commService.getWritableCommPath();

        if (comm != null)
        {
            if (logger.isTraceEnabled()) logger.trace("Writing {} bytes of text on: {}", text.length(), sourceName);
            comm.write(text);
        }
    }
}
