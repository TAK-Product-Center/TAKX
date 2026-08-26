package gov.takx.core.commservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.invoke.MethodHandles;

/**
 * Empty output stream used to capture a plugins inadvertant attempt to write output across
 * a GenericCommService that was not provided a valid (non-null) output stream.
 *
 */
public class NullOutputStream extends OutputStream
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void close()
    {
    }

    public void flush()
    {
    }

    public void write(byte[] b)
    {
        logger.warn("Attempt to write via a NullOutputStream.  CommPath does not support output.");
    }

    public void write(byte[] b, int off, int len)
    {
        logger.warn("Attempt to write via a NullOutputStream.  CommPath does not support output.");
    }

    public void write(int b)
    {
        logger.warn("Attempt to write via a NullOutputStream.  CommPath does not support output.");
    }
}
