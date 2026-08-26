package gov.takx.core.commservices;

import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.api.plugin.commservices.ICommPath;
import gov.takx.api.plugin.commservices.IWritableCommPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Serial port echo class.  Takes the comm path out in the constructor, and
 * registers self as a listener to the comm in.  Everything that comes in, goes
 * back out (kind of like in real life!).
 *
 */
class SerialPortEcho implements ICommListener
{
    private IWritableCommPath writableCommPath;
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected SerialPortEcho(IWritableCommPath commPath)
    {
        writableCommPath = commPath;
        logger.debug("Echoing data for port {}", commPath);
    }

    @Override
    public void commPathClosed(ICommPath commPath, boolean wasExpected)
    {
        writableCommPath = null;
    }

    @Override
    public void commPathStopped(ICommPath commPath, boolean wasExpected)
    {
    }

    /**
     * Echo the data back out the comm port
     *
     * @param data   byte array of data
     * @param length number of bytes in array
     * @param source String representation of source, can be {@code null} if source is unknown
     */
    @Override
    public void parseBytes(byte[] data, int length, String source)
    {
        logger.debug("Echoing {} bytes back out {}", length, source);
        try
        {
            if (writableCommPath != null)
            {
                writableCommPath.write(data, length, source);
            }
        } catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.error("Error echoing data back out the comm port", e);
            } else
            {
                logger.error("{} Error echoing data back out the comm port", e.getMessage());
            }
        }
    }
}
