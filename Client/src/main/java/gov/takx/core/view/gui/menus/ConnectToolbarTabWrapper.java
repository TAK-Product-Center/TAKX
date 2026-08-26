package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.PreconfiguredConnectionAdded;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.view.gui.connections.ConnectionMenuSupport;
import gov.takx.core.view.gui.connections.ConnectionMenuSupport.ConnectionInfo;
import gov.takx.core.view.gui.event.ProxyButtonEvent;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 */
@PseudoBean
public class ConnectToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SERIAL_PORT_KEY = KeyUtils.getUniqueKey("gov.raptor.SerialPort", "Serial Port");
    private static final String ETHERNET_SERVER_KEY = KeyUtils.getUniqueKey("gov.raptor.EthernetConnection", "Ethernet Server");
    private static final String ETHERNET_CLIENT_KEY = KeyUtils.getUniqueKey("gov.raptor.EthernetConnection", "Ethernet Client");

    @Inject
    private ConnectionMenuSupport connectionMenuSupport;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    private final Map<String, ToolbarButton> proxyConnectionButtons = new HashMap<>();

    @PostConstruct
    private void postConstruct()
    {
        uiEventBus.register(this);
        createTab(TabType.CONNECT, false); // Hidden by default
    }

    @Override
    protected void addComponents()
    {
        final var sortedConnections = new TreeSet<>(new ConnectionComparator());

        // Add all connection types that support manual creation
        connectionPluginRegistry.getPluginDefinitions()
                .stream()
                .filter(IConnectionPluginDefinition::isManualCreationAllowed)
                .map(def -> connectionMenuSupport.makeConnectionInfo(def))
                .forEach(sortedConnections::add);

        // Add the pre-configured connections
        connectionPluginRegistry.applyToPreconfiguredConnections(pc -> sortedConnections.add(connectionMenuSupport.makeConnectionInfo(pc)));

        sortedConnections.forEach(this::addButton);
    }

    /**
     * Handle hot deployed connection plugins.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        if (pluginAdded.pluginDefinition() instanceof IConnectionPluginDefinition connectionPluginDefinition
                && connectionPluginDefinition.isManualCreationAllowed())
        {
            addButton(connectionMenuSupport.makeConnectionInfo(connectionPluginDefinition));
        }
    }

    /**
     * Handle hot deployed preconfigured connection plugins.
     * @since 5.7
     */
    private void onPreconfiguredConnectionAdded(@Observes PreconfiguredConnectionAdded event)
    {
        addButton(connectionMenuSupport.makeConnectionInfo(event.preconfiguredConnection()));
    }

    /**
     * Add a button for the given connection {@code info}.
     * @since 5.7
     */
    private void addButton(ConnectionInfo info)
    {
        final String buttonText = info.getDisplayName();

        // Make the button available to be added to toolbar configurations. Since the CONNECT tab includes this
        // button type as a dynamic button, it will also be added to any CONNECT tab instances automatically.
        configurationManager.addReferenceButton(createButton(ButtonType.CONNECTION_PLUGIN,
                buttonText, info.getImage(), connectionMenuSupport.makeActionListener(info),
                "Click to create a " + buttonText + " connection."));
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void proxyConnection(ProxyButtonEvent event)
    {
        final String eventId = event.getId();

        if (event.isAddButton())
        {
            if (!proxyConnectionButtons.containsKey(eventId))
            {
                addProxyConnection(eventId, event.getName(), event.getIconPath(), event.getAction());
            } else
            {
                logger.warn("Tried to add a proxy {} connection that already exists", event.getName());
            }
        } else
        {
            if (proxyConnectionButtons.containsKey(eventId))
            {
                removeProxyConnection(eventId);
            } else
            {
                logger.warn("Tried to remove a proxy connection that doesn't exist, id = {}", eventId);
            }
        }
    }

    private void addProxyConnection(String id, String name, String iconPath, Action action)
    {
        final ToolbarButton button = createButton(ButtonType.CONNECTION_PLUGIN, name,
                connectionMenuSupport.getImage(iconPath, action.getClass().getClassLoader()),
                action, "Click to create a " + name + " connection.");

        proxyConnectionButtons.put(id, button);
    }

    private void removeProxyConnection(String id)
    {
        ToolbarButton button = proxyConnectionButtons.remove(id);
        if (button != null) configurationManager.removeReferenceButton(button, false);
    }

    private static final class ConnectionComparator implements Comparator<ConnectionInfo>
    {
        @Override
        public int compare(ConnectionInfo conn1, ConnectionInfo conn2)
        {
            final String key1 = KeyUtils.getUniqueKey(conn1.getFamily(), conn1.getType());
            final String key2 = KeyUtils.getUniqueKey(conn2.getFamily(), conn2.getType());

            if (key1.equals(SERIAL_PORT_KEY))
            {
                return -1;
            } else if (key2.equals(SERIAL_PORT_KEY))
            {
                return 1;
            } else if (key1.equals(ETHERNET_SERVER_KEY))
            {
                return -1;
            } else if (key2.equals(ETHERNET_SERVER_KEY))
            {
                return 1;
            } else if (key1.equals(ETHERNET_CLIENT_KEY))
            {
                return -1;
            } else if (key2.equals(ETHERNET_CLIENT_KEY))
            {
                return 1;
            } else
            {
                // Compare the names, but if the names are the same, use the unique key.
                final int compareTo = conn1.getDisplayName().compareTo(conn2.getDisplayName());

                return compareTo == 0
                        ? key1.compareTo(key2)
                        : compareTo;
            }
        }
    }
}
