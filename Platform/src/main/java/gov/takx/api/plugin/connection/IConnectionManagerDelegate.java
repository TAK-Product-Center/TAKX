package gov.takx.api.plugin.connection;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.Collection;
import java.util.Map;

/**
 * PseudoBean delegate for connection manager related methods.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused") // injected by plugins
@InjectableService
@NoPluginImplementations
public interface IConnectionManagerDelegate
{
    /**
     * Create a connection using the specified plugin family and type. A notification will be sent to
     * the onOpenConnection callback.
     * If the plugin in question does not provide implementation for this callback/events, this method will have no
     * effect. Note: this method will *not* attempt to reuse an existing connection of the specified family and type.
     *
     * @param family        Connection plugin family
     * @param type          Connection plugin type
     * @param configuration Map of key/value configuration parameters used to initialize the connection before
     *                      it attempts to actually create the connection
     * @param autoStart     whether or not to auto-start this connection when it is added.
     */
    void createAndAddConfiguredConnection(String family, String type, Map<String, Object> configuration, boolean autoStart) throws TooManyConnectionsException;

    /**
     * Open an existing connection, identified by its key. A notification will be sent to
     * the onOpenConnection callback.
     * If the plugin in question does not provide implementation for this callback/events, this method will have no
     * effect. Note: this method *will* attempt to reuse an existing connection of the specified family and type, and if
     * the connection does not exist, a {@link NoSuchConnectionControllerException} will be thrown.
     *
     * @param uniqueKey     Unique key that represents the IConnectionPluginController
     * @param configuration Map of key/value configuration parameters used to initialize the connection before
     *                      it attempts to actually create the connection
     * @param autoStart     whether or not to auto-start this connection when it is added.
     * @throws NoSuchConnectionControllerException if no plugin is found matching the provided unique key
     * @since 2.18
     */
    void openConfiguredConnection(String uniqueKey, Map<String, Object> configuration, boolean autoStart) throws NoSuchConnectionControllerException;

    /**
     * Edit an existing connected, identified by its key. A notification will be sent to the
     * onUpdateConfiguredConnection callback.
     * If the plugin in question does not provide implementation for this callback/events, this method will have no
     * effect.
     *
     * @param uniqueKey     Unique key that represents the IConnectionPluginController
     * @param configuration Map of key/value configuration parameters used to initialize the connection before
     *                      it attempts to actually create the connection
     * @throws NoSuchConnectionControllerException if no plugin is found matching the provided unique key
     * @since 2.18
     */
    void updateConfiguredConnection(String uniqueKey, Map<String, Object> configuration) throws NoSuchConnectionControllerException;

    /**
     * Returns a list of active connection plugin controllers.
     *
     * @return List of active connection plugin controllers.
     * @since 2.23
     */
    Collection<IConnectionPluginController> getConnectionPluginControllers();

    /**
     * Returns the display name of a connection, given the connection controller.  Returns {@code null} if for some
     * inexplicable reason no corresponding connection can be found.
     *
     * @param controller the connection controller
     * @return the display name set on the connection
     * @see IConnectionPluginDelegate#setConnectionDisplayName(String)
     * @since 2.23
     */
    String getDisplayName(IConnectionPluginController controller);
}
