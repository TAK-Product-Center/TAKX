package gov.takx.core.view.gui.routes;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IRoute;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ColorChooserUtils;
import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.swing.SplitButton;
import gov.takx.core.globe.ElevationProfileService;
import gov.takx.core.routes.RouteExportService;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import net.java.dev.colorchooser.ColorChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static gov.takx.api.mapobject.IMapObject.NAME;
import static gov.takx.api.shapes.IRoute.DIRECTION;
import static gov.takx.api.shapes.IRoute.DISPLAY_NAME;
import static gov.takx.api.shapes.IRoute.IS_A_WAYPOINT;
import static gov.takx.api.shapes.IRoute.PREFIX;
import static gov.takx.api.shapes.IRoute.ROUTE_ORDER;
import static gov.takx.api.shapes.IRoute.WAYPOINT_ADDED;
import static gov.takx.api.shapes.IRoute.WAYPOINT_NAME;
import static gov.takx.api.shapes.IRoute.WAYPOINT_REMOVED;
import static gov.takx.api.shapes.ShapeProperties.EDITABLE;
import static gov.takx.api.shapes.ShapeProperties.LINE_COLOR;
import static gov.takx.api.shapes.ShapeProperties.LOCATION;
import static gov.takx.api.shapes.ShapeProperties.LOCATIONS;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * A panel displaying the details for a particular {@link IRoute} and giving controls to edit it.
 *
 * @since 3.3
 */
@PseudoBean
public class RouteDetailsPanel extends ATitledPanel implements PropertyChangeListener, IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected static final String PANEL_TITLE = "Route Details";

    private static final ImageIcon ROUTE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "route.png");
    private static final ImageIcon ELEVATION_PROFILE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "terrain.png");
    private static final ImageIcon SEND_BUTTON_ICON = UIResources.getStandardSizedIcon(TAK_ASSETS_PNG, "Chat_32x32.png");

    private static final ImageIcon LOCK_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "lock.png");
    private static final ImageIcon UNLOCK_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "lock_open.png");

    private static final RowSpec DEFAULT_ROW_SPEC = RowSpec.decode("default");

    private static final List<String> PROPERTIES_TO_WATCH = List.of(LOCATION, WAYPOINT_ADDED, WAYPOINT_REMOVED,
            LOCATIONS, DISPLAY_NAME, WAYPOINT_NAME, PREFIX, ROUTE_ORDER,
            DIRECTION, NAME, LINE_COLOR, EDITABLE, DIRECTION);

    @Inject
    private RoutesManager routesManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    ILocationProviderManager locationProviderManager;

    @Inject
    private ElevationProfileService elevationProfileService;

    private final CellConstraints cc = new CellConstraints();
    private final Color backgroundColor = UIManager.getLookAndFeelDefaults().getColor("Panel.background");
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, uf -> loadWaypoints());

    private IRoute routeModel;
    private boolean updatingForm;
    private boolean ignoreEvent;

    private final List<RouteDetailsWaypointPanel> detailsWaypointPanels = new ArrayList<>();
    private final RouteDrawingInProgressPanel drawingInProgressPanel;
    private JPanel currentBottomPanel;

    /**
     * A panel showing details for a {@link IRoute}, such as the waypoints and length
     */
    public RouteDetailsPanel()
    {
        super(PANEL_TITLE);

        initComponents();

        exportButton.setIcon(UIResources.EXPORT_ICON);
        exportButton.setPopupMenu(createExportPopupMenu());

        elevationProfileButton.setIcon(ELEVATION_PROFILE_ICON);
        adjustRouteSettingsButton.setIcon(UIResources.SETTINGS_ICON);
        sendRouteButton.setIcon(SEND_BUTTON_ICON);
        editRouteButton.setIcon(UIResources.EDIT_ICON);
        routesButton.setIcon(UIResources.ROUTE_ICON);

        GUIUtils.addSingleActionDocumentListener(remarksTextField, e -> updateProperty(() -> routeModel.getShapeMapObject().setRemarks(remarksTextField.getText())));
        GUIUtils.addSingleActionDocumentListener(routeNameTextField, e -> updateProperty(() -> routeModel.setName(routeNameTextField.getText())));

        colorChooser.setPalettes(ColorChooserUtils.getDefaultPalettes());
        colorChooser.addPropertyChangeListener("color", pce -> updateProperty(() -> routeModel.setLineColor(colorChooser.getColor())));

        propertiesPanelHolder.setVisible(false);

        drawingInProgressPanel = new RouteDrawingInProgressPanel(finish -> routesManager.finishRouteEditing(), cancel -> routesManager.cancelRouteEditing());

        currentBottomPanel = sendEditPanel;

        lockButton.setIcon(UNLOCK_ICON);
        lockButton.setSelectedIcon(LOCK_ICON);
    }

    private JPopupMenu createExportPopupMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new SimpleAction("GPX", e -> routesManager.exportRoute(routeModel, RouteExportService.FileType.GPX)));
        menu.add(new SimpleAction("KML", e -> routesManager.exportRoute(routeModel, RouteExportService.FileType.KML)));
        menu.add(new SimpleAction("KMZ", e -> routesManager.exportRoute(routeModel, RouteExportService.FileType.KMZ)));
        return menu;
    }

    /**
     * @param route the route to display in this panel
     * @since 5.7
     */
    public void setRoute(@Nonnull IRoute route)
    {
        if (routeModel != route)
        {
            if (routeModel != null)
            {
                routeModel.removePropertyChangeListener(PROPERTIES_TO_WATCH, this);
            }
            routeModel = route;
            routePropertiesPanel.setRoute(route);
            route.addPropertyChangeListener(PROPERTIES_TO_WATCH, this);
            loadModel();
            loadWaypoints();
            validate();
        }
    }

    /**
     * @return The {@link IRoute} currently being displayed in this panel, or {@code null} if there is none.
     * @since 3.3 (renamed from getRouteModel in 5.7)
     */
    @Nonnull
    public IRoute getRoute()
    {
        return routeModel;
    }

    /**
     * Loads properties from our {@link IRoute} into the UI.
     */
    @InvokeOnEDT
    private void loadModel()
    {
        updatingForm = true;

        routeNameTextField.setText(routeModel.getName());
        remarksTextField.setText(routeModel.getShapeMapObject().getRemarks());
        colorChooser.setColor(routeModel.getLineColor());
        startNavigationButton.setEnabled(locationProviderManager.getCurrentLocation() != null);
        routePropertiesButton.setText(routeModel.getRouteType().getDisplayText() + ", " + routeModel.getDirection().getDisplayText() + ", " + routeModel.getMethod().getDisplayText());

        updatingForm = false;

        if (routeModel.isEditable())
        {
            editRouteButton.setEnabled(true);
            editRouteButton.setToolTipText("Edit");
            lockButton.setToolTipText("Disable Editing");
        } else
        {
            editRouteButton.setEnabled(false);
            editRouteButton.setToolTipText("Route is not currently editable.");
            lockButton.setToolTipText("Enable Editing");
        }
    }

    /**
     * Creates all of the {@link RouteDetailsWaypointPanel} for the waypoints of this route.
     */
    @InvokeOnEDT
    private void loadWaypoints()
    {
        clearWaypoints();

        final FormLayout layout = (FormLayout) waypointPanel.getLayout();
        final List<GeoPointMetaData> routeControlPoints = routeModel.getControlPoints();
        totalDistanceLabel.setText(getDistanceString(routeControlPoints));

        final int lastPointIndex = routeControlPoints.size() - 1;
        int lastWaypoint = 0;
        int waypointCount = 0;
        final LayoutManager layoutManager = waypointTitlePanel.getLayout();

        for (int i = 0; i < routeControlPoints.size(); i++)
        {
            final GeoPointMetaData waypoint = routeControlPoints.get(i);
            final AttributeSet attributeSet = waypoint.getMetaData();

            // We only update on waypoints
            if (!attributeSet.getBooleanAttribute(IS_A_WAYPOINT, false)) continue;

            layout.appendRow(DEFAULT_ROW_SPEC);

            final boolean showDelete = i != 0 && i != lastPointIndex;
            RouteDetailsWaypointPanel detailsWaypointPanel = new RouteDetailsWaypointPanel(waypoint, layoutManager, showDelete,
                    this::deleteWaypoint,
                    location -> globeManager.getLastFocusedGlobe().centerOn(location));

            int componentRow = 2 + waypointCount; // Our title is in row 1, so waypoints start at 2

            waypointPanel.add(detailsWaypointPanel, cc.xywh(1, componentRow, layout.getColumnCount(), 1));

            detailsWaypointPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    detailsWaypointPanels.stream()
                            .filter(panel -> panel != detailsWaypointPanel)
                            .forEach(panel -> panel.setBackground(backgroundColor));
                }
            });
            detailsWaypointPanels.add(detailsWaypointPanel);

            detailsWaypointPanel.setDistanceValue(getDistanceString(routeControlPoints.subList(lastWaypoint, i + 1)));

            lastWaypoint = i;
            waypointCount++;
        }
    }

    /**
     * Gets a string representation of the length for a list of points.
     *
     * @param controlPoints The {@link GeoPointMetaData} points to calculate the length of
     * @return A string, rounded to one decimal place, with unit of measure attached
     */
    private String getDistanceString(@Nonnull List<GeoPointMetaData> controlPoints)
    {
        // For the first point in the route, no distance to display
        if (controlPoints.size() <= 1) return "--";

        //noinspection ConstantConditions
        final List<Position> geoPoints = controlPoints.stream()
                .map(geoPointMetaData -> WWUtils.locationToPosition(geoPointMetaData.get()))
                .toList();

        final LengthMeasurer measurer = new LengthMeasurer();
        measurer.setFollowTerrain(true);
        measurer.setPathType(AVKey.GREAT_CIRCLE);
        measurer.setPositions(new ArrayList<>(geoPoints));
        double routeLengthMeters = measurer.getLength(globeManager.getLastFocusedGlobe().getWorldWindow().getModel().getGlobe());

        // Rounding display to one decimal place
        return rangeUnitsMonitor.getCurrentValue().format(routeLengthMeters, 1);
    }

    /**
     * Removes all the waypoints from this panel, but re-adds the title panel while clearing the total distance.
     */
    @InvokeOnEDT
    private void clearWaypoints()
    {
        waypointPanel.removeAll();

        final FormLayout layout = new FormLayout(
                "center:default, $lcgap, center:default:grow, 3*($lcgap, default)",
                "default");
        waypointPanel.setLayout(layout);

        waypointPanel.add(waypointTitlePanel, cc.xywh(1, 1, layout.getColumnCount(), 1));
        totalDistanceLabel.setText("");
        detailsWaypointPanels.clear();
    }

    /**
     * Deletes a waypoint for the given Uid by demoting it.  This will not remove the control point in the route.
     *
     * @param waypointUid the UID of the waypoint to remove.
     */
    private void deleteWaypoint(@Nonnull String waypointUid)
    {
        List<GeoPointMetaData> waypoints = routeModel.getControlPoints();

        for (int i = 0, size = waypoints.size(); i < size; i++)
        {
            if (waypoints.get(i).getMetaData().getStringAttribute(StandardAttributeNames.UID).equals(waypointUid))
            {
                routeModel.demoteToControlPoint(i);
                break;
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (ignoreEvent) return;

        switch (evt.getPropertyName())
        {
            case LOCATION, WAYPOINT_ADDED, WAYPOINT_REMOVED, LOCATIONS, DISPLAY_NAME, WAYPOINT_NAME, PREFIX, ROUTE_ORDER -> loadWaypoints();
            case NAME, LINE_COLOR, EDITABLE -> loadModel();
            default -> logger.warn("Unexpected PCE: {}", evt);
        }
    }

    private void routePropertiesPanelFinished()
    {
        var properties = routePropertiesPanel.getRoute();
        propertiesPanelHolder.setVisible(false);
        if (properties != null)
        {
            ignoreEvent = true;
            routeModel.setDirection(properties.getDirection());
            routeModel.setMethod(properties.getMethod());
            routeModel.setRouteType(properties.getRouteType());
            routeModel.setRouteOrder(properties.getRouteOrder());
            ignoreEvent = false;

            loadModel();
            loadWaypoints();
        }
    }

    /**
     * If we are not {@code updatingPanel}, then process a property update.
     *
     * @param updater Update action to perform
     * @since 3.4
     */
    private void updateProperty(Runnable updater)
    {
        if (!updatingForm)
        {
            ignoreEvent = true;
            updater.run();
            globeManager.repaintAllGlobes();
            ignoreEvent = false;
        }
    }

    /**
     * Call when route editing or creation is finished, to re-enable UI elements
     */
    @InvokeOnEDT
    protected void finishEditing()
    {
        setBottomPanel(sendEditPanel);
        editRouteButton.setEnabled(true);

        loadModel();
        loadWaypoints();
    }

    @InvokeOnEDT
    private void editRouteButtonPressed()
    {
        routesManager.startRouteEditing(routeModel);
    }

    /**
     * Sets the panel to editing mode.
     */
    @InvokeOnEDT
    public void startRouteEditing()
    {
        setBottomPanel(drawingInProgressPanel);
    }

    @InvokeOnEDT
    private void setBottomPanel(JPanel panel)
    {
        remove(currentBottomPanel);
        currentBottomPanel = panel;

        final FormLayout layout = (FormLayout) getLayout();
        add(panel, cc.xywh(1, layout.getRowCount(), layout.getColumnCount(), 1));
        revalidate();
        repaint();
    }

    private void sendRouteButtonPressed()
    {
        routesManager.publishRouteAsCot(routeModel);
    }

    /**
     * Disable the edit route button while other operations prevent editing.
     */
    @InvokeOnEDT
    protected void disableEditing()
    {
        editRouteButton.setEnabled(false);
    }

    private void routePropertiesButtonPressed()
    {
        propertiesPanelHolder.setVisible(true); // Show the route properties panel
    }

    private void routesButtonPressed()
    {
        routesManager.openRoutesPanel();
    }

    private void elevationProfileButtonPressed()
    {
        elevationProfileService.showProfile(routeModel.getControlPoints().stream()
                .map(GeoPointMetaData::get).toList(), null, null);
    }

    private void adjustRouteSettingsButtonPressed()
    {
        // TODO add your code here
    }

    private void startNavigationButtonPressed()
    {
        routesManager.navigateRoute(routeModel);
    }

    private void lockButtonActionPressed()
    {
        // Toggle editable state
        routeModel.setEditable(!routeModel.isEditable());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var buttonPanel = new JPanel();
        routesButton = new JButton();
        exportButton = new SplitButton();
        elevationProfileButton = new JButton();
        adjustRouteSettingsButton = new JButton();
        var routeNameLabel = new JLabel();
        routeNameTextField = new JTextField();
        startNavigationButton = new JButton();
        var colorLabel = new JLabel();
        var optionsPanel = new JPanel();
        colorChooser = new ColorChooser();
        var optionsLabel = new JLabel();
        routePropertiesButton = new JButton();
        var remarksLabel = new JLabel();
        remarksTextField = new JTextField();
        propertiesPanelHolder = new JPanel();
        routePropertiesPanel = new RoutePropertiesPanel();
        var waypointScrollPane = new JScrollPane();
        waypointPanel = new JPanel();
        waypointTitlePanel = new JPanel();
        var totalTitleLabel = new JLabel();
        totalDistanceLabel = new JLabel();
        sendEditPanel = new JPanel();
        sendRouteButton = new JButton();
        editRouteButton = new JButton();
        lockButton = new JToggleButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setName("this");
        setLayout(new FormLayout(
                "right:default, $lcgap, default:grow, $lcgap, default",
                "4*(default, $lgap), fill:default, default, fill:default:grow, bottom:default"));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 5*($lcgap, default)",
                    "default"));

            //---- routesButton ----
            routesButton.setText("Routes");
            routesButton.setName("routesButton");
            routesButton.addActionListener(e -> routesButtonPressed());
            buttonPanel.add(routesButton, cc.xy(3, 1));

            //---- exportButton ----
            exportButton.setToolTipText("Export");
            exportButton.setName("exportButton");
            buttonPanel.add(exportButton, cc.xy(5, 1));

            //---- elevationProfileButton ----
            elevationProfileButton.setToolTipText("Elevation Profile");
            elevationProfileButton.setName("elevationProfileButton");
            elevationProfileButton.addActionListener(e -> elevationProfileButtonPressed());
            buttonPanel.add(elevationProfileButton, cc.xy(7, 1));

            //---- adjustRouteSettingsButton ----
            adjustRouteSettingsButton.setEnabled(false);
            adjustRouteSettingsButton.setToolTipText("Adjust Route Settings");
            adjustRouteSettingsButton.setName("adjustRouteSettingsButton");
            adjustRouteSettingsButton.addActionListener(e -> adjustRouteSettingsButtonPressed());
            buttonPanel.add(adjustRouteSettingsButton, cc.xy(9, 1));
        }
        add(buttonPanel, cc.xywh(1, 1, 5, 1));

        //---- routeNameLabel ----
        routeNameLabel.setText("Name");
        routeNameLabel.setName("routeNameLabel");
        add(routeNameLabel, cc.xy(1, 3));

        //---- routeNameTextField ----
        routeNameTextField.setName("routeNameTextField");
        add(routeNameTextField, cc.xy(3, 3));

        //---- startNavigationButton ----
        startNavigationButton.setText("GO");
        startNavigationButton.setToolTipText("Navigate Route");
        startNavigationButton.setName("startNavigationButton");
        startNavigationButton.addActionListener(e -> startNavigationButtonPressed());
        add(startNavigationButton, cc.xy(5, 3));

        //---- colorLabel ----
        colorLabel.setText("Color:");
        colorLabel.setName("colorLabel");
        add(colorLabel, cc.xy(1, 5));

        //======== optionsPanel ========
        {
            optionsPanel.setName("optionsPanel");
            optionsPanel.setLayout(new FormLayout(
                    "2*(default, $lcgap), default:grow",
                    "default"));

            //---- colorChooser ----
            colorChooser.setFont(new Font("Tahoma", Font.BOLD, 14));
            colorChooser.setName("colorChooser");
            optionsPanel.add(colorChooser, cc.xy(1, 1));

            //---- optionsLabel ----
            optionsLabel.setText("Options");
            optionsLabel.setName("optionsLabel");
            optionsPanel.add(optionsLabel, cc.xy(3, 1));

            //---- routePropertiesButton ----
            routePropertiesButton.setName("routePropertiesButton");
            routePropertiesButton.addActionListener(e -> routePropertiesButtonPressed());
            optionsPanel.add(routePropertiesButton, cc.xy(5, 1));
        }
        add(optionsPanel, cc.xywh(3, 5, 3, 1));

        //---- remarksLabel ----
        remarksLabel.setText("Remarks");
        remarksLabel.setName("remarksLabel");
        add(remarksLabel, cc.xy(1, 7));

        //---- remarksTextField ----
        remarksTextField.setName("remarksTextField");
        add(remarksTextField, cc.xywh(3, 7, 3, 1));

        //======== propertiesPanelHolder ========
        {
            propertiesPanelHolder.setName("propertiesPanelHolder");
            propertiesPanelHolder.setLayout(new FormLayout(
                    "pref:grow",
                    "fill:pref:grow, $lgap"));

            //---- routePropertiesPanel ----
            routePropertiesPanel.setName("routePropertiesPanel");
            routePropertiesPanel.addFinishListener(e -> routePropertiesPanelFinished());
            propertiesPanelHolder.add(routePropertiesPanel, cc.xy(1, 1));
        }
        add(propertiesPanelHolder, cc.xywh(1, 9, 5, 1));

        //======== waypointScrollPane ========
        {
            waypointScrollPane.setName("waypointScrollPane");

            //======== waypointPanel ========
            {
                waypointPanel.setName("waypointPanel");
                waypointPanel.setLayout(new FormLayout(
                        "default, $lcgap, center:default:grow, 3*($lcgap, default)",
                        "default"));

                //======== waypointTitlePanel ========
                {
                    waypointTitlePanel.setName("waypointTitlePanel");
                    waypointTitlePanel.setLayout(new FormLayout(
                            "[default,150px], $lcgap, center:default:grow, 3*($lcgap, default)",
                            "default"));

                    //---- totalTitleLabel ----
                    totalTitleLabel.setText("Total");
                    totalTitleLabel.setName("totalTitleLabel");
                    waypointTitlePanel.add(totalTitleLabel, cc.xy(1, 1));

                    //---- totalDistanceLabel ----
                    totalDistanceLabel.setName("totalDistanceLabel");
                    waypointTitlePanel.add(totalDistanceLabel, cc.xy(3, 1));
                }
                waypointPanel.add(waypointTitlePanel, cc.xywh(1, 1, 9, 1));
            }
            waypointScrollPane.setViewportView(waypointPanel);
        }
        add(waypointScrollPane, cc.xywh(1, 10, 5, 1));

        //======== sendEditPanel ========
        {
            sendEditPanel.setName("sendEditPanel");
            sendEditPanel.setLayout(new FormLayout(
                    "2*(default:grow, $lcgap), default",
                    "bottom:default"));

            //---- sendRouteButton ----
            sendRouteButton.setText("SEND");
            sendRouteButton.setName("sendRouteButton");
            sendRouteButton.addActionListener(e -> sendRouteButtonPressed());
            sendEditPanel.add(sendRouteButton, cc.xy(1, 1));

            //---- editRouteButton ----
            editRouteButton.setText("EDIT");
            editRouteButton.setName("editRouteButton");
            editRouteButton.addActionListener(e -> editRouteButtonPressed());
            sendEditPanel.add(editRouteButton, cc.xy(3, 1));

            //---- lockButton ----
            lockButton.setName("lockButton");
            lockButton.addActionListener(e -> lockButtonActionPressed());
            sendEditPanel.add(lockButton, cc.xy(5, 1));
        }
        add(sendEditPanel, cc.xywh(1, 12, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton routesButton;
    private SplitButton exportButton;
    private JButton elevationProfileButton;
    private JButton adjustRouteSettingsButton;
    private JTextField routeNameTextField;
    private JButton startNavigationButton;
    private ColorChooser colorChooser;
    private JButton routePropertiesButton;
    private JTextField remarksTextField;
    private JPanel propertiesPanelHolder;
    private RoutePropertiesPanel routePropertiesPanel;
    private JPanel waypointPanel;
    private JPanel waypointTitlePanel;
    private JLabel totalDistanceLabel;
    private JPanel sendEditPanel;
    private JButton sendRouteButton;
    private JButton editRouteButton;
    private JToggleButton lockButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
