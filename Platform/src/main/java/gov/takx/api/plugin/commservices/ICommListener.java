package gov.takx.api.plugin.commservices;

import gov.takx.api.plugin.connection.ICommServiceDelegate;

/**
 * Interface to extend to register as a listener to an ICommManager.  ICommManager is returned when
 * an IConnection implementation requests creation of a new connection using the IConnectionCommServiceDelegate
 * method createCommPath
 * <p/>
 * When this listener is used in the context of a server socket, i.e. TAKX stood up a server socket that accepts
 * connections from remote clients, pay particular attention to the distinction between the commPathStopped and the
 * commPathClosed events. Similar in concept to a thread pool, TAKX doesn't immediately tear down the server when
 * a client disconnects. This is done by design since many devices engage in multiple short-length conversations. For
 * example: Connect, Send Ack, Disconnect, Repeat every 5 seconds. Instead of incurring the overhead of perpetually
 * standing up and tearing down resources, the server sticks around for a period of time. Therefore, to capture that
 * a client has disconnected, developers should rely on the commPathStopped event. To capture that the server has been
 * completely torn down, developers should rely on the commPathClosed event.
 *
 * @see ICommServiceDelegate
 */
public interface ICommListener
{
    /**
     * Notification of a closed comm path.  Comm path has already closed at this point.
     * <p/>
     * <b>Note:</b> When this event fires in the context of a server socket, i.e. TAKX stood up a server socket
     * that accepts connections from remote clients, this event indicates that the <b>server</b> is being torn down.
     *
     * @param commPath    comm path that was closed
     * @param wasExpected flag to indicate if this was an expected (true) or unexpected (false) comm path closure
     */
    void commPathClosed(ICommPath commPath, boolean wasExpected);

    /**
     * Notification of a stopped comm path.  Physical connection is dropped, but the comm path/service is
     * still available for restart through the ICommManager interface.
     * <p/>
     * <b>Note:</b> When this event fires in the context of a server socket, i.e. TAKX stood up a server socket
     * that accepts connections from remote clients, this event indicates that the <b>client</b> has disconnected.
     *
     * @param commPath    ICommPath service that was stopped.
     * @param wasExpected boolean value indicating whether or not it was expected.
     * @see ICommManager
     */
    void commPathStopped(ICommPath commPath, boolean wasExpected);

    /**
     * Parse a byte array of data of length bytes, originating from source location (if known)
     *
     * @param data   byte array of data
     * @param length number of bytes in array
     * @param source String representation of source, can be {@code null} if source is unknown
     *               (ie for UDP data"127.0.0.1:1234" for local IP address on port 1234)
     */
    void parseBytes(byte[] data, int length, String source);
}
