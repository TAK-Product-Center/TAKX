package gov.takx.api.plugin.connection;

import gov.takx.api.commservices.ISerialPortManager;
import gov.takx.api.plugin.IPluginDelegate;

/**
 * Delegate injected into the IConnectionPluginController. Contains methods for getting the
 * {@code ICommServiceDelegate} and for setting the connection state, the latter being required to keep
 * the state of the connections panel in sync.
 *
 * @see IConnectionPluginController
 * @since 2.0.0
 */
public interface IConnectionPluginDelegate extends IPluginDelegate, ISerialPortManager
{
    /**
     * Get the ICommServiceDelegate object for use in opening / closing / managing the physical connections.
     *
     * @return the ICommServiceDelegate
     */
    ICommServiceDelegate getCommServiceDelegate();

    /**
     * Set the connection state. Since the {@code IConnectionPluginController} is the one responsible for
     * managing its custom connection, it is required to notify TAKX when a connection is opened or closed.
     *
     * @param connectionState enum representing the current state
     */
    void setConnectionState(ConnectionState connectionState);

    /**
     * Set the connection state. Since the {@code IConnectionPluginController} is the one responsible for
     * managing its custom connection, it is required to notify TAKX when a connection is opened or closed.
     * This method supports a message that will be displayed in the connection panel. If the message it is too long it
     * will be truncated.
     *
     * @param connectionState enum representing the current state
     * @param message         message to display next to the state, it will be truncated if its too long
     */
    void setConnectionState(ConnectionState connectionState, String message);

    /**
     * Call this method to set the display name specific to this connection.  This is the name that will
     * show up in the connection panel.  Useful when a connection plugin supports multiple connections so that
     * the operator can tell one connection apart from another in the connection panel.
     *
     * @param displayName connection display name.
     */
    void setConnectionDisplayName(String displayName);

    /**
     * Explicit call to remove the connection.  Will result in the connection being removed from the connection
     * panel and an eventual call to the IConnectionPluginController.onShutdown() method.  For use in something like
     * the onNewConnection() method where a connection is not successful.
     *
     * @see IConnectionPluginController#onShutdown()
     * @see IConnectionPluginController#onNewConnection()
     */
    void removeConnection();
}
