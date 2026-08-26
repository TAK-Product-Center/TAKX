package gov.takx.core.view.gui.connections;

import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.PreconfiguredConnection;
import gov.takx.api.plugin.connection.TooManyConnectionsException;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.platform.ui.UIResources;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.JOptionPane;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Utility methods to support creating menus/buttons for connection creation.
 *
 * @since 2.20
 */
@ApplicationScoped
public class ConnectionMenuSupport
{
    private static final BufferedImage DEFAULT_CONNECTION_IMAGE = UIResources.getImage(PLATFORM_ICONS, "data.png");

    @Inject
    private SwingUIBinder swingUIBinder;

    @Inject
    private IConnectionManager connectionManager;

    /**
     * Construct a {@link ConnectionInfo} from a connection plugin definition.
     *
     * @param definition Plugin definition to extract from
     */
    public ConnectionInfo makeConnectionInfo(IConnectionPluginDefinition definition)
    {
        return new ConnectionInfo(definition);
    }

    /**
     * Construct a {@link ConnectionInfo} from a preconfigured connection definition.
     *
     * @param pc Preconfigured connection definition to extract from
     */
    public ConnectionInfo makeConnectionInfo(PreconfiguredConnection pc)
    {
        return new ConnectionInfo(pc);
    }

    /**
     * Construct the action listener to properly initiate a connection from a given connection plugin definition.
     *
     * @param definition The definition to process
     * @return ActionListener to perform the connection initiation
     */
    public ActionListener makeAddConnectionActionListener(final IConnectionPluginDefinition definition)
    {
        return actionEvent -> {
            try
            {
                IComposedConnectionController connectionController = connectionManager.createConnectionController(definition.getFamily(), definition.getType());

                boolean uiProviderPresent = swingUIBinder.isUIProviderPresent(connectionController.getPluginConnectionController().getClass());

                if (uiProviderPresent)
                {
                    new ConnectionDialog(connectionController).setVisible(true);
                } else
                {
                    swingUIBinder.showNoSwingUIDialog(definition.getName() + " connections");
                    connectionManager.removeConnection(connectionController.getUniqueKey(), false);
                }
            } catch (TooManyConnectionsException e)
            {
                showMaxConnectionsReachedDialog(definition.getName(), e.getMaxConnections());
            }
        };
    }

    /**
     * Construct the action listener for a preconfigured connection menu entry.
     *
     * @param preconfiguredConnection PreconfiguredConnection definition to work from
     * @return ActionListener to perform the actual connection operation
     */
    public ActionListener makePreconfiguredConnectionActionListener(PreconfiguredConnection preconfiguredConnection)
    {
        return actionEvent -> {
            final String family = preconfiguredConnection.getFamily();
            final String type = preconfiguredConnection.getType();

            try
            {
                final IComposedConnectionController connectionController = connectionManager.createConnectionController(family, type);
                final IConnectionPluginController connectionPluginController = connectionController.getPluginConnectionController();

                // Install the custom name
                connectionController.setConnectionDisplayName(preconfiguredConnection.getName());

                // Add the format to the configuration
                final Map<String, Object> configuration = preconfiguredConnection.getConfiguration();
                configuration.put("NAME", preconfiguredConnection.getName());
                configuration.put("FORMAT", preconfiguredConnection.getFormat());

                // If we are supposed to start the UI, then do that, otherwise crank up the connection
                if (preconfiguredConnection.isShowConnectionDialog())
                {
                    // Install the configuration, pass 'false' for autoStart so we just get the config installed
                    connectionPluginController.onOpenConnection(configuration, false);

                    if (swingUIBinder.isUIProviderPresent(connectionPluginController.getClass()))
                    {
                        new ConnectionDialog(connectionController).setVisible(true);
                    } else
                    {
                        swingUIBinder.showNoSwingUIDialog(type + " connections");
                        connectionManager.removeConnection(connectionController.getUniqueKey(), false);
                    }
                } else
                {
                    connectionManager.addPreconfiguredConnection(connectionController, configuration);
                }
            } catch (TooManyConnectionsException e)
            {
                showMaxConnectionsReachedDialog(preconfiguredConnection.getName(), e.getMaxConnections());
            }
        };
    }

    /**
     * Make the action listener according to the data in the given {@code ConnectionInfo}.
     * If {@code connectionInfo} contains a plugin definition, then this method will call
     * {@code makeAddConnectionActionListener}, or if it contains a preconfigured connection, this method
     * will call {@code makePreconfiguredConnectionActionListener}.
     *
     * @param connectionInfo Connection info to process
     * @return Appropriate action listener based on the content of the connection info
     * @see #makePreconfiguredConnectionActionListener(PreconfiguredConnection)
     * @see #makeAddConnectionActionListener(IConnectionPluginDefinition)
     */
    public ActionListener makeActionListener(ConnectionInfo connectionInfo)
    {
        return connectionInfo.isPreconfiguredConnection()
                ? makePreconfiguredConnectionActionListener(connectionInfo.preconfiguredConnection)
                : makeAddConnectionActionListener(connectionInfo.definition);
    }

    /**
     * Prepare the icon to be used for a connection. If no icon URL string is provided, use a default icon.
     *
     * @param urlString      The location of the plugin's icon; may be {@code null}.
     * @param resourceLoader ClassLoader to use to load resources
     * @return The icon to be used for the connection.
     */
    public BufferedImage getImage(String urlString, ClassLoader resourceLoader)
    {
        return internalGetImage(urlString, resourceLoader);
    }

    /**
     * Prepare the icon to be used for a connection. If no icon URL string is provided, use a default icon.
     *
     * @param urlString      The location of the plugin's icon; may be {@code null}.
     * @param resourceLoader ClassLoader to use to load resources
     * @return The icon to be used for the connection.
     */
    private static BufferedImage internalGetImage(String urlString, ClassLoader resourceLoader)
    {
        if (urlString == null || urlString.isEmpty()) return DEFAULT_CONNECTION_IMAGE; // Default if nothing specified

        final URL url = resourceLoader.getResource(urlString.startsWith("/") ? urlString : "/" + urlString);

        return UIResources.getImage(url);
    }

    /**
     * Show a dialog that the max number of connections has been reached for the specified connection plugin.
     *
     * @param type           The connection type.
     * @param maxConnections The max number of connections.
     */
    private void showMaxConnectionsReachedDialog(String type, int maxConnections)
    {
        AlwaysOnTopJOptionPane.showMessageDialog(
                "<html><center>The max number of " + type + " connections (" + maxConnections + ") <br>has already been reached.",
                "Max Connections Reached",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Class to hold distinguishing data regarding a connection based upon a plugin definition or a preconfigured
     * connection definition.  This information extract provides a mechanism for handling both types of connection
     * in a common manner.
     * <p>
     * This class is public, but the constructors are not.
     * Refer to {@link ConnectionMenuSupport#makeConnectionInfo(PreconfiguredConnection)} and
     * {@link ConnectionMenuSupport#makeConnectionInfo(IConnectionPluginDefinition)} for how to construct instances
     * of this class.
     */
    public static final class ConnectionInfo
    {
        private final String family;
        private final String type;
        private final String displayName;
        private final PreconfiguredConnection preconfiguredConnection;
        private final IConnectionPluginDefinition definition;
        private final BufferedImage image;
        private final BufferedImage panelImage;

        /**
         * Construct from a connection plugin definition.
         *
         * @param definition Plugin definition to extract from
         */
        private ConnectionInfo(IConnectionPluginDefinition definition)
        {
            preconfiguredConnection = null;
            this.definition = definition;

            family = definition.getFamily();
            type = definition.getType();
            displayName = definition.getName();
            image = definition.getIcon();
            panelImage = definition.getPanelIcon();
        }

        /**
         * Construct from a preconfigured connection definition.
         *
         * @param pc Preconfigured connection definition to extract from
         */
        private ConnectionInfo(PreconfiguredConnection pc)
        {
            preconfiguredConnection = pc;
            definition = null;

            family = pc.getFamily();
            type = pc.getType();
            displayName = pc.getName();

            final ClassLoader resourceLoader = pc.getClassLoader();

            image = internalGetImage(pc.getIconPath(), resourceLoader);
            panelImage = internalGetImage(pc.getPanelIconPath(), resourceLoader);
        }

        /**
         * @return {@code true} if this extract is from a preconfigured connection, {@code false} otherwise
         */
        public boolean isPreconfiguredConnection()
        {
            return preconfiguredConnection != null;
        }

        public String getFamily()
        {
            return family;
        }

        public String getType()
        {
            return type;
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public BufferedImage getImage()
        {
            return image;
        }

        public BufferedImage getPanelImage()
        {
            return panelImage;
        }
    }
}
