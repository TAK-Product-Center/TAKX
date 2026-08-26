package gov.takx.api.plugin.connection;

import gov.takx.api.plugin.IPluginDefinition;

import java.awt.image.BufferedImage;

/**
 * Contract for Connection Plugin definitions.  This includes those attributes that are unique to this type of plugin.
 *
 * @see IPluginDefinition
 * @since 2.0.0
 */
public interface IConnectionPluginDefinition extends IPluginDefinition<IConnectionPlugin>
{
    /**
     * @return whether or not auto-reconnect is supported.
     */
    boolean isAutoReconnectSupported();

    /**
     * Defines whether or not a connection plugin should contribute its connection to the toolbar, allowing the
     * operator to manually create a connection.  In use cases where this is not desired, connections can still be
     * opened by App plugins, or created on startup via connection templates.
     *
     * @return whether or not manual creation is supported.
     * @since 2.5
     */
    boolean isManualCreationAllowed();

    /**
     * Defines whether or not a connection plugin supports editing while in a connected state.  Defaults to false;
     *
     * @return whether or not edit is supported.
     */
    boolean isEditSupported();

    /**
     * Property for a connection plugin to specify the maximum number of connections that can be opened in a project.
     * If there is no hard limit, just specify some arbitrarily large number that couldn't reasonably be exceeded.
     * Default, if not specified, is 1.
     *
     * @return maximum number of connections allowed in a project.
     */
    int getMaxSupportedConnections();

    /**
     * Defines the path to the resource that is used on the connection panel LED and in the dropdown menu for
     * connections.  Not to be confused with {@link IPluginDefinition#getIconPath()}
     *
     * @return String path to the resource
     */
    String getPanelIconPath();

    /**
     * @return The image to use as an icon for this plugin, defaults to the unknown icon image if not specified or not found
     * @since 2.20
     */
    BufferedImage getPanelIcon();
}
