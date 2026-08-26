package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.core.comms.takserver.TakServerConnection;
import gov.tak.core.comms.takserver.TakServerConnectionManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.takserver.TakServerConnectionAdded;
import gov.takx.api.event.takserver.TakServerConnectionRemoved;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This panel offers controls to add/remove/edit TAK Server connections.
 *
 * @since 3.0
 */
@PseudoBean
public class TakServerPanel extends ATitledPanel
{
    public static final String TITLE = ButtonType.TAK_SERVER.getDisplayText();

    @Inject
    private TakServerConnectionManager takServerManager;

    private final Map<ITakServerConnection, ConnectionPanel> connectionPanelsMap = new ConcurrentHashMap<>();

    private TakServerConnectionDetailsPanel addConnectionPanel;

    /**
     * Currently unused, but retained description for posterity or eventual resurrection.
     */
    public String getDescription()
    {
        return "<html>The status of each of the TAK Server connections in the current project is displayed " +
                "below. Add or remove connections using the add and delete buttons.</html>";
    }

    public TakServerPanel()
    {
        super(TITLE);

        initComponents();

        addButton.setIcon(UIResources.ADD_ACTION_BUTTON_ICON);
        connectionsScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
        connectionsScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        // When one of the connection panels resizes (because of opening/closing the edit sub-panel), we need to revalidate
        connectionsPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateView();
            }
        });
    }

    @PostConstruct
    private void postConstruct()
    {
        // The connections will be empty when this panel is being embedded in the regular server display you
        // see when clicking the network status button in the bottom right. That gets initialized before
        // the servers are populated in the server manager.
        //
        // This is used for when you go through 'Settings > Network Preferences > Manage Server Connections'
        // in which case, the manager has already populated the server lists if any were registered in prior runs.
        takServerManager.getTakServerConnections().forEach(this::addConnection);
    }

    private void updateView()
    {
        revalidate();
        repaint();
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        connectionPanelsMap.values().forEach(ConnectionPanel::releaseResources);
        connectionPanelsMap.clear();
        connectionsPanel.removeAll();
    }

    private void onServerConnectionAdded(@Observes TakServerConnectionAdded event)
    {
        addConnection(event.getConnection());
    }

    private void onServerConnectionRemoved(@Observes TakServerConnectionRemoved event)
    {
        removeConnection(event.getConnection());
    }

    @InvokeOnEDT
    private void addConnection(@Nonnull ITakServerConnection connection)
    {
        // Avoid duplicate entries, which can happen with the instance of this panel that is created as the MainAppPanel
        // is created when opening a second project in a single session.
        if (!connectionPanelsMap.containsKey(connection))
        {
            ITakServerConnection connectionByUuid = takServerManager.findConnectionByUuid(connection.getId());
            final var connectionPanel = new ConnectionPanel((TakServerConnection) (connectionByUuid == null ? connection : connectionByUuid));

            // Determine where to place the connection panel based on the sorted order of
            // the connections creation time.
            var creationTime = connection.getCreationTime();
            int insertionIndex = 0;
            for (Component component : connectionsPanel.getComponents())
            {
                if (component instanceof ConnectionPanel cp)
                {
                    if (creationTime.compareTo(cp.getConnection().getCreationTime()) >= 0)
                    {
                        insertionIndex++;
                    } else
                    {
                        break;
                    }
                }
            }
            connectionPanelsMap.put(connection, connectionPanel);
            connectionsPanel.add(connectionPanel, insertionIndex);
            updateView();
        }
    }

    @InvokeOnEDT
    private void removeConnection(@Nonnull ITakServerConnection connection)
    {
        final var removed = connectionPanelsMap.remove(connection);
        if (removed != null)
        {
            connectionsPanel.remove(removed);
            removed.releaseResources();
            updateView();
        }
    }

    private void addButtonActionPerformed()
    {
        if (addConnectionPanel == null)
        {
            addConnectionPanel = new TakServerConnectionDetailsPanel(this::removeAddPanel);

            connectionsPanel.add(addConnectionPanel, 0); // Make it the first component
            addButton.setEnabled(false);
            updateView();
        }
    }

    private void removeAddPanel()
    {
        connectionsPanel.remove(addConnectionPanel);
        addConnectionPanel = null;
        addButton.setEnabled(true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var addRemovePanel = new JPanel();
        addButton = new JButton();
        connectionsScrollPane = new JScrollPane();
        connectionsPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, fill:default:grow"));

        //======== addRemovePanel ========
        {
            addRemovePanel.setName("addRemovePanel");
            addRemovePanel.setLayout(new FormLayout(
                    "default",
                    "fill:default"));

            //---- addButton ----
            addButton.setText("Add");
            addButton.setToolTipText("Add a new TAK Server connection");
            addButton.setVerticalTextPosition(SwingConstants.BOTTOM);
            addButton.setIconTextGap(1);
            addButton.setHorizontalTextPosition(SwingConstants.CENTER);
            addButton.setName("addButton");
            addButton.addActionListener(e -> addButtonActionPerformed());
            addRemovePanel.add(addButton, cc.xy(1, 1));
        }
        add(addRemovePanel, cc.xy(1, 1));

        //======== connectionsScrollPane ========
        {
            connectionsScrollPane.setName("connectionsScrollPane");

            //======== connectionsPanel ========
            {
                connectionsPanel.setName("connectionsPanel");
                connectionsPanel.setLayout(new VerticalLayout());
            }
            connectionsScrollPane.setViewportView(connectionsPanel);
        }
        add(connectionsScrollPane, cc.xy(1, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton addButton;
    private JScrollPane connectionsScrollPane;
    private JPanel connectionsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
