package gov.takx.api.commservices;

import gov.takx.api.platform.IParser;
import gov.takx.api.plugin.commservices.ICommListener;
import gov.takx.platform.commservices.ACommIn;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.List;

/**
 * Interface for abstract {@link ACommIn}.  Recommended to extend the abstract
 * class rather than implement this interface
 *
 * @since 2.0.0
 */
public interface ICommIn
{
    /**
     * Startup the Comm In.  Default implementation will start the thread.
     *
     * @param input the input stream to act on.  Can be {@code null} if input stream needs to be created in the run method.
     */
    void startCommIn(InputStream input);

    /**
     * Stop the comm in and notify the listeners.
     *
     * @param wasExpected flag to indicate if this was an expected (true) or unexpected (false) comm termination
     */
    void stopCommIn(boolean wasExpected);

    /**
     * Shutdown on CommIn, do the cleanup, remove parsers and any other references.
     *
     * @param wasExpected flag to indicate if this was an expected (true) or unexpected (false) comm termination
     */
    void shutdownCommIn(boolean wasExpected);

    /**
     * Register a comm listener to bytes coming in and port closing.
     *
     * @param listener the comm listener
     */
    void registerCommListener(@Nonnull ICommListener listener);

    /**
     * Remove the comm listener.
     *
     * @param listener the comm listener
     */
    void removeCommListener(@Nonnull ICommListener listener);

    /**
     * Set the parsers to be notified of incoming byte data.
     *
     * @param parserList list of parsers
     */
    void setParsers(List<? extends IParser> parserList);
}
