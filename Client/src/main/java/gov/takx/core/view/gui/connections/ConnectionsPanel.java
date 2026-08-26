package gov.takx.core.view.gui.connections;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.connection.IConnectionManagerListener;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.ConnectionTemplateManager;
import gov.takx.platform.utils.DataMonitorUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.MEDIUM_ICON_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Panel in TAKX that displays the list of connections and their status (connected/disconnected).
 *
 */
@PseudoBean
public class ConnectionsPanel extends ATitledPanel implements IConnectionManagerListener, IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IConnectionManager connectionManager;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    private final DefaultListModel<ConnectionInfo> model;

    public ConnectionsPanel()
    {
        super(ButtonType.CONNECTIONS.getDisplayText());
        initComponents();
        removeButton.setEnabled(false);
        exportButton.setEnabled(false);

        model = new DefaultListModel<>();
        listConnections.setModel(model);
        listConnections.setCellRenderer(new ConnectionInfoCellRenderer());

        listConnections.getSelectionModel().addListSelectionListener(e -> SwingUtilities.invokeLater(() -> removeButton.setEnabled(!listConnections.isSelectionEmpty())));

        addButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        removeButton.setIcon(UIResources.REMOVE_ACTION_BUTTON_ICON);
        exportButton.setIcon(UIResources.EXPORT_ACTION_BUTTON_ICON);
        importButton.setIcon(UIResources.IMPORT_ACTION_BUTTON_ICON);

        addButton.setArrowLocation(SplitButton.ArrowLocation.BOTTOM);
        addButton.setPopupMenuProvider(new PopupMenuProvider());
    }

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>The status of each connection in the current project is displayed below. Add or remove " +
                "connections using the add and delete buttons.  To edit a connection, double-click on its entry " +
                "in the panel below.</html>";
    }

    /**
     * Remove all connections from model/table, and remove self as IClientConnectionStateListener
     * from ClientConnectionManager.
     *
     * @param progressMonitor interface to progress monitor with option to update the progress text
     */
    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        model.removeAllElements();
        connectionManager.removeConnectionManagerListener(this);
    }

    /**
     * Add self as IClientConnectionStateListener to the ClientConnectionManager
     *
     * @param progressMonitor interface to progress dialog which can be updated for this method.
     */
    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        connectionManager.addConnectionManagerListener(this);
    }

    @Override
    public void onConnectionNameChanged(IRaptorObject connectionController, String displayName)
    {
        String uniqueKey = connectionController.getUniqueKey();

        for (int i = 0; i < model.size(); i++)
        {
            ConnectionInfo next = model.elementAt(i);

            if (next.key.equals(uniqueKey))
            {
                next.name = displayName;
                model.set(i, next);
                return;
            }
        }
    }

    @Override
    @InvokeOnEDT
    public void onConnectionAdded(String key, String displayName, String imageURL, ConnectionState connectionState, Action action)
    {
        for (int i = 0; i < model.size(); i++)
        {
            ConnectionInfo next = model.elementAt(i);

            if (next.key.equals(key))
            {
                next.status = connectionState.toString();
                next.message = connectionState.toString();
                listConnections.repaint(listConnections.getCellBounds(i, i));
                return;
            }
        }

        model.addElement(new ConnectionInfo(key, displayName, connectionState.toString(), imageURL, action.getClass(), connectionState.toString()));
        if (model.size() == 1) updateExportButton();
    }

    @InvokeOnEDT
    @Override
    public void onConnectionAdded(IRaptorObject connectionController, String displayName, ConnectionState connectionState)
    {
        final String uniqueKey = connectionController.getUniqueKey();

        for (int i = 0; i < model.size(); i++)
        {
            ConnectionInfo next = model.elementAt(i);

            if (next.key.equals(uniqueKey))
            {
                next.status = connectionState.toString();
                listConnections.repaint(listConnections.getCellBounds(i, i));
                return;
            }
        }

        final Optional<IConnectionPluginDefinition> optionalPD = connectionPluginRegistry.getPluginDefinition(connectionController.getFamily(), connectionController.getType());

        if (optionalPD.isPresent())
        {
            final IConnectionPluginDefinition definition = optionalPD.get();
            final String panelIconPath = definition.getPanelIconPath();

            model.addElement(
                    new ConnectionInfo(
                            uniqueKey,
                            displayName,
                            connectionState.toString(),
                            panelIconPath,
                            definition.getPluginClass(), null));
            if (model.size() == 1) updateExportButton();
        }
    }

    @Override
    @InvokeOnEDT
    public void onConnectionRemoved(final IRaptorObject controller)
    {
        String uniqueKey = controller.getUniqueKey();
        onConnectionRemoved(uniqueKey);
    }

    @Override
    @InvokeOnEDT
    public void onConnectionRemoved(final String key)
    {
        for (int i = 0; i < model.size(); i++)
        {
            ConnectionInfo next = model.elementAt(i);

            if (next.key.equals(key))
            {
                model.removeElementAt(i);
                if (model.isEmpty()) updateExportButton();
                return;
            }
        }
    }

    @Override
    public void onConnectionStateChanged(IRaptorObject connectionController, ConnectionState connectionState, String message)
    {
        String uniqueKey = connectionController.getUniqueKey();

        for (int i = 0; i < model.size(); i++)
        {
            ConnectionInfo next = model.elementAt(i);

            if (next.key.equals(uniqueKey))
            {
                next.status = connectionState.toString();
                next.message = message;
                model.set(i, next);
                return;
            }
        }
    }

    private void updateExportButton()
    {
        exportButton.setEnabled(!model.isEmpty());
    }

    /**
     * Check to see if a user double-clicked on a connection in the table.  If so, ask the connection manager to
     * request the connection be edited by it's name, as it appears in the panel.  The name will correspond to the
     * IPluginConnectionController method getName().
     *
     * @param event Mouse event
     */
    private void listConnectionsMouseClicked(MouseEvent event)
    {
        if (!event.isPopupTrigger() && event.getClickCount() > 1)
        {
            int index = listConnections.locationToIndex(event.getPoint());

            if (index >= 0)
            {
                String key = model.getElementAt(index).key;

                if (connectionManager.exists(key))
                {
                    //
                    // Connection manager knows about the connection, i.e. a physical connection has at one
                    // point already occurred during the currently executing TAKX session.  Therefore, we
                    // interpret the double-click as a request to open up the Edit Connection dialog associated
                    // with the connection that the user double-clicked on.
                    connectionManager.displayEditDialog(key);
                } else
                {
                    // A TAKX project was re-opened and the connection list was automatically populated with the
                    // connections that were in the list when the project was shutdown. During the currently executing
                    // TAKX session, the connection has not yet been physically connected. Therefore, we interpret
                    // the double-click as a request to re-establish the connection.
                    try
                    {
                        connectionManager.getControllerFromConnectionKey(key)
                                .getPluginConnectionController().onNewConnection();
                    } catch (NoSuchConnectionControllerException e)
                    {
                        logger.error("This situation should never occur; a connection made it to the connections panel without a connection controller being registered", e);
                    }
                }
            } else
            {
                removeButton.setEnabled(false);
            }
        }
    }

    /**
     * User selected the "-" button with the connection tab active and a connection selected in the panel.
     * Ask for confirmation, then request the ConnectionManager remove the connection corresponding
     * to the name in the table.
     */
    private void removeButtonActionPerformed()
    {
        int index = listConnections.getSelectedIndex();

        if (index >= 0)
        {
            ConnectionInfo connectionInfo = model.getElementAt(index);
            String selectedName = "";

            String selectedKey = connectionInfo.key;
            Object objName = connectionInfo.name;

            if (null != objName)
            {
                selectedName = " " + objName + " ";
            }

            int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete the" + selectedName + "connection?", "Delete connection", JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION)
            {
                connectionManager.removeConnection(selectedKey, true);
                removeButton.setEnabled(false);
            }
        } else
        {
            logger.warn("Connection remove button was set, even though no connections were set!");
            removeButton.setEnabled(false);
        }
    }

    /**
     * Operator chose to export connections in the Connections Panel to a connection template.  Use the
     * {@link ConnectionTemplateManager} to do the JAXB marshalling to the file.
     */
    private void exportButtonActionPerformed()
    {
        Map<String, IConnectionPluginController> controllerMap = new HashMap<>();
        Enumeration<ConnectionInfo> connections = model.elements();
        while (connections.hasMoreElements())
        {
            ConnectionInfo connectionInfo = connections.nextElement();
            try
            {
                IComposedConnectionController controller = connectionManager.getControllerFromConnectionKey(connectionInfo.key);
                IConnectionPluginController pluginController = controller.getPluginConnectionController();
                String name = controller.getConnectionDisplayName();
                if (pluginController.getRestorableParametersMap() != null)
                {
                    controllerMap.put(name, pluginController);
                }
            } catch (NoSuchConnectionControllerException e)
            {
                logger.warn("Error looking up connection controller to save to template, key = {}", connectionInfo.key);
            }
        }

        if (!controllerMap.isEmpty())
        {
            JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
            fileChooser.setDialogTitle("Export Connections to Template File");
            FileFilter xmlType = new FileNameExtensionFilter("Template File (.xml)", "xml");
            fileChooser.resetChoosableFileFilters();
            fileChooser.addChoosableFileFilter(xmlType);

            int option = fileChooser.showSaveDialog(this);
            if (option == JOptionPane.OK_OPTION)
            {
                File file = fileChooser.getSelectedFile();
                boolean addSuffix = !file.getAbsolutePath().toLowerCase().endsWith(".xml");
                final File xmlFile = (addSuffix) ? new File(file.getAbsolutePath() + ".xml") : file;
                ConnectionTemplateManager.saveConnectionTemplate(controllerMap, xmlFile);
            }
        } else
        {
            AlwaysOnTopJOptionPane.showMessageDialog(this, "No connections that support template creation found in list", "Connection Template", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Operator chose to import connections from a template.  Select a file to import from (should be an XML file).
     * Use the {@link ConnectionTemplateManager} to do the JAXB parsing and turn into an object containing all of
     * the connections to store.  Give the operator a chance to select specific connections, and whether or not they
     * should auto-start.
     */
    private void importButtonActionPerformed()
    {
        JFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setDialogTitle("Import Connections from a Template File");
        FileFilter xmlType = new FileNameExtensionFilter("Template File (.xml)", "xml");
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(xmlType);

        int option = fileChooser.showOpenDialog(this);
        if (option == JOptionPane.OK_OPTION)
        {
            File file = fileChooser.getSelectedFile();
            connectionManager.loadConnectionTemplate(file);
        }
    }

    private static class ConnectionInfo
    {
        String key;
        String name;
        String status;
        String message;
        ImageIcon connectedIcon;
        ImageIcon disconnectedIcon;
        ImageIcon connectingIcon;
        ImageIcon errorIcon;

        ConnectionInfo(String key, String name, String status, String iconURLString, Class<?> resourceSource, String message)
        {
            this.key = key;
            this.name = name;
            this.status = status;
            this.message = message;
            BufferedImage connectedImage = GraphicsUtils.createCircle(32, 200, Color.GREEN);
            BufferedImage disconnectedImage = GraphicsUtils.createCircle(32, 200, Color.GRAY);
            BufferedImage errorImage = GraphicsUtils.createCircle(32, 200, Color.RED);
            BufferedImage connectingImage = GraphicsUtils.createCircle(32, 200, Color.ORANGE);

            URL iconURL = iconURLString == null
                    ? UIResources.getUrl(PLATFORM_ICONS, "data.png")
                    : resourceSource.getResource(iconURLString.startsWith("/") ? iconURLString : '/' + iconURLString);

            BufferedImage connectionImage = iconURL != null ? ImageUtils.getScaledImage(iconURL, MEDIUM_ICON_WIDTH, MEDIUM_ICON_HEIGHT, false) : null;

            if (connectionImage != null)
            {
                // Should move this utility to a more general class...
                DataMonitorUtils.overlayImage(connectedImage, connectionImage);
                DataMonitorUtils.overlayImage(disconnectedImage, connectionImage);
                DataMonitorUtils.overlayImage(errorImage, connectionImage);
                DataMonitorUtils.overlayImage(connectingImage, connectionImage);
            }

            connectedIcon = new ImageIcon(connectedImage);
            disconnectedIcon = new ImageIcon(disconnectedImage);
            connectingIcon = new ImageIcon(connectingImage);
            errorIcon = new ImageIcon(errorImage);
        }

        public ImageIcon getStateIcon()
        {
            return switch (ConnectionState.valueOf(status))
            {
                case Connected -> connectedIcon;
                case Connecting -> connectingIcon;
                case Error -> errorIcon;
                case Disconnected -> disconnectedIcon;
            };
        }
    }

    private class ConnectionInfoCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            ConnectionInfo connectionInfo = (ConnectionInfo) value;
            Component result = new ConnectionInfoPanel(
                    connectionInfo.name,
                    connectionInfo.status,
                    connectionInfo.getStateIcon(),
                    connectionInfo.message);
            final Dimension maximumSize = result.getMaximumSize();
            result.setMaximumSize(new Dimension(connectionScrollPane.getWidth(), (int) maximumSize.getHeight()));
            result.setMinimumSize(new Dimension(connectionScrollPane.getWidth() + 70, (int) result.getMinimumSize().getHeight()));

            if (isSelected)
            {
                result.setBackground(list.getSelectionBackground());
            }

            return result;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panelButtons = new JPanel();
        addButton = new SplitButton();
        removeButton = new JButton();
        exportButton = new JButton();
        importButton = new JButton();
        var connectionPanel = new JPanel();
        connectionScrollPane = new JScrollPane();
        listConnections = new JList<>();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow"));

        //======== panelButtons ========
        {
            panelButtons.setName("panelButtons");
            panelButtons.setLayout(new FormLayout(
                    "3*(default, $lcgap), default",
                    "default"));

            //---- addButton ----
            addButton.setText("Add");
            addButton.setToolTipText("Add a new Connection.");
            addButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            addButton.setIconTextGap(1);
            addButton.setHorizontalTextPosition(SwingConstants.CENTER);
            addButton.setName("addButton");
            panelButtons.add(addButton, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- removeButton ----
            removeButton.setText("Delete");
            removeButton.setToolTipText("Delete the selected Connection. ");
            removeButton.setEnabled(false);
            removeButton.setHorizontalTextPosition(SwingConstants.CENTER);
            removeButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            removeButton.setIconTextGap(1);
            removeButton.setName("removeButton");
            removeButton.addActionListener(e -> removeButtonActionPerformed());
            panelButtons.add(removeButton, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- exportButton ----
            exportButton.setText("Export");
            exportButton.setToolTipText("Export Connections to a Template File");
            exportButton.setIconTextGap(1);
            exportButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            exportButton.setEnabled(false);
            exportButton.setHorizontalTextPosition(SwingConstants.CENTER);
            exportButton.setName("exportButton");
            exportButton.addActionListener(e -> exportButtonActionPerformed());
            panelButtons.add(exportButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //---- importButton ----
            importButton.setText("Import");
            importButton.setIconTextGap(1);
            importButton.setHorizontalTextPosition(SwingConstants.CENTER);
            importButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            importButton.setToolTipText("Import one or more Connections from a Template File");
            importButton.setName("importButton");
            importButton.addActionListener(e -> importButtonActionPerformed());
            panelButtons.add(importButton, cc.xy(7, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        }
        add(panelButtons, cc.xy(1, 1));

        //======== connectionPanel ========
        {
            connectionPanel.setName("connectionPanel");
            connectionPanel.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow"));

            //======== connectionScrollPane ========
            {
                connectionScrollPane.setName("connectionScrollPane");

                //---- listConnections ----
                listConnections.setName("listConnections");
                listConnections.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        listConnectionsMouseClicked(e);
                    }
                });
                connectionScrollPane.setViewportView(listConnections);
            }
            connectionPanel.add(connectionScrollPane, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        }
        add(connectionPanel, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private SplitButton addButton;
    private JButton removeButton;
    private JButton exportButton;
    private JButton importButton;
    private JScrollPane connectionScrollPane;
    private JList<ConnectionInfo> listConnections;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Popup menu provider for the add button that recreates the popup menu whenever the button is pressed in order to
     * get the correct connection options.
     *
     * @since 3.3
     */
    private static class PopupMenuProvider implements IPopupMenuProvider
    {
        @Override
        public JPopupMenu getPopupMenu()
        {
            return new ChooseConnectionTypePopupMenu();
        }
    }
}
