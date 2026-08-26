package gov.takx.api.plugin.connection;

import gov.takx.api.persist.IPersistableState;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.annotations.ConnectionPlugin;

import java.util.Map;

/**
 * Connection lifecycle listener from the perspective of a plugin's connection controller. Notification callbacks
 * are meant to take the place of the (formerly convoluted) series of events a plugin was required to implement
 * to maintain state between the plugin controller and connection. Now that the plugin controller manages both
 * the connection and the UI, the controller is expected to act on the lifecycle events to directly open/close
 * a connection.
 * <p>
 * It is recommended that logic that could result in blocking when opening a connection be done on a separate
 * thread since many of these notifications could come from the EDT.
 *
 * @since 2.0.0
 */
public interface IConnectionLifecycleListener extends IPersistableState, IRaptorObject
{
    /**
     * Controller is being notified that the operator is initiating a connection by clicking the "Connect" button in the
     * connection dialog. At this point, the {@code IConnectionPluginController} should perform the logic to open the
     * connection and on success, use its delegate ({@code IConnectionPluginDelegate}) to signal an open connection.
     * This method should stay as required to implement.
     */
    void onNewConnection();

    /**
     * Notification to open a connection based on a set of configuration parameters. Implement this method to support
     * things like connections read from a template or specified in a script. Pay attention to the autoStart
     * parameter: if it is {@code false}, simply store the configuration and don't open the connection.
     *
     * @param configuration configuration map
     * @param autoStart     whether to actually open the connection at this point
     */
    void onOpenConnection(Map<String, Object> configuration, boolean autoStart);

    /**
     * Auto-reconnect notice. Will not be called if the {@link AConnectionPlugin} does not specify that it
     * supports auto-reconnect in an annotation {@link ConnectionPlugin#autoReconnectSupported()}. Also, only
     * applicable for connections from restored projects where the property autoReconnectConnections is set
     * to true, and auto re-connect is specified for the family and type of the connection supporting auto re-connect
     * i.e.
     * <pre>
     *     <node name="AutoReconnectService">
     *         <map/>
     *         <node name="Acme Connection">
     *             <map>
     *                 <entry key="connectionKey" value="gov.takx.acme/AcmeV2Connection"/>
     *            </map>
     *         </node>
     *     </node>
     * </pre>
     * As this is applicable only to restored connections, the base class connection controller should also implement
     * store/restore so that the connection can be re-established with the properties it was originally create with.
     *
     * @since 2.0.0
     */
    void onAutoReconnect();

    /**
     * Notification that an operator has requested to edit connection parameters (not yet applied). Not required for
     * override whether or not edit is supported, but this method will be called just before the connection is
     * about to be edited if the connection plugin is annotated such that it supports editing
     *
     * @see IConnectionPlugin#isEditSupported()
     * @see ConnectionPlugin#editSupported()
     */
    void onEditConnection();

    /**
     * Notification that an operator has requested the connection be updated with new values to be gathered
     * from the UI, only valid if a {@code IPluginConnection} has specified that the connection is
     * editable. Not required for override if a connection is not editable.
     *
     * @see IConnectionPlugin#isEditSupported()
     * @see ConnectionPlugin#editSupported()
     */
    void onUpdateConnection();

    /**
     * Notification to update a connection with new values provided in the configuration map. Implement this method to
     * support things like updating a connection programmatically.
     *
     * @param configuration configuration map containing the parameters for the connection
     * @since 2.18
     */
    void onUpdateConfiguredConnection(Map<String, Object> configuration);

    /**
     * Notification of a request to close a connection. {@code IPluginConnectionController} is responsible
     * for closing the physical connection and setting the connection state to false with its delegate.
     */
    void onCloseConnection();

    /**
     * Connection is being shutdown, either deleted by the operator or because the TAKX project is closing.
     * Required for implementation mostly for good practices.
     */
    void onShutdown();
}
