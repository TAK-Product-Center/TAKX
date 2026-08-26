package gov.takx.core.commservices;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommEventDelegate;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.commservices.ACommOut;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;

/**
 * Comm port data out. Contains implementations of interface methods exposed through the API to allow for
 * sending data out the port.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class CommOut extends ACommOut
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String sourceName;

    private static final Object syncObject = new Object();

    @Inject
    private ICommEventDelegate commEventDelegate;

    /**
     * Construct comm out object.  Port name is injected just for logging and debugging purposes.
     *
     * @param sourceName name of port "127.0.0.1:1024"
     * @see CommServiceType
     */
    protected CommOut(String sourceName)
    {
        this.sourceName = sourceName;
    }

    /**
     * Write an array of bytes to the output stream
     *
     * @param data   byte array
     * @param length number of bytes
     * @throws IOException exception on write failure.
     */
    @Override
    public void write(byte[] data, int length) throws IOException
    {
        if (outputStream != null)
        {
            commEventDelegate.publishCommOut(sourceName);

            synchronized (syncObject)
            {
                outputStream.write(data, 0, length);
                outputStream.flush();
            }
            logger.trace("Wrote {} bytes on {}", length, sourceName);
        } else
        {
            logger.info("Output stream is null when trying to write");
            throw new IOException("Cannot write to null output stream");
        }
    }

    /**
     * Write an array of bytes to the output stream
     *
     * @param data     data byte array
     * @param length   length number of bytes
     * @param location not used.
     * @throws IOException exception on write error
     */
    @Override
    public void write(byte[] data, int length, String location) throws IOException
    {
        write(data, length);
    }

    /**
     * Write a line of text using a print writer, similar to println.  Appends the CR and LF bytes.
     *
     * @param text line of text to write
     * @throws IOException exception on write error
     */
    @Override
    public void write(String text) throws IOException
    {
        if (outputStream != null)
        {
            commEventDelegate.publishCommOut(sourceName);

            synchronized (syncObject)
            {
                PrintWriter printWriter = new PrintWriter(outputStream, true);
                printWriter.println(text);
                printWriter.flush();
            }
            logger.trace("Wrote text = {}", text);
        } else
        {
            logger.info("Output stream is null when trying to write");
            throw new IOException("Cannot write to null output stream");
        }
    }

    @Override
    public String toString()
    {
        return "Comm path: " + sourceName;
    }
}
