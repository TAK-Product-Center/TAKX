package gov.takx.api.connection;

import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.connection.ConnectionState;

import javax.swing.Action;

/**
 * Listener to the connection manager.  Main purpose is to pass notification to UI elements that may be
 * listening to connection state changes.
 *
 */
public interface IConnectionManagerListener
{
    /**
     * Notification that a connection state has changed
     *
     * @param connectionController connection controller
     * @param connectionState      state of connection
     * @param message              Message to display next to the connection state. The user will also be notified with a message
     */
    void onConnectionStateChanged(IRaptorObject connectionController, ConnectionState connectionState, String message);

    /**
     * Notification that a connection has been added, referenced by the controller
     *
     * @param connectionController connection controller
     * @param displayName          display name for connection
     * @param connectionState      connection state
     */
    void onConnectionAdded(IRaptorObject connectionController, String displayName, ConnectionState connectionState);

    /**
     * Notification that a connection has been added, referenced by the connection controller key
     *
     * @param key             connection controller key
     * @param displayName     display name for connection
     * @param iconPath        path to connection icon
     * @param connectionState status of connection
     * @param action          Action associated with the added connection
     */
    void onConnectionAdded(String key, String displayName, String iconPath, ConnectionState connectionState, Action action);

    /**
     * Notification of a connection that has been removed referenced by controller
     *
     * @param connectionController connection controller
     */
    void onConnectionRemoved(IRaptorObject connectionController);

    /**
     * Notification that a connection has been removed, referenced by controller key
     *
     * @param key controller key
     */
    void onConnectionRemoved(String key);

    /**
     * Notification that a connection name has been changed
     *
     * @param connectionController connnection controller with name change.
     * @param displayName          display name for connection
     */
    void onConnectionNameChanged(IRaptorObject connectionController, String displayName);
}
