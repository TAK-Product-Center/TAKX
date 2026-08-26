package gov.takx.core.view.gui.connections;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.connection.IConnectionManagerListener;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.utils.ConnectionTemplateManager;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.platform.ui.UIResources.ADD_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.EXPORT_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.IMPORT_ACTION_BUTTON_ICON;

/**
 * Panel that displays the list of connections and their status (connected/disconnected).
 */
@PseudoBean
public class ConnectionsPanel extends ATitledPanel implements IConnectionManagerListener, IRaptorLifecycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IConnectionManager connectionManager;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    private final Map<String, ConnectionInfo> connectionInfoMap = new HashMap<>(); // Thread safe via @InvokeOnEDT
    private final ConnectionEntityDefinition entityDefinition = new ConnectionEntityDefinition();
    private final EntityTableModel<ConnectionInfo> entityTableModel = new EntityTableModel<>(entityDefinition);

    public ConnectionsPanel()
    {
        super(ButtonType.CONNECTIONS.getDisplayText());
        initComponents();
        table.setModel(entityTableModel);

        table.getSummaryColumn().setVisible(true);
        table.getIconColumn().setVisible(true);
        table.getActionsColumn().setVisible(true);
        table.requireColumn(entityDefinition.stateColumn).setVisible(false);

        addButton.setIcon(ADD_ACTION_BUTTON_ICON);
        exportButton.setIcon(EXPORT_ACTION_BUTTON_ICON);
        importButton.setIcon(IMPORT_ACTION_BUTTON_ICON);

        var header = table.getTableHeader();
        header.addActionButton(addButton);
        header.addActionButton(exportButton);
        header.addActionButton(importButton);
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
        connectionInfoMap.get(connectionController.getUniqueKey()).setName(displayName);
    }

    @Override
    public void onConnectionAdded(String key, String displayName, String imageURL, ConnectionState connectionState, Action action)
    {
        var connectionInfo = connectionInfoMap.get(key);
        if (connectionInfo != null)
        {
            connectionInfo.setState(connectionState);
        } else
        {
            addConnection(new ConnectionInfo(key, displayName, connectionState, imageURL, action.getClass(), null));
        }
    }

    @Override
    public void onConnectionAdded(IRaptorObject connectionController, String displayName, ConnectionState connectionState)
    {
        final String uniqueKey = connectionController.getUniqueKey();
        var connectionInfo = connectionInfoMap.get(uniqueKey);
        if (connectionInfo != null)
        {
            connectionInfo.setState(connectionState);
        } else
        {
            connectionPluginRegistry
                    .getPluginDefinition(connectionController.getFamily(), connectionController.getType())
                    .ifPresent(pluginDefinition -> addConnection(new ConnectionInfo(uniqueKey, displayName, connectionState, pluginDefinition.getPanelIconPath(), pluginDefinition.getPluginClass(), null)));
        }
    }

    @Override
    public void onConnectionRemoved(final IRaptorObject controller)
    {
        removeConnection(controller.getUniqueKey());
    }

    @Override
    public void onConnectionRemoved(final String key)
    {
        removeConnection(key);
    }

    @Override
    public void onConnectionStateChanged(IRaptorObject connectionController, ConnectionState connectionState, String message)
    {
        var connectionInfo = connectionInfoMap.get(connectionController.getUniqueKey());
        if (connectionInfo != null)
        {
            connectionInfo.setState(connectionState);
            connectionInfo.setMessage(message);
        }
    }

    @InvokeOnEDT
    private void removeConnection(String key)
    {
        var connectionInfo = connectionInfoMap.remove(key);
        if (connectionInfo != null) entityTableModel.removeEntity(connectionInfo);
    }

    @InvokeOnEDT
    private void addConnection(ConnectionInfo connectionInfo)
    {
        connectionInfoMap.put(connectionInfo.getKey(), connectionInfo);
        entityTableModel.addEntity(connectionInfo);
        exportButton.setEnabled(entityTableModel.getRowCount() > 0);
    }

    /**
     * Check to see if a user double-clicked on a connection in the table.  If so, ask the connection manager to
     * request the connection be edited by it's name, as it appears in the panel.  The name will correspond to the
     * IPluginConnectionController method getName().
     *
     * @param event Mouse event
     */
    private void tableMouseClicked(MouseEvent event)
    {
        if (!event.isPopupTrigger() && event.getClickCount() > 1)
        {
            var point = event.getPoint();
            var connection = table.getEntityAtPoint(point);
            var column = table.getColumnAtPoint(point);
            if (connection != null && column == entityDefinition.getSummaryColumn())
            {
                var key = connection.getKey();

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
            }
        }
    }

    private void exportButtonActionPerformed()
    {
        entityDefinition.export(entityTableModel.getEntities(), this);
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

    private void addActionPerformed()
    {
        var menu = new ChooseConnectionTypePopupMenu();
        menu.show(addButton, 0, addButton.getHeight());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var scrollPane = new JScrollPane();
        table = new CommonEntityTable<>();
        addButton = new JButton();
        exportButton = new JButton();
        importButton = new JButton();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== scrollPane ========
        {
            scrollPane.setName("scrollPane");

            //---- table ----
            table.setRowSelectionAllowed(false);
            table.setTextLinesForRowHeight(2);
            table.setName("table");
            table.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    tableMouseClicked(e);
                }
            });
            scrollPane.setViewportView(table);
        }
        add(scrollPane, BorderLayout.CENTER);

        //---- addButton ----
        addButton.setText("Add");
        addButton.setToolTipText("Add a new Connection.");
        addButton.setName("addButton");
        addButton.addActionListener(e -> addActionPerformed());

        //---- exportButton ----
        exportButton.setText("Export");
        exportButton.setToolTipText("Export All Connections to a Template File");
        exportButton.setEnabled(false);
        exportButton.setName("exportButton");
        exportButton.addActionListener(e -> exportButtonActionPerformed());

        //---- importButton ----
        importButton.setText("Import");
        importButton.setToolTipText("Import one or more Connections from a Template File");
        importButton.setName("importButton");
        importButton.addActionListener(e -> importButtonActionPerformed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private CommonEntityTable<ConnectionInfo> table;
    private JButton addButton;
    private JButton exportButton;
    private JButton importButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
