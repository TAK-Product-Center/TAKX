package gov.takx.core.view.gui.drifter;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.drifter.DrifterController;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static gov.takx.core.view.gui.drifter.DrifterEntityDefinition.DELAY_ICON_NAME;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.ADD_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.PAUSE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.PLAY_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.REFRESH_ACTION_BUTTON_ICON;

/**
 * A swing panel for drifter configuration. Displays all configured objects in a table with appropriate buttons in a row action
 * column. Depends heavily on {@link CommonEntityTable} for rendering the table and actions.
 * 
 * @since 5.6
 */
@PseudoBean
public class DrifterPanel extends ATitledPanel implements DrifterController.DrifterConfigurationListener
{
    private static final Icon DELAY_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, DELAY_ICON_NAME, ACTION_BUTTON_IMAGE_SIZE, true);

    private final IPreferenceMonitor<SpeedUnits> speedMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, this::updateSummary);
    private final IPreferenceMonitor<BearingUnits> bearingMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, this::updateSummary);
    private final IPreferenceMonitor<NorthReference> northMonitor = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, this::updateSummary);

    private final DrifterEntityDefinition entityDefinition = new DrifterEntityDefinition();
    private final EntityTableModel<IMapObject> tableModel = new EntityTableModel<>(entityDefinition);

    @Inject
    private DrifterController controller;
    @Inject
    private IExtendedGlobeManager globeManager;

    /**
     * Construct a new panel with an empty Drifter table.
     */
    public DrifterPanel()
    {
        super("Drifter");
        initComponents();
        addButton.setIcon(ADD_ACTION_BUTTON_ICON);
        resumeButton.setIcon(PLAY_ACTION_BUTTON_ICON);
        pauseButton.setIcon(PAUSE_ACTION_BUTTON_ICON);
        delayButton.setIcon(DELAY_ICON);
        restartButton.setIcon(REFRESH_ACTION_BUTTON_ICON);

        table.setModel(tableModel);
        table.setRowHeight(55);

        table.getSummaryColumn().setVisible(true);
        table.getIconColumn().setVisible(true);
        table.getActionsColumn().setVisible(true);
        table.getVisibilityColumn().setVisible(true);
        table.requireColumn(entityDefinition.speedColumn).setVisible(false);

        table.getTableHeader().addActionButton(addButton);
        table.getTableHeader().addActionButton(resumeButton);
        table.getTableHeader().addActionButton(pauseButton);
        table.getTableHeader().addActionButton(delayButton);
        table.getTableHeader().addActionButton(restartButton);
    }

    @PostConstruct
    private void postConstruct()
    {
        controller.addDrifterConfigurationListener(this);
    }

    @Override
    @InvokeOnEDT
    public void added(@Nonnull IMapObject mapObject)
    {
        tableModel.addEntity(mapObject);
    }

    @Override
    @InvokeOnEDT
    public void removed(@Nonnull IMapObject mapObject)
    {
        tableModel.removeEntity(mapObject);
    }

    @Override
    public void releaseResources()
    {
        speedMonitor.dispose();
        bearingMonitor.dispose();
        northMonitor.dispose();
    }

    @InvokeOnEDT
    private void updateSummary(Object ignored)
    {
        tableModel.fireTableColumnUpdated(entityDefinition.getSummaryColumn());
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        tableModel.clear();
    }

    private void addActionPerformed()
    {
        controller.addConfiguration();
    }

    private void delayActionPerformed()
    {
        new SimulateTimeDrifterDialog().setVisible(true);
    }

    private void resumeActionPerformed()
    {
        controller.setPausedAllConfigurations(false);
    }

    private void pauseActionPerformed()
    {
        controller.setPausedAllConfigurations(true);
    }

    private void restartActionPerformed()
    {
        if (AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], "<html><center>Are you sure you want to restart all route-based Drifter configurations?</center></html>", "Confirm Restart All", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
        {
            controller.restartAll();
        }
    }

    private void tableMouseClicked(MouseEvent e)
    {
        var point = e.getPoint();
        var row = table.rowAtPoint(point);
        if (row != -1)
        {
            var col = table.columnAtPoint(point);
            if (col != -1)
            {
                var modelColumn = table.convertColumnIndexToModel(col);
                if (modelColumn == tableModel.getColumnIndex(entityDefinition.getSummaryColumn()))
                {
                    var mapObject = tableModel.getRow(table.convertRowIndexToModel(row));
                    var lastFocusedGlobe = globeManager.getLastFocusedGlobe();
                    if (lastFocusedGlobe != null) lastFocusedGlobe.goTo(mapObject.getLocation());
                }
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var scrollPane = new JScrollPane();
        table = new CommonEntityTable<>();
        addButton = new JButton();
        pauseButton = new JButton();
        resumeButton = new JButton();
        delayButton = new JButton();
        restartButton = new JButton();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== scrollPane ========
        {
            scrollPane.setName("scrollPane");

            //---- table ----
            table.setFocusable(false);
            table.setRowSelectionAllowed(false);
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
        addButton.setToolTipText("Add or Edit a Drifter Configuration");
        addButton.setName("addButton");
        addButton.addActionListener(e -> addActionPerformed());

        //---- pauseButton ----
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause All Drifter Configurations");
        pauseButton.setName("pauseButton");
        pauseButton.addActionListener(e -> pauseActionPerformed());

        //---- resumeButton ----
        resumeButton.setText("Resume");
        resumeButton.setToolTipText("Resume All Drifter Configurations");
        resumeButton.setName("resumeButton");
        resumeButton.addActionListener(e -> resumeActionPerformed());

        //---- delayButton ----
        delayButton.setText("Delay");
        delayButton.setToolTipText("Delay All Drifter Configurations");
        delayButton.setName("delayButton");
        delayButton.addActionListener(e -> delayActionPerformed());

        //---- restartButton ----
        restartButton.setText("Restart");
        restartButton.setToolTipText("Restart All Drifter Configurations");
        restartButton.setName("restartButton");
        restartButton.addActionListener(e -> restartActionPerformed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private CommonEntityTable<IMapObject> table;
    private JButton addButton;
    private JButton pauseButton;
    private JButton resumeButton;
    private JButton delayButton;
    private JButton restartButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
