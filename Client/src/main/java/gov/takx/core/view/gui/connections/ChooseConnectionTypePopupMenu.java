package gov.takx.core.view.gui.connections;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.event.ProxyButtonEvent;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.SMALL_ICON_WIDTH;

/**
 * Creates a popup menu that displays all available connection types. Selecting a connection
 * type opens a {@link ConnectionDialog} for that connection type.
 *
 * @since 1.14
 */
@PseudoBean
public class ChooseConnectionTypePopupMenu extends JPopupMenu
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Order the connections by display name.
     */
    private static final Comparator<ConnectionMenuSupport.ConnectionInfo> conxNameOrder = Comparator.comparing(ConnectionMenuSupport.ConnectionInfo::getDisplayName);

    @Inject
    private ConnectionMenuSupport connectionMenuSupport;

    @Inject
    private IConnectionManager connectionManager;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    /**
     * Constructor.
     */
    public ChooseConnectionTypePopupMenu()
    {
    }

    /**
     * Here is where we build our connection menu from the connection plugins defined in the registry.
     */
    @PostConstruct
    private void postConstruct()
    {
        createMenuEntries();
    }

    /**
     * Create the popup menu entries from all our connection data.
     *
     * @since 2.20
     */
    private void createMenuEntries()
    {
        // Add the proxy connections contributed by app plugins
        for (ProxyButtonEvent event : connectionManager.getProxyConnections())
        {
            final Icon icon = makeIcon(connectionMenuSupport.getImage(event.getIconPath(), event.getPlugin().getClass().getClassLoader()));
            final Action eventAction = event.getAction();
            JMenuItem item = new JMenuItem(event.getName(), icon);
            item.addActionListener(eventAction);
            add(item);
        }

        for (final ConnectionMenuSupport.ConnectionInfo info : collectConnectionInfo())
        {
            final JMenuItem item = new JMenuItem(info.getDisplayName(), makeIcon(info.getImage()));
            final ActionListener actionListener = connectionMenuSupport.makeActionListener(info);

            item.addActionListener(actionListener);

            add(item);
        }
    }

    /**
     * @return An ordered set of the {@link ConnectionMenuSupport.ConnectionInfo} instances from which to construct our menu.
     * @since 2.20
     */
    private Set<ConnectionMenuSupport.ConnectionInfo> collectConnectionInfo()
    {
        final SortedSet<ConnectionMenuSupport.ConnectionInfo> sortedConnectionInfo = new TreeSet<>(conxNameOrder);

        // Get all available connection types.
        connectionPluginRegistry.getPluginDefinitions().stream()
                // Don't even THINK about adding the connection to the menu if user creation is not allowed.
                .filter(IConnectionPluginDefinition::isManualCreationAllowed)
                // And only allow connections that haven't reached their max count
                .filter(definition -> {
                    int maxConnections = definition.getMaxSupportedConnections();
                    if (maxConnections <= 0)
                    {
                        logger.warn("Illogical connection state for {}: connection reports 0 max connections allowed", definition.getUniqueKey());
                        return false;
                    }

                    return connectionManager.getConnectionCount(definition.getFamily(), definition.getType()) < maxConnections;
                })
                .forEach(definition -> sortedConnectionInfo.add(connectionMenuSupport.makeConnectionInfo(definition)));

        // Add the pre-configured connections
        connectionPluginRegistry.applyToPreconfiguredConnections(pc -> sortedConnectionInfo.add(connectionMenuSupport.makeConnectionInfo(pc)));

        return sortedConnectionInfo;
    }

    /**
     * Convert a {@code BufferedImage} into an {@code ImageIcon} for use in the menu.
     *
     * @param panelImage Image to process
     * @return Icon created from image
     * @since 2.20
     */
    private ImageIcon makeIcon(BufferedImage panelImage)
    {
        return ImageUtils.getScaledIcon(panelImage, SMALL_ICON_WIDTH, SMALL_ICON_HEIGHT);
    }
}
