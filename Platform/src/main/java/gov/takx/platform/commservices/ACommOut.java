package gov.takx.platform.commservices;

import gov.takx.api.commservices.ICommOut;
import gov.takx.api.plugin.commservices.IWritableCommPath;

import java.io.OutputStream;

/**
 * Abstract class for the ICommOut interface.  Basically just sets and unsets the OutputStream on start
 * and stop.  It will be necessary to implement the interface methods from
 * {@link IWritableCommPath} to do the custom writing of data to the
 * output stream.
 *
 * @since 2.0.0
 */
public abstract class ACommOut implements ICommOut
{
    protected OutputStream outputStream;

    /**
     * Stops the CommOut by setting the output stream to {@code null}.
     */
    @Override
    public void stopCommOut()
    {
        outputStream = null;
    }

    /**
     * Initializes the CommOut by injecting the output stream to be used for writing data.
     *
     * @param outputStream output stream
     */
    @Override
    public void startCommOut(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }
}
