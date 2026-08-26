package gov.takx.core.view.gui.bloodhound;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.icons.TakColor;
import gov.tak.api.location.ILocationTracker;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.event.MapObjectTrackingEvent;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.IconTableCellRenderer;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.entity.EntityTableModel;
import gov.takx.commons.swing.entity.common.CommonEntityTable;
import gov.takx.core.navigation.NavigationConfiguration;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.navigation.NavigateToDialog;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Dimension;

import static gov.takx.api.ui.event.MapObjectTrackingEvent.TrackingType.KEEP_IN_VIEW;
import static gov.takx.core.navigation.NavigationConstants.NAVIGATE_TO_ACTION_BUTTON_ICON;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_INNER_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_LOW_SPEED_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_COLOR;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_MIDDLE_ETA_THRESHOLD;
import static gov.takx.platform.preferences.PreferenceEntry.BLOODHOUND_OUTER_ETA_COLOR;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_BEARING_UNIT;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_NORTH_REFERENCE;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;
import static gov.takx.platform.ui.UIResources.ADD_ACTION_BUTTON_ICON;

/**
 * Swing panel for bloodhound configurations.
 *
 * @since 5.6
 */
@PseudoBean
public class BloodhoundPanel extends ATitledPanel implements NavigationManager.NavigationManagerListener
{
    private final IPreferenceMonitor<NorthReference> northReferenceMonitor = DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, ignored -> updateHeadingFormat());
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, ignored -> updateRangeFormat());
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor = DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, ignored -> updateHeadingFormat());

    private final IPreferenceMonitor<TakColor> middleColor = BLOODHOUND_MIDDLE_ETA_COLOR.newMonitor(TakColor.class, ignored -> generateEtaIcons());
    private final IPreferenceMonitor<TakColor> innerColor = BLOODHOUND_INNER_ETA_COLOR.newMonitor(TakColor.class, ignored -> generateEtaIcons());
    private final IPreferenceMonitor<TakColor> outerColor = BLOODHOUND_OUTER_ETA_COLOR.newMonitor(TakColor.class, ignored -> generateEtaIcons());
    private final IPreferenceMonitor<TakColor> unreachableColor = BLOODHOUND_LOW_SPEED_COLOR.newMonitor(TakColor.class, ignored -> generateEtaIcons());
    private final IPreferenceMonitor<Long> middleThreshold = BLOODHOUND_MIDDLE_ETA_THRESHOLD.newMonitor(Long.class, ignored -> updateStateIcons());
    private final IPreferenceMonitor<Long> innerThreshold = BLOODHOUND_INNER_ETA_THRESHOLD.newMonitor(Long.class, ignored -> updateStateIcons());

    private final BloodhoundEntityDefinition entityDefinition = new BloodhoundEntityDefinition();
    private final EntityTableModel<NavigationConfiguration> model = new EntityTableModel<>(entityDefinition);

    private ImageIcon unreachableIcon;
    private ImageIcon outerIcon;
    private ImageIcon innerIcon;
    private ImageIcon middleIcon;

    @Inject
    private ILocationTracker locationTracker;

    @Inject
    private NavigationManager navigationManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    /**
     * Construct a new panel with an empty Bloodhound table.
     */
    public BloodhoundPanel()
    {
        super("Bloodhound");
        initComponents();

        addButton.setIcon(ADD_ACTION_BUTTON_ICON);
        navButton.setIcon(NAVIGATE_TO_ACTION_BUTTON_ICON);
        generateEtaIcons();

        // Setup table ui/ux
        table.setModel(model);

        // Setup table ui/ux
        var rangeColumn = table.requireColumn(entityDefinition.rangeColumn);
        var etaColumn = table.requireColumn(entityDefinition.etaColumn);
        var headingColumn = table.requireColumn(entityDefinition.headingColumn);
        var stateColumn = table.requireColumn(entityDefinition.stateColumn);
        var dirColumn = table.requireColumn(entityDefinition.dirColumn);

        rangeColumn.setVisible(false);
        dirColumn.setVisible(false);
        headingColumn.setVisible(false);
        etaColumn.setVisible(false);
        table.getSummaryColumn().setVisible(true);
        table.getActionsColumn().setVisible(true);
        table.getNameColumn().setVisible(false);
        table.getNameColumn().setSortable(false);
        table.requireColumn(entityDefinition.toColumn).setVisible(false);
        table.requireColumn(entityDefinition.fromColumn).setVisible(false);
        table.requireColumn(entityDefinition.toIconColumn).setVisible(false);

        stateColumn.setCellRenderer(new IconTableCellRenderer<>(this::getEtaIcon));

        table.setRowHeight(75);
        table.moveColumn(table.getActionsColumn(), table.getColumnCount() - 1);
        table.moveColumn(stateColumn, 0);
        GUIUtils.setColumnWidths(CommonEntityTable.ICON_COLUMN_SIZE, stateColumn);

        // Setup table header
        table.getTableHeader().addActionButton(addButton);
        table.getTableHeader().addActionButton(navButton);
    }

    @PostConstruct
    private void postConstruct()
    {
        navigationManager.addConfigurationListener(this);
        uiEventBus.register(this);
    }

    @Override
    public void releaseResources()
    {
        northReferenceMonitor.dispose();
        rangeUnitsMonitor.dispose();
        bearingUnitsMonitor.dispose();
        uiEventBus.unregister(this);
    }

    /**
     * @param event the tracking event to check
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    @InvokeOnEDT
    public void onTrackingEvent(MapObjectTrackingEvent event)
    {
        if (event.getTrackingType() == KEEP_IN_VIEW)
        {
            var mapObject = event.getMapObjectController().getMapObject();
            for (var entity : model.getEntities())
            {
                if (entity.getSourceMapObject() == mapObject || entity.getTargetMapObject() == mapObject)
                {
                    model.fireTableCellUpdated(entity, entityDefinition.getActionsColumn());
                }
            }
        }
    }

    @Override
    @InvokeOnEDT
    public void added(@Nonnull NavigationConfiguration configuration)
    {
        model.addEntity(configuration);
    }

    @Override
    @InvokeOnEDT
    public void removed(@Nonnull NavigationConfiguration configuration)
    {
        model.removeEntity(configuration);
    }

    @InvokeOnEDT
    private void updateRangeFormat()
    {
        model.fireTableColumnUpdated(entityDefinition.getSummaryColumn());
    }

    @InvokeOnEDT
    private void updateStateIcons()
    {
        model.fireTableColumnUpdated(entityDefinition.stateColumn);
    }

    @InvokeOnEDT
    private void updateHeadingFormat()
    {
        model.fireTableColumnUpdated(entityDefinition.getSummaryColumn());
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        model.clear();
    }

    private void generateEtaIcons()
    {
        innerIcon = new ImageIcon(GraphicsUtils.createGlossyCircle(ImageUtils.MEDIUM_ICON_WIDTH, innerColor.getCurrentValue().getColor()));
        middleIcon = new ImageIcon(GraphicsUtils.createGlossyCircle(ImageUtils.MEDIUM_ICON_WIDTH, middleColor.getCurrentValue().getColor()));
        outerIcon = new ImageIcon(GraphicsUtils.createGlossyCircle(ImageUtils.MEDIUM_ICON_WIDTH, outerColor.getCurrentValue().getColor()));
        unreachableIcon = new ImageIcon(GraphicsUtils.createGlossyCircle(ImageUtils.MEDIUM_ICON_WIDTH, unreachableColor.getCurrentValue().getColor()));
        updateStateIcons();
    }

    private Icon getEtaIcon(long eta)
    {
        return eta < 0 ? unreachableIcon : eta < innerThreshold.getCurrentValue() ? innerIcon : eta < middleThreshold.getCurrentValue() ? middleIcon : outerIcon;
    }

    private void addActionPerformed()
    {
        new NavigateToDialog().setVisible(true);
    }

    private void navActionPerformed()
    {
        var dialog = new NavigateToDialog();
        dialog.setFrom((IPointMapObject) locationTracker.getMarkerMapObject());
        dialog.setVisible(true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var scrollPane = new JScrollPane();
        table = new CommonEntityTable<>();
        addButton = new JButton();
        navButton = new JButton();

        //======== this ========
        setMinimumSize(new Dimension(0, 54));
        setName("this");
        setLayout(new BorderLayout());

        //======== scrollPane ========
        {
            scrollPane.setName("scrollPane");

            //---- table ----
            table.setRowSelectionAllowed(false);
            table.setFocusable(false);
            table.setName("table");
            scrollPane.setViewportView(table);
        }
        add(scrollPane, BorderLayout.CENTER);

        //---- addButton ----
        addButton.setText("Add");
        addButton.setToolTipText("Add New Configuration");
        addButton.setName("addButton");
        addButton.addActionListener(e -> addActionPerformed());

        //---- navButton ----
        navButton.setText("Navigate");
        navButton.setToolTipText("Navigate From Self");
        navButton.setName("navButton");
        navButton.addActionListener(e -> navActionPerformed());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private CommonEntityTable<NavigationConfiguration> table;
    private JButton addButton;
    private JButton navButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
