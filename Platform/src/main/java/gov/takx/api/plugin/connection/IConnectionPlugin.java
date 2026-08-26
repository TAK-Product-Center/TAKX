package gov.takx.api.plugin.connection;

import gov.takx.api.persist.IPersistableState;
import gov.takx.api.plugin.IAnnotatedPlugin;
import gov.takx.api.plugin.annotations.ConnectionPlugin;

/**
 * Contract for a Connection plugin.  The plugin must provide an {@link IConnectionPluginController},
 * which is expected to both manage the physical connection and the UI.
 * Recommendation is to override {@link AConnectionPlugin} rather than implementing this interface directly.
 *
 * @since 2.0.0
 */
public interface IConnectionPlugin extends IAnnotatedPlugin<ConnectionPlugin>
{
    /**
     * Create a new connection controller.  Note that a single controller can be managing multiple physical connections
     * but that there is a 1:1 relationship between the controller and the entry in the Connection Panel.
     *
     * @return non-null connection controller
     * @see IPersistableState#restore(String)
     */
    IConnectionPluginController createConnectionController();

    /**
     * Specifies whether or not this connection plugin supports editing while in a connected state.  Use case would
     * be a connection that does something like a poll on an interval, where the interval can be updated without
     * affecting the connection state.  If Edit is supported, the connection plugin controller will get a notification
     * the the operator has chosen to edit the connection so there is an opportunity to update the UI if
     * necessary.
     *
     * @return {@code true} if editing is supported, {@code false} otherwise.
     */
    default boolean isEditSupported()
    {
        return getPluginAnnotation().editSupported();
    }

    /**
     * Identifies the number of concurrent connections plugin intends to support.
     *
     * @return - The maximum number of connections supported by the plugin
     * @since 1.6
     */
    default int getMaxSupportedConnections()
    {
        return getPluginAnnotation().maxSupportedConnections();
    }
}
