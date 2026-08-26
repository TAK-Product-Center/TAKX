package gov.takx.api.plugin.commservices;

import gov.takx.api.plugin.connection.ICommServiceDelegate;

/**
 * Implement this listener interface and set as a listener when requesting that a server socket
 * type comm path be opened with the method addServerSocketListener using the IConnectionCommServiceDelegate.
 * <p/>
 * Many devices exist which engage in multiple short-duration connections: Connect, send a quick ack, disconnect.
 * This process might repeat every couple of seconds. To accommodate this fact, the TAKX Platform uses a design
 * conceptually similar to a thread pool. It is very important to respond to the appropriate event- be sure to review
 * the event documentation in {@link ICommListener}
 *
 * @see ICommServiceDelegate
 */
public interface ICommSocketListener extends ICommListener
{
    /**
     * Notification of that a client socket connection has been accepted
     *
     * @param commManager ICommManager interface for opened comm path
     */
    void socketClientAccepted(ICommManager commManager);
}
