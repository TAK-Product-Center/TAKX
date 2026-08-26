package gov.takx.api.connection;

import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionPanel;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDelegate;
import jakarta.annotation.Nonnull;

/**
 * Contract for the Connection Controller (delegate/listener) for
 * {@link IConnectionPluginController}.
 *
 */
public interface IComposedConnectionController extends IRaptorObject, IConnectionPluginDelegate
{
    /**
     * Connection state, as represented by the composed IConnection.
     *
     * @return connection state.
     * @since 2.9
     */
    ConnectionState getState();

    /**
     * Gets the most current message for the associated connection state if a message exists. Otherwise {@code null} is returned
     *
     * @param connectionState The state that contains the message
     * @return The message
     */
    String getMessage(ConnectionState connectionState);

    /**
     * Return the plugin's connection controller managed by this composed controller.
     *
     * @return connection plugin controller
     */
    IConnectionPluginController getPluginConnectionController();

    /**
     * Get the connection panel associated with the managed connection.
     *
     * @return the connection panel
     */
    IConnectionPanel getConnectionPanel();

    /**
     * Trigger a shutdown of both the composed connection plugin controller and the composed connection.
     */
    void shutdown();

    /**
     * Trigger Invoked during the onProjectSaving() project lifecycle event.
     */
    void onProjectSaving();

    /**
     * This listener will be forwarded any connection state changes from the plugin connection
     *
     * @param listener listener to be notified of changes
     */
    void addConnectionStateListener(@Nonnull IConnectionManagerListener listener);

    /**
     * Remove connection state listener from this connection
     *
     * @param listener listener to be notified of changes
     */
    void removeConnectionStateListener(@Nonnull IConnectionManagerListener listener);

    /**
     * Get a metadata value declared on a connection plugin
     *
     * @param key          metadata key
     * @param defaultValue metadata value
     * @return the metadata value associated with the supplied key.
     */
    String getMetadataValue(String key, String defaultValue);

    /**
     * @return the connection name
     * @since 2.0.0
     */
    String getConnectionName();

    /**
     * Return the displayable name of the connection that is being managed by the implementation of this interface.
     * Display name should apply to something human read-able that an operator will see in a panel listing all of
     * the connections, and should be unique among other connections of the same family / type.
     *
     * @return connection display name
     * @since 2.0.0
     */
    String getConnectionDisplayName();
}
