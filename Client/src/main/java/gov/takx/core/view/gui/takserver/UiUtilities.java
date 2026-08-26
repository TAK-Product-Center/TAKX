package gov.takx.core.view.gui.takserver;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.takx.commons.swing.SplitButton;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.util.Collection;
import java.util.function.Consumer;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Utility methods supporting common UI operations/elements working with TAKServer connections.
 *
 * @since 5.0
 */
public class UiUtilities
{
    private static final ImageIcon DOWNLOAD_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "cloud_download.png");

    /**
     * Build a button that allows the user to select TAKServer connections from which to perform a download operation.
     * If there is a single valid TAK Server connection, then a simple button is created.  If more than one, then a
     * split button is created to allow the user to select "All" or an individual connection to process.  If there are
     * no valid connections, then the button will be disabled.
     *
     * @param takServerManager To obtain current connections
     * @param selectionHandler Method to invoke with selected connection, will be passed {@code null} if the user
     *                         selects "All" connections
     * @param connectedTooltip Tooltip to show on button if there are any valid connections
     * @return properly configured button
     */
    @Nonnull
    public static JButton buildDownloadButton(@Nonnull ITakServerConnectionManager takServerManager,
                                              @Nonnull Consumer<ITakServerConnection> selectionHandler,
                                              @Nonnull String connectedTooltip)
    {
        // Get the active connections and check if we have at least one of them
        final Collection<? extends ITakServerConnection> connections = takServerManager.getTakServerConnections(true);
        final boolean isConnected = !connections.isEmpty();

        final JButton downloadButton;

        if (connections.size() <= 1)
        {
            // Single button for one connection.
            downloadButton = new JButton();
            downloadButton.addActionListener(e -> selectionHandler.accept(null));
        } else
        {
            // Dropdown button for multiple connections.
            SplitButton splitDownloadButton = new SplitButton();
            splitDownloadButton.setPopupMenuProvider(() -> {
                JPopupMenu menu = new JPopupMenu();

                // Option to query from all servers.
                JMenuItem allConnections = new JMenuItem("All connections");
                allConnections.addActionListener(me -> selectionHandler.accept(null));
                menu.add(allConnections);

                menu.add(new JPopupMenu.Separator());

                // Options to download from a specific server, disabled if not connected.
                for (ITakServerConnection connection : connections)
                {
                    JMenuItem singleConnection = new JMenuItem(connection.getDisplayName());
                    singleConnection.setEnabled(connection.isConnected());
                    singleConnection.addActionListener(me -> selectionHandler.accept(connection));
                    menu.add(singleConnection);
                }
                return menu;
            });
            downloadButton = splitDownloadButton;
        }

        downloadButton.setIcon(DOWNLOAD_ICON);
        downloadButton.setEnabled(isConnected);
        downloadButton.setToolTipText(isConnected ? connectedTooltip : "There are no active TAK Server connections");

        return downloadButton;
    }

    private UiUtilities()
    {
        // No instances
    }
}
