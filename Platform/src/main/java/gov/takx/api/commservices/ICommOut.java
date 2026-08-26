package gov.takx.api.commservices;

import gov.takx.api.plugin.commservices.IWritableCommPath;
import gov.takx.platform.commservices.ACommOut;

import java.io.OutputStream;

/**
 * Interface for implementing the comm out portion of a comm service.  Recommendation is to extend the abstract
 * base class {@link ACommOut}.
 *
 * @since 2.0.0
 */
public interface ICommOut extends IWritableCommPath
{
    /**
     * Set the output stream for writing data out the comm path
     *
     * @param output the output stream
     */
    void startCommOut(OutputStream output);

    /**
     * Disable the comm out.
     */
    void stopCommOut();
}
