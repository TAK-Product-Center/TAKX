package gov.takx.api.connection;

import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionManagerDelegate;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.api.plugin.connection.TooManyConnectionsException;
import gov.takx.api.registry.NoSuchPluginException;
import gov.takx.core.view.gui.event.ProxyButtonEvent;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Interface for the Connection Manager.  Methods are mostly for requisition of new connections
 * or editing/removing old ones.  (Manager is responsible for managing)
 *
 */
public interface IConnectionManager extends IConnectionManagerDelegate
{
    /**
     * Returns a list of active composed connection controllers.
     *
     * @return List of active connection controllers.
     */
    Collection<IComposedConnectionController> getConnectionControllers();

    /**
     * Create the "composed" connection controller.
     *
     * @param connectionFamily connection family
     * @param connectionType   connection type
     * @return the "composed" controller
     * @throws NoSuchPluginException       If a plugin with the provided family and type cannot be located
     * @throws TooManyConnectionsException If the maximum number of connections of the provided family and type has
     *                                     already been reached
     */
    IComposedConnectionController createConnectionController(String connectionFamily, String connectionType) throws NoSuchPluginException, TooManyConnectionsException;

    /**
     * Translates an IConnection unique key into its associated IComposedConnectionController.
     *
     * @param uniqueKey Unique key that represents the IConnection inside of the IComposedConnectionController.
     * @return Composed connection controller associated with the IConnection unique key
     * @throws NoSuchConnectionControllerException when the key is not registered with the connection manager
     * @since 1.7
     */
    @Nonnull
    IComposedConnectionController getControllerFromConnectionKey(String uniqueKey) throws NoSuchConnectionControllerException;

    /**
     * Add a connection: set its comm service and gateway delegates and notify all relevant listeners.  In general,
     * called as a result of an operator filling out the connection parameters and clicking on "Connect" for a
     * new connection.
     *
     * @param controller composed connection controller
     * @see #isManagedConnection(String)
     */
    void addActiveConnection(@Nonnull IComposedConnectionController controller);

    /**
     * Add a new connection using the configuration given.  Notify all relevant listeners and persist the connection.
     *
     * @param controller    composed connection controller
     * @param configuration Map of configuration parameters to install into the connection controller
     * @since 2.20
     */
    void addPreconfiguredConnection(@Nonnull IComposedConnectionController controller, Map<String, Object> configuration);

    /**
     * Load connections from a template file.
     *
     * @param template the template file.
     */
    void loadConnectionTemplate(@Nonnull File template);

    /**
     * Call to display a proxy connection.  A proxy connection is a connection handled outside of the connection
     * manager, created by the IAppPluginDelegate
     *
     * @param id    id of proxy connection
     * @param state status of proxy connection
     */
    void displayProxyConnection(@Nonnull String id, @Nonnull ConnectionState state);

    /**
     * Remove a proxy connection specified by id.
     *
     * @param id id of proxy connection
     */
    void removeProxyConnection(String id);

    /**
     * @return Collection of proxy button events
     */
    Collection<ProxyButtonEvent> getProxyConnections();

    /**
     * Logical name assigned to the group of map objects that the concrete implementation is responsible for
     * managing.  This is the logical name used to locate connection managers, not the value of getType().
     *
     * @param uniqueKey Unique key that represents the IConnectionPluginController
     * @return true, if a composed connection controller exists that is associated with the unique key
     */
    boolean exists(String uniqueKey);

    /**
     * How many active connections exist for the given composed connection controller.
     *
     * @param connectionFamily family
     * @param connectionType   type
     * @return Number of connections
     */
    int getConnectionCount(String connectionFamily, String connectionType);

    /**
     * Request to edit a connection by its key.
     *
     * @param uniqueKey Unique key that represents the IConnectionPluginController
     */
    void displayEditDialog(String uniqueKey);

    /**
     * Notification that the edit dialog was closed.
     *
     * @param uniqueKey Unique key that represents the IConnectionPluginController
     */
    void editDialogClosed(String uniqueKey);

    /**
     * Process the Open connection logic for a given connection.
     *
     * @param connectionController the "composed" connection controller
     * @since 2.0.0
     */
    void openConnection(@Nonnull IComposedConnectionController connectionController);

    /**
     * Add a connection manager listener.
     *
     * @param listener connection manager listener
     */
    void addConnectionManagerListener(@Nonnull IConnectionManagerListener listener);

    /**
     * Remove a IClientConnectionStateListener
     *
     * @param listener IClientConnectionStateListener to remove
     */
    void removeConnectionManagerListener(@Nonnull IConnectionManagerListener listener);

    /**
     * Return true if the connection is being managed by the connection manager.  Generally this will
     * mean that the connection has been added to the connection panel and persisted in the database.  Do not
     * infer anything here about the {@link ConnectionState} value of the connection.
     *
     * @param controllerKey connection controller unique key
     * @return {@code true} if the connection is tracked in the connection manager
     * @see #addActiveConnection(IComposedConnectionController)
     * @since 2.36
     */
    boolean isManagedConnection(String controllerKey);
}
