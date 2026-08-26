package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.core.comms.takserver.TakServerConnection;
import gov.tak.core.comms.takserver.TakServerConnectionManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.takserver.TakServerConnectionEvent;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Panel for displaying a single TAK Server connection's status, name/URL, and control buttons to allow
 * editing, deleting, and selecting groups for the connection.
 *
 * @since 5.4
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ConnectionPanel extends JPanel implements IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon CONNECTED_ICON = UIResources.getFeatureIcon(PLATFORM_ICONS, "cloud_connected.png");
    private static final Icon DISCONNECTED_ICON = UIResources.getFeatureIcon(PLATFORM_ICONS, "cloud_disconnected.png");
    private static final Icon DELETE_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "delete.png");
    private static final Icon GROUPS_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "relations.png");

    private static final String NAME_AND_ADDRESS_FMT = "<html><font size=\"+1\">%s</font><br>%s</html>";

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private TakServerConnectionManager takServerManager;

    private TakServerConnection connection;
    private boolean updatingControls;

    /**
     * @param connection Connection to handle
     */
    ConnectionPanel(@Nonnull TakServerConnection connection)
    {
        initComponents();

        editButton.setIcon(UIResources.EDIT_ACTION_BUTTON_ICON);
        deleteButton.setIcon(DELETE_ICON);
        groupsButton.setIcon(GROUPS_ICON);

        updateControls(connection);
    }

    private void onServerConnectionEvent(@Observes TakServerConnectionEvent event)
    {
        logger.debug("got: {}", event);
        if (connection.equals(event.getConnection())) updateControls(event.getConnection());
    }

    @InvokeOnEDT
    private void updateControls(ITakServerConnection updatedConnection)
    {
        connection = (TakServerConnection) updatedConnection;
        final boolean connected = connection.isConnected();
        final boolean enabled = connection.isEnabled();

        updatingControls = true;
        statusIconLabel.setIcon(connected ? CONNECTED_ICON : DISCONNECTED_ICON);
        nameAndAddressLabel.setText(NAME_AND_ADDRESS_FMT.formatted(connection.getDisplayName(), connection.getUri()));
        enabledCheckBox.setSelected(enabled);
        groupsButton.setEnabled(enabled && connected);
        updatingControls = false;
    }

    private void groupsButtonActionPerformed()
    {
        new TakServerGroupsSelectionDialog(connection).setVisible(true);
    }

    private void editButtonActionPerformed()
    {
        final var detailsPanel = new TakServerConnectionDetailsPanel(connection, () -> detailsPanelHolder.removeAll());
        detailsPanelHolder.add(detailsPanel, new CellConstraints().xy(1, 1));
        revalidate();
        repaint();
    }

    private void deleteButtonActionPerformed()
    {
        ActionPopupFactory.yesNo("Are you sure you want to delete this connection?", deleteButton,
                () -> takServerManager.removeTakServerConnection(connection.getId())).show();
    }

    private void enabledCheckboxActionPerformed()
    {
        if (updatingControls) return;

        final var connectionConfig = connection.getConnectionConfig();
        connectionConfig.setEnabled(enabledCheckBox.isSelected());

        // Use executor service to get the connection logic off the UI thread
        executorService.execute(() -> {
            Thread.currentThread().setName("Update TAK Server connection");
            takServerManager.updateTakServerConnection(connection.getId(), connectionConfig);
        });
    }

    /**
     * @return This panel's wrapped connection.
     * @since 5.5.1
     */
    @Nonnull
    protected TakServerConnection getConnection()
    {
        return connection;
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        statusIconLabel = new JLabel();
        nameAndAddressLabel = new JLabel();
        enabledCheckBox = new JCheckBox();
        groupsButton = new JButton();
        editButton = new JButton();
        deleteButton = new JButton();
        detailsPanelHolder = new JPanel();
        var separator1 = new JSeparator();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default, 3*($lcgap, [default,30px])",
                "default, $lgap, 2*(default)"));

        //---- statusIconLabel ----
        statusIconLabel.setName("statusIconLabel");
        add(statusIconLabel, cc.xy(1, 1));

        //---- nameAndAddressLabel ----
        nameAndAddressLabel.setText("<html><font size=\"+1\">Name</font><br>https:some.ip:8334</html>");
        nameAndAddressLabel.setName("nameAndAddressLabel");
        nameAndAddressLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                editButtonActionPerformed();
            }
        });
        add(nameAndAddressLabel, cc.xy(3, 1));

        //---- enabledCheckBox ----
        enabledCheckBox.setFocusable(false);
        enabledCheckBox.setName("enabledCheckBox");
        enabledCheckBox.addActionListener(e -> enabledCheckboxActionPerformed());
        add(enabledCheckBox, cc.xy(5, 1));

        //---- groupsButton ----
        groupsButton.setToolTipText("Select active groups");
        groupsButton.setFocusable(false);
        groupsButton.setName("groupsButton");
        groupsButton.addActionListener(e -> groupsButtonActionPerformed());
        add(groupsButton, cc.xy(7, 1));

        //---- editButton ----
        editButton.setToolTipText("Edit this connection");
        editButton.setFocusable(false);
        editButton.setName("editButton");
        editButton.addActionListener(e -> editButtonActionPerformed());
        add(editButton, cc.xy(9, 1));

        //---- deleteButton ----
        deleteButton.setToolTipText("Delete this connection");
        deleteButton.setFocusable(false);
        deleteButton.setName("deleteButton");
        deleteButton.addActionListener(e -> deleteButtonActionPerformed());
        add(deleteButton, cc.xy(11, 1));

        //======== detailsPanelHolder ========
        {
            detailsPanelHolder.setName("detailsPanelHolder");
            detailsPanelHolder.setLayout(new FormLayout(
                    "[default,200dlu]:grow",
                    "fill:default:grow"));
        }
        add(detailsPanelHolder, cc.xywh(1, 3, 11, 1));

        //---- separator1 ----
        separator1.setName("separator1");
        add(separator1, cc.xywh(1, 4, 11, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel statusIconLabel;
    private JLabel nameAndAddressLabel;
    private JCheckBox enabledCheckBox;
    private JButton groupsButton;
    private JButton editButton;
    private JButton deleteButton;
    private JPanel detailsPanelHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
